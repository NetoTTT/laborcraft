package com.foque.laborcraft.client.ui.widget;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.network.OpenStationPayload;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Interactive top-down map for selecting a rectangular work zone.
 * Uses DynamicTexture with NativeImage for 144+ FPS performance (single blit call),
 * topographic relief shading (hillshading), and a subtle chunk grid (JourneyMap style).
 */
public final class LcAreaMap extends AbstractWidget implements AutoCloseable {
	/**
	 * Which vertical slice the map renders. AUTO follows the player: surface while under open sky,
	 * cave floor-scan once the player is covered.
	 */
	public enum Layer {
		AUTO,
		SURFACE,
		UNDERGROUND;

		public Layer next() {
			Layer[] all = values();
			return all[(ordinal() + 1) % all.length];
		}
	}

	private static final int BG_DARK = 0xFF141A22;
	private static final int GRID_COLOR = 0xFF222B36;
	private static final int SELECTION_FILL = 0x4548D7A0;
	private static final int SELECTION_BORDER = 0xFF48D7A0;
	private static final int SAVED_FILL = 0x28FFC65A;
	private static final int SAVED_BORDER = 0xFFFFC65A;
	private static final int OVERLAP_FILL = 0x77FF5555;
	private static final int OVERLAP_BORDER = 0xFFFF5555;
	private static final int NO_FLOOR = Integer.MIN_VALUE;

	private final BlockPos center;
	private final int maxRadius;
	private final BlockPos savedFrom;
	private final BlockPos savedTo;
	private final List<OpenStationPayload.ZoneView> savedZones;
	private final UUID boundWorkerId;
	private final String boundWorkerName;
	private final List<OpenStationPayload.AreaView> ownAreas;
	private final List<OpenStationPayload.AreaView> otherAreas;
	private final boolean readOnly;

	private final Identifier textureId;
	private DynamicTexture texture;
	private boolean dirty = true;
	private long lastUpdateTick;

	private Layer layer = Layer.AUTO;
	private boolean lastUnderground;
	private int pivotY;
	private Component layerLabel = Component.empty();

	private BlockPos from;
	private BlockPos to;
	private boolean dragging;
	private int pixelsPerBlock = 2;

	public LcAreaMap(
			int x, int y, int width, int height,
			BlockPos center, int maxRadius,
			BlockPos from, BlockPos to,
			BlockPos savedFrom, BlockPos savedTo,
			List<OpenStationPayload.ZoneView> savedZones,
			UUID boundWorkerId, String boundWorkerName,
			List<OpenStationPayload.AreaView> ownAreas,
			List<OpenStationPayload.AreaView> otherAreas,
			boolean readOnly
	) {
		super(x, y, width, height, Component.translatable("laborcraft.ui.station.map"));
		this.center = center;
		this.maxRadius = Math.max(1, Math.min(128, maxRadius));
		this.from = from;
		this.to = to;
		this.savedFrom = savedFrom;
		this.savedTo = savedTo;
		this.savedZones = savedZones == null ? List.of() : List.copyOf(savedZones);
		this.boundWorkerId = boundWorkerId;
		this.boundWorkerName = boundWorkerName;
		this.ownAreas = ownAreas == null ? List.of() : List.copyOf(ownAreas);
		this.otherAreas = otherAreas == null ? List.of() : List.copyOf(otherAreas);
		this.readOnly = readOnly;
		this.textureId = Identifier.fromNamespaceAndPath("laborcraft", "dynamic/area_map_" + System.identityHashCode(this));

		int span = this.maxRadius * 2 + 1;
		this.pixelsPerBlock = Mth.clamp(Math.min(width / span, height / span), 1, 4);
		this.lastUnderground = undergroundActive();
		this.layerLabel = buildLayerLabel(this.lastUnderground, effectivePivotY());
	}

	public BlockPos from() {
		return from;
	}

	public BlockPos to() {
		return to;
	}

	public void setLayer(Layer layer) {
		if (layer == null || this.layer == layer) {
			return;
		}
		this.layer = layer;
		this.dirty = true;
	}

	public Layer layer() {
		return layer;
	}

	public Component layerLabel() {
		return layerLabel;
	}

	public boolean hasSelection() {
		return from != null && to != null;
	}

	public int selectionWidth() {
		return hasSelection() ? Math.abs(to.getX() - from.getX()) + 1 : 0;
	}

	public int selectionDepth() {
		return hasSelection() ? Math.abs(to.getZ() - from.getZ()) + 1 : 0;
	}

	public boolean selectionFitsLimit() {
		return hasSelection() && selectionWidth() <= 32 && selectionDepth() <= 32;
	}

	/** True when the raw maximum square would overlap another post's drawn zone. */
	public boolean maxAreaOverlapsOther() {
		int minX = center.getX() - maxRadius;
		int maxX = center.getX() + maxRadius;
		int minZ = center.getZ() - maxRadius;
		int maxZ = center.getZ() + maxRadius;
		for (OpenStationPayload.AreaView area : otherAreas) {
			if (area == null || !area.blocksDraft()) {
				continue;
			}

			int areaMinX = Math.min(area.from().getX(), area.to().getX());
			int areaMaxX = Math.max(area.from().getX(), area.to().getX());
			int areaMinZ = Math.min(area.from().getZ(), area.to().getZ());
			int areaMaxZ = Math.max(area.from().getZ(), area.to().getZ());
			if (areaMinX <= maxX && areaMaxX >= minX && areaMinZ <= maxZ && areaMaxZ >= minZ) {
				return true;
			}
		}
		return false;
	}

	/** True when the current selection overlaps another post's drawn zone. */
	public boolean selectionOverlapsOther() {
		if (!hasSelection()) {
			return false;
		}

		int selMinX = Math.min(from.getX(), to.getX());
		int selMaxX = Math.max(from.getX(), to.getX());
		int selMinZ = Math.min(from.getZ(), to.getZ());
		int selMaxZ = Math.max(from.getZ(), to.getZ());
		for (OpenStationPayload.AreaView area : otherAreas) {
			if (area == null || !area.blocksDraft()) {
				continue;
			}

			int minX = Math.min(area.from().getX(), area.to().getX());
			int maxX = Math.max(area.from().getX(), area.to().getX());
			int minZ = Math.min(area.from().getZ(), area.to().getZ());
			int maxZ = Math.max(area.from().getZ(), area.to().getZ());
			if (minX <= selMaxX && maxX >= selMinX && minZ <= selMaxZ && maxZ >= selMinZ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() {
		if (texture != null) {
			try {
				Minecraft.getInstance().getTextureManager().release(textureId);
			} catch (Exception ignored) {
			}
			texture = null;
		}
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		long currentTick = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
		boolean underground = undergroundActive();
		int pivot = effectivePivotY();
		long refreshTicks = underground ? 200L : 100L;
		boolean pivotMoved = underground && Math.abs(pivot - pivotY) >= 2;

		if (dirty || texture == null || underground != lastUnderground || pivotMoved
				|| currentTick - lastUpdateTick >= refreshTicks) {
			pivotY = pivot;
			lastUnderground = underground;
			updateMapTexture(underground);
			lastUpdateTick = currentTick;
		}

		updateLayerLabel(underground, pivot);

		graphics.fill(getX(), getY(), getX() + width, getY() + height, BG_DARK);
		graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);

		// 1. Single Blit of the entire generated terrain image (0.001ms!)
		graphics.blit(textureId, getX(), getY(), getX() + width, getY() + height, 0.0f, 1.0f, 0.0f, 1.0f);

		// 2. Other nearby posts (blue)
		drawOtherAreas(graphics);

		// In edit mode, hide this post's amber area and radius limit so the selection is clear.
		if (readOnly) {
			// 3. Post limit (maximum area, not the work area)
			drawRadiusLimit(graphics);

			// 4/5. Only the drawn zones are the work area. Draw the union boundary once;
			// per-rectangle dashes would create visible seams inside the clipped shape.
			if (!ownAreas.isEmpty()) {
				drawEffectiveAreas(graphics, ownAreas, SAVED_BORDER, SAVED_FILL);
			} else if (savedFrom != null && savedTo != null) {
				drawDashedZone(graphics, savedFrom, savedTo, 0x88FFC65A);
			}
		}

		// 6. Current drag selection. Only the cells that overlap another zone are painted red;
		// the server clips them out and keeps the rest.
		if (hasSelection()) {
			boolean fits = selectionFitsLimit();
			int outline = fits ? SELECTION_BORDER : OVERLAP_BORDER;
			int fill = fits ? SELECTION_FILL : 0x45FF5555;
			drawZone(graphics, from, to, outline, fill, false);
			drawSelectionOverlap(graphics);
		}

		// 7. Bound worker entity marker (JourneyMap style)
		drawWorkerMarker(graphics);

		// 8. Workstation center marker (waypoint target pin)
		drawStationMarker(graphics);

		// 9. HUD Badges (Zoom and Dimensions)
		drawHud(graphics);

		graphics.disableScissor();

		// Modern dark border
		LcRender.border(graphics, getX(), getY(), width, height, 0xFF354250);
	}

	private void updateMapTexture(boolean underground) {
		if (texture == null) {
			texture = new DynamicTexture("laborcraft_area_map", width, height, false);
			Minecraft.getInstance().getTextureManager().register(textureId, texture);
		}

		NativeImage img = texture.getPixels();
		if (img == null) {
			return;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			img.fillRect(0, 0, width, height, BG_DARK);
			texture.upload();
			dirty = false;
			return;
		}

		int halfW = width / 2;
		int halfH = height / 2;

		BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos northPos = new BlockPos.MutableBlockPos();

		int firstDx = (int) Math.floor((-halfW) / (double) pixelsPerBlock) - 1;
		int lastDx = (int) Math.ceil((width - halfW) / (double) pixelsPerBlock) + 1;
		int firstDz = (int) Math.floor((-halfH) / (double) pixelsPerBlock) - 1;
		int lastDz = (int) Math.ceil((height - halfH) / (double) pixelsPerBlock) + 1;

		for (int dx = firstDx; dx <= lastDx; dx++) {
			int worldX = center.getX() + dx;
			int imgX = halfW + dx * pixelsPerBlock;
			if (imgX + pixelsPerBlock <= 0 || imgX >= width) {
				continue;
			}

			for (int dz = firstDz; dz <= lastDz; dz++) {
				int worldZ = center.getZ() + dz;
				int imgY = halfH + dz * pixelsPerBlock;
				if (imgY + pixelsPerBlock <= 0 || imgY >= height) {
					continue;
				}

				int argb;
				boolean chunkLoaded = level.hasChunkAt(worldX, worldZ);
				boolean isChunkGrid = Math.floorMod(worldX, 16) == 0 || Math.floorMod(worldZ, 16) == 0;

				if (!chunkLoaded) {
					argb = isChunkGrid ? GRID_COLOR : BG_DARK;
				} else if (underground) {
					argb = undergroundColumnColor(level, worldX, worldZ, tempPos);
					if (isChunkGrid && (argb >>> 24) != 0) {
						argb = applyChunkGridTint(argb);
					}
				} else {
					int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, tempPos.set(worldX, center.getY(), worldZ)).getY() - 1;
					BlockState state = level.getBlockState(tempPos.set(worldX, surfaceY, worldZ));
					MapColor mapColor = state.getMapColor(level, tempPos);

					if (mapColor == MapColor.NONE) {
						argb = 0xFF18202A;
					} else {
						// Topographic hillshading: compare surface height with north neighbor
						int northY = surfaceY;
						if (level.hasChunkAt(worldX, worldZ - 1)) {
							northY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, northPos.set(worldX, center.getY(), worldZ - 1)).getY() - 1;
						}

						int dy = surfaceY - northY;
						MapColor.Brightness brightness = dy > 0
								? MapColor.Brightness.HIGH
								: (dy < 0 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL);

						argb = mapColor.calculateARGBColor(brightness);
					}

					if (isChunkGrid) {
						argb = applyChunkGridTint(argb);
					}
				}

				int drawX = Math.max(0, imgX);
				int drawY = Math.max(0, imgY);
				int drawW = Math.min(width - drawX, pixelsPerBlock - (drawX - imgX));
				int drawH = Math.min(height - drawY, pixelsPerBlock - (drawY - imgY));

				if (drawW > 0 && drawH > 0) {
					if (drawW == 1 && drawH == 1) {
						img.setPixel(drawX, drawY, argb);
					} else {
						img.fillRect(drawX, drawY, drawW, drawH, argb);
					}
				}
			}
		}

		texture.upload();
		dirty = false;
	}

	private static int applyChunkGridTint(int argb) {
		int a = (argb >> 24) & 0xFF;
		int r = Math.min(255, ((argb >> 16) & 0xFF) + 38);
		int g = Math.min(255, ((argb >> 8) & 0xFF) + 38);
		int b = Math.min(255, (argb & 0xFF) + 38);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Cave layer: finds the nearest floor around the pivot Y and colors the block below it.
	 * Columns with no floor are left transparent so the map background shows through.
	 */
	private int undergroundColumnColor(ClientLevel level, int x, int z, BlockPos.MutableBlockPos pos) {
		int floorY = findFloor(level, x, z, pivotY, pos);
		if (floorY == NO_FLOOR) {
			return 0x00000000;
		}

		pos.set(x, floorY - 1, z);
		BlockState floor = level.getBlockState(pos);
		MapColor mapColor = floor.getMapColor(level, pos);
		if (mapColor == MapColor.NONE) {
			return 0x00000000;
		}

		int depth = pivotY - (floorY - 1);
		MapColor.Brightness brightness = depth <= 3
				? MapColor.Brightness.NORMAL
				: (depth <= 8 ? MapColor.Brightness.LOW : MapColor.Brightness.LOWEST);
		return mapColor.calculateARGBColor(brightness);
	}

	/**
	 * Returns the Y of the first open cell above a floor, scanning from the pivot: downward when
	 * the pivot is open, upward (max 10) when it is inside a solid block. Mirrors the cave-mode
	 * floor scan used by open-source map mods.
	 */
	private static int findFloor(ClientLevel level, int x, int z, int pivot, BlockPos.MutableBlockPos pos) {
		BlockState state = level.getBlockState(pos.set(x, pivot, z));

		if (!occludes(state)) {
			for (int y = pivot - 1; y >= level.getMinY(); y--) {
				state = level.getBlockState(pos.set(x, y, z));
				if (occludes(state)) {
					return y + 1;
				}
			}
			return NO_FLOOR;
		}

		int top = Math.min(pivot + 10, level.getMaxY());
		for (int y = pivot + 1; y <= top; y++) {
			state = level.getBlockState(pos.set(x, y, z));
			if (!occludes(state)) {
				return y;
			}
		}
		return NO_FLOOR;
	}

	private static boolean occludes(BlockState state) {
		return state.canOcclude() || state.is(Blocks.LAVA);
	}

	private boolean undergroundActive() {
		return switch (layer) {
			case SURFACE -> false;
			case UNDERGROUND -> true;
			case AUTO -> playerUnderground();
		};
	}

	private int effectivePivotY() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? center.getY() : Mth.floor(client.player.getY());
	}

	private static boolean playerUnderground() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return false;
		}

		BlockPos pos = client.player.blockPosition();
		if (client.level.getBrightness(LightLayer.SKY, pos) <= 0) {
			return true;
		}

		int surfaceY = client.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos).getY();
		return pos.getY() < surfaceY - 2;
	}

	private void updateLayerLabel(boolean underground, int pivot) {
		Component next = buildLayerLabel(underground, pivot);
		if (!next.getString().equals(layerLabel.getString())) {
			layerLabel = next;
		}
	}

	private Component buildLayerLabel(boolean underground, int pivot) {
		return switch (layer) {
			case SURFACE -> Component.translatable("laborcraft.ui.station.map.layer.surface");
			case UNDERGROUND -> Component.translatable("laborcraft.ui.station.map.layer.underground", pivot);
			case AUTO -> underground
					? Component.translatable("laborcraft.ui.station.map.layer.auto_underground", pivot)
					: Component.translatable("laborcraft.ui.station.map.layer.auto_surface");
		};
	}

	private void drawOtherAreas(GuiGraphicsExtractor graphics) {
		drawEffectiveAreas(graphics, otherAreas, 0xFF4C8BF5, 0x284C8BF5);
	}

	private void drawRadiusLimit(GuiGraphicsExtractor graphics) {
		int minX = center.getX() - maxRadius;
		int maxX = center.getX() + maxRadius;
		int minZ = center.getZ() - maxRadius;
		int maxZ = center.getZ() + maxRadius;

		int x0 = blockToScreenX(minX);
		int y0 = blockToScreenZ(minZ);
		int x1 = blockToScreenX(maxX) + pixelsPerBlock;
		int y1 = blockToScreenZ(maxZ) + pixelsPerBlock;

		drawDashedRect(graphics, x0, y0, x1, y1, 0xCC5FA8FF);
	}

	private void drawEffectiveAreas(
			GuiGraphicsExtractor graphics,
			List<OpenStationPayload.AreaView> areas,
			int outline,
			int fill
	) {
		if (areas.isEmpty()) {
			return;
		}

		Set<Long> cells = new HashSet<>();
		for (OpenStationPayload.AreaView area : areas) {
			if (area == null) {
				continue;
			}
			int minX = Math.min(area.from().getX(), area.to().getX());
			int maxX = Math.max(area.from().getX(), area.to().getX());
			int minZ = Math.min(area.from().getZ(), area.to().getZ());
			int maxZ = Math.max(area.from().getZ(), area.to().getZ());
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					cells.add(cellKey(x, z));
				}
			}
		}

		for (long key : cells) {
			int x = (int) (key >> 32);
			int z = (int) key;
			int x0 = blockToScreenX(x);
			int y0 = blockToScreenZ(z);
			graphics.fill(x0, y0, x0 + pixelsPerBlock, y0 + pixelsPerBlock, fill);
		}

		for (long key : cells) {
			int x = (int) (key >> 32);
			int z = (int) key;
			int x0 = blockToScreenX(x);
			int y0 = blockToScreenZ(z);
			int x1 = x0 + pixelsPerBlock;
			int y1 = y0 + pixelsPerBlock;
			if (!cells.contains(cellKey(x, z - 1))) {
				graphics.fill(x0, y0, x1, y0 + 1, outline);
			}
			if (!cells.contains(cellKey(x, z + 1))) {
				graphics.fill(x0, y1 - 1, x1, y1, outline);
			}
			if (!cells.contains(cellKey(x - 1, z))) {
				graphics.fill(x0, y0, x0 + 1, y1, outline);
			}
			if (!cells.contains(cellKey(x + 1, z))) {
				graphics.fill(x1 - 1, y0, x1, y1, outline);
			}
		}
	}

	private void drawSelectionOverlap(GuiGraphicsExtractor graphics) {
		if (!hasSelection()) {
			return;
		}

		int selMinX = Math.min(from.getX(), to.getX());
		int selMaxX = Math.max(from.getX(), to.getX());
		int selMinZ = Math.min(from.getZ(), to.getZ());
		int selMaxZ = Math.max(from.getZ(), to.getZ());

		Set<Long> cells = new HashSet<>();
		for (OpenStationPayload.AreaView area : otherAreas) {
			if (area == null) {
				continue;
			}
			int minX = Math.max(selMinX, Math.min(area.from().getX(), area.to().getX()));
			int maxX = Math.min(selMaxX, Math.max(area.from().getX(), area.to().getX()));
			int minZ = Math.max(selMinZ, Math.min(area.from().getZ(), area.to().getZ()));
			int maxZ = Math.min(selMaxZ, Math.max(area.from().getZ(), area.to().getZ()));
			if (minX > maxX || minZ > maxZ) {
				continue;
			}

			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					cells.add(cellKey(x, z));
				}
			}
		}

		for (long key : cells) {
			int x = (int) (key >> 32);
			int z = (int) key;
			int x0 = blockToScreenX(x);
			int y0 = blockToScreenZ(z);
			graphics.fill(x0, y0, x0 + pixelsPerBlock, y0 + pixelsPerBlock, OVERLAP_FILL);
		}

		for (long key : cells) {
			int x = (int) (key >> 32);
			int z = (int) key;
			int x0 = blockToScreenX(x);
			int y0 = blockToScreenZ(z);
			int x1 = x0 + pixelsPerBlock;
			int y1 = y0 + pixelsPerBlock;
			if (!cells.contains(cellKey(x, z - 1))) {
				graphics.fill(x0, y0, x1, y0 + 1, OVERLAP_BORDER);
			}
			if (!cells.contains(cellKey(x, z + 1))) {
				graphics.fill(x0, y1 - 1, x1, y1, OVERLAP_BORDER);
			}
			if (!cells.contains(cellKey(x - 1, z))) {
				graphics.fill(x0, y0, x0 + 1, y1, OVERLAP_BORDER);
			}
			if (!cells.contains(cellKey(x + 1, z))) {
				graphics.fill(x1 - 1, y0, x1, y1, OVERLAP_BORDER);
			}
		}
	}

	private void drawDashedZone(GuiGraphicsExtractor graphics, BlockPos a, BlockPos b, int color) {
		int minX = Math.min(a.getX(), b.getX());
		int maxX = Math.max(a.getX(), b.getX());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxZ = Math.max(a.getZ(), b.getZ());
		int x0 = blockToScreenX(minX);
		int y0 = blockToScreenZ(minZ);
		int x1 = blockToScreenX(maxX) + pixelsPerBlock;
		int y1 = blockToScreenZ(maxZ) + pixelsPerBlock;
		drawDashedRect(graphics, x0, y0, x1, y1, color);
	}

	private static long cellKey(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	private void drawZone(GuiGraphicsExtractor graphics, BlockPos a, BlockPos b, int outline, int fill, boolean dashed) {
		int minX = Math.min(a.getX(), b.getX());
		int maxX = Math.max(a.getX(), b.getX());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxZ = Math.max(a.getZ(), b.getZ());

		int x0 = blockToScreenX(minX);
		int y0 = blockToScreenZ(minZ);
		int x1 = blockToScreenX(maxX) + pixelsPerBlock;
		int y1 = blockToScreenZ(maxZ) + pixelsPerBlock;
		if (x1 < getX() || x0 > getX() + width || y1 < getY() || y0 > getY() + height) {
			return;
		}

		graphics.fill(x0, y0, x1, y1, fill);
		if (dashed) {
			drawDashedRect(graphics, x0, y0, x1, y1, outline);
		} else {
			LcRender.border(graphics, x0, y0, x1 - x0, y1 - y0, outline);
		}
	}

	private void drawDashedRect(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		int dash = 4;
		for (int x = x0; x < x1; x += dash * 2) {
			int end = Math.min(x + dash, x1);
			graphics.fill(x, y0, end, y0 + 1, color);
			graphics.fill(x, y1 - 1, end, y1, color);
		}
		for (int y = y0; y < y1; y += dash * 2) {
			int end = Math.min(y + dash, y1);
			graphics.fill(x0, y, x0 + 1, end, color);
			graphics.fill(x1 - 1, y, x1, end, color);
		}
	}

	private void drawStationMarker(GuiGraphicsExtractor graphics) {
		int stX = blockToScreenX(center.getX()) + pixelsPerBlock / 2;
		int stY = blockToScreenZ(center.getZ()) + pixelsPerBlock / 2;

		// JourneyMap styled target pin
		graphics.fill(stX - 4, stY - 4, stX + 5, stY + 5, 0x99000000);
		graphics.fill(stX - 3, stY - 1, stX + 4, stY + 2, 0xFFFFC65A);
		graphics.fill(stX - 1, stY - 3, stX + 2, stY + 4, 0xFFFFC65A);
		graphics.fill(stX - 1, stY - 1, stX + 2, stY + 2, 0xFFFFFFFF);
	}

	private void drawWorkerMarker(GuiGraphicsExtractor graphics) {
		if (boundWorkerId == null || Minecraft.getInstance().level == null) {
			return;
		}

		for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
			if (entity.getUUID().equals(boundWorkerId)) {
				int wx = blockToScreenX(entity.getBlockX()) + pixelsPerBlock / 2;
				int wy = blockToScreenZ(entity.getBlockZ()) + pixelsPerBlock / 2;

				// Circular icon with border (JourneyMap mob marker)
				graphics.fill(wx - 5, wy - 5, wx + 6, wy + 6, 0xDD0B121A);
				LcRender.border(graphics, wx - 5, wy - 5, 11, 11, 0xFFFFFFFF);
				// Face
				graphics.fill(wx - 3, wy - 3, wx + 4, wy + 4, 0xFFC68A5E);
				graphics.fill(wx - 2, wy - 2, wx + 3, wy - 1, 0xFF4A3425);
				graphics.fill(wx - 1, wy - 1, wx + 2, wy + 2, 0xFFB3734A);

				// Dark name badge below icon
				if (boundWorkerName != null && !boundWorkerName.isBlank()) {
					Font font = Minecraft.getInstance().font;
					int textW = font.width(boundWorkerName);
					int badgeX = wx - textW / 2 - 3;
					int badgeY = wy + 7;
					graphics.fill(badgeX, badgeY, badgeX + textW + 6, badgeY + 11, 0xDD111820);
					LcRender.border(graphics, badgeX, badgeY, textW + 6, 11, 0x77FFFFFF);
					graphics.text(font, boundWorkerName, badgeX + 3, badgeY + 2, 0xFFFFFFFF);
				}
				break;
			}
		}
	}

	private void drawHud(GuiGraphicsExtractor graphics) {
		Font font = Minecraft.getInstance().font;

		// Zoom badge (top-right)
		String zoom = pixelsPerBlock + "x";
		int zw = font.width(zoom);
		graphics.fill(getX() + width - zw - 10, getY() + 6, getX() + width - 4, getY() + 18, 0xAA111820);
		graphics.text(font, zoom, getX() + width - zw - 7, getY() + 8, 0xFFB5C2D0);

		// Size badge (bottom-left) when selecting
		if (hasSelection()) {
			int w = selectionWidth();
			int d = selectionDepth();
			int blocks = w * d;
			boolean fits = selectionFitsLimit();
			String text = w + "x" + d + " (" + blocks + " bl)";
			int tw = font.width(text);
			graphics.fill(getX() + 6, getY() + height - 19, getX() + tw + 12, getY() + height - 5, 0xDD111820);
			graphics.text(font, text, getX() + 9, getY() + height - 16, fits ? 0xFF48D7A0 : 0xFFFF5555);
		}
	}

	private int blockToScreenX(int blockX) {
		return getX() + width / 2 + (blockX - center.getX()) * pixelsPerBlock;
	}

	private int blockToScreenZ(int blockZ) {
		return getY() + height / 2 + (blockZ - center.getZ()) * pixelsPerBlock;
	}

	private int screenToBlockX(double mouseX) {
		double relX = mouseX - (getX() + width / 2.0D);
		int delta = (int) Math.floor(relX / pixelsPerBlock);
		return Mth.clamp(center.getX() + delta, center.getX() - maxRadius, center.getX() + maxRadius);
	}

	private int screenToBlockZ(double mouseY) {
		double relY = mouseY - (getY() + height / 2.0D);
		int delta = (int) Math.floor(relY / pixelsPerBlock);
		return Mth.clamp(center.getZ() + delta, center.getZ() - maxRadius, center.getZ() + maxRadius);
	}

	private BlockPos positionAt(double mouseX, double mouseY) {
		return new BlockPos(screenToBlockX(mouseX), center.getY(), screenToBlockZ(mouseY));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (readOnly || !active || !visible) {
			return false;
		}
		if (isValidClickButton(event.buttonInfo()) && isMouseOver(event.x(), event.y())) {
			from = positionAt(event.x(), event.y());
			to = from;
			dragging = true;
			return true;
		}
		return false;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (readOnly) {
			return;
		}
		from = positionAt(event.x(), event.y());
		to = from;
		dragging = true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (dragging) {
			to = positionAt(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
		if (dragging) {
			to = positionAt(event.x(), event.y());
		}
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			to = positionAt(event.x(), event.y());
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		if (dragging) {
			to = positionAt(event.x(), event.y());
		}
		dragging = false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!active || !visible || !isMouseOver(mouseX, mouseY)) {
			return false;
		}
		int next = Mth.clamp(pixelsPerBlock + (scrollY > 0 ? 1 : -1), 1, 4);
		if (next != pixelsPerBlock) {
			pixelsPerBlock = next;
			dirty = true;
		}
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

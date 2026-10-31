package com.foque.laborcraft.client.render;

import com.quillui.theme.QuillColors;

import com.foque.laborcraft.network.OpenStationPayload;
import com.foque.laborcraft.worker.StationAreas;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;

/**
 * Manages in-world 3D holographic particle preview for candidate work zones,
 * and preserves draft selections when reopening the Station UI.
 */
public final class StationPreviewManager {
	public record Draft(
			BlockPos from,
			BlockPos to,
			int verticalRadius,
			long expiresAtTick,
			List<StationAreas.Span> spans
	) {
	}

	private static final Map<BlockPos, Draft> DRAFTS = new HashMap<>();
	private static final int PREVIEW_SECONDS = 10;
	private static BlockPos activePreviewStation;
	private static long previewEndTick;
	private static List<StationAreas.Span> previewSpans = List.of();
	private static int previewStationY;
	private static int previewVerticalRadius = 1;

	private StationPreviewManager() {
	}

	private static void setDraft(
			BlockPos station,
			BlockPos from,
			BlockPos to,
			int verticalRadius,
			List<StationAreas.Span> spans
	) {
		long endTick = Minecraft.getInstance().level != null
				? Minecraft.getInstance().level.getGameTime() + 20L * 60L
				: 0L;
		DRAFTS.put(station, new Draft(from, to, verticalRadius, endTick, List.copyOf(spans)));
	}

	public static Draft getDraft(BlockPos station) {
		Draft draft = DRAFTS.get(station);
		if (draft == null) {
			return null;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getGameTime() > draft.expiresAtTick()) {
			DRAFTS.remove(station);
			if (station.equals(activePreviewStation)) {
				activePreviewStation = null;
			}
			return null;
		}

		return draft;
	}

	public static boolean hasDraft(BlockPos station) {
		return getDraft(station) != null;
	}

	public static void startPreview(
			BlockPos station,
			BlockPos from,
			BlockPos to,
			int verticalRadius,
			List<OpenStationPayload.AreaView> nearbyAreas,
			List<BlockPos> otherPosts
	) {
		List<StationAreas.Span> spans = effectiveSpans(from, to, nearbyAreas, otherPosts);
		setDraft(station, from, to, verticalRadius, spans);
		activate(station, spans, station.getY(), verticalRadius);
	}

	/**
	 * Previews an already confirmed area (the effective, clipped spans sent by the server).
	 * Used by the Config tab when no draft selection is being edited.
	 */
	public static void startSavedPreview(
			BlockPos station,
			List<OpenStationPayload.AreaView> areas,
			int verticalRadius
	) {
		List<StationAreas.Span> spans = spansOf(areas);
		activate(station, spans, station.getY(), verticalRadius);
	}

	private static void activate(
			BlockPos station,
			List<StationAreas.Span> spans,
			int stationY,
			int verticalRadius
	) {
		activePreviewStation = station;
		previewSpans = List.copyOf(spans);
		previewStationY = stationY;
		previewVerticalRadius = Math.max(1, verticalRadius);
		previewEndTick = Minecraft.getInstance().level != null
				? Minecraft.getInstance().level.getGameTime() + 20L * PREVIEW_SECONDS
				: 0L;
	}

	private static List<StationAreas.Span> spansOf(List<OpenStationPayload.AreaView> areas) {
		if (areas == null || areas.isEmpty()) {
			return List.of();
		}

		List<StationAreas.Span> spans = new ArrayList<>(areas.size());
		for (OpenStationPayload.AreaView area : areas) {
			if (area == null) {
				continue;
			}
			int x0 = Math.min(area.from().getX(), area.to().getX());
			int x1 = Math.max(area.from().getX(), area.to().getX());
			int z0 = Math.min(area.from().getZ(), area.to().getZ());
			int z1 = Math.max(area.from().getZ(), area.to().getZ());
			for (int z = z0; z <= z1; z++) {
				spans.add(new StationAreas.Span(z, x0, x1));
			}
		}
		return List.copyOf(spans);
	}

	public static void clear(BlockPos station) {
		DRAFTS.remove(station);
		if (station != null && station.equals(activePreviewStation)) {
			activePreviewStation = null;
			previewSpans = List.of();
		}
	}

	public static void resetAll() {
		DRAFTS.clear();
		activePreviewStation = null;
		previewSpans = List.of();
	}

	public static void tick(Minecraft client) {
		if (client.level == null) {
			return;
		}

		long now = client.level.getGameTime();
		DRAFTS.entrySet().removeIf(entry -> now > entry.getValue().expiresAtTick());

		if (activePreviewStation == null) {
			return;
		}

		if (now > previewEndTick) {
			activePreviewStation = null;
			previewSpans = List.of();
			return;
		}

		if (client.player == null) {
			return;
		}

		if (client.player.blockPosition().distSqr(activePreviewStation) > 64 * 64) {
			return;
		}

		int remaining = Math.max(1, (int) Math.ceil((previewEndTick - now) / 20.0));
		client.gui.hud.setOverlayMessage(
				Component.translatable("laborcraft.ui.station.preview.countdown", remaining),
				false
		);

		// Spawn particle wireframe every 2 ticks
		if (now % 2L != 0L) {
			return;
		}

		spawnBoundaryParticles(client.level, previewSpans, previewStationY, previewVerticalRadius);
	}

	/**
	 * Computes the cells a draft would own for the particle preview. Only drawn zones block the
	 * draft; maximum areas may overlap. Post block positions are excluded.
	 */
	public static List<StationAreas.Span> effectiveSpans(
			BlockPos a,
			BlockPos b,
			List<OpenStationPayload.AreaView> nearbyAreas,
			List<BlockPos> otherPosts
	) {
		int minX = Math.min(a.getX(), b.getX());
		int maxX = Math.max(a.getX(), b.getX());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxZ = Math.max(a.getZ(), b.getZ());

		Set<Long> blocked = new HashSet<>();
		if (nearbyAreas != null) {
			for (OpenStationPayload.AreaView area : nearbyAreas) {
				if (area == null || !area.blocksDraft()) {
					continue;
				}

				int areaMinX = Math.min(area.from().getX(), area.to().getX());
				int areaMaxX = Math.max(area.from().getX(), area.to().getX());
				int areaMinZ = Math.min(area.from().getZ(), area.to().getZ());
				int areaMaxZ = Math.max(area.from().getZ(), area.to().getZ());
				for (int z = areaMinZ; z <= areaMaxZ; z++) {
					for (int x = areaMinX; x <= areaMaxX; x++) {
						blocked.add(cellKey(x, z));
					}
				}
			}
		}

		if (otherPosts != null) {
			for (BlockPos post : otherPosts) {
				if (post != null) {
					blocked.add(cellKey(post.getX(), post.getZ()));
				}
			}
		}

		List<StationAreas.Span> spans = new ArrayList<>();
		for (int z = minZ; z <= maxZ; z++) {
			int start = Integer.MIN_VALUE;
			for (int x = minX; x <= maxX; x++) {
				boolean free = !blocked.contains(cellKey(x, z));
				if (free && start == Integer.MIN_VALUE) {
					start = x;
				} else if (!free && start != Integer.MIN_VALUE) {
					spans.add(new StationAreas.Span(z, start, x - 1));
					start = Integer.MIN_VALUE;
				}
			}
			if (start != Integer.MIN_VALUE) {
				spans.add(new StationAreas.Span(z, start, maxX));
			}
		}

		return List.copyOf(spans);
	}

	private static void spawnBoundaryParticles(
			ClientLevel level,
			List<StationAreas.Span> spans,
			int stationY,
			int verticalRadius
	) {
		if (spans.isEmpty()) {
			return;
		}

		Set<Long> cells = new HashSet<>();
		for (StationAreas.Span span : spans) {
			for (int x = span.x0(); x <= span.x1(); x++) {
				cells.add(cellKey(x, span.z()));
			}
		}

		double yBottom = stationY - verticalRadius;
		double yTop = stationY + verticalRadius + 1.0;
		DustParticleOptions dust = new DustParticleOptions(QuillColors.rgb(72, 215, 160), 0.9f);

		Set<Long> corners = new HashSet<>();
		for (long key : cells) {
			int x = (int) (key >> 32);
			int z = (int) key;

			if (!cells.contains(cellKey(x, z - 1))) {
				spawnEdge(level, dust, x, z, x + 1, z, yBottom, yTop);
			}
			if (!cells.contains(cellKey(x, z + 1))) {
				spawnEdge(level, dust, x, z + 1, x + 1, z + 1, yBottom, yTop);
			}
			if (!cells.contains(cellKey(x - 1, z))) {
				spawnEdge(level, dust, x, z, x, z + 1, yBottom, yTop);
			}
			if (!cells.contains(cellKey(x + 1, z))) {
				spawnEdge(level, dust, x + 1, z, x + 1, z + 1, yBottom, yTop);
			}

			addCorner(cells, corners, x, z);
			addCorner(cells, corners, x + 1, z);
			addCorner(cells, corners, x, z + 1);
			addCorner(cells, corners, x + 1, z + 1);
		}

		for (long key : corners) {
			int x = (int) (key >> 32);
			int z = (int) key;
			for (double y = yBottom; y <= yTop; y += 1.0) {
				level.addParticle(dust, x, y, z, 0, 0, 0);
			}
		}
	}

	private static void spawnEdge(
			ClientLevel level,
			DustParticleOptions dust,
			double x0,
			double z0,
			double x1,
			double z1,
			double yBottom,
			double yTop
	) {
		for (int step = 0; step <= 1; step++) {
			double x = x0 + (x1 - x0) * step;
			double z = z0 + (z1 - z0) * step;
			level.addParticle(dust, x, yBottom, z, 0, 0, 0);
			level.addParticle(dust, x, yTop, z, 0, 0, 0);
		}
	}

	private static void addCorner(Set<Long> cells, Set<Long> corners, int x, int z) {
		boolean northWest = cells.contains(cellKey(x - 1, z - 1));
		boolean northEast = cells.contains(cellKey(x, z - 1));
		boolean southWest = cells.contains(cellKey(x - 1, z));
		boolean southEast = cells.contains(cellKey(x, z));
		int inside = (northWest ? 1 : 0) + (northEast ? 1 : 0)
				+ (southWest ? 1 : 0) + (southEast ? 1 : 0);

		boolean corner = inside == 1 || inside == 3
				|| (northWest && southEast && !northEast && !southWest)
				|| (northEast && southWest && !northWest && !southEast);
		if (corner) {
			corners.add(cellKey(x, z));
		}
	}

	private static long cellKey(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}
}

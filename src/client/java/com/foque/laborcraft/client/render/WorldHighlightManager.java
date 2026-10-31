package com.foque.laborcraft.client.render;

import com.quillui.theme.QuillColors;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Draws a temporary particle wireframe around a block or an entity. The entity variant follows the
 * entity's bounding box every tick, so it does not look like a static block outline. Reusable for
 * any "highlight this target" action (chests, workers, builder targets, miner nodes).
 */
public final class WorldHighlightManager {
	private static final int SECONDS = 10;
	private static final int EDGE_STEPS = 4;

	private static BlockPos blockTarget;
	private static UUID entityId;
	private static BlockPos entityFallback;
	private static ItemStack icon = ItemStack.EMPTY;
	private static int color = QuillColors.rgb(255, 198, 90);
	private static long endTick;

	private WorldHighlightManager() {
	}

	public static void highlight(BlockPos pos, int rgb, ItemStack iconStack) {
		if (pos == null) {
			return;
		}

		blockTarget = pos.immutable();
		entityId = null;
		entityFallback = null;
		color = QuillColors.withAlpha(rgb, 255);
		icon = iconStack == null ? ItemStack.EMPTY : iconStack.copy();
		Minecraft client = Minecraft.getInstance();
		endTick = client.level == null ? 0L : client.level.getGameTime() + 20L * SECONDS;
	}

	/**
	 * Highlights an entity while it moves. Returns false when the entity is not loaded nearby; the
	 * caller can then tell the player instead of showing an empty highlight.
	 */
	public static boolean highlightEntity(UUID id, int rgb, ItemStack iconStack) {
		if (id == null) {
			return false;
		}

		entityId = id;
		blockTarget = null;
		entityFallback = null;
		color = QuillColors.withAlpha(rgb, 255);
		icon = iconStack == null ? ItemStack.EMPTY : iconStack.copy();
		Minecraft client = Minecraft.getInstance();
		endTick = client.level == null ? 0L : client.level.getGameTime() + 20L * SECONDS;

		Entity entity = findEntity(client);
		if (entity != null) {
			entityFallback = entity.blockPosition();
		}
		return entity != null;
	}

	public static void clear() {
		blockTarget = null;
		entityId = null;
		entityFallback = null;
	}

	public static void reset() {
		clear();
		icon = ItemStack.EMPTY;
	}

	/** Current position used by the off-screen HUD (entity position when loaded). */
	public static BlockPos target() {
		if (entityId != null) {
			Entity entity = findEntity(Minecraft.getInstance());
			if (entity != null) {
				entityFallback = entity.blockPosition();
			}
			return entityFallback;
		}
		return blockTarget;
	}

	public static ItemStack icon() {
		return icon;
	}

	public static int color() {
		return color;
	}

	public static int remainingSeconds() {
		Minecraft client = Minecraft.getInstance();
		if (target() == null || client.level == null) {
			return 0;
		}
		return Math.max(0, (int) Math.ceil((endTick - client.level.getGameTime()) / 20.0));
	}

	public static void tick(Minecraft client) {
		if (client.level == null) {
			return;
		}

		long now = client.level.getGameTime();
		if (now > endTick) {
			clear();
			return;
		}

		if (client.player == null) {
			return;
		}

		BlockPos hudTarget = target();
		if (hudTarget == null) {
			return;
		}

		if (client.player.blockPosition().distSqr(hudTarget) > 128 * 128) {
			return;
		}

		int remaining = remainingSeconds();
		if (remaining <= 0) {
			clear();
			return;
		}

		client.gui.hud.setOverlayMessage(
				Component.translatable("laborcraft.ui.highlight.countdown", remaining),
				false
		);

		if (now % 2L != 0L) {
			return;
		}

		DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
		if (entityId != null) {
			Entity entity = findEntity(client);
			if (entity != null) {
				entityFallback = entity.blockPosition();
				spawnBox(client.level, entity.getBoundingBox().inflate(0.12D), dust);
				return;
			}
		}

		if (hudTarget != null) {
			spawnBlock(client.level, hudTarget, dust);
		}
	}

	private static Entity findEntity(Minecraft client) {
		if (entityId == null || client.level == null) {
			return null;
		}

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity.getUUID().equals(entityId)) {
				return entity;
			}
		}
		return null;
	}

	private static void spawnBlock(ClientLevel level, BlockPos pos, DustParticleOptions dust) {
		double x0 = pos.getX();
		double y0 = pos.getY();
		double z0 = pos.getZ();
		spawnBox(level, new AABB(x0, y0, z0, x0 + 1.0, y0 + 1.0, z0 + 1.0), dust);
	}

	private static void spawnBox(ClientLevel level, AABB box, DustParticleOptions dust) {
		double x0 = box.minX;
		double y0 = box.minY;
		double z0 = box.minZ;
		double x1 = box.maxX;
		double y1 = box.maxY;
		double z1 = box.maxZ;

		// Bottom square
		line(level, dust, x0, y0, z0, x1, y0, z0);
		line(level, dust, x1, y0, z0, x1, y0, z1);
		line(level, dust, x1, y0, z1, x0, y0, z1);
		line(level, dust, x0, y0, z1, x0, y0, z0);

		// Top square
		line(level, dust, x0, y1, z0, x1, y1, z0);
		line(level, dust, x1, y1, z0, x1, y1, z1);
		line(level, dust, x1, y1, z1, x0, y1, z1);
		line(level, dust, x0, y1, z1, x0, y1, z0);

		// Verticals
		line(level, dust, x0, y0, z0, x0, y1, z0);
		line(level, dust, x1, y0, z0, x1, y1, z0);
		line(level, dust, x1, y0, z1, x1, y1, z1);
		line(level, dust, x0, y0, z1, x0, y1, z1);
	}

	private static void line(
			ClientLevel level,
			DustParticleOptions dust,
			double ax,
			double ay,
			double az,
			double bx,
			double by,
			double bz
	) {
		for (int step = 0; step <= EDGE_STEPS; step++) {
			double t = step / (double) EDGE_STEPS;
			level.addParticle(
					dust,
					ax + (bx - ax) * t,
					ay + (by - ay) * t,
					az + (bz - az) * t,
					0.0,
					0.0,
					0.0
			);
		}
	}
}

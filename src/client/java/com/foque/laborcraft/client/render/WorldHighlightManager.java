package com.foque.laborcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;

/**
 * Draws a temporary particle wireframe around a single block. Used by the storage tab so the
 * player can see which chest a row refers to; reusable for any future "highlight this block"
 * action (builder targets, miner nodes, waypoints).
 */
public final class WorldHighlightManager {
	private static final int SECONDS = 10;
	private static final int EDGE_STEPS = 4;

	private static BlockPos target;
	private static int color = 0xFFFFC65A;
	private static long endTick;

	private WorldHighlightManager() {
	}

	public static void highlight(BlockPos pos, int rgb) {
		if (pos == null) {
			return;
		}

		target = pos.immutable();
		color = 0xFF000000 | (rgb & 0xFFFFFF);
		Minecraft client = Minecraft.getInstance();
		endTick = client.level == null ? 0L : client.level.getGameTime() + 20L * SECONDS;
	}

	public static void clear() {
		target = null;
	}

	public static BlockPos target() {
		return target;
	}

	public static int color() {
		return color;
	}

	public static int remainingSeconds() {
		Minecraft client = Minecraft.getInstance();
		if (target == null || client.level == null) {
			return 0;
		}
		return Math.max(0, (int) Math.ceil((endTick - client.level.getGameTime()) / 20.0));
	}

	public static void reset() {
		target = null;
	}

	public static void tick(Minecraft client) {
		if (target == null || client.level == null) {
			return;
		}

		long now = client.level.getGameTime();
		if (now > endTick) {
			target = null;
			return;
		}

		if (client.player == null) {
			return;
		}

		if (client.player.blockPosition().distSqr(target) > 96 * 96) {
			return;
		}

		int remaining = Math.max(1, (int) Math.ceil((endTick - now) / 20.0));
		client.gui.hud.setOverlayMessage(
				Component.translatable("laborcraft.ui.station.storage.highlight.countdown", remaining),
				false
		);

		if (now % 2L != 0L) {
			return;
		}

		DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
		spawnWireframe(client.level, target, dust);
	}

	private static void spawnWireframe(ClientLevel level, BlockPos pos, DustParticleOptions dust) {
		double x0 = pos.getX();
		double y0 = pos.getY();
		double z0 = pos.getZ();
		double x1 = x0 + 1.0;
		double y1 = y0 + 1.0;
		double z1 = z0 + 1.0;

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

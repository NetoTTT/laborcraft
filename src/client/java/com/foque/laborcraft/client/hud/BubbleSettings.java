package com.foque.laborcraft.client.hud;

/** Client mirror of the server-wide bubble visibility policy. */
public final class BubbleSettings {
	private static boolean throughWalls;

	private BubbleSettings() {
	}

	public static boolean throughWalls() {
		return throughWalls;
	}

	public static void update(boolean enabled) {
		throughWalls = enabled;
	}
}

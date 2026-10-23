package com.foque.laborcraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class LaborCraftUiActions {
	private LaborCraftUiActions() {
	}

	public static void run(String command) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		player.connection.sendCommand(command);
	}
}

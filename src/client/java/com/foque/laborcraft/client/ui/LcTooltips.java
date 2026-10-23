package com.foque.laborcraft.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class LcTooltips {
	private LcTooltips() {
	}

	public static Tooltip of(Component title, Component body, String command) {
		MutableComponent text = Component.empty().append(title);
		text.append(Component.literal("\n"));
		text.append(Component.empty().append(body).withStyle(ChatFormatting.GRAY));

		if (command != null && !command.isBlank()) {
			text.append(Component.literal("\n" + command).withStyle(ChatFormatting.GOLD));
		}

		return Tooltip.create(text);
	}
}

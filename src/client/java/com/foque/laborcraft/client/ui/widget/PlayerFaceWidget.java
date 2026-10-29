package com.foque.laborcraft.client.ui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;

/** Non-interactive player face for measured rows. */
public final class PlayerFaceWidget extends AbstractWidget {
	private final PlayerSkin skin;

	public PlayerFaceWidget(PlayerSkin skin, int size) {
		super(0, 0, size, size, Component.empty());
		this.skin = skin;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(
			GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta
	) {
		int size = Math.min(getWidth(), getHeight());
		PlayerFaceExtractor.extractRenderState(graphics, skin, getX(), getY(), size);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

package com.foque.laborcraft.client.ui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Non-interactive item icon for measured rows and cards. */
public final class ItemStackWidget extends AbstractWidget {
	private final ItemStack stack;

	public ItemStackWidget(ItemStack stack, int size) {
		super(0, 0, size, size, stack.getHoverName());
		this.stack = stack;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(
			GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta
	) {
		graphics.item(stack, getX(), getY());
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

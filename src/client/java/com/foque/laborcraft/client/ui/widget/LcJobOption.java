package com.foque.laborcraft.client.ui.widget;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class LcJobOption extends AbstractWidget {
	private final Component description;
	private final ItemStack icon;
	private final boolean selected;
	private final Runnable action;
	private final Tooltip tooltip;

	public LcJobOption(
			int x,
			int y,
			int width,
			int height,
			Component label,
			Component description,
			ItemStack icon,
			boolean selected,
			String command,
			Runnable action
	) {
		super(x, y, width, height, label);
		this.description = description;
		this.icon = icon.copy();
		this.selected = selected;
		this.action = action;
		this.tooltip = LcTooltips.of(label, description, command);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		boolean hovered = isMouseOver(mouseX, mouseY) && active;

		graphics.fill(
				x,
				y,
				x + width,
				y + height,
				selected ? LcTheme.SELECTED_BG : hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG
		);
		LcRender.border(graphics, x, y, width, height, selected || hovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);

		if (selected) {
			graphics.fill(x, y, x + 2, y + height, LcTheme.ACCENT);
		}

		graphics.item(icon, x + 10, y + (height - 16) / 2);
		graphics.text(Minecraft.getInstance().font, getMessage(), x + 34, y + 9, LcTheme.TITLE);
		graphics.text(Minecraft.getInstance().font, description, x + 34, y + 23, LcTheme.MUTED);

		if (selected) {
			Component current = Component.translatable("laborcraft.ui.employee.job.current");
			graphics.text(
					Minecraft.getInstance().font,
					current,
					x + width - 12 - Minecraft.getInstance().font.width(current),
					y + 9,
					LcTheme.ACCENT
			);
		}

		if (hovered) {
			graphics.setTooltipForNextFrame(
					Minecraft.getInstance().font,
					tooltip.toCharSequence(Minecraft.getInstance()),
					mouseX,
					mouseY
			);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (!active) {
			return;
		}

		playDownSound(Minecraft.getInstance().getSoundManager());
		action.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

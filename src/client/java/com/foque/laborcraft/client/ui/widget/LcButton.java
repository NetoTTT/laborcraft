package com.foque.laborcraft.client.ui.widget;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

public class LcButton extends AbstractWidget {
	public enum Variant {
		PRIMARY,
		SECONDARY,
		DANGER,
		GHOST
	}

	private final Variant variant;
	private final Runnable action;
	private boolean selected;
	private Tooltip tooltip;
	private SoundEvent clickSound;

	public LcButton(int x, int y, int width, int height, Component message, Variant variant, Runnable action) {
		super(x, y, width, height, message);
		this.variant = variant;
		this.action = action;
	}

	public LcButton selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public LcButton tip(Tooltip tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	public LcButton clickSound(SoundEvent sound) {
		this.clickSound = sound;
		return this;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		boolean hovered = isMouseOver(mouseX, mouseY) && active;

		int background;
		int border;
		int textColor;

		if (!active) {
			background = LcTheme.BUTTON_DISABLED;
			border = LcTheme.BUTTON_DISABLED_BORDER;
			textColor = LcTheme.BUTTON_DISABLED_TEXT;
		} else {
			switch (variant) {
				case PRIMARY -> {
					background = hovered ? LcTheme.PRIMARY_BG_HOVER : LcTheme.PRIMARY_BG;
					border = hovered ? LcTheme.ACCENT_HOVER : LcTheme.ACCENT;
					textColor = hovered ? LcTheme.ACCENT_HOVER : LcTheme.ACCENT;
				}
				case DANGER -> {
					background = hovered ? 0xFF352024 : LcTheme.BUTTON_BG;
					border = LcTheme.DANGER;
					textColor = LcTheme.DANGER;
				}
				case GHOST -> {
					background = hovered || isFocused() ? LcTheme.BUTTON_BG : 0;
					border = isFocused() ? LcTheme.ACCENT : 0;
					textColor = hovered ? LcTheme.TITLE : LcTheme.MUTED;
				}
				default -> {
					background = hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG;
					border = LcTheme.BUTTON_BORDER;
					textColor = LcTheme.TEXT;
				}
			}

			if (selected) {
				background = LcTheme.SELECTED_BG;
				border = LcTheme.ACCENT;
				textColor = LcTheme.TITLE;
			}
		}

		if (background != 0) {
			graphics.fill(x, y, x + width, y + height, background);
		}

		if (border != 0) {
			LcRender.border(graphics, x, y, width, height, border);
		}

		if (active && isFocused()) {
			LcRender.border(graphics, x - 1, y - 1, width + 2, height + 2, LcTheme.ACCENT);
		}

		if (selected) {
			graphics.fill(x, y, x + 2, y + height, LcTheme.ACCENT);
		}

		graphics.centeredText(
				Minecraft.getInstance().font,
				getMessage(),
				x + width / 2,
				y + (height - 8) / 2 + 1,
				textColor
		);

		if (tooltip != null && isMouseOver(mouseX, mouseY)) {
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

		playClickSound();
		action.run();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean handled = super.mouseClicked(event, doubleClick);
		if (handled) {
			setFocused(true);
		}
		return handled;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!active) {
			return false;
		}

		int key = event.key();
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE) {
			playClickSound();
			action.run();
			return true;
		}

		return false;
	}

	private void playClickSound() {
		Minecraft client = Minecraft.getInstance();
		if (clickSound == null) {
			playDownSound(client.getSoundManager());
			return;
		}

		client.getSoundManager().play(SimpleSoundInstance.forUI(clickSound, 1.0F));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

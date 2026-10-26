package com.foque.laborcraft.client.ui.widget;

import com.quillui.widget.QuillButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

/**
 * LaborCraft-facing alias for the shared Quill UI button. Keeping the local name avoids touching
 * every screen while the widget implementation lives in the framework.
 */
public final class LcButton extends QuillButton {
	public LcButton(
			int x,
			int y,
			int width,
			int height,
			Component message,
			Variant variant,
			Runnable action
	) {
		super(x, y, width, height, message, variant, action);
	}

	@Override
	public LcButton selected(boolean selected) {
		super.selected(selected);
		return this;
	}

	@Override
	public LcButton tip(Tooltip tooltip) {
		super.tip(tooltip);
		return this;
	}

	@Override
	public LcButton clickSound(SoundEvent sound) {
		super.clickSound(sound);
		return this;
	}
}

package com.foque.laborcraft.client.ui.widget;

import com.quillui.theme.QuillThemeData;
import com.quillui.theme.QuillThemeScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Decorative worker portrait for measured identity cards. */
public final class WorkerPortraitWidget extends AbstractWidget {
	private final int accent;

	public WorkerPortraitWidget(int size) {
		super(0, 0, size, size, Component.empty());
		this.accent = QuillThemeScope.current().color(QuillThemeData.ColorRole.ACCENT);
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(
			GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta
	) {
		int x = getX();
		int y = getY();
		int size = Math.min(getWidth(), getHeight());
		graphics.fill(x, y, x + size, y + size, 0xFF202A35);
		graphics.fill(x, y, x + size, y + 1, accent);
		graphics.fill(x, y + size - 1, x + size, y + size, accent);
		graphics.fill(x, y, x + 1, y + size, accent);
		graphics.fill(x + size - 1, y, x + size, y + size, accent);

		int center = x + size / 2;
		graphics.fill(center - 7, y + 9, center + 7, y + 23, 0xFF445367);
		graphics.fill(center - 14, y + 27, center + 14, y + size - 8, 0xFF2E3947);
		graphics.fill(center - 14, y + size - 8, center + 14, y + size - 5, accent);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}

package com.foque.laborcraft.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LcRender {
	private LcRender() {
	}

	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x + 4, y + 4, x + width + 4, y + height + 4, LcTheme.PANEL_SHADOW);
		graphics.fill(x, y, x + width, y + height, LcTheme.PANEL_BG);
		border(graphics, x, y, width, height, LcTheme.PANEL_BORDER);
	}

	public static void border(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	public static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
		graphics.fill(x, y, x + width, y + 1, LcTheme.DIVIDER);
	}
}

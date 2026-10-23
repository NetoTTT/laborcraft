package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class LaborCraftScreen extends Screen {
	private final int panelWidth;
	private final int panelHeight;
	protected int panelX;
	protected int panelY;

	protected LaborCraftScreen(Component title, int panelWidth, int panelHeight) {
		super(title);
		this.panelWidth = panelWidth;
		this.panelHeight = panelHeight;
	}

	@Override
	protected void init() {
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;
		buildContent();
	}

	protected abstract void buildContent();

	protected abstract Component subtitle();

	protected int panelWidth() {
		return panelWidth;
	}

	protected int panelHeight() {
		return panelHeight;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		LcRender.panel(graphics, panelX, panelY, panelWidth, panelHeight);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(panelX + 12, panelY + 14, panelX + 15, panelY + 32, LcTheme.ACCENT);
		graphics.text(font, title, panelX + 22, panelY + 14, LcTheme.TITLE);
		graphics.text(font, subtitle(), panelX + 22, panelY + 29, LcTheme.MUTED);
		LcRender.divider(graphics, panelX + 20, panelY + 47, panelWidth - 40);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.centeredText(
				font,
				Component.translatable("laborcraft.ui.footer"),
				panelX + panelWidth / 2,
				panelY + panelHeight - 16,
				LcTheme.MUTED
		);
	}

	protected LcButton closeButton(Runnable action) {
		LcButton close = new LcButton(
				panelX + panelWidth() - 30,
				panelY + 8,
				20,
				20,
				Component.literal("X"),
				LcButton.Variant.GHOST,
				action
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		return close;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

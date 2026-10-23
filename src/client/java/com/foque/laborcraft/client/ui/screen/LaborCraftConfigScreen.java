package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class LaborCraftConfigScreen extends LaborCraftScreen {
	public LaborCraftConfigScreen() {
		super(Component.translatable("laborcraft.ui.config.title"), 420, 250);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.config.subtitle");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int y = panelY + 64;
		int width = panelWidth() - 44;
		int gap = 8;
		int half = (width - 24 - gap) / 2;

		String current = Minecraft.getInstance().getLanguageManager().getSelected();
		boolean portuguese = current.startsWith("pt");
		boolean english = current.startsWith("en");

		LcButton pt = new LcButton(
				x + 12,
				y + 58,
				half,
				26,
				Component.translatable("laborcraft.ui.config.language.pt"),
				LcButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.LANGUAGE_PT_BR)
		);
		pt.selected(portuguese);
		pt.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.config.language.pt"),
				Component.translatable("laborcraft.ui.config.language.desc"),
				"/" + LaborCraftCommands.LANGUAGE_PT_BR
		));
		addRenderableWidget(pt);

		LcButton en = new LcButton(
				x + 12 + half + gap,
				y + 58,
				half,
				26,
				Component.translatable("laborcraft.ui.config.language.en"),
				LcButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.LANGUAGE_EN_US)
		);
		en.selected(english);
		en.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.config.language.en"),
				Component.translatable("laborcraft.ui.config.language.desc"),
				"/" + LaborCraftCommands.LANGUAGE_EN_US
		));
		addRenderableWidget(en);

		LcButton back = new LcButton(
				x,
				panelY + panelHeight() - 46,
				90,
				24,
				Component.translatable("laborcraft.ui.config.back"),
				LcButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		back.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.config.back"),
				Component.translatable("laborcraft.ui.config.back.tip"),
				"/" + LaborCraftCommands.UI
		));
		addRenderableWidget(back);

		LcButton close = new LcButton(
				panelX + panelWidth() - 30,
				panelY + 8,
				20,
				20,
				Component.literal("X"),
				LcButton.Variant.GHOST,
				this::onClose
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.config.close"),
				Component.translatable("laborcraft.ui.config.close.tip"),
				null
		));
		addRenderableWidget(close);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int y = panelY + 64;
		int width = panelWidth() - 44;
		graphics.fill(x, y, x + width, y + 96, 0x401B232D);
		LcRender.border(graphics, x, y, width, 96, LcTheme.DIVIDER);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.config.language"),
				x + 12,
				y + 10,
				LcTheme.TEXT
		);
		graphics.textWithWordWrap(
				font,
				Component.translatable("laborcraft.ui.config.language.desc"),
				x + 12,
				y + 24,
				width - 24,
				LcTheme.MUTED
		);
	}
}

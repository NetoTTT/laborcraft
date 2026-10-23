package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.chat.LcValuePrompt;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class LaborCraftAdminScreen extends LaborCraftScreen {
	public LaborCraftAdminScreen() {
		super(Component.translatable("laborcraft.ui.admin.title"), 420, 280);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.admin.subtitle");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int y = panelY + 64;
		int width = panelWidth() - 44;

		LcButton reload = new LcButton(
				x,
				y,
				width,
				26,
				Component.translatable("laborcraft.ui.admin.reload"),
				LcButton.Variant.PRIMARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.ADMIN_RELOAD)
		);
		reload.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.reload"),
				Component.translatable("laborcraft.ui.admin.reload.tip"),
				"/" + LaborCraftCommands.ADMIN_RELOAD
		));
		addRenderableWidget(reload);

		LcButton status = new LcButton(
				x,
				y + 34,
				width,
				26,
				Component.translatable("laborcraft.ui.admin.status"),
				LcButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.ADMIN_STATUS)
		);
		status.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.status"),
				Component.translatable("laborcraft.ui.admin.status.tip"),
				"/" + LaborCraftCommands.ADMIN_STATUS
		));
		addRenderableWidget(status);

		int gap = 8;
		int half = (width - gap) / 2;

		LcButton wage = new LcButton(
				x,
				y + 68,
				half,
				26,
				Component.translatable("laborcraft.ui.admin.wage"),
				LcButton.Variant.SECONDARY,
				() -> LcValuePrompt.integer(
						Component.translatable("laborcraft.ui.admin.wage"),
						LaborCraftCommands.ADMIN_SET_WAGE + " ",
						0,
						100000
				)
						.quick("0", "0")
						.quick("24", "24")
						.quick("48", "48")
						.ask()
		);
		wage.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.wage"),
				Component.translatable("laborcraft.ui.admin.wage.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_WAGE + " <value>"
		));
		addRenderableWidget(wage);

		List<String> providers = CurrencyProviders.all().stream()
				.map(CurrencyProvider::id)
				.toList();
		LcButton provider = new LcButton(
				x + half + gap,
				y + 68,
				width - half - gap,
				26,
				Component.translatable("laborcraft.ui.admin.provider"),
				LcButton.Variant.SECONDARY,
				() -> LcValuePrompt.choice(
						Component.translatable("laborcraft.ui.admin.provider"),
						LaborCraftCommands.ADMIN_SET_PROVIDER + " ",
						providers
				).ask()
		);
		provider.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.provider"),
				Component.translatable("laborcraft.ui.admin.provider.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_PROVIDER + " <id>"
		));
		addRenderableWidget(provider);

		LcButton items = new LcButton(
				x,
				y + 102,
				width,
				26,
				Component.translatable("laborcraft.ui.admin.items"),
				LcButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.TEST_ITEMS)
		);
		items.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.items"),
				Component.translatable("laborcraft.ui.admin.items.tip"),
				"/" + LaborCraftCommands.TEST_ITEMS
		));
		addRenderableWidget(items);

		LcButton players = new LcButton(
				x,
				y + 136,
				width,
				26,
				Component.translatable("laborcraft.ui.admin.players"),
				LcButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(LaborCraftCommands.PLAYERS)
		);
		players.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.players"),
				Component.translatable("laborcraft.ui.admin.players.tip"),
				"/" + LaborCraftCommands.PLAYERS
		));
		addRenderableWidget(players);

		LcButton config = new LcButton(
				panelX + panelWidth() - 100,
				panelY + 8,
				64,
				20,
				Component.translatable("laborcraft.ui.admin.config"),
				LcButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.CONFIG)
		);
		config.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.config"),
				Component.translatable("laborcraft.ui.admin.config.tip"),
				"/" + LaborCraftCommands.CONFIG
		));
		addRenderableWidget(config);

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
				Component.translatable("laborcraft.ui.admin.close"),
				Component.translatable("laborcraft.ui.admin.close.tip"),
				null
		));
		addRenderableWidget(close);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int y = panelY + 200;
		int width = panelWidth() - 44;
		graphics.fill(x, y, x + width, y + 46, 0x401B232D);
		LcRender.border(graphics, x, y, width, 46, LcTheme.DIVIDER);
		graphics.textWithWordWrap(
				font,
				Component.translatable("laborcraft.ui.admin.note"),
				x + 10,
				y + 9,
				width - 20,
				LcTheme.MUTED
		);
	}
}

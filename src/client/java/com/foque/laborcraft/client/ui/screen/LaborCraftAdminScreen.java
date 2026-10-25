package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.chat.LcValuePrompt;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Server-authoritative admin panel: salary table, billing policy and system actions. */
public final class LaborCraftAdminScreen extends LaborCraftScreen {
	private enum Tab {
		SALARY,
		BILLING,
		SYSTEM
	}

	private static Tab pendingTab;

	private final OpenAdminPayload state;
	private Tab tab;

	public LaborCraftAdminScreen(OpenAdminPayload state) {
		this(state, consumePendingTab());
	}

	private LaborCraftAdminScreen(OpenAdminPayload state, Tab initialTab) {
		super(Component.translatable("laborcraft.ui.admin.title"), 520, 390);
		this.state = state;
		this.tab = initialTab == null ? Tab.SALARY : initialTab;
	}

	private static Tab consumePendingTab() {
		Tab value = pendingTab;
		pendingTab = null;
		return value;
	}

	private static void rememberTab(Tab tab) {
		pendingTab = tab;
	}

	@Override
	protected Component subtitle() {
		return switch (tab) {
			case SALARY -> Component.translatable("laborcraft.ui.admin.subtitle.salary");
			case BILLING -> Component.translatable("laborcraft.ui.admin.subtitle.billing");
			case SYSTEM -> Component.translatable("laborcraft.ui.admin.subtitle.system");
		};
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int tabY = panelY + 54;

		addRenderableWidget(tabButton(Tab.SALARY, "laborcraft.ui.admin.tab.salary", x, 146, tabY));
		addRenderableWidget(tabButton(Tab.BILLING, "laborcraft.ui.admin.tab.billing", x + 154, 146, tabY));
		addRenderableWidget(tabButton(Tab.SYSTEM, "laborcraft.ui.admin.tab.system", x + 308, 146, tabY));

		switch (tab) {
			case SALARY -> buildSalary(x, width);
			case BILLING -> buildBilling(x, width);
			case SYSTEM -> buildSystem(x, width);
		}

		addRenderableWidget(closeButton(this::onClose));
	}

	private LcButton tabButton(Tab target, String key, int x, int width, int y) {
		return new LcButton(
				x,
				y,
				width,
				20,
				Component.translatable(key),
				tab == target ? LcButton.Variant.PRIMARY : LcButton.Variant.GHOST,
				() -> switchTab(target)
		);
	}

	private void switchTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		clearWidgets();
		rememberTab(next);
		init();
	}

	private void buildSalary(int x, int width) {
		int left = width / 2 - 8;
		int rightX = x + left + 16;
		int right = width - left - 16;
		int y = panelY + 88;
		int step = 23;

		addRenderableWidget(integerButton(
				x, y, left, "laborcraft.ui.admin.salary.base", state.baseWage(),
				LaborCraftCommands.BILLING_SET + " base-wage ", 0, 1_000_000, "0", "24", "48"
		));
		addRenderableWidget(integerButton(
				x, y + step, left, "laborcraft.ui.admin.salary.period", state.periodDays(),
				LaborCraftCommands.BILLING_SET + " period-days ", 1, 365, "1", "7", "30"
		));
		addRenderableWidget(integerButton(
				x, y + step * 2, left, "laborcraft.ui.admin.salary.price", (int) Math.min(Integer.MAX_VALUE, state.price()),
				LaborCraftCommands.BILLING_SET + " price ", 0, 1_000_000, "64", "96"
		));
		addRenderableWidget(integerButton(
				x, y + step * 3, left, "laborcraft.ui.admin.salary.minimum", state.minimumDaily(),
				LaborCraftCommands.BILLING_SET + " minimum-daily ", 0, 1_000_000, "0", "24"
		));
		addRenderableWidget(decimalButton(
				x, y + step * 4, left, "laborcraft.ui.admin.salary.boost_cap", state.boostCap(),
				LaborCraftCommands.BILLING_SET + " boost-cap ", 0.1D, 100.0D
		));
		addRenderableWidget(toggleButton(
				x, y + step * 5, left, "laborcraft.ui.admin.salary.use_table",
				state.useSalaryTable(), "use-table"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 6, left, "laborcraft.ui.admin.salary.use_rank",
				state.useRankMultiplier(), "use-rank"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 7, left, "laborcraft.ui.admin.salary.use_rarity",
				state.useRarityMultiplier(), "use-rarity"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 8, left, "laborcraft.ui.admin.salary.use_boost",
				state.useBoostUpkeep(), "use-boost"
		));

		int rightY = panelY + 104;
		for (WorkerRank rank : WorkerRank.values()) {
			double value = multiplier(state.rankMultipliers(), rank.ordinal(), rank.upkeepMultiplier());
			addRenderableWidget(multiplierButton(
					rightX, rightY, right, rank.translationKey(), value,
					LaborCraftCommands.BILLING_RANK + " " + rank.id() + " "
			));
			rightY += 22;
		}

		int rarityY = panelY + 210;
		for (WorkerRarity rarity : WorkerRarity.values()) {
			double value = multiplier(state.rarityMultipliers(), rarity.ordinal(), 1.0D);
			addRenderableWidget(multiplierButton(
					rightX, rarityY, right, rarity.translationKey(), value,
					LaborCraftCommands.BILLING_RARITY + " " + rarity.id() + " "
			));
			rarityY += 22;
		}

		buildActions(x, left, rightX, right);
	}

	private void buildBilling(int x, int width) {
		int left = width / 2 - 8;
		int rightX = x + left + 16;
		int right = width - left - 16;
		int y = panelY + 88;
		int step = 23;

		addRenderableWidget(toggleButton(
				x, y, left, "laborcraft.ui.admin.billing.enabled", state.enabled(), "enabled"
		));
		addRenderableWidget(integerButton(
				x, y + step, left, "laborcraft.ui.admin.billing.check_ticks", state.checkTicks(),
				LaborCraftCommands.BILLING_SET + " check-ticks ", 20, 24_000, "20", "100", "200"
		));
		addRenderableWidget(integerButton(
				x, y + step * 2, left, "laborcraft.ui.admin.billing.grace", state.gracePeriods(),
				LaborCraftCommands.BILLING_SET + " grace ", 0, 1000, "0", "1", "2"
		));
		addRenderableWidget(integerButton(
				x, y + step * 3, left, "laborcraft.ui.admin.billing.dismiss", state.dismissAfterMissed(),
				LaborCraftCommands.BILLING_SET + " dismiss-after ", 0, 1000, "0", "3"
		));
		addRenderableWidget(integerButton(
				x, y + step * 4, left, "laborcraft.ui.admin.billing.mood", state.missedMoodPenalty(),
				LaborCraftCommands.BILLING_SET + " mood-penalty ", 0, 100, "0", "10"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 5, left, "laborcraft.ui.admin.billing.debt", state.allowDebt(), "debt"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 6, left, "laborcraft.ui.admin.billing.notify", state.notifyOwner(), "notify"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 7, left, "laborcraft.ui.admin.billing.use_schedule", state.useSchedule(), "use-schedule"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 8, left, "laborcraft.ui.admin.billing.pause_off_duty",
				state.pauseOffDuty(), "pause-off-duty"
		));

		buildActions(x, left, rightX, right);
	}

	private void buildSystem(int x, int width) {
		int y = panelY + 88;
		addRenderableWidget(systemButton(
				x, y, width, "laborcraft.ui.admin.reload", LaborCraftCommands.ADMIN_RELOAD
		));
		addRenderableWidget(systemButton(
				x, y + 30, width, "laborcraft.ui.admin.status", LaborCraftCommands.ADMIN_STATUS
		));
		addRenderableWidget(systemButton(
				x, y + 60, width, "laborcraft.ui.admin.items", LaborCraftCommands.TEST_ITEMS
		));
		addRenderableWidget(systemButton(
				x, y + 90, width, "laborcraft.ui.admin.players", LaborCraftCommands.PLAYERS
		));

		LcButton provider = new LcButton(
				x,
				y + 120,
				width,
				24,
				Component.translatable("laborcraft.ui.admin.provider"),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.choice(
							Component.translatable("laborcraft.ui.admin.provider"),
							LaborCraftCommands.ADMIN_SET_PROVIDER + " ",
							CurrencyProviders.all().stream().map(CurrencyProvider::id).toList()
					).ask();
				}
		);
		provider.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.provider"),
				Component.translatable("laborcraft.ui.admin.provider.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_PROVIDER + " <id>"
		));
		addRenderableWidget(provider);

		addRenderableWidget(systemButton(
				x, y + 150, width, "laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH
		));

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
	}

	private void buildActions(int x, int left, int rightX, int right) {
		int y = panelY + 314;
		LcButton apply = actionButton(
				x, y, left, "laborcraft.ui.admin.apply", LaborCraftCommands.BILLING_APPLY
		);
		apply.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.apply"),
				Component.translatable("laborcraft.ui.admin.apply.tip"),
				"/" + LaborCraftCommands.BILLING_APPLY
		));
		addRenderableWidget(apply);

		LcButton force = actionButton(
				rightX, y, right, "laborcraft.ui.admin.force", LaborCraftCommands.BILLING_FORCE
		);
		force.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.force"),
				Component.translatable("laborcraft.ui.admin.force.tip"),
				"/" + LaborCraftCommands.BILLING_FORCE
		));
		addRenderableWidget(force);

		LcButton market = actionButton(
				x + left + 16, y + 26, right, "laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH
		);
		market.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.market"),
				Component.translatable("laborcraft.ui.admin.market.tip"),
				"/" + LaborCraftCommands.ADMIN_MARKET_REFRESH
		));
		addRenderableWidget(market);
	}

	private LcButton integerButton(
			int x, int y, int width, String key, int current, String command,
			int min, int max, String... quickValues
	) {
		LcButton button = new LcButton(
				x, y, width, 21,
				label(key, Integer.toString(current)),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt prompt = LcValuePrompt.integer(Component.translatable(key), command, min, max)
							.current(Integer.toString(current));
					for (String quick : quickValues) {
						prompt.quick(quick, quick);
					}
					prompt.ask();
				}
		);
		button.tip(editTip(key, command, false));
		return button;
	}

	private LcButton decimalButton(
			int x, int y, int width, String key, double current, String command, double min, double max
	) {
		LcButton button = new LcButton(
				x, y, width, 21,
				label(key, format(current)),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.decimal(Component.translatable(key), command, min, max)
							.current(format(current))
							.ask();
				}
		);
		button.tip(editTip(key, command, true));
		return button;
	}

	private LcButton multiplierButton(int x, int y, int width, String key, double value, String command) {
		LcButton button = new LcButton(
				x, y, width, 20,
				Component.translatable(key).append(": " + format(value)),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.decimal(Component.translatable(key), command, 0.01D, 100.0D)
							.current(format(value))
							.ask();
				}
		);
		button.tip(editTip(key, command, true));
		return button;
	}

	private LcButton toggleButton(int x, int y, int width, String key, boolean current, String field) {
		LcButton button = new LcButton(
				x, y, width, 21,
				label(key, Component.translatable(current
						? "laborcraft.ui.admin.state.on"
						: "laborcraft.ui.admin.state.off").getString()),
				current ? LcButton.Variant.PRIMARY : LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(
							LaborCraftCommands.BILLING_SET + " " + field + " " + !current
					);
				}
		);
		button.selected(current);
		button.tip(LcTooltips.of(
				Component.translatable(key),
				Component.translatable("laborcraft.ui.admin.toggle.tip"),
				"/" + LaborCraftCommands.BILLING_SET + " " + field + " " + !current
		));
		return button;
	}

	private LcButton systemButton(int x, int y, int width, String key, String command) {
		LcButton button = new LcButton(
				x, y, width, 24,
				Component.translatable(key),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(command);
				}
		);
		button.tip(LcTooltips.of(
				Component.translatable(key),
				Component.translatable(key + ".tip"),
				"/" + command
		));
		return button;
	}

	private LcButton actionButton(int x, int y, int width, String key, String command) {
		return new LcButton(
				x, y, width, 22,
				Component.translatable(key),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(command);
				}
		);
	}

	private Tooltip editTip(String key, String command, boolean decimal) {
		return LcTooltips.of(
				Component.translatable(key),
				Component.translatable(decimal
						? "laborcraft.ui.admin.edit.tip.decimal"
						: "laborcraft.ui.admin.edit.tip"),
				"/" + command + "<valor>"
		);
	}

	private static Component label(String key, String value) {
		return Component.translatable(key).append(": " + value);
	}

	private static double multiplier(double[] values, int index, double fallback) {
		if (values == null || index < 0 || index >= values.length || values[index] <= 0.0D) {
			return fallback;
		}
		return values[index];
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = panelX + 22;
		int width = panelWidth() - 44;

		if (tab == Tab.SALARY) {
			int left = width / 2 - 8;
			int rightX = x + left + 16;
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.admin.salary.rank_header"),
					rightX,
					panelY + 88,
					LcTheme.MUTED
			);
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.admin.salary.rarity_header"),
					rightX,
					panelY + 194,
					LcTheme.MUTED
			);
		} else if (tab == Tab.BILLING) {
			int left = width / 2 - 8;
			int rightX = x + left + 16;
			int right = width - left - 16;
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.admin.billing.note"),
					rightX,
					panelY + 92,
					right,
					LcTheme.MUTED
			);
		} else {
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.admin.note"),
					x,
					panelY + 272,
					width,
					LcTheme.MUTED
			);
		}
	}
}

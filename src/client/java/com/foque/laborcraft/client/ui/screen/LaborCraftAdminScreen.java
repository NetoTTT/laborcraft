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
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillRect;
import com.quillui.widget.QuillNodes;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Server-authoritative admin panel: salary table, billing policy and system actions. */
public final class LaborCraftAdminScreen extends com.quillui.screen.QuillScreen {
	private enum Tab {
		SALARY,
		BILLING,
		SCHEDULE,
		AREA,
		MARKET,
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
	protected boolean constrainPanel() {
		return false;
	}

	@Override
	protected Component subtitle() {
		return switch (tab) {
			case SALARY -> Component.translatable("laborcraft.ui.admin.subtitle.salary");
			case BILLING -> Component.translatable("laborcraft.ui.admin.subtitle.billing");
			case SCHEDULE -> Component.translatable("laborcraft.ui.admin.subtitle.schedule");
			case AREA -> Component.translatable("laborcraft.ui.admin.subtitle.area");
			case MARKET -> Component.translatable("laborcraft.ui.admin.subtitle.market");
			case SYSTEM -> Component.translatable("laborcraft.ui.admin.subtitle.system");
		};
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int tabY = panelY + 54;

		addRenderableWidget(tabButton(Tab.SALARY, "laborcraft.ui.admin.tab.salary", x, 76, tabY));
		addRenderableWidget(tabButton(Tab.BILLING, "laborcraft.ui.admin.tab.billing", x + 80, 76, tabY));
		addRenderableWidget(tabButton(Tab.SCHEDULE, "laborcraft.ui.admin.tab.schedule", x + 160, 76, tabY));
		addRenderableWidget(tabButton(Tab.AREA, "laborcraft.ui.admin.tab.area", x + 240, 76, tabY));
		addRenderableWidget(tabButton(Tab.MARKET, "laborcraft.ui.admin.tab.market", x + 320, 76, tabY));
		addRenderableWidget(tabButton(Tab.SYSTEM, "laborcraft.ui.admin.tab.system", x + 400, 76, tabY));

		switch (tab) {
			case SALARY -> buildSalary(x, width);
			case BILLING -> buildBilling(x, width);
			case SCHEDULE -> buildSchedule(x, width);
			case AREA -> buildArea(x, width);
			case MARKET -> buildMarket(x, width);
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
		QuillFlexBox column = QuillNodes.column(2);
		column.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		column.add(control(toggleButton(0, 0, width, "laborcraft.ui.admin.billing.enabled", state.enabled(), "enabled"), 21));
		column.add(control(integerButton(0, 0, width, "laborcraft.ui.admin.billing.check_ticks", state.checkTicks(), LaborCraftCommands.BILLING_SET + " check-ticks ", 20, 24_000, "20", "100", "200"), 21));
		column.add(control(integerButton(0, 0, width, "laborcraft.ui.admin.billing.grace", state.gracePeriods(), LaborCraftCommands.BILLING_SET + " grace ", 0, 1000, "0", "1", "2"), 21));
		column.add(control(integerButton(0, 0, width, "laborcraft.ui.admin.billing.dismiss", state.dismissAfterMissed(), LaborCraftCommands.BILLING_SET + " dismiss-after ", 0, 1000, "0", "3"), 21));
		column.add(control(integerButton(0, 0, width, "laborcraft.ui.admin.billing.mood", state.missedMoodPenalty(), LaborCraftCommands.BILLING_SET + " mood-penalty ", 0, 100, "0", "10"), 21));
		column.add(control(toggleButton(0, 0, width, "laborcraft.ui.admin.billing.debt", state.allowDebt(), "debt"), 21));
		column.add(control(toggleButton(0, 0, width, "laborcraft.ui.admin.billing.notify", state.notifyOwner(), "notify"), 21));
		column.add(control(toggleButton(0, 0, width, "laborcraft.ui.admin.billing.use_schedule", state.useSchedule(), "use-schedule"), 21));
		column.add(control(toggleButton(0, 0, width, "laborcraft.ui.admin.billing.pause_off_duty", state.pauseOffDuty(), "pause-off-duty"), 21));
		setContent(column, new QuillRect(x, panelY + 88, width, 214));
	}
	private void buildSchedule(int x, int width) {
		int left = width / 2 - 8;
		int y = panelY + 88;
		int step = 26;

		LcButton mode = new LcButton(
				x, y, left, 21,
				label("laborcraft.ui.admin.schedule.mode", Component.translatable(
						"laborcraft.ui.admin.schedule.mode." + state.scheduleMode()
				).getString()),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.choice(
							Component.translatable("laborcraft.ui.admin.schedule.mode"),
							LaborCraftCommands.ADMIN_CONFIG + " schedule.mode ",
							java.util.List.of("daylight", "night", "custom", "always")
					).current(state.scheduleMode()).ask();
				}
		);
		mode.tip(editTip("laborcraft.ui.admin.schedule.mode", LaborCraftCommands.ADMIN_CONFIG + " schedule.mode ", false));
		addRenderableWidget(mode);

		addRenderableWidget(configIntegerButton(
				x, y + step, left, "laborcraft.ui.admin.schedule.start", state.scheduleStartTick(),
				"schedule.start-tick", 0, 23_999
		));
		addRenderableWidget(configIntegerButton(
				x, y + step * 2, left, "laborcraft.ui.admin.schedule.end", state.scheduleEndTick(),
				"schedule.end-tick", 0, 23_999
		));
		addRenderableWidget(configToggleButton(
				x, y + step * 3, left, "laborcraft.ui.admin.schedule.rain", state.workInRain(),
				"schedule.work-in-rain"
		));
		addRenderableWidget(toggleButton(
				x, y + step * 4, left, "laborcraft.ui.admin.billing.pause_off_duty",
				state.pauseOffDuty(), "pause-off-duty"
		));
		addRenderableWidget(configIntegerButton(
				x, y + step * 5, left, "laborcraft.ui.admin.schedule.idle", state.idleCheckTicks(),
				"schedule.idle-check-ticks", 20, 24_000
		));
	}

	private void buildArea(int x, int width) {
		int left = width / 2 - 8;
		int rightX = x + left + 16;
		int right = width - left - 16;
		int y = panelY + 88;
		int step = 25;

		addRenderableWidget(configChoiceButton(
				x, y, left, "laborcraft.ui.admin.area.shape", state.areaShape(),
				"area.shape", java.util.List.of("square", "circle")
		));
		addRenderableWidget(configIntegerButton(
				x, y + step, left, "laborcraft.ui.admin.area.vertical", state.areaVerticalRadius(),
				"area.vertical-radius", 1, 32
		));
		addRenderableWidget(configIntegerButton(
				x, y + step * 2, left, "laborcraft.ui.admin.area.max_radius", state.areaMaxRadius(),
				"area.max-radius", 1, 64
		));
		addRenderableWidget(configToggleButton(
				x, y + step * 3, left, "laborcraft.ui.admin.area.house_bonus", state.areaUseHouseBonus(),
				"area.use-house-bonus"
		));
		addRenderableWidget(configToggleButton(
				x, y + step * 4, left, "laborcraft.ui.admin.area.preview", state.areaPreviewParticles(),
				"area.preview-particles"
		));
		addRenderableWidget(configToggleButton(
				x, y + step * 5, left, "laborcraft.ui.admin.housing.required", state.housingRequired(),
				"housing.required"
		));
		addRenderableWidget(configIntegerButton(
				x, y + step * 6, left, "laborcraft.ui.admin.housing.recheck", state.housingRecheckSeconds(),
				"housing.recheck-seconds", 1, 3600
		));

		addRenderableWidget(configToggleButton(
				rightX, y, right, "laborcraft.ui.admin.housing.bed", state.requireBed(), "housing.require-bed"
		));
		addRenderableWidget(configToggleButton(
				rightX, y + step, right, "laborcraft.ui.admin.housing.roof", state.requireRoof(), "housing.require-roof"
		));
		addRenderableWidget(configToggleButton(
				rightX, y + step * 2, right, "laborcraft.ui.admin.housing.door", state.requireDoor(), "housing.require-door"
		));
		addRenderableWidget(configToggleButton(
				rightX, y + step * 3, right, "laborcraft.ui.admin.housing.enclosure", state.requireEnclosure(), "housing.require-enclosure"
		));
		addRenderableWidget(configToggleButton(
				rightX, y + step * 4, right, "laborcraft.ui.admin.housing.light", state.requireLight(), "housing.require-light"
		));
		addRenderableWidget(configIntegerButton(
				rightX, y + step * 5, right, "laborcraft.ui.admin.housing.distance", state.housingMaxDistance(),
				"housing.max-distance", 1, 256
		));
		addRenderableWidget(configIntegerButton(
				rightX, y + step * 6, right, "laborcraft.ui.admin.housing.volume", state.housingMaxVolume(),
				"housing.max-volume", 1, 100000
		));
		addRenderableWidget(configIntegerButton(
				rightX, y + step * 7, right, "laborcraft.ui.admin.housing.interior", state.housingMinInterior(),
				"housing.min-interior", 1, 64
		));
		addRenderableWidget(configIntegerButton(
				rightX, y + step * 8, right, "laborcraft.ui.admin.housing.beds", state.housingMaxBeds(),
				"housing.max-beds", 1, 64
		));
	}

	private LcButton configChoiceButton(
			int x, int y, int width, String key, String current, String field, java.util.List<String> options
	) {
		LcButton button = new LcButton(
				x, y, width, 21,
				label(key, Component.translatable("laborcraft.ui.admin.value." + current).getString()),
				LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.choice(
							Component.translatable(key),
							LaborCraftCommands.ADMIN_CONFIG + " " + field + " ",
							options
					).current(current).ask();
				}
		);
		button.tip(editTip(key, LaborCraftCommands.ADMIN_CONFIG + " " + field + " ", false));
		return button;
	}

	private void buildMarket(int x, int width) {
		int left = width / 2 - 8;
		int y = panelY + 88;
		int step = 26;

		addRenderableWidget(configIntegerButton(
				x, y, left, "laborcraft.ui.admin.market.offers", state.offerCount(),
				"offers.offer-count", 1, 20
		));
		addRenderableWidget(configIntegerButton(
				x, y + step, left, "laborcraft.ui.admin.market.rotation", state.rotationDays(),
				"offers.rotation-days", 1, 365
		));
		addRenderableWidget(configIntegerButton(
				x, y + step * 2, left, "laborcraft.ui.admin.market.max_workers", state.maxWorkersPerPlayer(),
				"worker.max-workers-per-player", 0, 100
		));

		LcButton refresh = actionButton(
				x, y + step * 3, left, "laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH
		);
		refresh.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.market"),
				Component.translatable("laborcraft.ui.admin.market.tip"),
				"/" + LaborCraftCommands.ADMIN_MARKET_REFRESH
		));
		addRenderableWidget(refresh);
	}

	private LcButton configIntegerButton(
			int x, int y, int width, String key, int current, String field, int min, int max
	) {
		return integerButton(
				x, y, width, key, current,
				LaborCraftCommands.ADMIN_CONFIG + " " + field + " ", min, max
		);
	}

	private LcButton configToggleButton(
			int x, int y, int width, String key, boolean current, String field
	) {
		LcButton button = new LcButton(
				x, y, width, 21,
				label(key, Component.translatable(current
						? "laborcraft.ui.admin.state.on"
						: "laborcraft.ui.admin.state.off").getString()),
				current ? LcButton.Variant.PRIMARY : LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(LaborCraftCommands.ADMIN_CONFIG + " " + field + " " + !current);
				}
		);
		button.selected(current);
		button.tip(editTip(key, LaborCraftCommands.ADMIN_CONFIG + " " + field + " ", false));
		return button;
	}

	private void buildSystem(int x, int width) {
		QuillFlexBox column = QuillNodes.column(6);
		column.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		column.add(control(systemButton(0, 0, width, "laborcraft.ui.admin.reload", LaborCraftCommands.ADMIN_RELOAD), 24));
		column.add(control(systemButton(0, 0, width, "laborcraft.ui.admin.status", LaborCraftCommands.ADMIN_STATUS), 24));
		column.add(control(systemButton(0, 0, width, "laborcraft.ui.admin.items", LaborCraftCommands.TEST_ITEMS), 24));
		column.add(control(systemButton(0, 0, width, "laborcraft.ui.admin.players", LaborCraftCommands.PLAYERS), 24));

		LcButton provider = new LcButton(
				0, 0, width, 24,
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
		column.add(control(provider, 24));

		column.add(control(systemButton(0, 0, width, "laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH), 24));

		LcButton bubble = new LcButton(
				0, 0, width, 24,
				label("laborcraft.ui.admin.bubble_walls", Component.translatable(state.bubbleThroughWalls()
						? "laborcraft.ui.admin.state.on"
						: "laborcraft.ui.admin.state.off").getString()),
				state.bubbleThroughWalls() ? LcButton.Variant.PRIMARY : LcButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(
							LaborCraftCommands.ADMIN_SET_BUBBLE_WALLS + " " + !state.bubbleThroughWalls()
					);
				}
		);
		bubble.selected(state.bubbleThroughWalls());
		bubble.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.admin.bubble_walls"),
				Component.translatable("laborcraft.ui.admin.bubble_walls.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_BUBBLE_WALLS + " " + !state.bubbleThroughWalls()
		));
		column.add(control(bubble, 24));
		column.add(text(Component.translatable("laborcraft.ui.admin.note"), MUTED));

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
		setContent(column, new QuillRect(x, panelY + 88, width, panelHeight() - 88 - 28));
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

		LcRender.tabStrip(graphics, x - 4, panelY + 50, width + 8, 28);
		if (tab == Tab.SYSTEM) {
			LcRender.section(graphics, x - 6, panelY + 82, width + 12, 250);
		} else {
			int left = width / 2 - 8;
			int rightX = x + left + 16;
			int right = width - left - 16;
			LcRender.section(graphics, x - 6, panelY + 82, left + 12, 230);
			LcRender.section(graphics, rightX - 6, panelY + 82, right + 12, 230);
		}

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
		} else if (tab == Tab.SCHEDULE) {
			int left = width / 2 - 8;
			int rightX = x + left + 16;
			int right = width - left - 16;
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.admin.schedule.note"),
					rightX,
					panelY + 92,
					right,
					LcTheme.MUTED
			);
		} else if (tab == Tab.AREA) {
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.admin.area.note"),
					x,
					panelY + 320,
					width,
					LcTheme.MUTED
			);
		} else if (tab == Tab.MARKET) {
			int left = width / 2 - 8;
			int rightX = x + left + 16;
			int right = width - left - 16;
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.admin.market.note"),
					rightX,
					panelY + 92,
					right,
					LcTheme.MUTED
			);
		}

	}
}

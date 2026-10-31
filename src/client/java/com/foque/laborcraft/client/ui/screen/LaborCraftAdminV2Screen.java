package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.chat.LcValuePrompt;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillNodes;
import java.util.List;
import net.minecraft.client.gui.components.Tooltip;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * Server-authoritative admin panel rebuilt on the measured Quill tree.
 *
 * <p>Six measured tabs share one panel: salary table and multipliers, billing policy, schedule,
 * area/housing limits, market and system actions. Every control is command-first: the UI sends a
 * command and the server validates and reopens the panel with the new snapshot.</p>
 */
public final class LaborCraftAdminV2Screen extends QuillScreen {
	public enum Tab {
		SALARY,
		BILLING,
		SCHEDULE,
		AREA,
		MARKET,
		SYSTEM
	}

	private static final int PANEL_WIDTH = 560;
	private static final int PANEL_HEIGHT = 420;
	private static final int CONTROL_HEIGHT = 21;
	private static final int ACTION_HEIGHT = 24;

	private static Tab pendingTab;

	private final OpenAdminPayload state;
	private Tab tab;

	public LaborCraftAdminV2Screen(OpenAdminPayload state) {
		this(state, consumePendingTab());
	}

	private LaborCraftAdminV2Screen(OpenAdminPayload state, Tab initialTab) {
		super(Component.translatable("laborcraft.ui.admin.title"), PANEL_WIDTH, PANEL_HEIGHT);
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
			case SCHEDULE -> Component.translatable("laborcraft.ui.admin.subtitle.schedule");
			case AREA -> Component.translatable("laborcraft.ui.admin.subtitle.area");
			case MARKET -> Component.translatable("laborcraft.ui.admin.subtitle.market");
			case SYSTEM -> Component.translatable("laborcraft.ui.admin.subtitle.system");
		};
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(QuillNodes.tabs(
				theme(),
				4,
				List.of(
						Component.translatable("laborcraft.ui.admin.tab.salary"),
						Component.translatable("laborcraft.ui.admin.tab.billing"),
						Component.translatable("laborcraft.ui.admin.tab.schedule"),
						Component.translatable("laborcraft.ui.admin.tab.area"),
						Component.translatable("laborcraft.ui.admin.tab.market"),
						Component.translatable("laborcraft.ui.admin.tab.system")
				),
				tab.ordinal(),
				index -> switchTab(Tab.values()[index])
		));

		switch (tab) {
			case SALARY -> {
				root.add(QuillNodes.flexible(salaryBody(), 1));
				root.add(actions(true));
			}
			case BILLING -> {
				root.add(QuillNodes.flexible(billingBody(), 1));
				root.add(actions(true));
			}
			case SCHEDULE -> root.add(QuillNodes.flexible(scheduleBody(), 1));
			case AREA -> root.add(QuillNodes.flexible(areaBody(), 1));
			case MARKET -> root.add(QuillNodes.flexible(marketBody(), 1));
			case SYSTEM -> root.add(QuillNodes.flexible(systemBody(), 1));
		}

		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private void switchTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		rememberTab(next);
		refreshContent();
	}

	// --- Salary -----------------------------------------------------------------------------

	private QuillLayoutNode salaryBody() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_3);
		row.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		QuillFlexBox left = column();
		left.add(control(integerButton(
				"laborcraft.ui.admin.salary.base", state.baseWage(),
				LaborCraftCommands.BILLING_SET + " base-wage ", 0, 1_000_000, "0", "24", "48"
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.salary.period", state.periodDays(),
				LaborCraftCommands.BILLING_SET + " period-days ", 1, 365, "1", "7", "30"
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.salary.price", (int) Math.min(Integer.MAX_VALUE, state.price()),
				LaborCraftCommands.BILLING_SET + " price ", 0, 1_000_000, "64", "96"
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.salary.minimum", state.minimumDaily(),
				LaborCraftCommands.BILLING_SET + " minimum-daily ", 0, 1_000_000, "0", "24"
		), CONTROL_HEIGHT));
		left.add(control(decimalButton(
				"laborcraft.ui.admin.salary.boost_cap", state.boostCap(),
				LaborCraftCommands.BILLING_SET + " boost-cap ", 0.1D, 100.0D
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.salary.use_table", state.useSalaryTable(),
				LaborCraftCommands.BILLING_SET + " use-table "
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.salary.use_rank", state.useRankMultiplier(),
				LaborCraftCommands.BILLING_SET + " use-rank "
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.salary.use_rarity", state.useRarityMultiplier(),
				LaborCraftCommands.BILLING_SET + " use-rarity "
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.salary.use_boost", state.useBoostUpkeep(),
				LaborCraftCommands.BILLING_SET + " use-boost "
		), CONTROL_HEIGHT));

		QuillFlexBox right = column();
		right.add(control(label(Component.translatable(
				"laborcraft.ui.admin.salary.rank_header"
		)).muted()));
		for (WorkerRank rank : WorkerRank.values()) {
			right.add(control(multiplierButton(
					rank.translationKey(),
					multiplier(state.rankMultipliers(), rank.ordinal(), rank.upkeepMultiplier()),
					LaborCraftCommands.BILLING_RANK + " " + rank.id() + " "
			), 20));
		}
		right.add(control(label(Component.translatable(
				"laborcraft.ui.admin.salary.rarity_header"
		)).muted()));
		for (WorkerRarity rarity : WorkerRarity.values()) {
			right.add(control(multiplierButton(
					rarity.translationKey(),
					multiplier(state.rarityMultipliers(), rarity.ordinal(), 1.0D),
					LaborCraftCommands.BILLING_RARITY + " " + rarity.id() + " "
			), 20));
		}

		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.admin.tab.salary"), left), 1));
		row.add(QuillNodes.flexible(new QuillCardBox(null, right), 1));
		return row;
	}

	// --- Billing ----------------------------------------------------------------------------

	private QuillLayoutNode billingBody() {
		QuillFlexBox column = column();
		column.add(control(toggleButton(
				"laborcraft.ui.admin.billing.enabled", state.enabled(),
				LaborCraftCommands.BILLING_SET + " enabled "
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.billing.check_ticks", state.checkTicks(),
				LaborCraftCommands.BILLING_SET + " check-ticks ", 20, 24_000, "20", "100", "200"
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.billing.grace", state.gracePeriods(),
				LaborCraftCommands.BILLING_SET + " grace ", 0, 1000, "0", "1", "2"
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.billing.dismiss", state.dismissAfterMissed(),
				LaborCraftCommands.BILLING_SET + " dismiss-after ", 0, 1000, "0", "3"
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.billing.mood", state.missedMoodPenalty(),
				LaborCraftCommands.BILLING_SET + " mood-penalty ", 0, 100, "0", "10"
		), CONTROL_HEIGHT));
		column.add(control(toggleButton(
				"laborcraft.ui.admin.billing.debt", state.allowDebt(),
				LaborCraftCommands.BILLING_SET + " debt "
		), CONTROL_HEIGHT));
		column.add(control(toggleButton(
				"laborcraft.ui.admin.billing.notify", state.notifyOwner(),
				LaborCraftCommands.BILLING_SET + " notify "
		), CONTROL_HEIGHT));
		column.add(control(toggleButton(
				"laborcraft.ui.admin.billing.use_schedule", state.useSchedule(),
				LaborCraftCommands.BILLING_SET + " use-schedule "
		), CONTROL_HEIGHT));
		column.add(control(toggleButton(
				"laborcraft.ui.admin.billing.pause_off_duty", state.pauseOffDuty(),
				LaborCraftCommands.BILLING_SET + " pause-off-duty "
		), CONTROL_HEIGHT));
		return new QuillCardBox(Component.translatable("laborcraft.ui.admin.tab.billing"), column);
	}

	// --- Schedule ---------------------------------------------------------------------------

	private QuillLayoutNode scheduleBody() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_3);
		row.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		QuillFlexBox left = column();
		left.add(control(choiceButton(
				"laborcraft.ui.admin.schedule.mode", state.scheduleMode(),
				LaborCraftCommands.ADMIN_CONFIG + " schedule.mode ",
				List.of("daylight", "night", "custom", "always"),
				"laborcraft.ui.admin.schedule.mode."
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.schedule.start", state.scheduleStartTick(),
				LaborCraftCommands.ADMIN_CONFIG + " schedule.start-tick ", 0, 23_999
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.schedule.end", state.scheduleEndTick(),
				LaborCraftCommands.ADMIN_CONFIG + " schedule.end-tick ", 0, 23_999
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.schedule.rain", state.workInRain(),
				LaborCraftCommands.ADMIN_CONFIG + " schedule.work-in-rain "
		), CONTROL_HEIGHT));
		left.add(control(toggleButton(
				"laborcraft.ui.admin.billing.pause_off_duty", state.pauseOffDuty(),
				LaborCraftCommands.BILLING_SET + " pause-off-duty "
		), CONTROL_HEIGHT));
		left.add(control(integerButton(
				"laborcraft.ui.admin.schedule.idle", state.idleCheckTicks(),
				LaborCraftCommands.ADMIN_CONFIG + " schedule.idle-check-ticks ", 20, 24_000
		), CONTROL_HEIGHT));

		QuillFlexBox note = column();
		note.add(text(Component.translatable("laborcraft.ui.admin.schedule.note"), MUTED));

		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.admin.tab.schedule"), left), 3));
		row.add(QuillNodes.flexible(new QuillCardBox(null, note), 2));
		return row;
	}

	// --- Area and housing -------------------------------------------------------------------

	private QuillLayoutNode areaBody() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_3);
		row.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		QuillFlexBox area = column();
		area.add(control(choiceButton(
				"laborcraft.ui.admin.area.shape", state.areaShape(),
				LaborCraftCommands.ADMIN_CONFIG + " area.shape ",
				List.of("square", "circle")
		), CONTROL_HEIGHT));
		area.add(control(integerButton(
				"laborcraft.ui.admin.area.vertical", state.areaVerticalRadius(),
				LaborCraftCommands.ADMIN_CONFIG + " area.vertical-radius ", 1, 32
		), CONTROL_HEIGHT));
		area.add(control(integerButton(
				"laborcraft.ui.admin.area.max_radius", state.areaMaxRadius(),
				LaborCraftCommands.ADMIN_CONFIG + " area.max-radius ", 1, 64
		), CONTROL_HEIGHT));
		area.add(control(toggleButton(
				"laborcraft.ui.admin.area.house_bonus", state.areaUseHouseBonus(),
				LaborCraftCommands.ADMIN_CONFIG + " area.use-house-bonus "
		), CONTROL_HEIGHT));
		area.add(control(toggleButton(
				"laborcraft.ui.admin.area.preview", state.areaPreviewParticles(),
				LaborCraftCommands.ADMIN_CONFIG + " area.preview-particles "
		), CONTROL_HEIGHT));

		QuillFlexBox housing = column();
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.required", state.housingRequired(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.required "
		), CONTROL_HEIGHT));
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.bed", state.requireBed(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.require-bed "
		), CONTROL_HEIGHT));
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.roof", state.requireRoof(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.require-roof "
		), CONTROL_HEIGHT));
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.door", state.requireDoor(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.require-door "
		), CONTROL_HEIGHT));
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.enclosure", state.requireEnclosure(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.require-enclosure "
		), CONTROL_HEIGHT));
		housing.add(control(toggleButton(
				"laborcraft.ui.admin.housing.light", state.requireLight(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.require-light "
		), CONTROL_HEIGHT));
		housing.add(control(integerButton(
				"laborcraft.ui.admin.housing.distance", state.housingMaxDistance(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.max-distance ", 1, 256
		), CONTROL_HEIGHT));
		housing.add(control(integerButton(
				"laborcraft.ui.admin.housing.volume", state.housingMaxVolume(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.max-volume ", 1, 100_000
		), CONTROL_HEIGHT));

		QuillFlexBox housingLimits = column();
		housingLimits.add(control(integerButton(
				"laborcraft.ui.admin.housing.interior", state.housingMinInterior(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.min-interior ", 1, 64
		), CONTROL_HEIGHT));
		housingLimits.add(control(integerButton(
				"laborcraft.ui.admin.housing.beds", state.housingMaxBeds(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.max-beds ", 1, 64
		), CONTROL_HEIGHT));
		housingLimits.add(control(integerButton(
				"laborcraft.ui.admin.housing.recheck", state.housingRecheckSeconds(),
				LaborCraftCommands.ADMIN_CONFIG + " housing.recheck-seconds ", 1, 3600
		), CONTROL_HEIGHT));

		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.admin.tab.area"), area), 1));
		row.add(QuillNodes.flexible(new QuillCardBox(null, housing), 1));
		row.add(QuillNodes.flexible(new QuillCardBox(null, housingLimits), 1));
		return row;
	}

	// --- Market -----------------------------------------------------------------------------

	private QuillLayoutNode marketBody() {
		QuillFlexBox column = column();
		column.add(control(integerButton(
				"laborcraft.ui.admin.market.offers", state.offerCount(),
				LaborCraftCommands.ADMIN_CONFIG + " offers.offer-count ", 1, 20
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.market.rotation", state.rotationDays(),
				LaborCraftCommands.ADMIN_CONFIG + " offers.rotation-days ", 1, 365
		), CONTROL_HEIGHT));
		column.add(control(integerButton(
				"laborcraft.ui.admin.market.max_workers", state.maxWorkersPerPlayer(),
				LaborCraftCommands.ADMIN_CONFIG + " worker.max-workers-per-player ", 0, 100
		), CONTROL_HEIGHT));
		column.add(control(actionButton(
				"laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH,
				"laborcraft.ui.admin.market.tip", ACTION_HEIGHT
		), ACTION_HEIGHT));
		column.add(text(Component.translatable("laborcraft.ui.admin.market.tip"), MUTED));
		return new QuillCardBox(Component.translatable("laborcraft.ui.admin.tab.market"), column);
	}

	// --- System -----------------------------------------------------------------------------

	private QuillLayoutNode systemBody() {
		QuillFlexBox column = column();
		column.add(control(systemButton(
				"laborcraft.ui.admin.reload", LaborCraftCommands.ADMIN_RELOAD
		), ACTION_HEIGHT));
		column.add(control(systemButton(
				"laborcraft.ui.admin.status", LaborCraftCommands.ADMIN_STATUS
		), ACTION_HEIGHT));
		column.add(control(systemButton(
				"laborcraft.ui.admin.items", LaborCraftCommands.TEST_ITEMS
		), ACTION_HEIGHT));
		column.add(control(systemButton(
				"laborcraft.ui.admin.players", LaborCraftCommands.PLAYERS
		), ACTION_HEIGHT));
		column.add(control(providerButton(), ACTION_HEIGHT));
		column.add(control(systemButton(
				"laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH
		), ACTION_HEIGHT));
		column.add(control(bubbleButton(), ACTION_HEIGHT));
		column.add(control(actionButton(
				"laborcraft.ui.admin.config", LaborCraftCommands.CONFIG,
				"laborcraft.ui.admin.config.tip", ACTION_HEIGHT
		), ACTION_HEIGHT));
		column.add(text(Component.translatable("laborcraft.ui.admin.note"), MUTED));
		return new QuillCardBox(Component.translatable("laborcraft.ui.admin.tab.system"), column);
	}

	private QuillButton providerButton() {
		QuillButton provider = new QuillButton(
				0, 0, 220, ACTION_HEIGHT,
				Component.translatable("laborcraft.ui.admin.provider"),
				QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.choice(
							Component.translatable("laborcraft.ui.admin.provider"),
							LaborCraftCommands.ADMIN_SET_PROVIDER + " ",
							CurrencyProviders.all().stream().map(CurrencyProvider::id).toList()
					).ask();
				}
		);
		provider.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.admin.provider"),
				Component.translatable("laborcraft.ui.admin.provider.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_PROVIDER + " <id>"
		));
		return provider;
	}

	private QuillButton bubbleButton() {
		boolean enabled = state.bubbleThroughWalls();
		QuillButton bubble = new QuillButton(
				0, 0, 220, ACTION_HEIGHT,
				label("laborcraft.ui.admin.bubble_walls", Component.translatable(enabled
						? "laborcraft.ui.admin.state.on"
						: "laborcraft.ui.admin.state.off")),
				enabled ? QuillButton.Variant.PRIMARY : QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(
							LaborCraftCommands.ADMIN_SET_BUBBLE_WALLS + " " + !enabled
					);
				}
		).selected(enabled);
		bubble.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.admin.bubble_walls"),
				Component.translatable("laborcraft.ui.admin.bubble_walls.tip"),
				"/" + LaborCraftCommands.ADMIN_SET_BUBBLE_WALLS + " " + !enabled
		));
		return bubble;
	}

	// --- Shared -----------------------------------------------------------------------------

	private QuillFlexBox actions(boolean billingActions) {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);
		if (!billingActions) {
			bar.add(QuillNodes.flexible(QuillNodes.spacer()));
			return bar;
		}

		QuillButton apply = actionButton(
				"laborcraft.ui.admin.apply", LaborCraftCommands.BILLING_APPLY,
				"laborcraft.ui.admin.apply.tip", ACTION_HEIGHT
		);
		bar.add(QuillNodes.flexible(QuillNodes.widget(apply, 120, ACTION_HEIGHT), 2));

		QuillButton force = actionButton(
				"laborcraft.ui.admin.force", LaborCraftCommands.BILLING_FORCE,
				"laborcraft.ui.admin.force.tip", ACTION_HEIGHT
		);
		bar.add(QuillNodes.flexible(QuillNodes.widget(force, 120, ACTION_HEIGHT), 2));

		QuillButton market = actionButton(
				"laborcraft.ui.admin.market", LaborCraftCommands.ADMIN_MARKET_REFRESH,
				"laborcraft.ui.admin.market.tip", ACTION_HEIGHT
		);
		bar.add(QuillNodes.widget(market, 130, ACTION_HEIGHT));
		return bar;
	}

	private QuillFlexBox column() {
		QuillFlexBox column = QuillNodes.column(QuillSpacing.SPACE_1);
		column.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		return column;
	}

	private QuillButton integerButton(
			String key, int current, String command, int min, int max, String... quickValues
	) {
		QuillButton button = new QuillButton(
				0, 0, 220, CONTROL_HEIGHT,
				label(key, Integer.toString(current)),
				QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt prompt = LcValuePrompt.integer(
							Component.translatable(key), command, min, max
					).current(Integer.toString(current));
					for (String quick : quickValues) {
						prompt.quick(quick, quick);
					}
					prompt.ask();
				}
		);
		button.tip(editTip(key, command, false));
		return button;
	}

	private QuillButton decimalButton(String key, double current, String command, double min, double max) {
		QuillButton button = new QuillButton(
				0, 0, 220, CONTROL_HEIGHT,
				label(key, format(current)),
				QuillButton.Variant.SECONDARY,
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

	private QuillButton multiplierButton(String key, double value, String command) {
		QuillButton button = new QuillButton(
				0, 0, 220, 20,
				Component.translatable(key).append(": " + format(value)),
				QuillButton.Variant.SECONDARY,
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

	private QuillButton toggleButton(String key, boolean current, String command) {
		QuillButton button = new QuillButton(
				0, 0, 220, CONTROL_HEIGHT,
				label(key, Component.translatable(current
						? "laborcraft.ui.admin.state.on"
						: "laborcraft.ui.admin.state.off")),
				current ? QuillButton.Variant.PRIMARY : QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(command + !current);
				}
		);
		button.selected(current);
		button.tip(QuillTooltips.of(
				Component.translatable(key),
				Component.translatable("laborcraft.ui.admin.toggle.tip"),
				"/" + command + !current
		));
		return button;
	}

	private QuillButton choiceButton(
			String key, String current, String command, List<String> options
	) {
		return choiceButton(key, current, command, options, "laborcraft.ui.admin.value.");
	}

	private QuillButton choiceButton(
			String key, String current, String command, List<String> options, String valuePrefix
	) {
		QuillButton button = new QuillButton(
				0, 0, 220, CONTROL_HEIGHT,
				label(key, Component.translatable(valuePrefix + current).getString()),
				QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LcValuePrompt.choice(Component.translatable(key), command, options)
							.current(current)
							.ask();
				}
		);
		button.tip(editTip(key, command, false));
		return button;
	}

	private QuillButton systemButton(String key, String command) {
		QuillButton button = new QuillButton(
				0, 0, 220, ACTION_HEIGHT,
				Component.translatable(key),
				QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(command);
				}
		);
		button.tip(QuillTooltips.of(
				Component.translatable(key),
				Component.translatable(key + ".tip"),
				"/" + command
		));
		return button;
	}

	private QuillButton actionButton(String key, String command, String tipKey, int height) {
		QuillButton button = new QuillButton(
				0, 0, 220, height,
				Component.translatable(key),
				QuillButton.Variant.SECONDARY,
				() -> {
					rememberTab(tab);
					LaborCraftUiActions.run(command);
				}
		);
		button.tip(QuillTooltips.of(
				Component.translatable(key),
				Component.translatable(tipKey),
				"/" + command
		));
		return button;
	}

	private static Tooltip editTip(String key, String command, boolean decimal) {
		return QuillTooltips.of(
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

	private static Component label(String key, Component value) {
		return Component.translatable(key).append(": ").append(value);
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
}

package com.foque.laborcraft.client.ui.screen;

import com.quillui.theme.QuillColors;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.WorkerListFilter;
import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;
import com.foque.laborcraft.client.ui.widget.ItemStackWidget;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.text.QuillTextStyle;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Player profile with balance, employee count and a filterable worker list. */
public final class LaborCraftProfileScreen extends QuillScreen {
	private static final ItemStack EYE_ICON = new ItemStack(Items.ENDER_EYE);

	private static WorkerListFilter pendingFilter;

	private final OpenProfilePayload profile;
	private WorkerListFilter filter;

	public LaborCraftProfileScreen(OpenProfilePayload profile) {
		super(Component.translatable("laborcraft.ui.profile.title"), 460, 360, LaborCraftTheme.currentData());
		this.profile = profile;
		this.filter = pendingFilter == null ? WorkerListFilter.ALL : pendingFilter;
		pendingFilter = null;
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.profile.subtitle", profile.playerName());
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(stats());
		root.add(themeCard());
		root.add(header());

		List<OpenProfilePayload.WorkerSummary> visible = visibleWorkers();
		if (visible.isEmpty()) {
			root.add(text(
					Component.translatable(profile.summaries().isEmpty()
							? "laborcraft.ui.profile.empty"
							: "laborcraft.ui.filter.empty"),
					MUTED
			));
		}
		else {
			QuillListView<OpenProfilePayload.WorkerSummary> list = new QuillListView<>(
					0, 0, 400, 146, 52,
					this::workerRow,
					null
			);
			list.replaceItems(visible);
			root.add(QuillNodes.flexible(control(list), 1));
		}

		root.add(text(Component.translatable("laborcraft.ui.profile.highlight.hint"), MUTED));
		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillFlexBox stats() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.profile.money"),
				text(Component.literal(profile.balanceText()))
		)));
		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.profile.employees"),
				text(employeesValue())
		)));
		return row;
	}

	private QuillCardBox themeCard() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_1);
		for (LaborCraftTheme theme : LaborCraftTheme.values()) {
			boolean active = theme == LaborCraftTheme.current();
			QuillButton button = new QuillButton(
					0, 0, 60, 20,
					theme.displayName(),
					active ? QuillButton.Variant.PRIMARY : QuillButton.Variant.SECONDARY,
					() -> selectTheme(theme)
			);
			button.selected(active);
			button.tip(QuillTooltips.of(
					theme.displayName(),
					theme.description(),
					null
			));
			row.add(QuillNodes.flexible(QuillNodes.widget(button, 60, 20), 1));
		}
		return new QuillCardBox(Component.translatable("laborcraft.ui.profile.theme"), row);
	}

	private void selectTheme(LaborCraftTheme theme) {
		if (theme == LaborCraftTheme.current()) {
			return;
		}
		LaborCraftTheme.select(theme);
		pendingFilter = filter;
		Minecraft.getInstance().setScreenAndShow(new LaborCraftProfileScreen(profile));
	}
	private QuillFlexBox header() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(text(Component.translatable("laborcraft.ui.profile.workers")));

		QuillButton filterButton = new QuillButton(
				0, 0, 108, 18,
				Component.translatable(filter.labelKey()),
				QuillButton.Variant.SECONDARY,
				() -> {
					filter = filter.next();
					refreshContent();
				}
		);
		filterButton.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.filter.label"),
				Component.translatable("laborcraft.ui.filter.tip"),
				null
		));
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		row.add(QuillNodes.widget(filterButton, 108, 18));
		return row;
	}

	private QuillLayoutNode workerRow(OpenProfilePayload.WorkerSummary summary, int index) {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(QuillNodes.widget(new ItemStackWidget(jobIcon(summary.jobId()), 16), 16, 16));

		QuillFlexBox identity = QuillNodes.column(2);
		identity.add(control(label(Component.literal(summary.name()))
				.style(QuillTextStyle.of(LcWorkerText.rarityColor(summary.rarityId())))));
		identity.add(control(label(LcWorkerText.join(
				List.of(
						LcWorkerText.rank(summary.rankId()),
						jobLabel(summary.jobId()),
						stateLabel(summary.state())
				),
				LcWorkerText.SEPARATOR
		)).muted()));
		identity.add(control(label(Component.translatable(summary.taskKey())).muted()));
		row.add(QuillNodes.flexible(identity));

		QuillFlexBox stats = QuillNodes.column(2);
		stats.crossAxisAlignment(QuillCrossAxisAlignment.END);
		stats.add(control(label(Component.translatable(
				"laborcraft.ui.profile.worker.health",
				(int) summary.health(),
				(int) summary.maxHealth()
		))));
		stats.add(control(label(Component.translatable(
				"laborcraft.ui.profile.worker.hunger",
				summary.hunger()
		)).muted()));
		stats.add(control(label(Component.translatable(
				"laborcraft.ui.profile.worker.mood",
				summary.mood()
		)).muted()));
		row.add(stats);

		if (summary.hasDebt()) {
			row.add(QuillNodes.widget(new QuillButton(
					0, 0, 58, 16,
					Component.translatable("laborcraft.ui.employee.pay"),
					QuillButton.Variant.SECONDARY,
					() -> payWorker(summary)
			), 58, 16));
		}

		row.add(QuillNodes.widget(highlightButton(summary), 18, 18));
		return row;
	}

	private QuillButton highlightButton(OpenProfilePayload.WorkerSummary summary) {
		QuillButton button = new QuillButton(
				0, 0, 18, 18,
				Component.empty(),
				QuillButton.Variant.GHOST,
				() -> highlightWorker(summary.id())
		).icon(EYE_ICON);
		button.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.profile.worker.highlight"),
				Component.translatable("laborcraft.ui.profile.highlight.hint"),
				null
		));
		return button;
	}

	private Component employeesValue() {
		Component value;
		if (profile.limit() <= 0) {
			value = Component.translatable("laborcraft.ui.profile.employees.unlimited", profile.workers());
		}
		else {
			value = Component.translatable(
					"laborcraft.ui.profile.employees.value",
					profile.workers(),
					profile.limit()
			);
		}

		if (profile.pending() <= 0) {
			return value;
		}
		return value.copy()
				.append(Component.literal(" "))
				.append(Component.translatable("laborcraft.ui.profile.pending", profile.pending()));
	}

	private List<OpenProfilePayload.WorkerSummary> visibleWorkers() {
		return profile.summaries().stream()
				.filter(summary -> filter.matches(summary.state(), summary.hasDebt()))
				.toList();
	}

	private void payWorker(OpenProfilePayload.WorkerSummary summary) {
		pendingFilter = filter;
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_PAY_PROFILE + " " + summary.id());
	}

	private void highlightWorker(UUID id) {
		Minecraft client = Minecraft.getInstance();
		boolean found = WorldHighlightManager.highlightEntity(id, QuillColors.rgb(72, 215, 160), EYE_ICON);
		onClose();
		if (!found && client.gui != null) {
			client.gui.hud.setOverlayMessage(
					Component.translatable("laborcraft.ui.highlight.missing"),
					false
			);
		}
	}

	private static Component jobLabel(String jobId) {
		if (jobId == null || jobId.isBlank()) {
			return Component.translatable("laborcraft.ui.worker.job.none");
		}
		JobType job = JobTypes.get(jobId);
		return job == null
				? Component.translatable("laborcraft.ui.worker.job.unknown", jobId)
				: Component.translatable(job.translationKey());
	}

	private static Component stateLabel(String state) {
		if (state == null || state.isBlank()) {
			return Component.empty();
		}
		return Component.translatable(
				"laborcraft.ui.worker.state." + state.toLowerCase(Locale.ROOT)
		);
	}

	private static ItemStack jobIcon(String jobId) {
		if (jobId != null && JobTypes.FARMER.id().equals(jobId)) {
			return new ItemStack(Items.WHEAT);
		}
		return new ItemStack(Items.EMERALD);
	}
}

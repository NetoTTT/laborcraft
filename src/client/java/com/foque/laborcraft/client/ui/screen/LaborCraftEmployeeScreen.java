package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.ItemStackWidget;
import com.foque.laborcraft.client.ui.widget.WorkerPortraitWidget;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.text.QuillTextStyle;
import com.quillui.theme.QuillSpacing;
import com.quillui.theme.QuillThemeData;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import com.quillui.widget.QuillWidgetBox;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Employee window: summary, job picker and house picker. */
public final class LaborCraftEmployeeScreen extends LaborCraftWorkerScreen {
	public enum Tab {
		OVERVIEW,
		JOB,
		HOUSE
	}

	private static Tab pendingTab;

	private Tab tab;
	private boolean confirmDismiss;
	private QuillButton dismissButton;

	public LaborCraftEmployeeScreen(OpenWorkerPayload worker) {
		this(worker, consumePendingTab());
	}

	private LaborCraftEmployeeScreen(OpenWorkerPayload worker, Tab initialTab) {
		super(worker, Component.translatable("laborcraft.ui.employee.title"), 460, 360);
		this.tab = initialTab == null ? Tab.OVERVIEW : initialTab;
	}

	public static void rememberTab(Tab tab) {
		pendingTab = tab;
	}

	private static Tab consumePendingTab() {
		Tab tab = pendingTab;
		pendingTab = null;
		return tab;
	}

	@Override
	public void added() {
		super.added();
		sendScreenState(true);
	}

	@Override
	public void removed() {
		sendScreenState(false);
		pendingTab = null;
		super.removed();
	}

	private void sendScreenState(boolean open) {
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null || !ClientPlayNetworking.canSend(WorkerScreenStatePayload.TYPE)) {
			return;
		}
		ClientPlayNetworking.send(new WorkerScreenStatePayload(worker.workerId(), open));
	}

	@Override
	protected Component subtitle() {
		return switch (tab) {
			case OVERVIEW -> Component.translatable("laborcraft.ui.employee.subtitle");
			case JOB -> Component.translatable("laborcraft.ui.employee.subtitle.job");
			case HOUSE -> Component.translatable("laborcraft.ui.employee.subtitle.house");
		};
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(QuillNodes.tabs(
				theme(),
				6,
				List.of(
						Component.translatable("laborcraft.ui.employee.tab.overview"),
						Component.translatable("laborcraft.ui.employee.tab.job"),
						Component.translatable("laborcraft.ui.employee.tab.house")
				),
				tab.ordinal(),
				index -> switchTab(Tab.values()[index])
		));
		root.add(identity());

		switch (tab) {
			case OVERVIEW -> root.add(overview());
			case JOB -> root.add(QuillNodes.flexible(jobList(), 1));
			case HOUSE -> {
				if (worker.hasHome()) {
					root.add(releaseRow());
				}
				if (worker.houses().isEmpty()) {
					root.add(text(Component.translatable("laborcraft.ui.employee.house.empty"), MUTED));
				}
				else {
					root.add(QuillNodes.flexible(houseList(), 1));
				}
			}
		}

		root.add(actions());
		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private void switchTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		confirmDismiss = false;
		refreshContent();
	}

	private QuillCardBox identity() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(QuillNodes.widget(new WorkerPortraitWidget(52), 52, 52));

		QuillFlexBox info = QuillNodes.column(2);
		info.add(control(label(workerName()).title()));

		boolean hasJob = worker.jobId() != null && !worker.jobId().isBlank();
		info.add(control(label(hasJob
				? stateLabel()
				: Component.translatable("laborcraft.ui.worker.status.no_job"))
				.color(hasJob ? ACCENT : MUTED)));
		info.add(control(label(rankLine())
				.style(QuillTextStyle.of(LcWorkerText.rarityColor(worker.rarityId())))));
		info.add(control(label(healthLabel()).muted()));

		QuillFlexBox vitals = QuillNodes.row(QuillSpacing.SPACE_2);
		vitals.add(control(label(Component.translatable(
				"laborcraft.ui.worker.hunger",
				worker.hunger()
		)).muted()));
		vitals.add(control(label(Component.translatable(
				"laborcraft.ui.worker.mood",
				worker.mood()
		)).muted()));
		info.add(vitals);
		row.add(QuillNodes.flexible(info));
		return new QuillCardBox(null, row);
	}

	private QuillCardBox overview() {
		QuillFlexBox column = QuillNodes.column(QuillSpacing.SPACE_1);
		column.add(control(label(Component.translatable(
				"laborcraft.ui.employee.current"
		)).muted()));

		QuillFlexBox jobRow = QuillNodes.row(QuillSpacing.SPACE_2);
		jobRow.add(control(label(jobLabel()).title()));
		jobRow.add(QuillNodes.flexible(QuillNodes.spacer()));
		jobRow.add(control(label(Component.translatable(
				"laborcraft.ui.employee.state",
				stateLabel()
		))));
		column.add(jobRow);

		boolean hasJob = worker.jobId() != null && !worker.jobId().isBlank();
		column.add(control(label(hasJob
				? LcWorkerText.join(List.of(aptitudeLabel(), masteryLabel()), LcWorkerText.SEPARATOR)
				: Component.translatable("laborcraft.ui.employee.no_cargo")).muted()));

		List<Component> boosts = LcWorkerText.boosts(worker.boosts());
		if (!boosts.isEmpty()) {
			column.add(control(label(LcWorkerText.join(boosts, LcWorkerText.SEPARATOR)).accent()));
		}

		Component house = worker.hasHome()
				? Component.translatable("laborcraft.ui.employee.home.named", worker.homeName())
				: Component.translatable("laborcraft.ui.employee.home.none");
		column.add(control(label(house)
				.color(!worker.hasHome() ? MUTED : worker.homeValid() ? QuillThemeData.ColorRole.TEXT : DANGER)));

		column.add(control(label(Component.translatable(
				"laborcraft.ui.employee.task",
				Component.translatable(worker.taskKey())
		)).muted()));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.employee.salary",
				worker.upkeepText(),
				worker.billingPeriodDays()
		))));

		if (worker.missedPayments() > 0) {
			column.add(control(label(Component.translatable(
					"laborcraft.ui.employee.salary.debt",
					worker.debtText(),
					worker.missedPayments()
			)).color(DANGER)));
		}
		return new QuillCardBox(null, column);
	}

	private QuillWidgetBox jobList() {
		QuillListView<JobType> list = new QuillListView<>(
				0, 0, 400, 134, 44,
				this::jobRow,
				(job, index) -> assignJob(job)
		);
		list.replaceItems(List.copyOf(JobTypes.all()));
		return control(list);
	}

	private QuillLayoutNode jobRow(JobType job, int index) {
		boolean current = job.id().equals(worker.jobId());
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(QuillNodes.widget(new ItemStackWidget(jobIcon(job), 16), 16, 16));

		QuillFlexBox text = QuillNodes.column(2);
		text.add(control(label(Component.translatable(job.translationKey())).title()));
		text.add(control(label(Component.translatable(job.translationKey() + ".desc")).muted()));
		row.add(QuillNodes.flexible(text));

		Component action = current
				? Component.translatable("laborcraft.ui.employee.job.current")
				: Component.translatable("laborcraft.ui.employee.job.assign");
		row.add(control(label(action).color(current ? ACCENT : QuillThemeData.ColorRole.TEXT)));
		return row;
	}

	private QuillFlexBox releaseRow() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		QuillButton release = new QuillButton(
				0, 0, 130, 20,
				Component.translatable("laborcraft.ui.employee.home.release"),
				QuillButton.Variant.SECONDARY,
				this::releaseHouse
		);
		release.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.employee.home.release"),
				Component.translatable("laborcraft.ui.employee.home.release.tip"),
				"/" + LaborCraftCommands.HOME_UNASSIGN + " " + workerId()
		));
		row.add(QuillNodes.widget(release, 130, 20));
		return row;
	}

	private QuillWidgetBox houseList() {
		QuillListView<OpenWorkerPayload.HouseSummary> list = new QuillListView<>(
				0, 0, 400, 114, 44,
				this::houseRow,
				(house, index) -> assignHouse(house)
		);
		list.replaceItems(worker.houses());
		return control(list);
	}

	private QuillLayoutNode houseRow(OpenWorkerPayload.HouseSummary house, int index) {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);

		QuillFlexBox text = QuillNodes.column(2);
		text.add(control(label(Component.literal(house.name())).title()));
		text.add(control(label(Component.translatable(
				"laborcraft.ui.employee.house.coords",
				house.x(),
				house.y(),
				house.z()
		)).muted()));

		Component detail;
		QuillThemeData.ColorRole role;
		if (!house.valid()) {
			detail = Component.translatable(
					"laborcraft.ui.employee.house.invalid_short",
					Component.translatable("laborcraft.housing.reason."
							+ (house.failure().isBlank() ? "invalid" : house.failure()))
			);
			role = DANGER;
		}
		else if (house.free() <= 0) {
			detail = Component.translatable("laborcraft.ui.employee.house.full", house.slots());
			role = MUTED;
		}
		else {
			detail = Component.translatable("laborcraft.ui.employee.house.free", house.slots(), house.free());
			role = QuillThemeData.ColorRole.TEXT;
		}
		text.add(control(label(detail).color(role)));
		row.add(QuillNodes.flexible(text));

		Component status;
		QuillThemeData.ColorRole statusRole;
		if (!house.valid()) {
			status = Component.translatable("laborcraft.ui.employee.house.invalid");
			statusRole = DANGER;
		}
		else if (house.current()) {
			status = Component.translatable("laborcraft.ui.employee.job.current");
			statusRole = ACCENT;
		}
		else if (house.free() <= 0) {
			status = Component.translatable("laborcraft.ui.employee.house.no_bed");
			statusRole = MUTED;
		}
		else {
			status = Component.translatable("laborcraft.ui.employee.house.assign");
			statusRole = QuillThemeData.ColorRole.TEXT;
		}
		row.add(control(label(status).color(statusRole)));
		return row;
	}

	private QuillFlexBox actions() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);

		dismissButton = new QuillButton(
				0, 0, 120, 24,
				Component.translatable(confirmDismiss
						? "laborcraft.ui.employee.dismiss.confirm"
						: "laborcraft.ui.employee.dismiss"),
				QuillButton.Variant.DANGER,
				this::dismiss
		);
		dismissButton.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.employee.dismiss"),
				Component.translatable(confirmDismiss
						? "laborcraft.ui.employee.dismiss.confirm.tip"
						: "laborcraft.ui.employee.dismiss.tip"),
				null
		));
		row.add(QuillNodes.widget(dismissButton, 120, 24));

		QuillButton backpack = new QuillButton(
				0, 0, 104, 24,
				Component.translatable("laborcraft.ui.employee.backpack"),
				QuillButton.Variant.SECONDARY,
				this::openBackpack
		);
		backpack.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.employee.backpack"),
				Component.translatable("laborcraft.ui.employee.backpack.tip"),
				"/" + LaborCraftCommands.WORKER_INV_OPEN + " " + workerId()
		));
		row.add(QuillNodes.widget(backpack, 104, 24));

		if (worker.hasDebt()) {
			QuillButton pay = new QuillButton(
					0, 0, 78, 24,
					Component.translatable("laborcraft.ui.employee.pay"),
					QuillButton.Variant.PRIMARY,
					this::payDebt
			);
			pay.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.employee.pay"),
					Component.translatable("laborcraft.ui.employee.pay.tip"),
					"/" + LaborCraftCommands.WORKER_PAY + " " + workerId()
			));
			row.add(QuillNodes.widget(pay, 78, 24));
		}

		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		QuillButton close = new QuillButton(
				0, 0, 90, 24,
				Component.translatable("laborcraft.ui.worker.close"),
				QuillButton.Variant.GHOST,
				this::onClose
		);
		close.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		row.add(QuillNodes.widget(close, 90, 24));
		return row;
	}

	private void assignJob(JobType job) {
		if (job.id().equals(worker.jobId())) {
			return;
		}
		rememberTab(Tab.JOB);
		LaborCraftUiActions.run(
				LaborCraftCommands.WORKER_ASSIGN + " " + workerId() + " " + job.id()
		);
	}

	private void assignHouse(OpenWorkerPayload.HouseSummary house) {
		if (house.current() || !house.valid() || house.free() <= 0) {
			return;
		}
		rememberTab(Tab.HOUSE);
		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_ASSIGN
						+ " " + workerId()
						+ " " + house.freeBedX()
						+ " " + house.freeBedY()
						+ " " + house.freeBedZ()
		);
	}

	private void releaseHouse() {
		rememberTab(Tab.HOUSE);
		LaborCraftUiActions.run(LaborCraftCommands.HOME_UNASSIGN + " " + workerId());
	}

	private void dismiss() {
		if (!confirmDismiss) {
			confirmDismiss = true;
			dismissButton.setMessage(Component.translatable(
					"laborcraft.ui.employee.dismiss.confirm"
			));
			dismissButton.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.employee.dismiss.confirm"),
					Component.translatable("laborcraft.ui.employee.dismiss.confirm.tip"),
					null
			));
			return;
		}
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_DISMISS + " " + workerId());
	}

	private void payDebt() {
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_PAY + " " + workerId());
	}

	private void openBackpack() {
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_INV_OPEN + " " + workerId());
	}

	private static ItemStack jobIcon(JobType job) {
		if (JobTypes.FARMER.id().equals(job.id())) {
			return new ItemStack(Items.WHEAT);
		}
		return new ItemStack(Items.EMERALD);
	}
}

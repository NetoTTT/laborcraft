package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
	private LcButton dismissButton;

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
		int x = panelX + 22;
		int width = panelWidth() - 44;

		addRenderableWidget(tabButton(Tab.OVERVIEW, "laborcraft.ui.employee.tab.overview", x, 100));
		addRenderableWidget(tabButton(Tab.JOB, "laborcraft.ui.employee.tab.job", x + 106, 100));
		addRenderableWidget(tabButton(Tab.HOUSE, "laborcraft.ui.employee.tab.house", x + 212, 100));

		buildActions(x, width);

		switch (tab) {
			case OVERVIEW -> {
			}
			case JOB -> buildJobs(x, width);
			case HOUSE -> buildHouses(x, width);
		}

		addRenderableWidget(closeButton(this::onClose));
	}

	private LcButton tabButton(Tab target, String key, int x, int width) {
		return new LcButton(
				x,
				panelY + 54,
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
		confirmDismiss = false;
		clearWidgets();
		init();
	}

	private void buildActions(int x, int width) {
		int buttonsY = panelY + 310;
		dismissButton = new LcButton(
				x,
				buttonsY,
				120,
				24,
				Component.translatable(
						confirmDismiss
								? "laborcraft.ui.employee.dismiss.confirm"
								: "laborcraft.ui.employee.dismiss"
				),
				LcButton.Variant.DANGER,
				this::dismiss
		);
		dismissButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.employee.dismiss"),
				Component.translatable(
						confirmDismiss
								? "laborcraft.ui.employee.dismiss.confirm.tip"
								: "laborcraft.ui.employee.dismiss.tip"
				),
				null
		));
		addRenderableWidget(dismissButton);

		LcButton backpack = new LcButton(
				x + 128,
				buttonsY,
				104,
				24,
				Component.translatable("laborcraft.ui.employee.backpack"),
				LcButton.Variant.SECONDARY,
				this::openBackpack
		);
		backpack.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.employee.backpack"),
				Component.translatable("laborcraft.ui.employee.backpack.tip"),
				"/" + LaborCraftCommands.WORKER_INV_OPEN + " " + workerId()
		));
		addRenderableWidget(backpack);

		if (worker.hasDebt()) {
			LcButton pay = new LcButton(
					x + 240,
					buttonsY,
					78,
					24,
					Component.translatable("laborcraft.ui.employee.pay"),
					LcButton.Variant.PRIMARY,
					this::payDebt
			);
			pay.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.employee.pay"),
					Component.translatable("laborcraft.ui.employee.pay.tip"),
					"/" + LaborCraftCommands.WORKER_PAY + " " + workerId()
			));
			addRenderableWidget(pay);
		}

		LcButton close = new LcButton(
				x + width - 90,
				buttonsY,
				90,
				24,
				Component.translatable("laborcraft.ui.worker.close"),
				LcButton.Variant.GHOST,
				this::onClose
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		addRenderableWidget(close);
	}

	private void buildJobs(int x, int width) {
		LcScrollList<JobType> list = new LcScrollList<>(
				x,
				panelY + 168,
				width,
				134,
				44,
				this::renderJobRow,
				(job, index) -> assignJob(job)
		);
		list.replaceItems(List.copyOf(JobTypes.all()));
		addRenderableWidget(list);
	}

	private void buildHouses(int x, int width) {
		if (worker.hasHome()) {
			LcButton release = new LcButton(
					x + width - 130,
					panelY + 164,
					130,
					20,
					Component.translatable("laborcraft.ui.employee.home.release"),
					LcButton.Variant.SECONDARY,
					this::releaseHouse
			);
			release.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.employee.home.release"),
					Component.translatable("laborcraft.ui.employee.home.release.tip"),
					"/" + LaborCraftCommands.HOME_UNASSIGN + " " + workerId()
			));
			addRenderableWidget(release);
		}

		LcScrollList<OpenWorkerPayload.HouseSummary> list = new LcScrollList<>(
				x,
				panelY + 188,
				width,
				114,
				44,
				this::renderHouseRow,
				(house, index) -> assignHouse(house)
		);
		list.replaceItems(worker.houses());
		addRenderableWidget(list);
	}

	private void renderJobRow(
			GuiGraphicsExtractor graphics,
			JobType job,
			int index,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			boolean hovered
	) {
		boolean current = job.id().equals(worker.jobId());
		int background = current ? LcTheme.SELECTED_BG : hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG;
		graphics.fill(x, y, x + width, y + height, background);
		LcRender.border(graphics, x, y, width, height, current || hovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);
		graphics.item(jobIcon(job), x + 10, y + (height - 16) / 2);
		graphics.text(font, Component.translatable(job.translationKey()), x + 34, y + 8, LcTheme.TITLE);
		graphics.text(
				font,
				Component.translatable(job.translationKey() + ".desc"),
				x + 34,
				y + 23,
				LcTheme.MUTED
		);

		Component action = current
				? Component.translatable("laborcraft.ui.employee.job.current")
				: Component.translatable("laborcraft.ui.employee.job.assign");
		int color = current ? LcTheme.ACCENT : LcTheme.TEXT;
		graphics.text(font, action, x + width - 12 - font.width(action), y + 8, color);
	}

	private void renderHouseRow(
			GuiGraphicsExtractor graphics,
			OpenWorkerPayload.HouseSummary house,
			int index,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			boolean hovered
	) {
		boolean current = house.current();
		boolean available = house.valid() && house.free() > 0 && !current;
		int background = current ? LcTheme.SELECTED_BG : hovered && available ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG;
		graphics.fill(x, y, x + width, y + height, background);
		int border = current || (hovered && available)
				? LcTheme.ACCENT
				: house.valid() ? LcTheme.BUTTON_BORDER : LcTheme.DANGER;
		LcRender.border(graphics, x, y, width, height, border);

		graphics.text(font, Component.literal(house.name()), x + 10, y + 7, LcTheme.TITLE);
		graphics.text(
				font,
				Component.translatable(
						"laborcraft.ui.employee.house.coords",
						house.x(),
						house.y(),
						house.z()
				),
				x + 10,
				y + 20,
				LcTheme.MUTED
		);

		Component detail;
		int detailColor;
		if (!house.valid()) {
			detail = Component.translatable(
					"laborcraft.ui.employee.house.invalid_short",
					Component.translatable(
							"laborcraft.housing.reason." + (house.failure().isBlank() ? "invalid" : house.failure())
					)
			);
			detailColor = LcTheme.DANGER;
		} else if (house.free() <= 0) {
			detail = Component.translatable("laborcraft.ui.employee.house.full", house.slots());
			detailColor = LcTheme.MUTED;
		} else {
			detail = Component.translatable(
					"laborcraft.ui.employee.house.free",
					house.slots(),
					house.free()
			);
			detailColor = LcTheme.TEXT;
		}
		graphics.text(font, detail, x + 10, y + 32, detailColor);

		Component status;
		int statusColor;
		if (!house.valid()) {
			status = Component.translatable("laborcraft.ui.employee.house.invalid");
			statusColor = LcTheme.DANGER;
		} else if (current) {
			status = Component.translatable("laborcraft.ui.employee.job.current");
			statusColor = LcTheme.ACCENT;
		} else if (house.free() <= 0) {
			status = Component.translatable("laborcraft.ui.employee.house.no_bed");
			statusColor = LcTheme.MUTED;
		} else {
			status = Component.translatable("laborcraft.ui.employee.house.assign");
			statusColor = LcTheme.TEXT;
		}
		graphics.text(font, status, x + width - 12 - font.width(status), y + 7, statusColor);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int width = panelWidth() - 44;
		Component status = worker.jobId() == null || worker.jobId().isBlank()
				? Component.translatable("laborcraft.ui.worker.status.no_job")
				: stateLabel();
		int statusColor = worker.jobId() == null || worker.jobId().isBlank() ? LcTheme.MUTED : LcTheme.ACCENT;
		drawIdentity(graphics, x, panelY + 80, width, 80, status, statusColor);

		switch (tab) {
			case OVERVIEW -> drawOverview(graphics, x, width);
			case JOB -> {
			}
			case HOUSE -> drawHouseHeader(graphics, x, width);
		}
	}

	private void drawOverview(GuiGraphicsExtractor graphics, int x, int width) {
		int jobY = panelY + 168;
		graphics.fill(x, jobY, x + width, jobY + 54, 0x401B232D);
		LcRender.border(graphics, x, jobY, width, 54, LcTheme.DIVIDER);
		graphics.text(font, Component.translatable("laborcraft.ui.employee.current"), x + 12, jobY + 9, LcTheme.MUTED);
		graphics.text(font, jobLabel(), x + 12, jobY + 25, LcTheme.TITLE);
		Component state = Component.translatable("laborcraft.ui.employee.state", stateLabel());
		graphics.text(font, state, x + width - 12 - font.width(state), jobY + 25, LcTheme.TEXT);

		Component detail = worker.jobId() == null || worker.jobId().isBlank()
				? Component.translatable("laborcraft.ui.employee.no_cargo")
				: LcWorkerText.join(List.of(aptitudeLabel(), masteryLabel()), LcWorkerText.SEPARATOR);
		graphics.text(font, detail, x + 12, jobY + 41, LcTheme.MUTED);

		List<Component> boostLabels = LcWorkerText.boosts(worker.boosts());
		if (!boostLabels.isEmpty()) {
			graphics.text(font, LcWorkerText.join(boostLabels, LcWorkerText.SEPARATOR), x, panelY + 228, LcTheme.ACCENT);
		}

		Component house = worker.hasHome()
				? Component.translatable("laborcraft.ui.employee.home.named", worker.homeName())
				: Component.translatable("laborcraft.ui.employee.home.none");
		int houseColor = !worker.hasHome()
				? LcTheme.MUTED
				: worker.homeValid() ? LcTheme.TEXT : LcTheme.DANGER;
		graphics.text(font, house, x, panelY + 244, houseColor);

		Component task = Component.translatable("laborcraft.ui.employee.task", Component.translatable(worker.taskKey()));
		graphics.text(
				font,
				Component.literal(font.plainSubstrByWidth(task.getString(), width)),
				x,
				panelY + 260,
				LcTheme.MUTED
		);

		graphics.text(
				font,
				Component.translatable(
						"laborcraft.ui.employee.salary",
						worker.upkeepText(),
						worker.billingPeriodDays()
				),
				x,
				panelY + 276,
				LcTheme.TEXT
		);
		if (worker.missedPayments() > 0) {
			graphics.text(
					font,
					Component.translatable(
							"laborcraft.ui.employee.salary.debt",
							worker.debtText(),
							worker.missedPayments()
					),
					x,
					panelY + 292,
					LcTheme.DANGER
			);
		}
	}

	private void drawHouseHeader(GuiGraphicsExtractor graphics, int x, int width) {
		graphics.text(font, Component.translatable("laborcraft.ui.employee.house.header"), x, panelY + 168, LcTheme.TEXT);
		if (worker.houses().isEmpty()) {
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.employee.house.empty"),
					x,
					panelY + 196,
					LcTheme.MUTED
			);
		}
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
			dismissButton.setMessage(Component.translatable("laborcraft.ui.employee.dismiss.confirm"));
			dismissButton.tip(LcTooltips.of(
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

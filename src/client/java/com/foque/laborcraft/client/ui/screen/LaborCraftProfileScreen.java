package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.WorkerListFilter;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenProfilePayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftProfileScreen extends LaborCraftScreen {
	private static final ItemStack EYE_ICON = new ItemStack(Items.ENDER_EYE);

	private static WorkerListFilter pendingFilter;

	private final OpenProfilePayload profile;
	private WorkerListFilter filter;
	private LcButton filterButton;
	private LcScrollList<OpenProfilePayload.WorkerSummary> workers;

	public LaborCraftProfileScreen(OpenProfilePayload profile) {
		super(Component.translatable("laborcraft.ui.profile.title"), 460, 360);
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
		int x = panelX + 22;
		int width = panelWidth() - 44;

		workers = new LcScrollList<>(
				x,
				panelY + 142,
				width,
				146,
				52,
				this::drawWorker,
				(summary, index) -> {
				}
		);
		workers.replaceItems(visibleWorkers());
		addRenderableWidget(workers);

		filterButton = new LcButton(
				x + width - 108,
				panelY + 118,
				108,
				18,
				Component.translatable(filter.labelKey()),
				LcButton.Variant.GHOST,
				this::cycleFilter
		);
		filterButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.filter.label"),
				Component.translatable("laborcraft.ui.filter.tip"),
				null
		));
		addRenderableWidget(filterButton);

		addRenderableWidget(closeButton(this::onClose));
	}

	private void cycleFilter() {
		filter = filter.next();
		workers.replaceItems(visibleWorkers());
		filterButton.setMessage(Component.translatable(filter.labelKey()));
	}

	private List<OpenProfilePayload.WorkerSummary> visibleWorkers() {
		return profile.summaries().stream()
				.filter(summary -> filter.matches(summary.state(), summary.hasDebt()))
				.toList();
	}

	private void rememberFilter() {
		pendingFilter = filter;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int width = panelWidth() - 44;
		int gap = 8;
		int statWidth = (width - gap) / 2;
		drawStat(
				graphics,
				x,
				panelY + 64,
				statWidth,
				Component.translatable("laborcraft.ui.profile.money"),
				Component.literal(profile.balanceText())
		);
		drawStat(
				graphics,
				x + statWidth + gap,
				panelY + 64,
				width - statWidth - gap,
				Component.translatable("laborcraft.ui.profile.employees"),
				employeesValue()
		);

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.profile.workers"),
				x,
				panelY + 124,
				LcTheme.TEXT
		);

		if (workers != null && workers.isEmpty()) {
			Component empty = profile.summaries().isEmpty()
					? Component.translatable("laborcraft.ui.profile.empty")
					: Component.translatable("laborcraft.ui.filter.empty");
			graphics.centeredText(
					font,
					empty,
					x + width / 2,
					panelY + 206,
					LcTheme.MUTED
			);
		}

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.profile.highlight.hint"),
				x,
				panelY + 296,
				LcTheme.MUTED
		);
	}

	private Component employeesValue() {
		Component value;
		if (profile.limit() <= 0) {
			value = Component.translatable("laborcraft.ui.profile.employees.unlimited", profile.workers());
		} else {
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

	private void drawStat(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			Component label,
			Component value
	) {
		graphics.fill(x, y, x + width, y + 46, 0x401B232D);
		LcRender.border(graphics, x, y, width, 46, LcTheme.DIVIDER);
		graphics.text(font, label, x + 12, y + 9, LcTheme.MUTED);
		graphics.text(font, value, x + 12, y + 24, LcTheme.TITLE);
	}

	private void drawWorker(
			GuiGraphicsExtractor graphics,
			OpenProfilePayload.WorkerSummary summary,
			int index,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			boolean hovered
	) {
		graphics.fill(
				x,
				y,
				x + width,
				y + height - 2,
				hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG
		);
		LcRender.border(graphics, x, y, width, height - 2, hovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);

		int payWidth = 58;
		int payX = x + width - 202;
		int payY = y + 32;
		int payHeight = 16;
		graphics.item(jobIcon(summary.jobId()), x + 9, y + 8);
		String name = font.plainSubstrByWidth(summary.name(), Math.max(0, width - 124));
		graphics.text(
				font,
				Component.literal(name),
				x + 32,
				y + 7,
				LcWorkerText.rarityColor(summary.rarityId())
		);
		List<Component> status = List.of(
				LcWorkerText.rank(summary.rankId()),
				jobLabel(summary.jobId()),
				stateLabel(summary.state())
		);
		graphics.text(font, LcWorkerText.join(status, LcWorkerText.SEPARATOR), x + 32, y + 22, LcTheme.TEXT);
		int taskLimit = summary.hasDebt() ? Math.max(10, payX - (x + 32) - 6) : width - 44;
		graphics.text(
				font,
				Component.literal(font.plainSubstrByWidth(
						Component.translatable(summary.taskKey()).getString(),
						taskLimit
				)),
				x + 32,
				y + 36,
				LcTheme.MUTED
		);

		int statsX = x + width - 38;
		Component health = Component.translatable(
				"laborcraft.ui.profile.worker.health",
				(int) summary.health(),
				(int) summary.maxHealth()
		);
		Component hunger = Component.translatable("laborcraft.ui.profile.worker.hunger", summary.hunger());
		Component mood = Component.translatable("laborcraft.ui.profile.worker.mood", summary.mood());
		graphics.text(font, health, statsX - font.width(health), y + 7, LcTheme.TEXT);
		graphics.text(font, hunger, statsX - font.width(hunger), y + 22, LcTheme.MUTED);
		graphics.text(font, mood, statsX - font.width(mood), y + 36, LcTheme.MUTED);

		if (summary.hasDebt()) {
			boolean payHovered = mouseX >= payX && mouseX < payX + payWidth
					&& mouseY >= payY && mouseY < payY + payHeight;
			graphics.fill(payX, payY, payX + payWidth, payY + payHeight,
					payHovered ? LcTheme.PRIMARY_BG : LcTheme.BUTTON_BG);
			LcRender.border(graphics, payX, payY, payWidth, payHeight,
					payHovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);
			graphics.centeredText(
					font,
					Component.translatable("laborcraft.ui.employee.pay"),
					payX + payWidth / 2,
					payY + 4,
					payHovered ? LcTheme.TITLE : LcTheme.TEXT
			);
		}

		int eyeX = x + width - 26;
		int eyeY = y + (52 - 2 - 18) / 2;
		boolean eyeHovered = mouseX >= eyeX && mouseX < eyeX + 18 && mouseY >= eyeY && mouseY < eyeY + 18;
		graphics.fill(eyeX, eyeY, eyeX + 18, eyeY + 18, eyeHovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG);
		LcRender.border(graphics, eyeX, eyeY, 18, 18, eyeHovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);
		graphics.item(EYE_ICON, eyeX + 1, eyeY + 1);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (workers != null) {
			int index = workers.rowAt(event.x(), event.y());
			if (index >= 0) {
				OpenProfilePayload.WorkerSummary summary = workers.get(index);
				int rowY = workers.rowTop(index);
				int payX = workers.getX() + workers.rowWidth() - 202;
				int payY = rowY + 32;
				if (summary.hasDebt()
						&& event.x() >= payX && event.x() < payX + 58
						&& event.y() >= payY && event.y() < payY + 16) {
					payWorker(summary);
					return true;
				}

				int eyeX = workers.getX() + workers.rowWidth() - 26;
				int eyeY = rowY + (52 - 2 - 18) / 2;
				if (event.x() >= eyeX && event.x() < eyeX + 18
						&& event.y() >= eyeY && event.y() < eyeY + 18) {
					highlightWorker(summary.id());
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void payWorker(OpenProfilePayload.WorkerSummary summary) {
		rememberFilter();
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_PAY_PROFILE + " " + summary.id());
	}

	private void highlightWorker(java.util.UUID id) {
		Minecraft client = Minecraft.getInstance();
		boolean found = WorldHighlightManager.highlightEntity(id, 0xFF48D7A0, EYE_ICON);
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

		return Component.translatable("laborcraft.ui.worker.state." + state.toLowerCase(Locale.ROOT));
	}

	private static ItemStack jobIcon(String jobId) {
		if (jobId != null && JobTypes.FARMER.id().equals(jobId)) {
			return new ItemStack(Items.WHEAT);
		}

		return new ItemStack(Items.EMERALD);
	}
}

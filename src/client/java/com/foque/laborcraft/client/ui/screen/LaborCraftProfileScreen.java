package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenProfilePayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftProfileScreen extends LaborCraftScreen {
	private final OpenProfilePayload profile;
	private LcScrollList<OpenProfilePayload.WorkerSummary> workers;

	public LaborCraftProfileScreen(OpenProfilePayload profile) {
		super(Component.translatable("laborcraft.ui.profile.title"), 460, 360);
		this.profile = profile;
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
		workers.replaceItems(profile.summaries());
		addRenderableWidget(workers);

		addRenderableWidget(closeButton(this::onClose));
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

		if (profile.summaries().isEmpty()) {
			graphics.centeredText(
					font,
					Component.translatable("laborcraft.ui.profile.empty"),
					x + width / 2,
					panelY + 206,
					LcTheme.MUTED
			);
		}
	}

	private Component employeesValue() {
		if (profile.limit() <= 0) {
			return Component.translatable("laborcraft.ui.profile.employees.unlimited", profile.workers());
		}

		return Component.translatable(
				"laborcraft.ui.profile.employees.value",
				profile.workers(),
				profile.limit()
		);
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

		graphics.item(jobIcon(summary.jobId()), x + 9, y + 8);
		String name = font.plainSubstrByWidth(summary.name(), Math.max(0, width - 124));
		graphics.text(font, Component.literal(name), x + 32, y + 7, LcTheme.TITLE);
		Component job = jobLabel(summary.jobId())
				.copy()
				.append(Component.literal(" · "))
				.append(stateLabel(summary.state()));
		graphics.text(font, job, x + 32, y + 22, LcTheme.TEXT);
		graphics.text(
				font,
				Component.translatable(summary.taskKey()),
				x + 32,
				y + 36,
				LcTheme.MUTED
		);

		int statsX = x + width - 12;
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

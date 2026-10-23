package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenWorkerPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class LaborCraftWorkerScreen extends LaborCraftScreen {
	protected final OpenWorkerPayload worker;

	protected LaborCraftWorkerScreen(OpenWorkerPayload worker, Component title, int panelWidth, int panelHeight) {
		super(title, panelWidth, panelHeight);
		this.worker = worker;
	}

	protected Component workerName() {
		return Component.literal(worker.name());
	}

	protected Component jobLabel() {
		if (worker.jobId() == null || worker.jobId().isBlank()) {
			return Component.translatable("laborcraft.ui.worker.job.none");
		}

		JobType job = JobTypes.get(worker.jobId());
		return job == null
				? Component.translatable("laborcraft.ui.worker.job.unknown", worker.jobId())
				: Component.translatable(job.translationKey());
	}

	protected Component stateLabel() {
		return Component.translatable("laborcraft.ui.worker.state." + worker.state().toLowerCase(Locale.ROOT));
	}

	protected Component healthLabel() {
		return Component.translatable(
				"laborcraft.ui.worker.health",
				(int) worker.health(),
				(int) worker.maxHealth()
		);
	}

	protected String workerId() {
		return worker.workerId().toString();
	}

	protected void drawIdentity(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			int height,
			Component status,
			int statusColor
	) {
		graphics.fill(x, y, x + width, y + height, 0x401B232D);
		LcRender.border(graphics, x, y, width, height, LcTheme.DIVIDER);
		drawPortrait(graphics, x + 12, y + 12, 52);
		graphics.text(font, workerName(), x + 76, y + 13, LcTheme.TITLE);
		graphics.text(font, status, x + 76, y + 29, statusColor);
		graphics.text(font, jobLabel(), x + 76, y + 43, LcTheme.TEXT);
		graphics.text(font, healthLabel(), x + 76, y + 57, LcTheme.MUTED);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.hunger", worker.hunger()),
				x + 76,
				y + 71,
				LcTheme.MUTED
		);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.mood", worker.mood()),
				x + 160,
				y + 71,
				LcTheme.MUTED
		);
	}

	private void drawPortrait(GuiGraphicsExtractor graphics, int x, int y, int size) {
		graphics.fill(x, y, x + size, y + size, 0xFF202A35);
		LcRender.border(graphics, x, y, size, size, LcTheme.ACCENT);
		int center = x + size / 2;
		graphics.fill(center - 7, y + 9, center + 7, y + 23, 0xFF445367);
		graphics.fill(center - 14, y + 27, center + 14, y + size - 8, 0xFF2E3947);
		graphics.fill(center - 14, y + size - 8, center + 14, y + size - 5, LcTheme.ACCENT);
	}

	protected LcButton closeButton(Runnable action) {
		LcButton close = new LcButton(
				panelX + panelWidth() - 30,
				panelY + 8,
				20,
				20,
				Component.literal("X"),
				LcButton.Variant.GHOST,
				action
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		return close;
	}
}

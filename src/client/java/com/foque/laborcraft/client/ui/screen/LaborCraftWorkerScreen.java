package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenWorkerPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

import com.quillui.screen.QuillScreen;

public abstract class LaborCraftWorkerScreen extends QuillScreen {
	protected final OpenWorkerPayload worker;

	protected LaborCraftWorkerScreen(OpenWorkerPayload worker, Component title, int panelWidth, int panelHeight) {
		super(title, panelWidth, panelHeight, LaborCraftTheme.currentData());
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

	protected Component rankLine() {
		return LcWorkerText.join(
				List.of(LcWorkerText.rank(worker.rankId()), LcWorkerText.rarity(worker.rarityId())),
				LcWorkerText.SEPARATOR
		);
	}

	protected Component aptitudeLabel() {
		if (worker.jobId() == null || worker.jobId().isBlank()) {
			return Component.translatable("laborcraft.ui.aptitude.none");
		}

		return LcWorkerText.aptitude(worker.aptitude(), worker.jobId());
	}

	protected Component masteryLabel() {
		if (worker.masteryLevel() <= 0) {
			return Component.translatable("laborcraft.ui.mastery.none");
		}

		return Component.translatable("laborcraft.ui.mastery", worker.masteryLevel(), worker.masteryXp());
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
}

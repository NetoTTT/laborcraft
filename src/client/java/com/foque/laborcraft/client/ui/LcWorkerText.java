package com.foque.laborcraft.client.ui;

import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.worker.WorkerAptitudes;
import com.foque.laborcraft.worker.WorkerBoost;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Textos compartilhados pelas telas para rank, raridade, boosts e aptidao. */
public final class LcWorkerText {
	public static final String SEPARATOR = " \u00b7 ";

	private LcWorkerText() {
	}

	public static Component rank(String rankId) {
		return Component.translatable(WorkerRank.of(rankId).translationKey());
	}

	public static Component rankTitle(String rankId) {
		return Component.translatable(WorkerRank.of(rankId).titleKey());
	}

	public static Component rarity(String rarityId) {
		return Component.translatable(WorkerRarity.of(rarityId).translationKey());
	}

	public static int rarityColor(String rarityId) {
		return WorkerRarity.of(rarityId).color();
	}

	public static Component job(String jobId) {
		if (jobId == null || jobId.isBlank()) {
			return Component.translatable("laborcraft.ui.worker.job.none");
		}

		JobType job = JobTypes.get(jobId);
		return job == null
				? Component.translatable("laborcraft.ui.worker.job.unknown", jobId)
				: Component.translatable(job.translationKey());
	}

	public static List<Component> boosts(List<String> serialized) {
		List<Component> labels = new ArrayList<>(serialized.size());
		for (String raw : serialized) {
			WorkerBoost boost = WorkerBoost.parse(raw);
			if (boost != null) {
				labels.add(Component.translatable(boost.translationKey(), boost.valueText()));
			}
		}

		return labels;
	}

	public static Component join(List<Component> parts, String separator) {
		MutableComponent result = Component.empty();
		for (int index = 0; index < parts.size(); index++) {
			if (index > 0) {
				result.append(Component.literal(separator));
			}

			result.append(parts.get(index));
		}

		return result;
	}

	public static Component aptitude(int stars, String jobId) {
		return Component.translatable(
				"laborcraft.ui.aptitude",
				job(jobId),
				WorkerAptitudes.starsText(stars)
		);
	}
}

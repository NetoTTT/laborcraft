package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.job.JobTypes;
import java.util.List;

public final class WorkerOffers {
	public static final String FARMER_STANDARD = "laborcraft:farmer_standard";

	private WorkerOffers() {
	}

	public static List<WorkerOffer> available() {
		LaborCraftConfig config = LaborCraftConfig.get();
		long price = "free".equalsIgnoreCase(config.acquisition.mode)
				? 0L
				: Math.max(0L, config.acquisition.price);
		long upkeep = Math.max(0L, config.farmer.baseWage);

		return List.of(new WorkerOffer(
				FARMER_STANDARD,
				JobTypes.FARMER.id(),
				"laborcraft.offer.farmer_standard",
				"laborcraft.offer.farmer_standard.desc",
				"",
				price,
				upkeep
		));
	}

	public static WorkerOffer get(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}

		for (WorkerOffer offer : available()) {
			if (offer.id().equals(id)) {
				return offer;
			}
		}

		return null;
	}

	public static WorkerOffer standard() {
		return available().getFirst();
	}
}

package com.foque.laborcraft.item;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOffers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

public record WorkerContractData(
		String offerId,
		String nameKey,
		String name,
		String jobId,
		String boostKey,
		long upkeep,
		int hunger,
		int mood
) {
	public static final Codec<WorkerContractData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("offer", "").forGetter(WorkerContractData::offerId),
			Codec.STRING.optionalFieldOf("name_key", "").forGetter(WorkerContractData::nameKey),
			Codec.STRING.optionalFieldOf("name", "").forGetter(WorkerContractData::name),
			Codec.STRING.optionalFieldOf("job", "").forGetter(WorkerContractData::jobId),
			Codec.STRING.optionalFieldOf("boost", "").forGetter(WorkerContractData::boostKey),
			Codec.LONG.optionalFieldOf("upkeep", 0L).forGetter(WorkerContractData::upkeep),
			Codec.INT.optionalFieldOf("hunger", 20).forGetter(WorkerContractData::hunger),
			Codec.INT.optionalFieldOf("mood", 100).forGetter(WorkerContractData::mood)
	).apply(instance, WorkerContractData::new));

	public static WorkerContractData empty() {
		return new WorkerContractData("", "", "", "", "", 0L, 20, 100);
	}

	public static WorkerContractData fromOffer(WorkerOffer offer) {
		return new WorkerContractData(
				offer.id(),
				offer.nameKey(),
				"",
				offer.jobId(),
				offer.boostKey(),
				offer.upkeep(),
				20,
				100
		);
	}

	public static WorkerContractData fromWorker(LaborCraftWorker worker) {
		WorkerOffer offer = WorkerOffers.get(worker.offerId());
		return new WorkerContractData(
				worker.offerId(),
				offer == null ? "" : offer.nameKey(),
				offer == null ? worker.getName().getString() : "",
				worker.jobId(),
				worker.boostId(),
				worker.upkeep(),
				worker.hunger(),
				worker.mood()
		);
	}

	public Component displayName() {
		if (nameKey != null && !nameKey.isBlank()) {
			return Component.translatable(nameKey);
		}

		if (name != null && !name.isBlank()) {
			return Component.literal(name);
		}

		return Component.translatable("entity.laborcraft.worker");
	}
}

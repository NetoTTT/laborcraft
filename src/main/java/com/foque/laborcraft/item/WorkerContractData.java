package com.foque.laborcraft.item;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.WorkerAptitudes;
import com.foque.laborcraft.worker.WorkerBoost;
import com.foque.laborcraft.worker.WorkerMastery;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * Dados da pessoa carregada pelo Contrato de Trabalhador (contrato v2).
 *
 * <p>A oferta vende a pessoa, nao o cargo: {@code jobId} vazio significa "sem cargo". O rank e a
 * raridade sao sorteados na loja e ficam fixos; boosts e aptidoes sao inatos; maestria e o que a
 * pessoa aprendeu trabalhando em cada cargo.</p>
 */
public record WorkerContractData(
		String offerId,
		String name,
		String legacyNameKey,
		String rankId,
		String rarityId,
		String jobId,
		List<WorkerBoost> boosts,
		Map<String, Integer> aptitudes,
		Map<String, Long> mastery,
		long upkeep,
		int hunger,
		int mood
) {
	public static final Codec<WorkerContractData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("offer", "").forGetter(WorkerContractData::offerId),
			Codec.STRING.optionalFieldOf("name", "").forGetter(WorkerContractData::name),
			Codec.STRING.optionalFieldOf("name_key", "").forGetter(WorkerContractData::legacyNameKey),
			Codec.STRING.optionalFieldOf("rank", "comum").forGetter(WorkerContractData::rankId),
			Codec.STRING.optionalFieldOf("rarity", "comum").forGetter(WorkerContractData::rarityId),
			Codec.STRING.optionalFieldOf("job", "").forGetter(WorkerContractData::jobId),
			Codec.STRING.optionalFieldOf("boosts", "").forGetter(data -> WorkerBoost.join(data.boosts())),
			Codec.STRING.optionalFieldOf("aptitudes", "").forGetter(data -> WorkerAptitudes.serialize(data.aptitudes())),
			Codec.STRING.optionalFieldOf("mastery", "").forGetter(data -> WorkerMastery.join(data.mastery())),
			Codec.LONG.optionalFieldOf("upkeep", 0L).forGetter(WorkerContractData::upkeep),
			Codec.INT.optionalFieldOf("hunger", 20).forGetter(WorkerContractData::hunger),
			Codec.INT.optionalFieldOf("mood", 100).forGetter(WorkerContractData::mood)
	).apply(instance, WorkerContractData::decode));

	public WorkerContractData {
		boosts = List.copyOf(boosts);
		aptitudes = Map.copyOf(aptitudes);
		mastery = Map.copyOf(mastery);
	}

	public static WorkerContractData empty() {
		return new WorkerContractData("", "", "", "comum", "comum", "", List.of(), Map.of(), Map.of(), 0L, 20, 100);
	}

	public static WorkerContractData fromOffer(WorkerOffer offer) {
		return new WorkerContractData(
				offer.id(),
				offer.name(),
				"",
				offer.rank().id(),
				offer.rarity().id(),
				"",
				offer.boosts(),
				offer.aptitudes(),
				Map.of(),
				offer.upkeep(),
				20,
				100
		);
	}

	public static WorkerContractData fromWorker(LaborCraftWorker worker) {
		return new WorkerContractData(
				worker.offerId(),
				worker.getName().getString(),
				"",
				worker.rank().id(),
				worker.rarity().id(),
				worker.jobId(),
				worker.boosts(),
				worker.aptitudes(),
				worker.mastery(),
				worker.upkeep(),
				worker.hunger(),
				worker.mood()
		);
	}

	public WorkerRank rank() {
		return WorkerRank.of(rankId);
	}

	public WorkerRarity rarity() {
		return WorkerRarity.of(rarityId);
	}

	public Component displayName() {
		if (name != null && !name.isBlank()) {
			return Component.literal(name);
		}

		if (legacyNameKey != null && !legacyNameKey.isBlank()) {
			return Component.translatable(legacyNameKey);
		}

		return Component.translatable("entity.laborcraft.worker");
	}

	private static WorkerContractData decode(
			String offerId,
			String name,
			String legacyNameKey,
			String rankId,
			String rarityId,
			String jobId,
			String boosts,
			String aptitudes,
			String mastery,
			long upkeep,
			int hunger,
			int mood
	) {
		return new WorkerContractData(
				offerId,
				name,
				legacyNameKey,
				rankId,
				rarityId,
				jobId,
				WorkerBoost.split(boosts),
				WorkerAptitudes.parse(aptitudes),
				WorkerMastery.parse(mastery),
				upkeep,
				hunger,
				mood
		);
	}
}

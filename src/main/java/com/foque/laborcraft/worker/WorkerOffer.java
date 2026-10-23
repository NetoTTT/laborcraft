package com.foque.laborcraft.worker;

import java.util.List;
import java.util.Map;

/**
 * Uma pessoa disponivel na loja publica de contratacao. A oferta vende a pessoa, nunca um cargo:
 * o cargo e escolhido depois, na tela de Empregado.
 */
public record WorkerOffer(
		String id,
		String name,
		WorkerRank rank,
		WorkerRarity rarity,
		List<WorkerBoost> boosts,
		Map<String, Integer> aptitudes,
		long price,
		long upkeep
) {
	public WorkerOffer {
		boosts = List.copyOf(boosts);
		aptitudes = Map.copyOf(aptitudes);
	}

	public boolean free() {
		return price <= 0L;
	}

	public boolean hasBoost() {
		return !boosts.isEmpty();
	}

	public int aptitudeFor(String jobId) {
		return WorkerAptitudes.starsWithBoosts(aptitudes, jobId, WorkerTraits.aptitudeBonus(boosts));
	}

	public String boostSerialize() {
		return WorkerBoost.join(boosts);
	}

	public String aptitudeSerialize() {
		return WorkerAptitudes.serialize(aptitudes);
	}
}

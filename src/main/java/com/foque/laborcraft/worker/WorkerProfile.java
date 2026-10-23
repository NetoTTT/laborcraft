package com.foque.laborcraft.worker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modelo persistivel do trabalhador (ainda nao ligado ao save da entidade). Separa o que a pessoa
 * e (rank, raridade, boosts, aptidoes) do que ela aprendeu (maestria por cargo).
 */
public record WorkerProfile(
		UUID id,
		UUID owner,
		String name,
		String activeJob,
		WorkerRank rank,
		WorkerRarity rarity,
		List<WorkerBoost> boosts,
		Map<String, Integer> aptitudes,
		Map<String, Long> mastery,
		int schemaVersion
) {
	public static final int CURRENT_SCHEMA = 2;

	public WorkerProfile {
		boosts = List.copyOf(boosts);
		aptitudes = Map.copyOf(aptitudes);
		mastery = Map.copyOf(mastery);
	}

	public static WorkerProfile create(UUID owner, String name) {
		return new WorkerProfile(
				UUID.randomUUID(),
				owner,
				name,
				"",
				WorkerRank.COMUM,
				WorkerRarity.COMUM,
				List.of(),
				Map.of(),
				Map.of(),
				CURRENT_SCHEMA
		);
	}

	public long masteryFor(String jobId) {
		return mastery.getOrDefault(jobId, 0L);
	}

	public int levelFor(String jobId) {
		return Mastery.levelFor(masteryFor(jobId));
	}

	public int aptitudeFor(String jobId) {
		return WorkerAptitudes.starsWithBoosts(aptitudes, jobId, WorkerTraits.aptitudeBonus(boosts));
	}

	public WorkerProfile withMastery(String jobId, long xp) {
		Map<String, Long> updated = new LinkedHashMap<>(mastery);
		updated.put(jobId, Math.max(0L, xp));
		return new WorkerProfile(id, owner, name, activeJob, rank, rarity, boosts, aptitudes, updated, schemaVersion);
	}

	public WorkerProfile withJob(String jobId) {
		return new WorkerProfile(id, owner, name, jobId, rank, rarity, boosts, aptitudes, mastery, schemaVersion);
	}

	public WorkerProfile withOwner(UUID newOwner) {
		return new WorkerProfile(id, newOwner, name, activeJob, rank, rarity, boosts, aptitudes, mastery, schemaVersion);
	}
}

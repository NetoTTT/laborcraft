package com.foque.laborcraft.worker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record WorkerProfile(
		UUID id,
		UUID owner,
		String name,
		String activeJob,
		Map<String, Long> mastery,
		int schemaVersion
) {
	public static final int CURRENT_SCHEMA = 1;

	public WorkerProfile {
		mastery = Map.copyOf(mastery);
	}

	public static WorkerProfile create(UUID owner, String name, String activeJob) {
		return new WorkerProfile(
				UUID.randomUUID(),
				owner,
				name,
				activeJob,
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

	public WorkerProfile withMastery(String jobId, long xp) {
		Map<String, Long> updated = new LinkedHashMap<>(mastery);
		updated.put(jobId, Math.max(0L, xp));
		return new WorkerProfile(id, owner, name, activeJob, updated, schemaVersion);
	}

	public WorkerProfile withJob(String jobId) {
		return new WorkerProfile(id, owner, name, jobId, mastery, schemaVersion);
	}

	public WorkerProfile withOwner(UUID newOwner) {
		return new WorkerProfile(id, newOwner, name, activeJob, mastery, schemaVersion);
	}
}

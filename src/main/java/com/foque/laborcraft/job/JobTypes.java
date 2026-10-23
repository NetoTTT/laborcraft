package com.foque.laborcraft.job;

import com.foque.laborcraft.LaborCraft;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JobTypes {
	public static final JobType FARMER = new JobType("laborcraft:farmer", "job.laborcraft.farmer");

	private static final Map<String, JobType> JOBS = new LinkedHashMap<>();

	private JobTypes() {
	}

	public static void register(JobType job) {
		JobType previous = JOBS.put(job.id(), job);
		if (previous != null) {
			LaborCraft.LOGGER.warn("Emprego '{}' substituido", job.id());
		}
	}

	public static JobType get(String id) {
		return JOBS.get(id);
	}

	public static Collection<JobType> all() {
		return JOBS.values();
	}

	public static void bootstrap() {
		register(FARMER);
	}
}

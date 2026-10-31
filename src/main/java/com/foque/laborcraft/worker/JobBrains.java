package com.foque.laborcraft.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.job.JobTypes;

/** Job id to brain factory. Unknown or empty jobs get a no-op brain. */
public final class JobBrains {
	private static final Map<String, Function<LaborCraftWorker, WorkerBrain>> FACTORIES = new HashMap<>();
	private static final WorkerBrain NONE = () -> {
	};

	private JobBrains() {
	}

	public static void register(String jobId, Function<LaborCraftWorker, WorkerBrain> factory) {
		if (jobId != null && !jobId.isBlank() && factory != null) {
			FACTORIES.put(jobId, factory);
		}
	}

	public static WorkerBrain create(String jobId, LaborCraftWorker worker) {
		if (jobId == null || jobId.isBlank()) {
			return NONE;
		}
		Function<LaborCraftWorker, WorkerBrain> factory = FACTORIES.get(jobId);
		return factory == null ? NONE : factory.apply(worker);
	}

	public static void bootstrap() {
		register(JobTypes.FARMER.id(), FarmerBrain::new);
		register(JobTypes.HERDER.id(), HerderBrain::new);
	}
}

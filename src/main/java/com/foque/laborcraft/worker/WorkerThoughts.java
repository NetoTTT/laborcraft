package com.foque.laborcraft.worker;

import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.List;

/** Small ambient lines that make a working worker feel alive without flooding chat. */
public final class WorkerThoughts {
	private static final List<String> THOUGHTS = List.of(
			"laborcraft.thought.sun",
			"laborcraft.thought.soil",
			"laborcraft.thought.crop",
			"laborcraft.thought.owner",
			"laborcraft.thought.hoe",
			"laborcraft.thought.rain",
			"laborcraft.thought.quiet",
			"laborcraft.thought.rest"
	);

	private WorkerThoughts() {
	}

	public static String pick(LaborCraftWorker worker) {
		if (worker.hunger() <= 4) {
			return "laborcraft.thought.hungry";
		}
		if (worker.mood() <= 25) {
			return "laborcraft.thought.tired";
		}
		return THOUGHTS.get(worker.getRandom().nextInt(THOUGHTS.size()));
	}
}

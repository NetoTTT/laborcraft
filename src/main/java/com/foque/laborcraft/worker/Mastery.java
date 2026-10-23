package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;

public final class Mastery {
	private Mastery() {
	}

	public static int levelFor(long xp) {
		long[] thresholds = LaborCraftConfig.get().worker.masteryLevelThresholds;
		int level = 1;

		for (int index = 0; index < thresholds.length; index++) {
			if (xp >= thresholds[index]) {
				level = index + 1;
			}
		}

		return level;
	}

	public static long xpToNextLevel(long xp) {
		long[] thresholds = LaborCraftConfig.get().worker.masteryLevelThresholds;
		int level = levelFor(xp);

		if (level >= thresholds.length) {
			return 0L;
		}

		return thresholds[level] - xp;
	}
}

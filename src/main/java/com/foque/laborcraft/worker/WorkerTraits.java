package com.foque.laborcraft.worker;

import java.util.List;

/**
 * Soma dos efeitos de rank + boosts. O rank e multiplicativo na base e os boosts entram de forma
 * aditiva por cima, para evitar power creep.
 */
public final class WorkerTraits {
	private WorkerTraits() {
	}

	public static double speed(WorkerRank rank, List<WorkerBoost> boosts) {
		return rank.speedMultiplier() + WorkerBoost.total(boosts, WorkerBoostType.SPEED);
	}

	public static double learning(WorkerRank rank, List<WorkerBoost> boosts) {
		return rank.learningMultiplier() + WorkerBoost.total(boosts, WorkerBoostType.LEARNING);
	}

	public static double stamina(WorkerRank rank, List<WorkerBoost> boosts) {
		return rank.staminaMultiplier() + WorkerBoost.total(boosts, WorkerBoostType.STAMINA);
	}

	public static double upkeep(WorkerRank rank, List<WorkerBoost> boosts) {
		return Math.max(0.1D, rank.upkeepMultiplier() + WorkerBoost.total(boosts, WorkerBoostType.UPKEEP));
	}

	public static double yield(WorkerRank rank, List<WorkerBoost> boosts) {
		return WorkerBoost.total(boosts, WorkerBoostType.YIELD);
	}

	public static double aptitudeBonus(List<WorkerBoost> boosts) {
		return WorkerBoost.total(boosts, WorkerBoostType.APTITUDE);
	}
}

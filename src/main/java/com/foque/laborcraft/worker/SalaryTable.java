package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.config.LaborCraftConfig.JobSettings;
import java.util.List;

/**
 * Central salary calculation. Everything here is configurable through the billing settings:
 * minimum, rank/rarity multipliers, boost cap and the job's billing period.
 */
public final class SalaryTable {
	private SalaryTable() {
	}

	public static long periodWage(
			LaborCraftConfig config,
			JobSettings job,
			WorkerRank rank,
			WorkerRarity rarity,
			List<WorkerBoost> boosts
	) {
		int days = Math.max(1, job == null ? 1 : job.billingPeriodDays);
		return dailyWage(config, job, rank, rarity, boosts) * days;
	}

	public static long dailyWage(
			LaborCraftConfig config,
			JobSettings job,
			WorkerRank rank,
			WorkerRarity rarity,
			List<WorkerBoost> boosts
	) {
		long base = Math.max(0L, job == null ? 0L : job.baseWage);
		base = Math.max(base, Math.max(0, config.billing.minimumDaily));
		if (base <= 0L) {
			return 0L;
		}

		rank = rank == null ? WorkerRank.COMUM : rank;
		rarity = rarity == null ? WorkerRarity.COMUM : rarity;
		double boostsUpkeep = WorkerBoost.total(boosts, WorkerBoostType.UPKEEP);

		if (!config.billing.useSalaryTable) {
			double multiplier = Math.max(0.1D, rank.upkeepMultiplier() + boostsUpkeep);
			return Math.max(0L, Math.round(base * multiplier));
		}

		double multiplier = 1.0D;
		if (config.billing.useRankMultiplier) {
			multiplier *= multiplier(config.billing.rankMultipliers, rank.ordinal(), rank.upkeepMultiplier());
		}
		if (config.billing.useRarityMultiplier) {
			multiplier *= multiplier(config.billing.rarityMultipliers, rarity.ordinal(), 1.0D);
		}
		if (config.billing.useBoostUpkeep) {
			multiplier += boostsUpkeep;
		}

		double cap = Math.max(0.1D, config.billing.boostCap);
		multiplier = Math.max(0.1D, Math.min(cap, multiplier));
		return Math.max(0L, Math.round(base * multiplier));
	}

	private static double multiplier(double[] values, int index, double fallback) {
		if (values == null || index < 0 || index >= values.length || values[index] <= 0.0D) {
			return fallback;
		}
		return values[index];
	}
}

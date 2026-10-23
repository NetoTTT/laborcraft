package com.foque.laborcraft.worker;

import java.util.List;
import java.util.Locale;
import net.minecraft.util.RandomSource;

/**
 * Competencia geral do trabalhador. O rank e sorteado na oferta e fica fixo no contrato;
 * ele nao e conquistado por promocao.
 */
public enum WorkerRank {
	COMUM("comum", 1.00D, 1.00D, 1.00D, 1.00D, 1.00D, 65),
	JUNIOR("junior", 1.02D, 1.10D, 1.05D, 1.10D, 1.25D, 25),
	PLENO("pleno", 1.05D, 1.25D, 1.12D, 1.25D, 1.60D, 8),
	SENIOR("senior", 1.10D, 1.50D, 1.20D, 1.50D, 2.00D, 2);

	private final String id;
	private final double speedMultiplier;
	private final double learningMultiplier;
	private final double staminaMultiplier;
	private final double upkeepMultiplier;
	private final double priceMultiplier;
	private final int weight;

	WorkerRank(
			String id,
			double speedMultiplier,
			double learningMultiplier,
			double staminaMultiplier,
			double upkeepMultiplier,
			double priceMultiplier,
			int weight
	) {
		this.id = id;
		this.speedMultiplier = speedMultiplier;
		this.learningMultiplier = learningMultiplier;
		this.staminaMultiplier = staminaMultiplier;
		this.upkeepMultiplier = upkeepMultiplier;
		this.priceMultiplier = priceMultiplier;
		this.weight = weight;
	}

	public String id() {
		return id;
	}

	public double speedMultiplier() {
		return speedMultiplier;
	}

	public double learningMultiplier() {
		return learningMultiplier;
	}

	public double staminaMultiplier() {
		return staminaMultiplier;
	}

	public double upkeepMultiplier() {
		return upkeepMultiplier;
	}

	public double priceMultiplier() {
		return priceMultiplier;
	}

	public int weight() {
		return weight;
	}

	public String translationKey() {
		return "laborcraft.rank." + id;
	}

	public String titleKey() {
		return "laborcraft.rank." + id + ".title";
	}

	public static WorkerRank of(String id) {
		if (id != null && !id.isBlank()) {
			String normalized = id.toLowerCase(Locale.ROOT);
			for (WorkerRank rank : values()) {
				if (rank.id.equals(normalized)) {
					return rank;
				}
			}
		}

		return COMUM;
	}

	public static WorkerRank roll(RandomSource random) {
		return roll(random, values());
	}

	public static WorkerRank roll(RandomSource random, WorkerRank... allowed) {
		return roll(random, List.of(allowed));
	}

	private static WorkerRank roll(RandomSource random, List<WorkerRank> pool) {
		int total = 0;
		for (WorkerRank rank : pool) {
			total += rank.weight;
		}

		int pick = random.nextInt(Math.max(1, total));
		for (WorkerRank rank : pool) {
			pick -= rank.weight;
			if (pick < 0) {
				return rank;
			}
		}

		return pool.getFirst();
	}
}

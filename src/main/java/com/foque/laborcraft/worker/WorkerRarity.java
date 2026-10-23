package com.foque.laborcraft.worker;

import java.util.Locale;
import net.minecraft.util.RandomSource;

/**
 * Raridade do trabalhador. Ela nao mede competencia (isso e o rank), e sim a quantidade e a
 * qualidade dos boosts inatos que vieram naquela pessoa.
 */
public enum WorkerRarity {
	COMUM("comum", 0, 0, 60, 1.00D, 0xFF9AA6B2),
	INCOMUM("incomum", 1, 0, 25, 1.15D, 0xFF4FAF6E),
	RARO("raro", 2, 1, 12, 1.40D, 0xFF4C8BF5),
	EPICO("epico", 3, 1, 3, 1.80D, 0xFFB862F5);

	private final String id;
	private final int boostCount;
	private final int strongBoosts;
	private final int weight;
	private final double priceMultiplier;
	private final int color;

	WorkerRarity(String id, int boostCount, int strongBoosts, int weight, double priceMultiplier, int color) {
		this.id = id;
		this.boostCount = boostCount;
		this.strongBoosts = strongBoosts;
		this.weight = weight;
		this.priceMultiplier = priceMultiplier;
		this.color = color;
	}

	public String id() {
		return id;
	}

	public int boostCount() {
		return boostCount;
	}

	public int strongBoosts() {
		return strongBoosts;
	}

	public int weight() {
		return weight;
	}

	public double priceMultiplier() {
		return priceMultiplier;
	}

	/** Cor ARGB usada pela UI para destacar a raridade. */
	public int color() {
		return color;
	}

	public String translationKey() {
		return "laborcraft.rarity." + id;
	}

	public static WorkerRarity of(String id) {
		if (id != null && !id.isBlank()) {
			String normalized = id.toLowerCase(Locale.ROOT);
			for (WorkerRarity rarity : values()) {
				if (rarity.id.equals(normalized)) {
					return rarity;
				}
			}
		}

		return COMUM;
	}

	public static WorkerRarity roll(RandomSource random) {
		int total = 0;
		for (WorkerRarity rarity : values()) {
			total += rarity.weight;
		}

		int pick = random.nextInt(Math.max(1, total));
		for (WorkerRarity rarity : values()) {
			pick -= rarity.weight;
			if (pick < 0) {
				return rarity;
			}
		}

		return COMUM;
	}
}

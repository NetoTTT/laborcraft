package com.foque.laborcraft.worker;

import java.util.Locale;
import net.minecraft.util.RandomSource;

/**
 * Tipos de boost inato. Cada boost tem uma versao fraca e uma forte; a raridade define quantos
 * boosts a pessoa recebe e quantos deles sao fortes.
 */
public enum WorkerBoostType {
	SPEED("speed", 0.05D, 0.10D, true),
	STAMINA("stamina", 0.10D, 0.20D, true),
	LEARNING("learning", 0.15D, 0.25D, true),
	YIELD("yield", 0.05D, 0.10D, true),
	UPKEEP("upkeep", -0.10D, -0.20D, true),
	APTITUDE("aptitude", 1.0D, 2.0D, false);

	private final String id;
	private final double weak;
	private final double strong;
	private final boolean percent;

	WorkerBoostType(String id, double weak, double strong, boolean percent) {
		this.id = id;
		this.weak = weak;
		this.strong = strong;
		this.percent = percent;
	}

	public String id() {
		return id;
	}

	public double weak() {
		return weak;
	}

	public double strong() {
		return strong;
	}

	/** true quando o valor e percentual (5 = +5%); false quando e em estrelas de aptidao. */
	public boolean percent() {
		return percent;
	}

	public String translationKey() {
		return "laborcraft.boost." + id;
	}

	public static WorkerBoostType of(String id) {
		if (id != null && !id.isBlank()) {
			String normalized = id.toLowerCase(Locale.ROOT);
			for (WorkerBoostType type : values()) {
				if (type.id.equals(normalized)) {
					return type;
				}
			}
		}

		return null;
	}

	public static WorkerBoostType roll(RandomSource random) {
		return values()[random.nextInt(values().length)];
	}
}

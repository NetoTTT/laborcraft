package com.foque.laborcraft.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Boost inato de uma pessoa. Serializado como {@code tipo=valor} (ex.: {@code speed=0.05}) para
 * caber no componente do contrato e no save da entidade sem precisar de codec proprio.
 */
public record WorkerBoost(WorkerBoostType type, double amount) {
	public static final String SEPARATOR = ";";

	public static WorkerBoost of(WorkerBoostType type, boolean strong) {
		return new WorkerBoost(type, strong ? type.strong() : type.weak());
	}

	public boolean strong() {
		return Math.abs(Math.abs(amount) - Math.abs(type.strong())) < 1.0E-6;
	}

	public String translationKey() {
		return type.translationKey();
	}

	public String valueText() {
		if (type.percent()) {
			int percent = (int) Math.round(amount * 100.0D);
			return (percent > 0 ? "+" : "") + percent + "%";
		}

		int stars = (int) Math.round(amount);
		return (stars > 0 ? "+" : "") + stars;
	}

	public String serialize() {
		return type.id() + "=" + amount;
	}

	public static WorkerBoost parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		int separator = raw.indexOf('=');
		if (separator <= 0) {
			return null;
		}

		WorkerBoostType type = WorkerBoostType.of(raw.substring(0, separator));
		if (type == null) {
			return null;
		}

		try {
			return new WorkerBoost(type, Double.parseDouble(raw.substring(separator + 1)));
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static String join(List<WorkerBoost> boosts) {
		StringBuilder builder = new StringBuilder();
		for (WorkerBoost boost : boosts) {
			if (boost == null) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(SEPARATOR);
			}

			builder.append(boost.serialize());
		}

		return builder.toString();
	}

	public static List<WorkerBoost> split(String raw) {
		List<WorkerBoost> boosts = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return boosts;
		}

		for (String part : raw.split(SEPARATOR)) {
			WorkerBoost boost = parse(part.trim());
			if (boost != null) {
				boosts.add(boost);
			}
		}

		return boosts;
	}

	public static double total(List<WorkerBoost> boosts, WorkerBoostType type) {
		double total = 0.0D;
		for (WorkerBoost boost : boosts) {
			if (boost != null && boost.type() == type) {
				total += boost.amount();
			}
		}

		return total;
	}

	public static String describe(WorkerBoostType type, double amount) {
		return type.id() + "=" + String.format(Locale.ROOT, "%.3f", amount).replace(',', '.');
	}
}

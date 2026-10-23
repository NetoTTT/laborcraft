package com.foque.laborcraft.worker;

import java.util.LinkedHashMap;
import java.util.Map;

/** Serializacao simples da maestria por cargo ({@code cargo=xp;cargo=xp}). */
public final class WorkerMastery {
	private WorkerMastery() {
	}

	public static String join(Map<String, Long> mastery) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, Long> entry : mastery.entrySet()) {
			if (!builder.isEmpty()) {
				builder.append(';');
			}

			builder.append(entry.getKey()).append('=').append(entry.getValue());
		}

		return builder.toString();
	}

	public static Map<String, Long> parse(String raw) {
		Map<String, Long> mastery = new LinkedHashMap<>();
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}

		for (String part : raw.split(";")) {
			int separator = part.indexOf('=');
			if (separator <= 0) {
				continue;
			}

			try {
				mastery.put(
						part.substring(0, separator).trim(),
						Math.max(0L, Long.parseLong(part.substring(separator + 1).trim()))
				);
			} catch (NumberFormatException ignored) {
				// entrada invalida: ignora
			}
		}

		return Map.copyOf(mastery);
	}
}

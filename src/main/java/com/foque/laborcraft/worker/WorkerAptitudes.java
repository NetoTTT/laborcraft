package com.foque.laborcraft.worker;

import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Aptidao e a natureza interna da pessoa para uma area (1 a 5 estrelas). Ela influencia o ganho
 * de maestria; maestria e o que ele aprendeu trabalhando naquele cargo.
 */
public final class WorkerAptitudes {
	public static final int MIN_STARS = 1;
	public static final int MAX_STARS = 5;

	private static final int[] WEIGHTS = {10, 25, 40, 20, 5};

	private WorkerAptitudes() {
	}

	public static Map<String, Integer> roll(RandomSource random) {
		Map<String, Integer> aptitudes = new LinkedHashMap<>();
		for (JobType job : JobTypes.all()) {
			aptitudes.put(job.id(), rollStars(random));
		}

		if (aptitudes.isEmpty()) {
			aptitudes.put(JobTypes.FARMER.id(), 3);
		}

		return Map.copyOf(aptitudes);
	}

	public static int rollStars(RandomSource random) {
		int total = 0;
		for (int weight : WEIGHTS) {
			total += weight;
		}

		int pick = random.nextInt(Math.max(1, total));
		for (int index = 0; index < WEIGHTS.length; index++) {
			pick -= WEIGHTS[index];
			if (pick < 0) {
				return MIN_STARS + index;
			}
		}

		return 3;
	}

	public static int stars(Map<String, Integer> aptitudes, String jobId) {
		if (aptitudes == null || jobId == null) {
			return 0;
		}

		Integer stars = aptitudes.get(jobId);
		return stars == null ? 0 : Math.clamp(stars, 0, MAX_STARS);
	}

	public static int starsWithBoosts(Map<String, Integer> aptitudes, String jobId, double bonusStars) {
		return Math.clamp(stars(aptitudes, jobId) + (int) Math.round(bonusStars), MIN_STARS, MAX_STARS);
	}

	public static String starsText(int stars) {
		int clamped = Math.clamp(stars, 0, MAX_STARS);
		StringBuilder builder = new StringBuilder(MAX_STARS * 2);
		for (int index = 1; index <= MAX_STARS; index++) {
			builder.append(index <= clamped ? '\u2605' : '\u2606');
		}

		return builder.toString();
	}

	public static String serialize(Map<String, Integer> aptitudes) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, Integer> entry : aptitudes.entrySet()) {
			if (!builder.isEmpty()) {
				builder.append(';');
			}

			builder.append(entry.getKey()).append('=').append(entry.getValue());
		}

		return builder.toString();
	}

	public static Map<String, Integer> parse(String raw) {
		Map<String, Integer> aptitudes = new LinkedHashMap<>();
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}

		for (String part : raw.split(";")) {
			int separator = part.indexOf('=');
			if (separator <= 0) {
				continue;
			}

			try {
				aptitudes.put(
						part.substring(0, separator).trim(),
						Math.clamp(Integer.parseInt(part.substring(separator + 1).trim()), MIN_STARS, MAX_STARS)
				);
			} catch (NumberFormatException ignored) {
				// entrada invalida: ignora
			}
		}

		return Map.copyOf(aptitudes);
	}
}

package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;

/**
 * Farmer rules stored per station, so two fields can use different soil/compost policies. Wages,
 * tiers and billing stay global (economy), not here.
 */
public final class FarmerSettings {
	public boolean useBoneMeal = true;
	public boolean useComposter = true;
	public int seedSurplusForCompost = 64;
	public String waterPolicy = "prefer";

	public static FarmerSettings defaults() {
		LaborCraftConfig.JobSettings global = LaborCraftConfig.get().job("laborcraft:farmer");
		FarmerSettings settings = new FarmerSettings();
		settings.useBoneMeal = global.useBoneMeal;
		settings.useComposter = global.useComposter;
		settings.seedSurplusForCompost = global.seedSurplusForCompost;
		settings.waterPolicy = normalizePolicy(global.waterPolicy);
		return settings;
	}

	public FarmerSettings copy() {
		FarmerSettings copy = new FarmerSettings();
		copy.useBoneMeal = useBoneMeal;
		copy.useComposter = useComposter;
		copy.seedSurplusForCompost = seedSurplusForCompost;
		copy.waterPolicy = waterPolicy;
		return copy;
	}

	public void clamp() {
		seedSurplusForCompost = Math.max(0, Math.min(1024, seedSurplusForCompost));
		waterPolicy = normalizePolicy(waterPolicy);
	}

	public boolean set(String key, String value) {
		String normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "usebonemeal" -> useBoneMeal = parseBoolean(value, useBoneMeal);
			case "usecomposter" -> useComposter = parseBoolean(value, useComposter);
			case "seedsurplusforcompost" -> {
				seedSurplusForCompost = parseInt(value, seedSurplusForCompost);
				yield true;
			}
			case "waterpolicy" -> {
				waterPolicy = normalizePolicy(value);
				yield true;
			}
			default -> false;
		};
	}

	public static String normalizePolicy(String policy) {
		String normalized = policy == null ? "prefer" : policy.toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "require", "ignore" -> normalized;
			default -> "prefer";
		};
	}

	private static boolean parseBoolean(String value, boolean fallback) {
		if (value == null) {
			return fallback;
		}
		String normalized = value.toLowerCase(java.util.Locale.ROOT);
		if (normalized.startsWith("t") || normalized.equals("1") || normalized.equals("on")) {
			return true;
		}
		if (normalized.startsWith("f") || normalized.equals("0") || normalized.equals("off")) {
			return false;
		}
		return fallback;
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}
}

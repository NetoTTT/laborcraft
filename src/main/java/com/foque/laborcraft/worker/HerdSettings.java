package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;

/**
 * Herder rules stored per station. New stations copy the server defaults; players can then tune
 * each pen independently (caps, products, delivery threshold, auto tool).
 */
public final class HerdSettings {
	public boolean collectDrops = true;
	public boolean shearSheep = true;
	public boolean milkCows = false;
	public boolean breeding = true;
	public boolean autoTools = true;
	public int maxPerSpecies = 8;
	public int minPerSpecies = 3;
	public int produceDeliverThreshold = 16;

	public static HerdSettings defaults() {
		LaborCraftConfig.HerderSettings global = LaborCraftConfig.get().herder;
		HerdSettings settings = new HerdSettings();
		settings.collectDrops = global.collectDrops;
		settings.shearSheep = global.shearSheep;
		settings.milkCows = global.milkCows;
		settings.breeding = global.breeding;
		settings.autoTools = global.autoTools;
		settings.maxPerSpecies = global.maxPerSpecies;
		settings.minPerSpecies = global.minPerSpecies;
		settings.produceDeliverThreshold = global.produceDeliverThreshold;
		return settings;
	}

	public HerdSettings copy() {
		HerdSettings copy = new HerdSettings();
		copy.collectDrops = collectDrops;
		copy.shearSheep = shearSheep;
		copy.milkCows = milkCows;
		copy.breeding = breeding;
		copy.autoTools = autoTools;
		copy.maxPerSpecies = maxPerSpecies;
		copy.minPerSpecies = minPerSpecies;
		copy.produceDeliverThreshold = produceDeliverThreshold;
		return copy;
	}

	public void clamp() {
		maxPerSpecies = Math.max(1, Math.min(64, maxPerSpecies));
		minPerSpecies = Math.max(0, Math.min(maxPerSpecies, minPerSpecies));
		produceDeliverThreshold = Math.max(1, Math.min(256, produceDeliverThreshold));
	}

	/** Applies one UI/command key; returns false when the key is unknown. */
	public boolean set(String key, String value) {
		String normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "collectdrops" -> collectDrops = parseBoolean(value, collectDrops);
			case "shearsheep" -> shearSheep = parseBoolean(value, shearSheep);
			case "milkcows" -> milkCows = parseBoolean(value, milkCows);
			case "breeding" -> breeding = parseBoolean(value, breeding);
			case "autotools" -> autoTools = parseBoolean(value, autoTools);
			case "maxperspecies" -> {
				maxPerSpecies = parseInt(value, maxPerSpecies);
				yield true;
			}
			case "minperspecies" -> {
				minPerSpecies = parseInt(value, minPerSpecies);
				yield true;
			}
			case "producedeliverthreshold" -> {
				produceDeliverThreshold = parseInt(value, produceDeliverThreshold);
				yield true;
			}
			default -> false;
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
		}
		catch (NumberFormatException exception) {
			return fallback;
		}
	}
}

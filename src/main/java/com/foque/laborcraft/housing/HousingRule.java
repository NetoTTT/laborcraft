package com.foque.laborcraft.housing;

import com.foque.laborcraft.config.LaborCraftConfig;

/** One pluggable housing requirement. The first failed rule becomes the report reason. */
public interface HousingRule {
	/** Short reason code translated by the UI/commands, e.g. {@code no_door}. */
	String reason();

	boolean passes(HousingScan scan, LaborCraftConfig config);
}

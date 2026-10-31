package com.foque.laborcraft.housing;

import java.util.List;

/** Result of one {@link HousingRule} for a scanned house. */
public record HousingRuleResult(String reason, boolean passed) {
}

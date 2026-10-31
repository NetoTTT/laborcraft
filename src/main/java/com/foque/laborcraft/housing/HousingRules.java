package com.foque.laborcraft.housing;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered housing rules. Consumers can {@link #register} extra checks without editing the scan or
 * the report; the first failed rule becomes the legacy {@code failureReason}.
 */
public final class HousingRules {
	private static final List<HousingRule> RULES = new CopyOnWriteArrayList<>(List.of(
			rule("too_small", (scan, config) -> {
				int minimum = Math.max(3, config.housing.minInteriorSize);
				return scan.width() >= minimum && scan.depth() >= minimum;
			}),
			rule("no_door", (scan, config) -> !config.housing.requireDoor || scan.doors() > 0),
			rule("no_light", (scan, config) -> !config.housing.requireLight || scan.lightSource())
	));

	private HousingRules() {
	}

	public static void register(HousingRule rule) {
		if (rule != null) {
			RULES.add(rule);
		}
	}

	public static List<HousingRuleResult> evaluate(HousingScan scan, LaborCraftConfig config) {
		List<HousingRuleResult> results = new ArrayList<>(RULES.size());
		for (HousingRule rule : RULES) {
			boolean passed;
			try {
				passed = rule.passes(scan, config);
			}
			catch (RuntimeException exception) {
				passed = false;
			}
			results.add(new HousingRuleResult(rule.reason(), passed));
		}
		return List.copyOf(results);
	}

	public static String firstFailure(List<HousingRuleResult> results) {
		for (HousingRuleResult result : results) {
			if (!result.passed()) {
				return result.reason();
			}
		}
		return "";
	}

	private static HousingRule rule(String reason, RuleCheck check) {
		return new HousingRule() {
			@Override
			public String reason() {
				return reason;
			}

			@Override
			public boolean passes(HousingScan scan, LaborCraftConfig config) {
				return check.passes(scan, config);
			}
		};
	}

	@FunctionalInterface
	private interface RuleCheck {
		boolean passes(HousingScan scan, LaborCraftConfig config);
	}
}

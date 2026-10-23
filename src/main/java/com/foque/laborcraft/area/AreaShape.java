package com.foque.laborcraft.area;

import java.util.Locale;

public enum AreaShape {
	SQUARE,
	CIRCLE;

	public static AreaShape parse(String value) {
		if (value == null) {
			return SQUARE;
		}

		return switch (value.toLowerCase(Locale.ROOT)) {
			case "circle", "round" -> CIRCLE;
			default -> SQUARE;
		};
	}

	public boolean contains(int dx, int dz, int radius) {
		return switch (this) {
			case SQUARE -> Math.abs(dx) <= radius && Math.abs(dz) <= radius;
			case CIRCLE -> (long) dx * dx + (long) dz * dz <= (long) radius * radius;
		};
	}
}

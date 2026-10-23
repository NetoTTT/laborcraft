package com.foque.laborcraft.area;

public record WorkArea(
		String dimension,
		int x,
		int y,
		int z,
		int radius,
		int verticalRadius,
		AreaShape shape
) {
	public static final int DEFAULT_VERTICAL_RADIUS = 4;

	public boolean contains(int blockX, int blockY, int blockZ) {
		if (Math.abs(blockY - y) > verticalRadius) {
			return false;
		}

		return shape.contains(blockX - x, blockZ - z, radius);
	}

	public WorkArea withRadius(int newRadius) {
		return new WorkArea(dimension, x, y, z, newRadius, verticalRadius, shape);
	}

	public WorkArea expand(int extraRadius, int extraVertical) {
		return new WorkArea(
				dimension,
				x,
				y,
				z,
				radius + extraRadius,
				verticalRadius + extraVertical,
				shape
		);
	}

	public int horizontalDiameter() {
		return radius * 2 + 1;
	}

	public int verticalDiameter() {
		return verticalRadius * 2 + 1;
	}
}

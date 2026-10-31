package com.foque.laborcraft.area;

/** One horizontal run of interior pen cells. */
public record EnclosureSpan(int z, int minX, int maxX) {
	public boolean contains(int x, int z) {
		return this.z == z && x >= minX && x <= maxX;
	}
}

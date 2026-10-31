package com.foque.laborcraft.area;

import net.minecraft.core.BlockPos;

/** Result of scanning an open-air enclosure (pen, pasture, yard). */
public record EnclosureScan(
		boolean valid,
		int area,
		int width,
		int depth,
		int gates,
		int boundaryBlocks,
		String failureReason,
		BlockPos min,
		BlockPos max
) {
	public static EnclosureScan invalid(String reason) {
		return new EnclosureScan(false, 0, 0, 0, 0, 0, reason, BlockPos.ZERO, BlockPos.ZERO);
	}
}

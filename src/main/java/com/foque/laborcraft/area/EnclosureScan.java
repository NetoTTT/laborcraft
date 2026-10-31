package com.foque.laborcraft.area;

import java.util.List;
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
		BlockPos max,
		BlockPos seed,
		List<EnclosureSpan> spans,
		List<BlockPos> gatePositions
) {
	public EnclosureScan {
		spans = spans == null ? List.of() : List.copyOf(spans);
		gatePositions = gatePositions == null ? List.of() : List.copyOf(gatePositions);
	}

	public static EnclosureScan invalid(String reason) {
		return new EnclosureScan(
				false,
				0,
				0,
				0,
				0,
				0,
				reason,
				BlockPos.ZERO,
				BlockPos.ZERO,
				BlockPos.ZERO,
				List.of(),
				List.of()
		);
	}
}

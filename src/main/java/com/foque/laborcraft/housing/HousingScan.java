package com.foque.laborcraft.housing;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Raw, rule-independent measurements of a candidate house. Measurement failures (not a bed, no
 * space, open sky, too large) are carried in {@link #failureReason}; the rules only run when it is
 * blank.
 */
public record HousingScan(
		boolean enclosed,
		int volume,
		int width,
		int depth,
		int doors,
		List<BlockPos> beds,
		int comfortBlocks,
		boolean lightSource,
		long checkedAtTick,
		String failureReason,
		BlockPos min,
		BlockPos max,
		int floorY,
		int ceilingY
) {
	public HousingScan {
		beds = List.copyOf(beds);
		failureReason = failureReason == null ? "" : failureReason;
	}

	public static HousingScan invalid(String reason, long checkedAtTick) {
		return new HousingScan(
				false,
				0,
				0,
				0,
				0,
				List.of(),
				0,
				false,
				checkedAtTick,
				reason,
				BlockPos.ZERO,
				BlockPos.ZERO,
				0,
				0
		);
	}

	public boolean failed() {
		return !failureReason.isBlank();
	}

	public int area() {
		return width * depth;
	}
}

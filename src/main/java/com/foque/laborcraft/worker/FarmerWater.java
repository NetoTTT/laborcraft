package com.foque.laborcraft.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single hydration predicate used by the farmer AI and the target scan. Vanilla rule: water counts
 * when it is within 4 blocks horizontally (Chebyshev) at the farmland's Y or one block above.
 * {@link FluidTags#WATER} covers source/flowing and waterlogged blocks. Rain and an already positive
 * moisture value also count. `moisture` alone is not trusted because it only updates on random ticks.
 */
public final class FarmerWater {
	private FarmerWater() {
	}

	public static boolean isHydrated(ServerLevel level, BlockPos farmland) {
		BlockState state = level.getBlockState(farmland);
		if (state.is(Blocks.FARMLAND) && state.getValue(FarmlandBlock.MOISTURE) > 0) {
			return true;
		}
		if (level.isRainingAt(farmland.above())) {
			return true;
		}

		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				for (int dy = 0; dy <= 1; dy++) {
					if (level.getFluidState(farmland.offset(dx, dy, dz)).is(FluidTags.WATER)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Pre-computes hydrated cells for a zone so the scan is O(1) per candidate instead of scanning
	 * a 9x9 area for every block. Coordinates outside the bounds are ignored.
	 */
	public static boolean[] mask(ServerLevel level, int[] bounds) {
		int width = bounds[1] - bounds[0] + 1;
		int depth = bounds[3] - bounds[2] + 1;
		int height = bounds[5] - bounds[4] + 1;
		boolean[] hydrated = new boolean[width * height * depth];

		for (int x = bounds[0] - 4; x <= bounds[1] + 4; x++) {
			for (int z = bounds[2] - 4; z <= bounds[3] + 4; z++) {
				for (int y = bounds[4] - 1; y <= bounds[5] + 1; y++) {
					BlockPos water = new BlockPos(x, y, z);
					if (!level.hasChunkAt(water) || !level.getFluidState(water).is(FluidTags.WATER)) {
						continue;
					}

					for (int dx = -4; dx <= 4; dx++) {
						int targetX = x + dx;
						if (targetX < bounds[0] || targetX > bounds[1]) {
							continue;
						}
						for (int dz = -4; dz <= 4; dz++) {
							int targetZ = z + dz;
							if (targetZ < bounds[2] || targetZ > bounds[3]) {
								continue;
							}
							mark(hydrated, bounds, targetX, y, targetZ);
							mark(hydrated, bounds, targetX, y - 1, targetZ);
						}
					}
				}
			}
		}

		return hydrated;
	}

	public static boolean maskAt(boolean[] mask, int[] bounds, BlockPos pos) {
		int width = bounds[1] - bounds[0] + 1;
		int depth = bounds[3] - bounds[2] + 1;
		if (pos.getX() < bounds[0] || pos.getX() > bounds[1]
				|| pos.getY() < bounds[4] || pos.getY() > bounds[5]
				|| pos.getZ() < bounds[2] || pos.getZ() > bounds[3]) {
			return false;
		}

		int index = ((pos.getY() - bounds[4]) * depth + (pos.getZ() - bounds[2])) * width
				+ (pos.getX() - bounds[0]);
		return mask[index];
	}

	private static void mark(boolean[] mask, int[] bounds, int x, int y, int z) {
		if (x < bounds[0] || x > bounds[1] || y < bounds[4] || y > bounds[5]
				|| z < bounds[2] || z > bounds[3]) {
			return;
		}
		int width = bounds[1] - bounds[0] + 1;
		int depth = bounds[3] - bounds[2] + 1;
		int index = ((y - bounds[4]) * depth + (z - bounds[2])) * width + (x - bounds[0]);
		mask[index] = true;
	}
}

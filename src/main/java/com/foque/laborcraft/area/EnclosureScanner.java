package com.foque.laborcraft.area;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scans an open-air pen from a point inside it.
 *
 * <p>This is a plan-view flood: the fence line is the boundary, not the floor. Each column is
 * tested against a vertical band around the clicked Y; a fence, wall or closed gate anywhere in
 * that band blocks the column. Terrain height, steps, water and roofs are irrelevant, so a hole in
 * the fence can never be closed by the ground. The map overlay and the scan therefore agree on
 * what counts as a pen: the same fence blocks.</p>
 */
public final class EnclosureScanner {
	private static final Direction[] HORIZONTAL = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.EAST,
			Direction.WEST
	};
	private static final int MIN_BOUNDARY = 4;
	private static final int BAND_UP = 8;
	private static final int BAND_DOWN = 4;
	private static final int SEED_SEARCH = 2;

	private EnclosureScanner() {
	}

	public static EnclosureScan scan(ServerLevel level, BlockPos origin, LaborCraftConfig config) {
		BlockPos seed = findSeed(level, origin);
		if (seed == null) {
			return EnclosureScan.invalid("blocked");
		}
		int baseY = seed.getY();
		int originX = seed.getX();
		int originZ = seed.getZ();

		int minArea = Math.max(4, config.enclosure.minArea);
		int maxArea = Math.max(minArea, config.enclosure.maxArea);
		int maxRadius = Math.max(4, config.enclosure.maxScanRadius);

		Set<Long> columns = new HashSet<>();
		ArrayDeque<long[]> queue = new ArrayDeque<>();
		columns.add(pack(originX, originZ));
		queue.add(new long[] {originX, originZ});

		Set<BlockPos> gates = new HashSet<>();
		Set<BlockPos> boundary = new HashSet<>();

		while (!queue.isEmpty()) {
			long[] current = queue.poll();
			int currentX = (int) current[0];
			int currentZ = (int) current[1];
			if (Math.abs(currentX - originX) > maxRadius || Math.abs(currentZ - originZ) > maxRadius) {
				return EnclosureScan.invalid("not_enclosed");
			}

			for (Direction direction : HORIZONTAL) {
				int nextX = currentX + direction.getStepX();
				int nextZ = currentZ + direction.getStepZ();
				long key = pack(nextX, nextZ);
				if (columns.contains(key)) {
					continue;
				}
				if (isBoundary(level, nextX, nextZ, baseY, gates, boundary)) {
					continue;
				}
				if (columns.size() >= maxArea) {
					return EnclosureScan.invalid("not_enclosed");
				}
				columns.add(key);
				queue.add(new long[] {nextX, nextZ});
			}
		}

		int area = columns.size();
		if (area < minArea) {
			return EnclosureScan.invalid("too_small", area);
		}
		if (boundary.size() < MIN_BOUNDARY) {
			return EnclosureScan.invalid("no_fence");
		}
		if (gates.isEmpty()) {
			return EnclosureScan.invalid("no_gate");
		}

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (long key : columns) {
			int x = unpackX(key);
			int z = unpackZ(key);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}

		List<EnclosureSpan> spans = new ArrayList<>();
		for (int z = minZ; z <= maxZ; z++) {
			int runStart = -1;
			for (int x = minX; x <= maxX + 1; x++) {
				boolean inside = x <= maxX && columns.contains(pack(x, z));
				if (inside && runStart < 0) {
					runStart = x;
				}
				else if (!inside && runStart >= 0) {
					spans.add(new EnclosureSpan(z, runStart, x - 1));
					runStart = -1;
				}
			}
		}

		return new EnclosureScan(
				true,
				area,
				maxX - minX + 1,
				maxZ - minZ + 1,
				gates.size(),
				boundary.size(),
				"",
				new BlockPos(minX, baseY, minZ),
				new BlockPos(maxX, baseY, maxZ),
				new BlockPos(originX, baseY, originZ),
				spans,
				List.copyOf(gates)
		);
	}

	/**
	 * Finds a free column near the click. Clicks often land on the fence itself, and starting the
	 * flood on a boundary column would report a one-column pen.
	 */
	private static BlockPos findSeed(ServerLevel level, BlockPos origin) {
		int baseY = origin.getY();
		for (int radius = 0; radius <= SEED_SEARCH; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					if (isBoundary(level, x, z, baseY, null, null)) {
						continue;
					}
					return new BlockPos(x, baseY, z);
				}
			}
		}
		return null;
	}

	/**
	 * Whether a column belongs to the pen boundary in the plan view. Fences, walls and closed gates
	 * block; an open gate is recorded but remains passable.
	 */
	private static boolean isBoundary(
			ServerLevel level,
			int x,
			int z,
			int baseY,
			Set<BlockPos> gates,
			Set<BlockPos> boundary
	) {
		for (int y = baseY + BAND_UP; y >= baseY - BAND_DOWN; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof FenceGateBlock) {
				if (gates != null) {
					gates.add(pos);
				}
				if (state.hasProperty(FenceGateBlock.OPEN) && state.getValue(FenceGateBlock.OPEN)) {
					continue;
				}
				if (boundary != null) {
					boundary.add(pos);
				}
				return true;
			}
			if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
				if (boundary != null) {
					boundary.add(pos);
				}
				return true;
			}
		}
		return false;
	}

	private static long pack(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	private static int unpackX(long key) {
		return (int) (key >> 32);
	}

	private static int unpackZ(long key) {
		return (int) key;
	}
}

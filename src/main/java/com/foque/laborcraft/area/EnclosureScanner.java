package com.foque.laborcraft.area;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * <p>This is a 2.5D flood: each visited column carries its own floor Y. Neighbouring columns may
 * step up or down by one block, so platforms, stairs and terraced terrain work without brute-force
 * Y offsets. A column is a boundary when a fence, wall or gate occupies its feet/head space, or
 * when it has no walkable floor within one step.</p>
 */
public final class EnclosureScanner {
	private static final Direction[] HORIZONTAL = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.EAST,
			Direction.WEST
	};
	private static final int MIN_BOUNDARY = 4;
	private static final int START_SCAN_UP = 8;
	private static final int START_SCAN_DOWN = 4;
	private static final int SEED_SEARCH = 2;

	private EnclosureScanner() {
	}

	public static EnclosureScan scan(ServerLevel level, BlockPos origin, LaborCraftConfig config) {
		BlockPos seed = findSeed(level, origin);
		if (seed == null) {
			return EnclosureScan.invalid("blocked");
		}
		int startFloor = seed.getY();
		int originX = seed.getX();
		int originZ = seed.getZ();

		int minArea = Math.max(4, config.enclosure.minArea);
		int maxArea = Math.max(minArea, config.enclosure.maxArea);
		int maxRadius = Math.max(4, config.enclosure.maxScanRadius);

		Map<Long, Integer> floors = new HashMap<>();
		ArrayDeque<Column> queue = new ArrayDeque<>();
		floors.put(pack(originX, originZ), startFloor);
		queue.add(new Column(originX, originZ, startFloor));

		Set<BlockPos> gates = new HashSet<>();
		Set<BlockPos> boundary = new HashSet<>();

		while (!queue.isEmpty()) {
			Column current = queue.poll();
			if (Math.abs(current.x() - originX) > maxRadius
					|| Math.abs(current.z() - originZ) > maxRadius) {
				return EnclosureScan.invalid("not_enclosed");
			}

			for (Direction direction : HORIZONTAL) {
				int nextX = current.x() + direction.getStepX();
				int nextZ = current.z() + direction.getStepZ();
				Column next = findNeighbour(level, nextX, nextZ, current.floorY(), gates, boundary);
				if (next == null) {
					continue;
				}

				long key = pack(nextX, nextZ);
				if (floors.containsKey(key)) {
					continue;
				}
				if (floors.size() >= maxArea) {
					return EnclosureScan.invalid("not_enclosed");
				}

				floors.put(key, next.floorY());
				queue.add(next);
			}
		}

		int area = floors.size();
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
		Set<Long> columns = new HashSet<>(floors.keySet());
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
				new BlockPos(minX, startFloor, minZ),
				new BlockPos(maxX, startFloor, maxZ),
				new BlockPos(originX, startFloor, originZ),
				spans,
				List.copyOf(gates)
		);
	}

	/**
	 * Finds a usable seed near the clicked column. Clicks often land on the fence itself or one
	 * block off the interior, and a seed on top of a fence would flood a one-column "pen".
	 */
	private static BlockPos findSeed(ServerLevel level, BlockPos origin) {
		for (int radius = 0; radius <= SEED_SEARCH; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					int floor = findStartFloor(level, x, z, origin.getY());
					if (floor == Integer.MIN_VALUE) {
						continue;
					}
					BlockPos below = new BlockPos(x, floor - 1, z);
					if (isBoundary(level.getBlockState(below))) {
						continue;
					}
					return new BlockPos(x, floor, z);
				}
			}
		}
		return null;
	}

	private static boolean isBoundary(BlockState state) {
		return state.getBlock() instanceof FenceGateBlock
				|| state.is(BlockTags.FENCES)
				|| state.is(BlockTags.WALLS);
	}

	private static int findStartFloor(ServerLevel level, int x, int z, int originY) {
		for (int y = originY + START_SCAN_UP; y >= originY - START_SCAN_DOWN; y--) {
			if (!level.getBlockState(new BlockPos(x, y, z)).getCollisionShape(level, new BlockPos(x, y, z)).isEmpty()) {
				continue;
			}
			if (!level.getBlockState(new BlockPos(x, y + 1, z)).getCollisionShape(level, new BlockPos(x, y + 1, z)).isEmpty()) {
				continue;
			}
			BlockPos below = new BlockPos(x, y - 1, z);
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * Finds a walkable neighbour at the same level or one block up/down. Fences, gates and walls in
	 * the feet/head space are recorded as boundaries instead of being traversed.
	 */
	private static Column findNeighbour(
			ServerLevel level,
			int x,
			int z,
			int currentFloor,
			Set<BlockPos> gates,
			Set<BlockPos> boundary
	) {
		for (int y = currentFloor + 1; y >= currentFloor - 1; y--) {
			BlockPos feet = new BlockPos(x, y, z);
			BlockPos head = new BlockPos(x, y + 1, z);
			if (recordBoundary(level, feet, gates, boundary)) {
				return null;
			}
			if (recordBoundary(level, head, gates, boundary)) {
				return null;
			}

			BlockPos below = new BlockPos(x, y - 1, z);
			if (recordBoundary(level, below, gates, boundary)) {
				return null;
			}

			BlockState feetState = level.getBlockState(feet);
			BlockState headState = level.getBlockState(head);
			if (!feetState.getCollisionShape(level, feet).isEmpty()
					|| !headState.getCollisionShape(level, head).isEmpty()) {
				if (!feetState.getCollisionShape(level, feet).isEmpty()) {
					boundary.add(feet.immutable());
				}
				if (!headState.getCollisionShape(level, head).isEmpty()) {
					boundary.add(head.immutable());
				}
				return null;
			}

			if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				continue;
			}
			return new Column(x, z, y);
		}
		return null;
	}

	private static boolean recordBoundary(
			ServerLevel level, BlockPos pos, Set<BlockPos> gates, Set<BlockPos> boundary
	) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof FenceGateBlock) {
			BlockPos immutable = pos.immutable();
			gates.add(immutable);
			boundary.add(immutable);
			return true;
		}
		if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
			boundary.add(pos.immutable());
			return true;
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

	private record Column(int x, int z, int floorY) {
	}
}

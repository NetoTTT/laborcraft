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
 * Scans an open-air enclosure from a point inside it. Unlike the house scan, it floods one
 * horizontal layer: fences, walls and gates are boundaries, the sky/roof is irrelevant, and at
 * least one fence gate is required for a valid pen.
 */
public final class EnclosureScanner {
	private static final Direction[] HORIZONTAL = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.EAST,
			Direction.WEST
	};
	private static final int MIN_AREA = 9;
	private static final int MIN_BOUNDARY = 4;

	private EnclosureScanner() {
	}

	public static EnclosureScan scan(ServerLevel level, BlockPos origin, LaborCraftConfig config) {
		if (!level.getBlockState(origin).getCollisionShape(level, origin).isEmpty()) {
			return EnclosureScan.invalid("blocked");
		}

		int maxArea = Math.max(MIN_AREA, config.housing.maxVolume);
		int maxRadius = Math.max(4, config.housing.maxDistanceFromAnchor);
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(origin);
		queue.add(origin);

		Set<BlockPos> gates = new HashSet<>();
		Set<BlockPos> boundary = new HashSet<>();

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();
			if (Math.abs(current.getX() - origin.getX()) > maxRadius
					|| Math.abs(current.getZ() - origin.getZ()) > maxRadius) {
				return EnclosureScan.invalid("too_large");
			}

			for (Direction direction : HORIZONTAL) {
				BlockPos next = current.relative(direction);
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof FenceGateBlock) {
					gates.add(next.immutable());
					boundary.add(next.immutable());
					continue;
				}
				if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
					boundary.add(next.immutable());
					continue;
				}
				if (!state.getCollisionShape(level, next).isEmpty()) {
					boundary.add(next.immutable());
					continue;
				}

				if (visited.add(next)) {
					if (visited.size() > maxArea) {
						return EnclosureScan.invalid("too_large");
					}
					queue.add(next);
				}
			}
		}

		int area = visited.size();
		if (area < MIN_AREA) {
			return EnclosureScan.invalid("too_small");
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
		Set<Long> columns = new HashSet<>();
		for (BlockPos cell : visited) {
			minX = Math.min(minX, cell.getX());
			maxX = Math.max(maxX, cell.getX());
			minZ = Math.min(minZ, cell.getZ());
			maxZ = Math.max(maxZ, cell.getZ());
			columns.add(pack(cell.getX(), cell.getZ()));
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
				new BlockPos(minX, origin.getY(), minZ),
				new BlockPos(maxX, origin.getY(), maxZ),
				origin.immutable(),
				spans,
				List.copyOf(gates)
		);
	}

	private static long pack(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}
}

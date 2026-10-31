package com.foque.laborcraft.area;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * A pen/pasture area described by its interior row runs, not by one rectangle. It keeps the scan
 * seed for revalidation and the gate positions for the map/UI.
 */
public record EnclosureDescriptor(
		String dimension,
		BlockPos seed,
		BlockPos min,
		BlockPos max,
		int area,
		List<EnclosureSpan> spans,
		List<BlockPos> gates
) {
	public EnclosureDescriptor {
		spans = spans == null ? List.of() : List.copyOf(spans);
		gates = gates == null ? List.of() : List.copyOf(gates);
	}

	public static EnclosureDescriptor from(EnclosureScan scan, String dimension) {
		return new EnclosureDescriptor(
				dimension,
				scan.seed(),
				scan.min(),
				scan.max(),
				scan.area(),
				scan.spans(),
				scan.gatePositions()
		);
	}

	public int width() {
		return max.getX() - min.getX() + 1;
	}

	public int depth() {
		return max.getZ() - min.getZ() + 1;
	}

	/** Horizontal containment; the station vertical radius still decides the Y range. */
	public boolean contains(int x, int z) {
		for (EnclosureSpan span : spans) {
			if (span.contains(x, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(BlockPos pos) {
		return contains(pos.getX(), pos.getZ());
	}
}

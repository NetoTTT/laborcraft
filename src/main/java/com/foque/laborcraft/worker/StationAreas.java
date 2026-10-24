package com.foque.laborcraft.worker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

public final class StationAreas {
	private StationAreas() {
	}

	public record Span(int z, int x0, int x1) {
	}

	public static boolean sameStation(StationRegistry.Station a, StationRegistry.Station b) {
		return a.id().equals(b.id())
				&& a.dimension().equals(b.dimension())
				&& a.pos().equals(b.pos());
	}

	public static boolean isHigherPriority(StationRegistry.Station a, StationRegistry.Station b) {
		int byTime = Long.compare(a.createdAt(), b.createdAt());
		if (byTime != 0) {
			return byTime < 0;
		}

		int byId = a.id().compareTo(b.id());
		if (byId != 0) {
			return byId < 0;
		}

		int byX = Integer.compare(a.pos().getX(), b.pos().getX());
		if (byX != 0) {
			return byX < 0;
		}

		int byZ = Integer.compare(a.pos().getZ(), b.pos().getZ());
		if (byZ != 0) {
			return byZ < 0;
		}

		return a.pos().getY() < b.pos().getY();
	}

	public static boolean declaredContains(
			StationRegistry.Station station,
			int x,
			int z,
			int maxRadius
	) {
		if (!station.zones().isEmpty()) {
			for (StationRegistry.Zone zone : station.zones()) {
				if (inside(zone.from(), zone.to(), x, z)) {
					return true;
				}
			}
			return false;
		}

		return Math.abs(x - station.pos().getX()) <= maxRadius
				&& Math.abs(z - station.pos().getZ()) <= maxRadius;
	}

	public static boolean isEffective(
			StationRegistry.Station station,
			List<StationRegistry.Station> all,
			int maxRadius,
			int x,
			int z
	) {
		if (!declaredContains(station, x, z, maxRadius)) {
			return false;
		}

		for (StationRegistry.Station other : all) {
			if (sameStation(station, other) || !other.dimension().equals(station.dimension())) {
				continue;
			}
			if (isHigherPriority(other, station) && declaredContains(other, x, z, maxRadius)) {
				return false;
			}
		}

		for (StationRegistry.Station other : all) {
			if (sameStation(station, other) || !other.dimension().equals(station.dimension())) {
				continue;
			}
			if (other.pos().getX() == x && other.pos().getZ() == z) {
				return false;
			}
		}

		return true;
	}

	public static boolean hasEffectiveCells(
			StationRegistry.Station station,
			List<StationRegistry.Station> all,
			int maxRadius
	) {
		int[] bounds = declaredBounds(station, maxRadius);
		for (int z = bounds[2]; z <= bounds[3]; z++) {
			for (int x = bounds[0]; x <= bounds[1]; x++) {
				if (isEffective(station, all, maxRadius, x, z)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean centerOwnedByOther(
			StationRegistry.Station station,
			List<StationRegistry.Station> all,
			int maxRadius
	) {
		int x = station.pos().getX();
		int z = station.pos().getZ();
		for (StationRegistry.Station other : all) {
			if (sameStation(station, other) || !other.dimension().equals(station.dimension())) {
				continue;
			}
			if (isHigherPriority(other, station) && isEffective(other, all, maxRadius, x, z)) {
				return true;
			}
		}
		return false;
	}

	public static List<Span> effectiveSpans(
			StationRegistry.Station station,
			List<StationRegistry.Station> all,
			int maxRadius
	) {
		int[] bounds = declaredBounds(station, maxRadius);
		List<Span> spans = new ArrayList<>();
		for (int z = bounds[2]; z <= bounds[3]; z++) {
			int start = Integer.MIN_VALUE;
			for (int x = bounds[0]; x <= bounds[1]; x++) {
				boolean effective = isEffective(station, all, maxRadius, x, z);
				if (effective && start == Integer.MIN_VALUE) {
					start = x;
				} else if (!effective && start != Integer.MIN_VALUE) {
					spans.add(new Span(z, start, x - 1));
					start = Integer.MIN_VALUE;
				}
			}
			if (start != Integer.MIN_VALUE) {
				spans.add(new Span(z, start, bounds[1]));
			}
		}
		return List.copyOf(spans);
	}

	public static int[] declaredBounds(StationRegistry.Station station, int maxRadius) {
		if (station.zones().isEmpty()) {
			return new int[]{
					station.pos().getX() - maxRadius,
					station.pos().getX() + maxRadius,
					station.pos().getZ() - maxRadius,
					station.pos().getZ() + maxRadius
			};
		}

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (StationRegistry.Zone zone : station.zones()) {
			minX = Math.min(minX, Math.min(zone.from().getX(), zone.to().getX()));
			maxX = Math.max(maxX, Math.max(zone.from().getX(), zone.to().getX()));
			minZ = Math.min(minZ, Math.min(zone.from().getZ(), zone.to().getZ()));
			maxZ = Math.max(maxZ, Math.max(zone.from().getZ(), zone.to().getZ()));
		}
		return new int[]{minX, maxX, minZ, maxZ};
	}

	private static boolean inside(BlockPos from, BlockPos to, int x, int z) {
		return x >= Math.min(from.getX(), to.getX())
				&& x <= Math.max(from.getX(), to.getX())
				&& z >= Math.min(from.getZ(), to.getZ())
				&& z <= Math.max(from.getZ(), to.getZ());
	}
}

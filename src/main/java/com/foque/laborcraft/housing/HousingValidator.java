package com.foque.laborcraft.housing;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Valida uma casa a partir da cama.
 *
 * <p>A cama e a ancora. O ar acima dela e semeado num flood fill de 6 direcoes que so atravessa ar.
 * Tudo que fecha o volume (parede, teto, porta fechada, vidro) vira fronteira. Se o fill escapar
 * para uma celula que ve o ceu, a casa esta aberta. O tamanho interno e medido no andar da cama,
 * para andar de cima nao inflar a capacidade do debaixo.</p>
 *
 * <p>A medicao vive em {@link HousingScan}; as exigencias sao {@link HousingRule}s registradas em
 * {@link HousingRules}. Adicionar uma regra nova nao muda o flood fill nem a UI.</p>
 */
public final class HousingValidator {
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final int AREA_PER_BED = 12;

	private HousingValidator() {
	}

	public static HousingReport validate(ServerLevel level, BlockPos bedPos, LaborCraftConfig config) {
		return report(scan(level, bedPos, config, true), config);
	}

	/**
	 * Mede a regiao como se houvesse uma cama em {@code bedPos}, sem exigir a cama real. E o que a
	 * Planta de Casa usa: o jogador aponta para o chao e recebe o relatorio do comodo.
	 */
	public static HousingReport measure(ServerLevel level, BlockPos bedPos, LaborCraftConfig config) {
		return report(scan(level, bedPos, config, false), config);
	}

	/** Raw measurements, without running the housing rules. */
	public static HousingScan scan(ServerLevel level, BlockPos bedPos, LaborCraftConfig config) {
		return scan(level, bedPos, config, true);
	}

	private static HousingReport report(HousingScan scan, LaborCraftConfig config) {
		if (scan.failed()) {
			return HousingReport.invalid(scan.failureReason(), scan.checkedAtTick());
		}

		List<HousingRuleResult> results = HousingRules.evaluate(scan, config);
		String failure = HousingRules.firstFailure(results);
		boolean valid = failure.isEmpty();
		int maxBeds = Math.max(1, config.housing.maxBedsPerHouse);
		int capacity = valid
				? Math.min(maxBeds, Math.max(2, scan.area() / AREA_PER_BED))
				: 0;
		double comfortBonus = Math.min(
				config.housing.comfortBonusMax,
				scan.comfortBlocks() * config.housing.comfortBonusPerBlock
		);

		return new HousingReport(
				valid,
				scan.enclosed(),
				scan.volume(),
				scan.width(),
				scan.depth(),
				scan.doors(),
				scan.beds().size(),
				capacity,
				scan.comfortBlocks(),
				comfortBonus,
				scan.checkedAtTick(),
				failure,
				scan.min(),
				scan.max(),
				scan.beds(),
				results
		);
	}

	private static HousingScan scan(
			ServerLevel level, BlockPos bedPos, LaborCraftConfig config, boolean requireBed
	) {
		long tick = level.getGameTime();
		if (requireBed) {
			BlockState bedState = level.getBlockState(bedPos);
			if (!bedState.is(BlockTags.BEDS)) {
				return HousingScan.invalid("not_a_bed", tick);
			}
			bedPos = canonicalBed(level, bedPos);
		}

		BlockPos seed = bedPos.above();
		if (!level.getBlockState(seed).isAir()) {
			return HousingScan.invalid("no_space", tick);
		}

		int maxVolume = Math.max(8, config.housing.maxVolume);
		int maxRadius = Math.max(4, config.housing.maxDistanceFromAnchor);

		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(seed);
		queue.add(seed);

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();

			if (Math.abs(current.getX() - bedPos.getX()) > maxRadius
					|| Math.abs(current.getZ() - bedPos.getZ()) > maxRadius
					|| Math.abs(current.getY() - bedPos.getY()) > maxRadius) {
				return HousingScan.invalid("too_large", tick);
			}

			if (seesSky(level, current)) {
				return HousingScan.invalid("open_sky", tick);
			}

			for (Direction direction : DIRECTIONS) {
				BlockPos next = current.relative(direction);
				if (visited.contains(next) || !level.getBlockState(next).isAir()) {
					continue;
				}

				if (visited.size() >= maxVolume) {
					return HousingScan.invalid("too_large", tick);
				}

				visited.add(next);
				queue.add(next);
			}
		}

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		int floorY = bedPos.getY() + 1;
		// Duas camadas (piso + altura da cabeca): um bau no canto nao pode encolher o interior.
		int ceilingY = floorY + 1;

		for (BlockPos cell : visited) {
			if (cell.getY() < floorY || cell.getY() > ceilingY) {
				continue;
			}

			minX = Math.min(minX, cell.getX());
			maxX = Math.max(maxX, cell.getX());
			minZ = Math.min(minZ, cell.getZ());
			maxZ = Math.max(maxZ, cell.getZ());
		}

		if (minX > maxX) {
			return HousingScan.invalid("no_space", tick);
		}

		int width = maxX - minX + 1;
		int depth = maxZ - minZ + 1;

		Set<BlockPos> doorPositions = new HashSet<>();
		Set<BlockPos> bedPositions = new HashSet<>();
		Set<BlockPos> comfortPositions = new HashSet<>();
		boolean lightSource = false;

		for (BlockPos cell : visited) {
			for (Direction direction : DIRECTIONS) {
				BlockPos neighbor = cell.relative(direction);
				BlockState state = level.getBlockState(neighbor);
				if (state.is(BlockTags.DOORS)) {
					// Porta ocupa dois blocos: conta so a metade de baixo.
					if (state.hasProperty(DoorBlock.HALF)
							&& state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
						doorPositions.add(neighbor);
					}
				}
				else if (state.is(BlockTags.BEDS)) {
					bedPositions.add(neighbor);
				}
				else if (isComfort(state, config)) {
					comfortPositions.add(neighbor);
				}
				if (state.getLightEmission() > 0) {
					lightSource = true;
				}
			}
		}

		Set<BlockPos> bedKeys = new HashSet<>();
		for (BlockPos bed : bedPositions) {
			if (insideInterior(bed, minX, maxX, minZ, maxZ, bedPos.getY())) {
				bedKeys.add(canonicalBed(level, bed));
			}
		}
		if (requireBed) {
			bedKeys.add(canonicalBed(level, bedPos));
		}

		List<BlockPos> bedList = new ArrayList<>(bedKeys);
		bedList.sort((left, right) -> {
			int byX = Integer.compare(left.getX(), right.getX());
			if (byX != 0) {
				return byX;
			}
			int byZ = Integer.compare(left.getZ(), right.getZ());
			return byZ != 0 ? byZ : Integer.compare(left.getY(), right.getY());
		});

		return new HousingScan(
				true,
				visited.size(),
				width,
				depth,
				doorPositions.size(),
				bedList,
				comfortPositions.size(),
				lightSource,
				tick,
				"",
				new BlockPos(minX, floorY, minZ),
				new BlockPos(maxX, ceilingY, maxZ),
				floorY,
				ceilingY
		);
	}

	private static boolean insideInterior(
			BlockPos pos,
			int minX,
			int maxX,
			int minZ,
			int maxZ,
			int bedY
	) {
		return pos.getY() == bedY
				&& pos.getX() >= minX
				&& pos.getX() <= maxX
				&& pos.getZ() >= minZ
				&& pos.getZ() <= maxZ;
	}

	/**
	 * Returns the foot of the bed (or the position itself when it is already the foot), so both
	 * halves of the same bed produce the same key.
	 */
	public static BlockPos canonicalBed(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.BEDS)
				|| !state.hasProperty(BedBlock.PART)
				|| state.getValue(BedBlock.PART) != BedPart.HEAD) {
			return pos.immutable();
		}

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos other = pos.relative(direction);
			BlockState otherState = level.getBlockState(other);
			if (otherState.is(BlockTags.BEDS)
					&& otherState.hasProperty(BedBlock.PART)
					&& otherState.getValue(BedBlock.PART) == BedPart.FOOT) {
				return other.immutable();
			}
		}

		return pos.immutable();
	}

	/** Uma celula "ve o ceu" quando esta na altura do heightmap ou acima dele. */
	private static boolean seesSky(ServerLevel level, BlockPos pos) {
		int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		return pos.getY() >= height;
	}

	private static boolean isComfort(BlockState state, LaborCraftConfig config) {
		if (config.housing.comfortBlocks == null || config.housing.comfortBlocks.isEmpty()) {
			return false;
		}

		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
		return config.housing.comfortBlocks.contains(id);
	}
}

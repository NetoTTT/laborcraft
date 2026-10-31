package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.job.JobEquipment;
import com.foque.laborcraft.job.JobTypes;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Minimal farmer AI: validate tool/seeds, scan the station area for mature crops, empty farmland or
 * trampled dirt, walk there (preferring a non-farmland stand position), harvest/replant/plant/till,
 * then walk to the linked chest and deposit. Pauses with a notice (including the worker name) when
 * a requirement is missing.
 */
public final class FarmerBrain extends AbstractWorkerBrain {
	private static final int CHECK_INTERVAL = 10;
	private static final double REACH_SQR = 20.25D;
	private static final int PRODUCE_DEPOSIT_THRESHOLD = 32;
	private static final int PATH_TIMEOUT = 100;
	private static final int IGNORE_TICKS = 200;

	private enum Action {
		HARVEST,
		FERTILIZE,
		PLANT,
		TILL
	}

	private record WorkTarget(BlockPos pos, Action action, BlockPos stand) {
	}

	private WorkTarget target;
	private BlockPos ignored;
	private long ignoredUntil;
	private int pathTicks;
	private int depositTicks;
	private UUID dropId;
	private boolean sawDryFarmland;
	private boolean sawGrowingCrops;
	private int dropTicks;
	private long lastWaterWarning;

	public FarmerBrain(LaborCraftWorker worker) {
		super(worker, CHECK_INTERVAL);
	}

	@Override
	protected void tickJob(ServerLevel level, StationRegistry.Station station) {
		if (FarmerStorage.count(worker, FarmerStorage::isSeed) <= 0) {
			FarmerStorage.withdraw(level, station, worker, LaborCraftConfig.get().job(worker.jobId()).useBoneMeal);
			if (FarmerStorage.count(worker, FarmerStorage::isSeed) <= 0) {
				pause(level, "laborcraft.ui.worker.task.need_seeds");
				return;
			}
		}

		if (LaborCraftConfig.get().job(worker.jobId()).useComposter) {
			int seeds = FarmerStorage.count(worker, FarmerStorage::isSeed);
			BlockPos composter = findComposter(level, station);
			if (composter != null) {
				BlockState state = level.getBlockState(composter);
				boolean ready = state.is(Blocks.COMPOSTER)
						&& state.getValue(ComposterBlock.LEVEL) >= ComposterBlock.READY;
				if (ready || seeds > LaborCraftConfig.get().job(worker.jobId()).seedSurplusForCompost) {
					compost(level, composter);
					return;
				}
			}
		}

		if (shouldDeposit()) {
			deposit(level, station);
			return;
		}

		if (freeSlots() > 0) {
			ItemEntity drop = findDroppedItem(level, station);
			if (drop != null) {
				collect(level, drop);
				return;
			}
		}

		if (target == null || !isTarget(level, target)) {
			target = findTarget(level, station);
		}

		if (target == null) {
			if (waterPolicy().equals("require") && sawDryFarmland && !sawGrowingCrops) {
				warnNeedWater(level);
				idle(station);
				return;
			}
			if (FarmerStorage.count(worker, FarmerStorage::isProduce) > 0) {
				deposit(level, station);
			} else {
				idle(station);
			}
			return;
		}

		Vec3 center = Vec3.atCenterOf(target.pos());
		if (worker.position().distanceToSqr(center) <= REACH_SQR) {
			switch (target.action()) {
				case HARVEST -> harvest(level, target.pos());
				case FERTILIZE -> fertilize(level, target.pos());
				case PLANT -> plant(level, target.pos());
				case TILL -> till(level, target.pos());
			}
			target = null;
			pathTicks = 0;
			return;
		}

		BlockPos step = target.stand() == null ? target.pos() : target.stand();
		if (worker.getNavigation().isDone() || pathTicks <= 0) {
			worker.getNavigation().moveTo(
					step.getX() + 0.5D,
					step.getY(),
					step.getZ() + 0.5D,
					1.0D
			);
			pathTicks = PATH_TIMEOUT;
		} else {
			pathTicks -= CHECK_INTERVAL;
			if (pathTicks <= 0) {
				ignored = target.pos();
				ignoredUntil = level.getGameTime() + IGNORE_TICKS;
				target = null;
			}
		}
	}

	private boolean shouldDeposit() {
		int produce = FarmerStorage.count(worker, FarmerStorage::isProduce);
		return produce >= PRODUCE_DEPOSIT_THRESHOLD || (freeSlots() <= 0 && produce > 0);
	}

	private int freeSlots() {
		int free = 0;
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			if (worker.inventoryItem(slot).isEmpty()) {
				free++;
			}
		}
		return free;
	}

	@Override
	protected boolean canResumeJob(ServerLevel level, StationRegistry.Station station) {
		String notice = worker.workNotice();
		return switch (notice == null ? "" : notice) {
			case "laborcraft.ui.worker.task.need_seeds" -> FarmerStorage.count(worker, FarmerStorage::isSeed) > 0
					|| hasSeedInStorage(level, station);
			case "laborcraft.ui.worker.task.no_chest" -> !WorkerStorage.linked(level, station, false).isEmpty();
			case "laborcraft.ui.worker.task.chest_full" -> hasContainerRoom(level, station);
			default -> false;
		};
	}
	private static boolean hasSeedInStorage(ServerLevel level, StationRegistry.Station station) {
		for (Container container : WorkerStorage.linked(level, station, true)) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty() && FarmerStorage.isSeed(stack)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasContainerRoom(ServerLevel level, StationRegistry.Station station) {
		for (Container container : WorkerStorage.linked(level, station, false)) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty()) {
					return true;
				}
				if (FarmerStorage.isProduce(stack) && stack.getCount() < stack.getMaxStackSize()) {
					return true;
				}
			}
		}
		return false;
	}

	private void deposit(ServerLevel level, StationRegistry.Station station) {
		BlockPos chest = nearestChest(level, station);
		if (chest == null) {
			pause(level, "laborcraft.ui.worker.task.no_chest");
			return;
		}

		if (worker.position().distanceToSqr(Vec3.atCenterOf(chest)) > REACH_SQR) {
			if (depositTicks <= 0) {
				depositTicks = PATH_TIMEOUT * 2;
			}
			depositTicks -= CHECK_INTERVAL;
			if (depositTicks > 0) {
				if (worker.getNavigation().isDone()) {
					worker.getNavigation().moveTo(
							chest.getX() + 0.5D,
							chest.getY(),
							chest.getZ() + 0.5D,
							1.0D
					);
				}
				return;
			}
		}

		int moved = FarmerStorage.deposit(level, station, worker);
		FarmerStorage.withdraw(level, station, worker, LaborCraftConfig.get().job(worker.jobId()).useBoneMeal);
		depositTicks = 0;
		target = null;

		if (moved <= 0 && FarmerStorage.count(worker, FarmerStorage::isProduce) > 0) {
			pause(level, "laborcraft.ui.worker.task.chest_full");
		}
	}

	private BlockPos nearestChest(ServerLevel level, StationRegistry.Station station) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (StationRegistry.StorageLink link : station.storage()) {
			if (link.role() == StationRegistry.StorageRole.TOOLS
					|| link.role() == StationRegistry.StorageRole.INPUT) {
				continue;
			}
			if (!(level.getBlockEntity(link.pos()) instanceof Container)) {
				continue;
			}

			double distance = worker.position().distanceToSqr(Vec3.atCenterOf(link.pos()));
			if (distance < bestDistance) {
				bestDistance = distance;
				best = link.pos();
			}
		}
		return best;
	}

	private ItemEntity findDroppedItem(ServerLevel level, StationRegistry.Station station) {
		int radius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		int vertical = Math.max(1, LaborCraftConfig.get().area.verticalRadius);
		BlockPos center = station.pos();
		AABB area = new AABB(
				center.getX() - radius,
				center.getY() - vertical,
				center.getZ() - radius,
				center.getX() + radius + 1.0D,
				center.getY() + vertical + 1.0D,
				center.getZ() + radius + 1.0D
		);
		ItemEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		long now = level.getGameTime();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
			if (!item.isAlive() || !FarmerStorage.isFarmerItem(item.getItem())) {
				continue;
			}
			BlockPos pos = item.blockPosition();
			if (pos.equals(ignored) && now < ignoredUntil) {
				continue;
			}
			double distance = worker.position().distanceToSqr(item.position());
			if (distance < bestDistance) {
				bestDistance = distance;
				best = item;
			}
		}
		return best;
	}

	private void collect(ServerLevel level, ItemEntity item) {
		if (dropId == null || !dropId.equals(item.getUUID())) {
			dropId = item.getUUID();
			dropTicks = PATH_TIMEOUT;
		}

		if (worker.position().distanceToSqr(item.position()) > 6.25D) {
			dropTicks -= CHECK_INTERVAL;
			if (dropTicks <= 0) {
				ignored = item.blockPosition();
				ignoredUntil = level.getGameTime() + IGNORE_TICKS;
				dropId = null;
				return;
			}
			if (worker.getNavigation().isDone()) {
				worker.getNavigation().moveTo(item.getX(), item.getY(), item.getZ(), 1.0D);
			}
			return;
		}

		ItemStack stack = item.getItem();
		ItemStack leftover = FarmerStorage.addToWorker(worker, stack.copy());
		int moved = stack.getCount() - leftover.getCount();
		if (moved <= 0) {
			return;
		}

		stack.shrink(moved);
		if (stack.isEmpty()) {
			item.discard();
		} else {
			item.setItem(stack);
		}
		dropId = null;
		dropTicks = 0;
	}

	private WorkTarget findTarget(ServerLevel level, StationRegistry.Station station) {
		int[] bounds = workBounds(station);
		WorkTarget bestCrop = null;
		WorkTarget bestFertilize = null;
		WorkTarget bestHydratedPlant = null;
		WorkTarget bestDryPlant = null;
		WorkTarget bestHydratedTill = null;
		WorkTarget bestDryTill = null;
		double bestCropDistance = Double.MAX_VALUE;
		double bestFertilizeDistance = Double.MAX_VALUE;
		double bestHydratedPlantDistance = Double.MAX_VALUE;
		double bestDryPlantDistance = Double.MAX_VALUE;
		double bestHydratedTillDistance = Double.MAX_VALUE;
		double bestDryTillDistance = Double.MAX_VALUE;
		boolean canPlant = canPlant();
		boolean useBoneMeal = LaborCraftConfig.get().job(worker.jobId()).useBoneMeal
				&& findItem(Items.BONE_MEAL) >= 0;
		String policy = waterPolicy();
		boolean require = policy.equals("require");
		boolean ignore = policy.equals("ignore");
		boolean[] hydrated = FarmerWater.mask(level, bounds);
		boolean raining = level.isRaining();
		sawDryFarmland = false;
		sawGrowingCrops = false;
		long now = level.getGameTime();

		for (int x = bounds[0]; x <= bounds[1]; x++) {
			for (int z = bounds[2]; z <= bounds[3]; z++) {
				for (int y = bounds[4]; y <= bounds[5]; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (pos.equals(ignored) && now < ignoredUntil) {
						continue;
					}
					if (!level.hasChunkAt(pos) || !StationAreas.zoneContains(station, pos)) {
						continue;
					}

					BlockState state = level.getBlockState(pos);
					double distance = worker.position().distanceToSqr(Vec3.atCenterOf(pos));
					if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
						sawGrowingCrops = true;
						if (useBoneMeal && distance < bestFertilizeDistance) {
							bestFertilizeDistance = distance;
							bestFertilize = new WorkTarget(pos, Action.FERTILIZE, findStand(level, pos));
						}
						continue;
					}
					if (isMatureCrop(state)) {
						if (distance < bestCropDistance) {
							bestCropDistance = distance;
							bestCrop = new WorkTarget(pos, Action.HARVEST, findStand(level, pos));
						}
						continue;
					}

					if (!canPlant || !level.getBlockState(pos.above()).isAir()) {
						continue;
					}

					boolean isFarmland = state.is(Blocks.FARMLAND);
					boolean waterHere = ignore
							|| FarmerWater.maskAt(hydrated, bounds, pos)
							|| (isFarmland && state.getValue(FarmlandBlock.MOISTURE) > 0)
							|| (raining && level.isRainingAt(pos.above()));

					if (isFarmland) {
						if (waterHere) {
							if (distance < bestHydratedPlantDistance) {
								bestHydratedPlantDistance = distance;
								bestHydratedPlant = new WorkTarget(pos, Action.PLANT, findStand(level, pos));
							}
						} else if (require) {
							sawDryFarmland = true;
						} else if (distance < bestDryPlantDistance) {
							bestDryPlantDistance = distance;
							bestDryPlant = new WorkTarget(pos, Action.PLANT, findStand(level, pos));
						}
					} else if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
						if (waterHere) {
							if (distance < bestHydratedTillDistance) {
								bestHydratedTillDistance = distance;
								bestHydratedTill = new WorkTarget(pos, Action.TILL, findStand(level, pos));
							}
						} else if (!require && distance < bestDryTillDistance) {
							bestDryTillDistance = distance;
							bestDryTill = new WorkTarget(pos, Action.TILL, findStand(level, pos));
						}
					}
				}
			}
		}

		if (bestCrop != null) {
			return bestCrop;
		}
		if (bestFertilize != null) {
			return bestFertilize;
		}
		if (bestHydratedPlant != null) {
			return bestHydratedPlant;
		}
		if (bestDryPlant != null) {
			return bestDryPlant;
		}
		if (bestHydratedTill != null) {
			return bestHydratedTill;
		}
		return bestDryTill;
	}

	private String waterPolicy() {
		String raw = LaborCraftConfig.get().job(worker.jobId()).waterPolicy;
		if (raw == null) {
			return "prefer";
		}
		String normalized = raw.toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "require", "ignore" -> normalized;
			default -> "prefer";
		};
	}

	private void warnDry(ServerLevel level) {
		warn(level, "laborcraft.ui.worker.task.dry_soil");
	}

	private void warnNeedWater(ServerLevel level) {
		warn(level, "laborcraft.ui.worker.task.need_water");
	}

	private void warn(ServerLevel level, String notice) {
		long now = level.getGameTime();
		if (now - lastWaterWarning < 20L * 60L) {
			return;
		}
		lastWaterWarning = now;
		worker.announce(notice, 10);

		ServerPlayer owner = worker.owner() == null
				? null
				: level.getServer().getPlayerList().getPlayer(worker.owner());
		if (owner != null) {
			owner.sendSystemMessage(WorkerChat.say(worker, Component.translatable(notice)));
		}
	}

	/**
	 * Prefers a non-farmland block adjacent to the target so the worker does not jump over crops.
	 * Falls back to the target itself when the field has no border nearby.
	 */
	private BlockPos findStand(ServerLevel level, BlockPos target) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos candidate = target.relative(direction).above(dy);
				if (!level.hasChunkAt(candidate)) {
					continue;
				}
				if (!level.getBlockState(candidate).isAir()
						|| !level.getBlockState(candidate.above()).isAir()) {
					continue;
				}

				BlockState below = level.getBlockState(candidate.below());
				if (!below.canOcclude() || below.is(Blocks.FARMLAND) || below.getBlock() instanceof CropBlock) {
					continue;
				}

				double distance = worker.position().distanceToSqr(Vec3.atCenterOf(candidate));
				if (distance < bestDistance) {
					bestDistance = distance;
					best = candidate;
				}
			}
		}
		return best;
	}

	private int[] workBounds(StationRegistry.Station station) {
		if (!station.zones().isEmpty()) {
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxZ = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (StationRegistry.Zone zone : station.zones()) {
				minX = Math.min(minX, Math.min(zone.from().getX(), zone.to().getX()));
				maxX = Math.max(maxX, Math.max(zone.from().getX(), zone.to().getX()));
				minZ = Math.min(minZ, Math.min(zone.from().getZ(), zone.to().getZ()));
				maxZ = Math.max(maxZ, Math.max(zone.from().getZ(), zone.to().getZ()));
				minY = Math.min(minY, Math.min(zone.from().getY(), zone.to().getY()));
				maxY = Math.max(maxY, Math.max(zone.from().getY(), zone.to().getY()));
			}
			return new int[]{minX, maxX, minZ, maxZ, minY, maxY};
		}

		int radius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		int vertical = Math.max(1, LaborCraftConfig.get().area.verticalRadius);
		BlockPos pos = station.pos();
		return new int[]{
				pos.getX() - radius,
				pos.getX() + radius,
				pos.getZ() - radius,
				pos.getZ() + radius,
				pos.getY() - vertical,
				pos.getY() + vertical
		};
	}

	private boolean isTarget(ServerLevel level, WorkTarget target) {
		if (!level.hasChunkAt(target.pos())) {
			return false;
		}
		BlockState state = level.getBlockState(target.pos());
		return switch (target.action()) {
			case HARVEST -> isMatureCrop(state);
			case FERTILIZE -> state.getBlock() instanceof CropBlock crop
					&& !crop.isMaxAge(state)
					&& findItem(Items.BONE_MEAL) >= 0;
			case PLANT -> state.is(Blocks.FARMLAND) && level.getBlockState(target.pos().above()).isAir();
			case TILL -> (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK))
					&& level.getBlockState(target.pos().above()).isAir();
		};
	}

	private static boolean isMatureCrop(BlockState state) {
		if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
			return false;
		}
		return seedFor(state) != null;
	}

	private void harvest(ServerLevel level, BlockPos pos) {
		if (freeSlots() <= 0) {
			target = null;
			return;
		}

		BlockState state = level.getBlockState(pos);
		ItemStack tool = worker.toolItem();
		List<ItemStack> drops = Block.getDrops(state, level, pos, null, worker, tool);
		level.removeBlock(pos, false);

		for (ItemStack drop : drops) {
			ItemStack leftover = FarmerStorage.addToWorker(worker, drop.copy());
			if (!leftover.isEmpty()) {
				Block.popResource(level, worker.blockPosition(), leftover);
			}
		}

		Item seed = seedFor(state);
		if (seed != null) {
			BlockPos farmland = pos.below();
			BlockState soil = level.getBlockState(farmland);
			boolean hydrated = soil.is(Blocks.FARMLAND) && FarmerWater.isHydrated(level, farmland);
			String policy = waterPolicy();
			boolean canReplant = hydrated || !policy.equals("require");
			if (!hydrated && policy.equals("prefer")) {
				warnDry(level);
			}
			int slot = canReplant ? findItem(seed) : -1;
			if (slot >= 0) {
				worker.inventoryItem(slot).shrink(1);
				level.setBlock(pos, ((CropBlock) state.getBlock()).getStateForAge(0), 3);
			}
		}

		worker.addMastery(worker.jobId(), LaborCraftConfig.get().worker.masteryXpPerHarvest);
	}

	private void fertilize(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
			return;
		}

		int slot = findItem(Items.BONE_MEAL);
		if (slot < 0) {
			return;
		}

		worker.inventoryItem(slot).shrink(1);
		crop.growCrops(level, pos, state);
	}

	private BlockPos findComposter(ServerLevel level, StationRegistry.Station station) {
		int radius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		int vertical = Math.max(1, LaborCraftConfig.get().area.verticalRadius);
		BlockPos center = station.pos();
		BlockPos bestReady = null;
		BlockPos bestInsert = null;
		double bestReadyDistance = Double.MAX_VALUE;
		double bestInsertDistance = Double.MAX_VALUE;

		for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
			for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
				for (int y = center.getY() - vertical; y <= center.getY() + vertical; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!level.hasChunkAt(pos)) {
						continue;
					}

					BlockState state = level.getBlockState(pos);
					if (!state.is(Blocks.COMPOSTER)) {
						continue;
					}

					int current = state.getValue(ComposterBlock.LEVEL);
					if (current == ComposterBlock.MAX_LEVEL) {
						// Full but not ready yet; the composter will finish on its own.
						continue;
					}

					double distance = worker.position().distanceToSqr(Vec3.atCenterOf(pos));
					if (current >= ComposterBlock.READY) {
						if (distance < bestReadyDistance) {
							bestReadyDistance = distance;
							bestReady = pos.immutable();
						}
					} else if (distance < bestInsertDistance) {
						bestInsertDistance = distance;
						bestInsert = pos.immutable();
					}
				}
			}
		}
		return bestReady != null ? bestReady : bestInsert;
	}

	private void compost(ServerLevel level, BlockPos pos) {
		if (worker.position().distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR) {
			if (worker.getNavigation().isDone()) {
				worker.getNavigation().moveTo(
						pos.getX() + 0.5D,
						pos.getY(),
						pos.getZ() + 0.5D,
						1.0D
				);
			}
			return;
		}

		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.COMPOSTER)) {
			return;
		}

		int current = state.getValue(ComposterBlock.LEVEL);
		if (current >= ComposterBlock.READY) {
			level.setBlock(pos, state.setValue(ComposterBlock.LEVEL, 0), 3);
			ItemStack leftover = FarmerStorage.addToWorker(worker, new ItemStack(Items.BONE_MEAL));
			if (!leftover.isEmpty()) {
				Block.popResource(level, pos.above(), leftover);
			}
			return;
		}
		if (current >= ComposterBlock.MAX_LEVEL) {
			return;
		}

		ItemStack seed = findSeedStack();
		if (seed.isEmpty()) {
			return;
		}

		ItemStack one = seed.copyWithCount(1);
		BlockState next = ComposterBlock.insertItem(worker, state, level, one, pos);
		if (one.isEmpty()) {
			seed.shrink(1);
			level.setBlock(pos, next, 3);
		}
	}

	private void plant(ServerLevel level, BlockPos farmland) {
		BlockState soil = level.getBlockState(farmland);
		if (!soil.is(Blocks.FARMLAND) || !level.getBlockState(farmland.above()).isAir()) {
			return;
		}

		String policy = waterPolicy();
		if (!FarmerWater.isHydrated(level, farmland)) {
			if (policy.equals("require")) {
				warnNeedWater(level);
				return;
			}
			if (policy.equals("prefer")) {
				warnDry(level);
			}
		}

		ItemStack seed = findSeedStack();
		if (seed.isEmpty()) {
			pause(level, "laborcraft.ui.worker.task.need_seeds");
			return;
		}

		Block crop = cropFor(seed.getItem());
		if (crop == null) {
			return;
		}

		seed.shrink(1);
		level.setBlock(farmland.above(), crop.defaultBlockState(), 3);
	}

	private void till(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if ((!state.is(Blocks.DIRT) && !state.is(Blocks.GRASS_BLOCK))
				|| !level.getBlockState(pos.above()).isAir()) {
			return;
		}

		String policy = waterPolicy();
		if (!FarmerWater.isHydrated(level, pos)) {
			if (policy.equals("require")) {
				warnNeedWater(level);
				return;
			}
			if (policy.equals("prefer")) {
				warnDry(level);
			}
		}

		level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
		ItemStack tool = worker.toolItem();
		if (tool.isDamageableItem()) {
			tool.hurtAndBreak(1, worker, EquipmentSlot.MAINHAND);
		}
		plant(level, pos);
	}

	private ItemStack findSeedStack() {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (!stack.isEmpty() && cropFor(stack.getItem()) != null) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private boolean canPlant() {
		return !findSeedStack().isEmpty();
	}

	private int findItem(Item item) {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				return slot;
			}
		}
		return -1;
	}

	private static Block cropFor(Item seed) {
		if (seed == Items.WHEAT_SEEDS) {
			return Blocks.WHEAT;
		}
		if (seed == Items.BEETROOT_SEEDS) {
			return Blocks.BEETROOTS;
		}
		if (seed == Items.CARROT) {
			return Blocks.CARROTS;
		}
		if (seed == Items.POTATO) {
			return Blocks.POTATOES;
		}
		return null;
	}

	private static Item seedFor(BlockState state) {
		if (state.is(Blocks.WHEAT)) {
			return Items.WHEAT_SEEDS;
		}
		if (state.getBlock() instanceof BeetrootBlock) {
			return Items.BEETROOT_SEEDS;
		}
		if (state.getBlock() instanceof CarrotBlock) {
			return Items.CARROT;
		}
		if (state.getBlock() instanceof PotatoBlock) {
			return Items.POTATO;
		}
		return null;
	}

	private void idle(StationRegistry.Station station) {
		BlockPos pos = station.pos();
		if (worker.blockPosition().distSqr(pos) > 9.0D) {
			if (worker.getNavigation().isDone()) {
				worker.getNavigation().moveTo(
						pos.getX() + 0.5D,
						pos.getY() + 1.0D,
						pos.getZ() + 0.5D,
						0.8D
				);
			}
		} else {
			worker.getNavigation().stop();
		}
	}

	@Override
	protected void onPaused() {
		target = null;
		depositTicks = 0;
	}
}
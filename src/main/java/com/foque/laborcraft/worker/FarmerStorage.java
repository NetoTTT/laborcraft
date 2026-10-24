package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Farmer storage rules: keep a seed reserve and some bone meal in the backpack, deposit only the
 * harvest (and surplus supplies), and withdraw seeds/supplies from the linked chests before work.
 */
public final class FarmerStorage {
	public static final int SEED_RESERVE = 32;
	public static final int BONE_MEAL_TARGET = 16;

	private static final Set<Item> PRODUCE = Set.of(
			Items.WHEAT,
			Items.BEETROOT,
			Items.CARROT,
			Items.POTATO,
			Items.POISONOUS_POTATO,
			Items.MELON_SLICE,
			Items.PUMPKIN
	);

	private FarmerStorage() {
	}

	public static boolean isSeed(ItemStack stack) {
		return stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
	}

	public static boolean isSupply(ItemStack stack) {
		return stack.is(Items.BONE_MEAL);
	}

	public static boolean isProduce(ItemStack stack) {
		return PRODUCE.contains(stack.getItem());
	}

	public static boolean isFarmerItem(ItemStack stack) {
		return isSeed(stack) || isProduce(stack) || isSupply(stack);
	}

	public static int count(LaborCraftWorker worker, Predicate<ItemStack> filter) {
		int count = 0;
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Deposits harvest/surplus into the linked output containers; returns how many items moved. */
	public static int deposit(ServerLevel level, StationRegistry.Station station, LaborCraftWorker worker) {
		int keptSeeds = 0;
		int keptMeal = 0;
		int moved = 0;
		int seedKeep = LaborCraftConfig.get().farmer.useComposter
				? Math.max(SEED_RESERVE, LaborCraftConfig.get().farmer.seedSurplusForCompost)
				: SEED_RESERVE;

		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (stack.isEmpty()) {
				continue;
			}

			int keep = 0;
			if (isSeed(stack)) {
				keep = Math.min(stack.getCount(), Math.max(0, seedKeep - keptSeeds));
				keptSeeds += keep;
			} else if (isSupply(stack)) {
				keep = Math.min(stack.getCount(), Math.max(0, BONE_MEAL_TARGET - keptMeal));
				keptMeal += keep;
			} else if (!isProduce(stack)) {
				continue;
			}

			int amount = stack.getCount() - keep;
			if (amount <= 0) {
				continue;
			}

			ItemStack leftover = WorkerStorage.insert(level, station, stack.copyWithCount(amount));
			int inserted = amount - leftover.getCount();
			if (inserted > 0) {
				stack.shrink(inserted);
				moved += inserted;
			}
		}

		return moved;
	}

	/** Pulls seeds (and bone meal when enabled) from the linked input containers. */
	public static int withdraw(
			ServerLevel level,
			StationRegistry.Station station,
			LaborCraftWorker worker,
			boolean boneMeal
	) {
		int moved = 0;
		int seeds = count(worker, FarmerStorage::isSeed);
		if (seeds < SEED_RESERVE) {
			ItemStack got = WorkerStorage.extract(level, station, FarmerStorage::isSeed, SEED_RESERVE - seeds);
			if (!got.isEmpty()) {
				ItemStack leftover = addToWorker(worker, got);
				moved += got.getCount() - leftover.getCount();
				WorkerStorage.insert(level, station, leftover);
			}
		}

		if (boneMeal) {
			int current = count(worker, FarmerStorage::isSupply);
			if (current < BONE_MEAL_TARGET) {
				ItemStack got = WorkerStorage.extract(
						level,
						station,
						FarmerStorage::isSupply,
						BONE_MEAL_TARGET - current
				);
				if (!got.isEmpty()) {
					ItemStack leftover = addToWorker(worker, got);
					moved += got.getCount() - leftover.getCount();
					WorkerStorage.insert(level, station, leftover);
				}
			}
		}

		return moved;
	}

	/** Adds to the nine general slots; returns the leftover stack that did not fit. */
	public static ItemStack addToWorker(LaborCraftWorker worker, ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS && !stack.isEmpty(); slot++) {
			ItemStack existing = worker.inventoryItem(slot);
			if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
				continue;
			}

			int room = existing.getMaxStackSize() - existing.getCount();
			if (room <= 0) {
				continue;
			}

			int moved = Math.min(room, stack.getCount());
			existing.grow(moved);
			stack.shrink(moved);
		}

		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS && !stack.isEmpty(); slot++) {
			if (!worker.inventoryItem(slot).isEmpty()) {
				continue;
			}

			int moved = Math.min(stack.getMaxStackSize(), stack.getCount());
			worker.setInventoryItem(slot, stack.copyWithCount(moved));
			stack.shrink(moved);
		}

		return stack;
	}
}

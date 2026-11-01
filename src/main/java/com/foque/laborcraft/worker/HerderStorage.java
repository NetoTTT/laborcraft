package com.foque.laborcraft.worker;

import java.util.Set;
import com.foque.laborcraft.entity.LaborCraftWorker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Herder produce rules. Eggs, wool and milk are outputs; shears and buckets are tools the worker
 * keeps. Only produce is deposited, so the worker never dumps its equipment into a chest.
 */
public final class HerderStorage {
	private static final Set<Item> PRODUCE = Set.of(
			Items.EGG,
			Items.MILK_BUCKET
	);

	private HerderStorage() {
	}

	public static boolean isFood(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.is(ItemTags.CHICKEN_FOOD)
						|| stack.is(ItemTags.COW_FOOD)
						|| stack.is(ItemTags.SHEEP_FOOD));
	}

	public static boolean isTool(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(Items.SHEARS) || stack.is(Items.BUCKET));
	}

	/** Everything the herder picks up from the pen floor: produce, breeding food and tools. */
	public static boolean isPickup(ItemStack stack) {
		return isProduce(stack) || isFood(stack) || isTool(stack);
	}

	public static boolean isProduce(ItemStack stack) {
		return !stack.isEmpty() && (PRODUCE.contains(stack.getItem()) || stack.is(ItemTags.WOOL));
	}

	public static boolean hasProduce(LaborCraftWorker worker) {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			if (isProduce(worker.inventoryItem(slot))) {
				return true;
			}
		}
		return false;
	}

	public static int countProduce(LaborCraftWorker worker) {
		int count = 0;
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (isProduce(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Moves every produce stack to the linked output containers; returns how many items moved. */
	public static int deposit(ServerLevel level, StationRegistry.Station station, LaborCraftWorker worker) {
		int moved = 0;
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack stack = worker.inventoryItem(slot);
			if (!isProduce(stack)) {
				continue;
			}

			ItemStack leftover = WorkerStorage.insert(level, station, stack.copy());
			int inserted = stack.getCount() - leftover.getCount();
			if (inserted > 0) {
				stack.shrink(inserted);
				moved += inserted;
			}
		}
		return moved;
	}
}

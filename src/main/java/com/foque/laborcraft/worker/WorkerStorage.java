package com.foque.laborcraft.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Container protocol for worker storage: inserts only into empty/same-item slots and extracts only
 * the item types the job asks for. Roles decide the direction (MIXED for both, OUTPUT/INPUT one way).
 */
public final class WorkerStorage {
	private WorkerStorage() {
	}

	public static List<Container> linked(ServerLevel level, StationRegistry.Station station, boolean input) {
		List<Container> containers = new ArrayList<>();
		for (StationRegistry.StorageLink link : station.storage()) {
			if (!(level.getBlockEntity(link.pos()) instanceof Container container)) {
				continue;
			}

			boolean allowed = switch (link.role()) {
				case MIXED -> true;
				case OUTPUT -> !input;
				case INPUT -> input;
				case TOOLS -> false;
			};
			if (allowed) {
				containers.add(container);
			}
		}
		return containers;
	}

	/** Inserts what fits and returns the leftover stack (mutates the given stack). */
	public static ItemStack insert(ServerLevel level, StationRegistry.Station station, ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		for (Container container : linked(level, station, false)) {
			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				ItemStack existing = container.getItem(slot);
				if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
					continue;
				}
				if (!container.canPlaceItem(slot, stack)) {
					continue;
				}

				int max = Math.min(container.getMaxStackSize(stack), existing.getMaxStackSize());
				int room = max - existing.getCount();
				if (room <= 0) {
					continue;
				}

				int moved = Math.min(room, stack.getCount());
				existing.grow(moved);
				stack.shrink(moved);
				container.setChanged();
			}

			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack)) {
					continue;
				}

				int moved = Math.min(container.getMaxStackSize(stack), stack.getCount());
				container.setItem(slot, stack.copyWithCount(moved));
				stack.shrink(moved);
				container.setChanged();
			}

			if (stack.isEmpty()) {
				break;
			}
		}

		return stack;
	}

	/**
	 * Extracts a single item type (the first one that matches) up to {@code amount}. Mixing different
	 * item types into one stack would be invalid, so later slots of another type are skipped.
	 */
	public static ItemStack extract(
			ServerLevel level,
			StationRegistry.Station station,
			Predicate<ItemStack> filter,
			int amount
	) {
		ItemStack result = ItemStack.EMPTY;
		if (amount <= 0) {
			return result;
		}

		for (Container container : linked(level, station, true)) {
			for (int slot = 0; slot < container.getContainerSize() && result.getCount() < amount; slot++) {
				ItemStack existing = container.getItem(slot);
				if (existing.isEmpty() || !filter.test(existing)) {
					continue;
				}
				if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, existing)) {
					continue;
				}

				int take = Math.min(amount - result.getCount(), existing.getCount());
				ItemStack removed = container.removeItem(slot, take);
				if (removed.isEmpty()) {
					continue;
				}

				if (result.isEmpty()) {
					result = removed.copy();
				} else {
					result.grow(removed.getCount());
				}
				container.setChanged();
			}

			if (result.getCount() >= amount) {
				break;
			}
		}

		return result;
	}
}

package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.item.LaborCraftItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WorkerOwnership {
	private WorkerOwnership() {
	}

	public static int ownedWorkers(ServerPlayer player) {
		return ownedWorkerList(player).size();
	}

	public static List<LaborCraftWorker> ownedWorkerList(ServerPlayer player) {
		List<LaborCraftWorker> workers = new ArrayList<>();
		if (player.level().getServer() == null) {
			return workers;
		}

		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LaborCraftWorker worker && worker.isOwnedBy(player.getUUID())) {
					workers.add(worker);
				}
			}
		}

		workers.sort(Comparator.comparing(worker -> worker.getName().getString(), String.CASE_INSENSITIVE_ORDER));
		return workers;
	}

	public static int pendingContracts(Player player) {
		int count = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(LaborCraftItems.WORKER_CONTRACT)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	public static int total(ServerPlayer player) {
		return ownedWorkers(player) + pendingContracts(player);
	}

	/**
	 * Total worker limit for the player: the base config plus one slot per valid house bed.
	 * A base of 0 (or less) means unlimited.
	 */
	public static int limit(ServerPlayer player) {
		int base = LaborCraftConfig.get().worker.maxWorkersPerPlayer;
		if (base <= 0) {
			return base;
		}

		MinecraftServer server = player.level().getServer();
		int houseSlots = server == null ? 0 : HomeRegistry.slots(server, player.getUUID());
		return base + houseSlots;
	}
}

package com.foque.laborcraft.contract;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.config.LaborCraftConfig.JobSettings;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.item.LaborCraftItems;
import com.foque.laborcraft.item.WorkerContractData;
import com.foque.laborcraft.schedule.WorkSchedule;
import com.foque.laborcraft.worker.SalaryTable;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerChat;
import com.foque.laborcraft.worker.WorkerState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Recurring salary billing. Each worker carries its own snapshot (wage per period, period days,
 * next due, debt and missed payments); this manager only reads the config to decide what to do.
 */
public final class BillingManager {
	private static final long TICKS_PER_DAY = 24_000L;

	private BillingManager() {
	}

	/** Forces the next due check for loaded workers (admin test command). */
	public static int forceDue(MinecraftServer server) {
		LaborCraftConfig config = LaborCraftConfig.get();
		if (!config.billing.enabled) {
			return 0;
		}

		int count = 0;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LaborCraftWorker worker && worker.hasOwner()) {
					worker.setNextBillingTick(1L);
					charge(config, worker);
					count++;
				}
			}
		}
		return count;
	}

	public static void tick(MinecraftServer server) {
		LaborCraftConfig config = LaborCraftConfig.get();
		if (!config.billing.enabled) {
			return;
		}

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LaborCraftWorker worker && worker.hasOwner()) {
					charge(config, worker);
				}
			}
		}
	}

	private static void charge(LaborCraftConfig config, LaborCraftWorker worker) {
		WorkerState state = worker.workerState();
		if (state == WorkerState.STORED || state == WorkerState.QUIT) {
			return;
		}

		int days = Math.max(1, worker.billingPeriodDays());
		long now = worker.level().getGameTime();
		if (worker.nextBillingTick() <= 0L) {
			worker.setBillingPeriodDays(days);
			worker.setNextBillingTick(now + days * TICKS_PER_DAY);
			return;
		}

		if (now < worker.nextBillingTick()) {
			applyStrike(config, worker);
			return;
		}

		if (config.schedule.pauseBillingOffDuty
				&& config.billing.useSchedule
				&& !WorkSchedule.isWorking(worker.jobId(), worker.level().getOverworldClockTime())) {
			worker.setNextBillingTick(now + days * TICKS_PER_DAY);
			return;
		}

		long wage = worker.upkeep();
		if (wage <= 0L) {
			worker.setMissedPayments(0);
			worker.setNextBillingTick(now + days * TICKS_PER_DAY);
			return;
		}

		long debt = config.billing.allowDebt ? worker.billingDebt() : 0L;
		long total = wage + debt;
		CurrencyProvider provider = CurrencyProviders.active();
		ServerPlayer owner = serverPlayer(worker);
		long balance = provider.balance(worker.owner());
		worker.setNextBillingTick(now + days * TICKS_PER_DAY);

		if (balance >= total && provider.withdraw(worker.owner(), total)) {
			worker.setBillingDebt(0L);
			worker.setMissedPayments(0);
			if (worker.workerState() == WorkerState.STRIKE) {
				worker.setWorkerState(WorkerState.ACTIVE);
				worker.setWorkNotice("");
				notify(config, owner, Component.translatable(
						"laborcraft.billing.paid_resumed",
						WorkerChat.name(worker),
						provider.format(total)
				));
			}
			return;
		}

		int missed = worker.missedPayments() + 1;
		worker.setMissedPayments(missed);
		worker.setBillingDebt(config.billing.allowDebt ? debt + wage : 0L);
		worker.setMood(worker.mood() - Math.max(0, config.billing.missedMoodPenalty));
		notify(config, owner, Component.translatable(
				"laborcraft.billing.unpaid",
				WorkerChat.name(worker),
				provider.format(wage),
				provider.format(worker.billingDebt()),
				missed
		));

		applyStrike(config, worker);
		applyDismissal(config, owner, worker, missed);
	}

	private static void applyDismissal(
			LaborCraftConfig config, ServerPlayer owner, LaborCraftWorker worker, int missed
	) {
		int threshold = config.billing.dismissAfterMissed;
		if (threshold <= 0 || owner == null) {
			return;
		}

		int strikeAt = config.billing.gracePeriods + 1;
		int postStrikeMisses = missed - strikeAt;
		if (postStrikeMisses < 0) {
			return;
		}

		if (postStrikeMisses >= threshold) {
			WorkerContractData data = WorkerContractData.fromWorker(worker);
			ItemStack contract = LaborCraftItems.newContract(
					config.acquisition.contractTradeable ? null : owner.getUUID(),
					data
			);
			Component name = WorkerChat.name(worker);

			ItemStack fallback = contract.copy();
			boolean hasRoom = owner.getInventory().getFreeSlot() >= 0;

			StationRegistry.unbindWorker(worker.level().getServer(), worker.getUUID());
			HomeRegistry.unassign(worker.level().getServer(), worker.getUUID());
			worker.discard();
			if ((!hasRoom || !owner.getInventory().add(contract)) && owner.level() instanceof ServerLevel level) {
				owner.spawnAtLocation(level, fallback);
			}

			notify(config, owner, Component.translatable("laborcraft.billing.dismissed", name));
			return;
		}

		if (postStrikeMisses == threshold - 1) {
			notify(config, owner, Component.translatable(
					"laborcraft.billing.final_warning",
					WorkerChat.name(worker),
					threshold - postStrikeMisses
			));
		}
	}

	private static void applyStrike(LaborCraftConfig config, LaborCraftWorker worker) {
		if (worker.missedPayments() <= config.billing.gracePeriods
				|| worker.workerState() == WorkerState.STRIKE) {
			return;
		}

		if (worker.isSleeping()) {
			worker.stopSleeping();
		}
		worker.getNavigation().stop();
		worker.setWorkerState(WorkerState.STRIKE);
		worker.setWorkNotice("laborcraft.ui.worker.task.unpaid");
		notify(config, serverPlayer(worker), Component.translatable(
				"laborcraft.billing.strike",
				WorkerChat.name(worker)
		));
	}

	/** Pays the current debt and resumes the worker. Returns 1 on success, 0 otherwise. */
	public static int pay(ServerPlayer player, LaborCraftWorker worker) {
		LaborCraftConfig config = LaborCraftConfig.get();
		if (worker == null || !worker.isOwnedBy(player.getUUID())) {
			return 0;
		}

		long debt = worker.billingDebt();
		if (debt <= 0L) {
			return 0;
		}

		CurrencyProvider provider = CurrencyProviders.active();
		if (!provider.withdraw(player.getUUID(), debt)) {
			return 0;
		}

		worker.setBillingDebt(0L);
		worker.setMissedPayments(0);
		worker.setNextBillingTick(worker.level().getGameTime() + Math.max(1, worker.billingPeriodDays()) * TICKS_PER_DAY);
		if (worker.workerState() == WorkerState.STRIKE) {
			worker.setWorkerState(WorkerState.ACTIVE);
			worker.setWorkNotice("");
		}
		player.sendSystemMessage(Component.translatable(
				"laborcraft.billing.pay.success",
				WorkerChat.name(worker),
				provider.format(debt)
		));
		return 1;
	}

	/** Recomputes the wage snapshot of loaded workers from the current salary table. */
	public static int applySalaryToExisting(MinecraftServer server) {
		LaborCraftConfig config = LaborCraftConfig.get();
		int changed = 0;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (!(entity instanceof LaborCraftWorker worker) || !worker.hasOwner()) {
					continue;
				}

				JobSettings job = jobSettings(config, worker.jobId());
				long wage = SalaryTable.periodWage(
						config,
						job,
						worker.rank(),
						worker.rarity(),
						worker.boosts()
				);
				worker.setUpkeep(wage);
				worker.setBillingPeriodDays(Math.max(1, job.billingPeriodDays));
				changed++;
			}
		}
		return changed;
	}

	private static JobSettings jobSettings(LaborCraftConfig config, String jobId) {
		return config.job(jobId);
	}

	private static ServerPlayer serverPlayer(LaborCraftWorker worker) {
		MinecraftServer server = worker.level().getServer();
		if (server == null || worker.owner() == null) {
			return null;
		}
		return server.getPlayerList().getPlayer(worker.owner());
	}

	private static void notify(LaborCraftConfig config, ServerPlayer owner, Component message) {
		if (config.billing.notifyOwner && owner != null) {
			owner.sendSystemMessage(message);
		}
	}
}

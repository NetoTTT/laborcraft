package com.foque.laborcraft.worker;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.job.JobEquipment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Shared job-brain flow: cooldown, attendance, station/zone checks, pause/resume, tool selection and
 * durability. Jobs implement {@link #tickJob} for production and override the hooks they need.
 */
public abstract class AbstractWorkerBrain implements WorkerBrain {
	protected final LaborCraftWorker worker;
	private final int checkInterval;
	private int cooldown;

	protected AbstractWorkerBrain(LaborCraftWorker worker, int checkInterval) {
		this.worker = worker;
		this.checkInterval = Math.max(1, checkInterval);
	}

	protected final LaborCraftWorker worker() {
		return worker;
	}

	@Override
	public final void tick() {
		if (worker.level().isClientSide() || worker.isAttending()) {
			return;
		}
		if (--cooldown > 0) {
			return;
		}
		cooldown = checkInterval;

		if (!(worker.level() instanceof ServerLevel level)) {
			return;
		}
		StationRegistry.Station station = StationRegistry.byWorker(level.getServer(), worker.getUUID());
		if (station == null) {
			return;
		}

		if (worker.workerState() == WorkerState.PAUSED) {
			if (!canResume(level, station)) {
				return;
			}
			worker.setWorkerState(WorkerState.ACTIVE);
		}
		if (worker.workerState() != WorkerState.ACTIVE) {
			return;
		}

		if (!station.hasZone()) {
			pause(level, "laborcraft.ui.worker.task.need_zone");
			return;
		}

		prepareTool();
		if (!toolReady()) {
			pause(level,
					toolUsable() ? "laborcraft.ui.worker.task.tool_breaking" : "laborcraft.ui.worker.task.need_tool");
			return;
		}

		tickJob(level, station);
	}

	/** Job production step, called only when the worker is active, in a zone, with a valid tool. */
	protected abstract void tickJob(ServerLevel level, StationRegistry.Station station);

	/** Equips a valid tool from the backpack when the tool slot is empty or broken. */
	protected final void prepareTool() {
		ItemStack tool = worker.toolItem();
		if (toolUsable(tool)) {
			return;
		}

		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			ItemStack candidate = worker.inventoryItem(slot);
			if (!toolUsable(candidate)) {
				continue;
			}

			worker.setInventoryItem(slot, tool);
			worker.setInventoryItem(LaborCraftWorker.TOOL_SLOT, candidate);
			return;
		}
	}

	protected final boolean toolReady() {
		return toolUsable(worker.toolItem());
	}

	private boolean toolUsable(ItemStack stack) {
		return !stack.isEmpty()
				&& JobEquipment.validTool(worker.jobId(), stack)
				&& (!stack.isDamageableItem()
						|| stack.getMaxDamage() - stack.getDamageValue() > 2);
	}

	private boolean toolUsable() {
		ItemStack tool = worker.toolItem();
		return !tool.isEmpty() && JobEquipment.validTool(worker.jobId(), tool);
	}

	/** Whether a paused worker can return to work; the default handles zone and tool notices. */
	protected boolean canResume(ServerLevel level, StationRegistry.Station station) {
		String notice = worker.workNotice();
		if (notice == null || notice.isBlank()) {
			return false;
		}

		return switch (notice) {
			case "laborcraft.ui.worker.task.need_zone" -> station.hasZone();
			case "laborcraft.ui.worker.task.need_tool",
					"laborcraft.ui.worker.task.tool_breaking" -> {
				prepareTool();
				yield toolReady();
			}
			default -> canResumeJob(level, station);
		};
	}

	/** Job-specific pause reasons (seeds, chest, etc.). */
	protected boolean canResumeJob(ServerLevel level, StationRegistry.Station station) {
		return false;
	}

	protected void pause(ServerLevel level, String notice) {
		if (worker.workerState() != WorkerState.PAUSED) {
			ServerPlayer owner = worker.owner() == null
					? null
					: level.getServer().getPlayerList().getPlayer(worker.owner());
			if (owner != null) {
				owner.sendSystemMessage(WorkerChat.say(worker, Component.translatable(notice)));
			}
		}

		worker.setWorkNotice(notice);
		worker.setWorkerState(WorkerState.PAUSED);
		worker.getNavigation().stop();
		onPaused();
	}

	/** Reset job-local path/target state when this brain pauses the worker. */
	protected void onPaused() {
	}
}

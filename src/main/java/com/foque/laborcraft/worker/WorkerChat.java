package com.foque.laborcraft.worker;

import com.foque.laborcraft.entity.LaborCraftWorker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Chat formatting shared by worker notices and billing messages. */
public final class WorkerChat {
	private WorkerChat() {
	}

	public static Component name(LaborCraftWorker worker) {
		MutableComponent name = worker.getName().copy();
		return name.withStyle(style -> style.withColor(worker.rarity().color()));
	}

	public static Component say(LaborCraftWorker worker, Component message) {
		return Component.translatable("laborcraft.chat.worker", name(worker), message);
	}
}

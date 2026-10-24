package com.foque.laborcraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Command IDs and registration root. Each domain lives in its own module
 * ({@link WorkerCommands}, {@link StationCommands}, {@link HomeCommands},
 * {@link AdminCommands}, {@link UiCommands}, {@link LanguageCommands});
 * the constants here are the stable API used by the client screens.
 */
public final class LaborCraftCommands {
	public static final String UI = "laborcraft ui";
	public static final String CONFIG = "laborcraft config";
	public static final String TEST_ITEMS = "laborcraft test items";
	public static final String PLAYERS = "laborcraft players";
	public static final String PROFILE = "laborcraft profile";
	public static final String HIRE = "laborcraft hire";
	public static final String WORKER = "laborcraft worker";
	public static final String WORKER_SPAWN = WORKER + " spawn";
	public static final String WORKER_INFO = WORKER + " info";
	public static final String WORKER_REMOVE = WORKER + " remove";
	public static final String WORKER_BUY = WORKER + " buy";
	public static final String WORKER_ASSIGN = WORKER + " assign";
	public static final String WORKER_DISMISS = WORKER + " dismiss";
	public static final String WORKER_OPEN = WORKER + " open";
	public static final String WORKER_INV_OPEN = WORKER + " inv open";
	public static final String WORKER_INV_TAKE = WORKER + " inv take";
	public static final String WORKER_INV_GIVE = WORKER + " inv give";
	public static final String WORKER_INV_SWAP = WORKER + " inv swap";
	public static final String WORKER_INV_DEPOSIT = WORKER + " inv deposit";
	public static final String WORKER_INV_WITHDRAW = WORKER + " inv withdraw";
	public static final String STATION = "laborcraft station";
	public static final String STATION_BIND = STATION + " bind";
	public static final String STATION_UNBIND = STATION + " unbind";
	public static final String STATION_RECALL = STATION + " recall";
	public static final String STATION_RESUME = STATION + " resume";
	public static final String STATION_ZONE_SET = STATION + " zone set";
	public static final String STATION_ZONE_CLEAR = STATION + " zone clear";
	public static final String STATION_STORAGE_LINK = STATION + " storage link";
	public static final String STATION_STORAGE_UNLINK = STATION + " storage unlink";
	public static final String STATION_STORAGE_ROLE = STATION + " storage role";
	public static final String STATION_STORAGE_SCAN = STATION + " storage scan";
	public static final String LANGUAGE = "laborcraft language";
	public static final String LANGUAGE_PT_BR = LANGUAGE + " pt_br";
	public static final String LANGUAGE_EN_US = LANGUAGE + " en_us";
	public static final String ADMIN_RELOAD = "laborcraft admin reload";
	public static final String ADMIN_STATUS = "laborcraft admin status";
	public static final String ADMIN_SET_WAGE = "laborcraft admin set wage";
	public static final String ADMIN_SET_PROVIDER = "laborcraft admin set provider";
	public static final String ADMIN_SET_BALANCE = "laborcraft admin set balance";

	private LaborCraftCommands() {
	}

	public static void bootstrap() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("laborcraft");
			UiCommands.attach(root);
			root.then(WorkerCommands.node());
			root.then(StationCommands.node());
			root.then(HomeCommands.node());
			root.then(LanguageCommands.node());
			root.then(AdminCommands.node());
			dispatcher.register(root);
		});
	}
}

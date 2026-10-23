package com.foque.laborcraft;

import com.foque.laborcraft.block.LaborCraftBlocks;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.item.LaborCraftItems;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.CloseScreenPayload;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.network.OpenConfigPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenItemTestPayload;
import com.foque.laborcraft.network.OpenPlayersPayload;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.SetLanguagePayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import com.foque.laborcraft.worker.WorkerMarket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaborCraft implements ModInitializer {
	public static final String MOD_ID = "laborcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LaborCraftConfig.load();
		LaborCraftBlocks.bootstrap();
		LaborCraftItems.bootstrap();
		LaborCraftEntities.bootstrap();
		CurrencyProviders.bootstrap();
		JobTypes.bootstrap();
		OpenAdminPayload.bootstrap();
		OpenConfigPayload.bootstrap();
		OpenHiringPayload.bootstrap();
		OpenItemTestPayload.bootstrap();
		OpenPlayersPayload.bootstrap();
		OpenProfilePayload.bootstrap();
		OpenWorkerPayload.bootstrap();
		SetLanguagePayload.bootstrap();
		CloseScreenPayload.bootstrap();
		WorkerScreenStatePayload.bootstrap();
		WorkerScreenStatePayload.registerReceiver();
		LaborCraftCommands.bootstrap();
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> WorkerMarket.clear());

		LOGGER.info(
				"LaborCraft carregado: {} empregos, {} provedores de dinheiro, provedor ativo '{}'",
				JobTypes.all().size(),
				CurrencyProviders.all().size(),
				LaborCraftConfig.get().currencyProvider
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

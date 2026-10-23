package com.foque.laborcraft.client;

import com.foque.laborcraft.client.render.LaborCraftWorkerRenderer;
import com.foque.laborcraft.client.ui.chat.LcValuePrompt;
import com.foque.laborcraft.client.ui.screen.LaborCraftAdminScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftConfigScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftEmployeeScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftHiringScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftItemTestScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftPlayersScreen;
import com.foque.laborcraft.client.ui.screen.LaborCraftProfileScreen;
import com.foque.laborcraft.network.CloseScreenPayload;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.network.OpenConfigPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenItemTestPayload;
import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.network.OpenPlayersPayload;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.SetLanguagePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class LaborCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(LaborCraftEntities.WORKER, LaborCraftWorkerRenderer::new);
		ClientSendMessageEvents.ALLOW_CHAT.register(LcValuePrompt::allowChat);
		ClientSendMessageEvents.ALLOW_COMMAND.register(LcValuePrompt::allowCommand);
		ClientSendMessageEvents.CHAT_CANCELED.register(LcValuePrompt::chatCanceled);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LcValuePrompt.cancelActive());

		ClientPlayNetworking.registerGlobalReceiver(OpenAdminPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftAdminScreen());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenConfigPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftConfigScreen());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(SetLanguagePayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> applyLanguage(client, payload.languageCode()));
		});

		ClientPlayNetworking.registerGlobalReceiver(CloseScreenPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				// Só fecha a tela do trabalhador: o servidor não deve fechar inventário/chat do jogador.
				if (client.gui.screen() instanceof LaborCraftEmployeeScreen) {
					client.setScreenAndShow(null);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenItemTestPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftItemTestScreen());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenPlayersPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftPlayersScreen());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenProfilePayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftProfileScreen(payload));
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenHiringPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftHiringScreen(payload));
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenWorkerPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			client.execute(() -> {
				LcValuePrompt.cancelActive();
				client.setScreenAndShow(new LaborCraftEmployeeScreen(payload));
			});
		});
	}

	private static void applyLanguage(Minecraft client, String code) {
		if (!code.equals("pt_br") && !code.equals("en_us")) {
			return;
		}

		if (code.equals(client.getLanguageManager().getSelected())) {
			return;
		}

		boolean configOpen = client.gui.screen() instanceof LaborCraftConfigScreen;
		client.getLanguageManager().setSelected(code);
		client.options.languageCode = code;
		client.options.save();
		client.reloadResourcePacks();

		if (configOpen) {
			client.setScreenAndShow(new LaborCraftConfigScreen());
		}
	}
}

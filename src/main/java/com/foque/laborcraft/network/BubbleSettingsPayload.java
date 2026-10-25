package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-wide visual setting for worker speech bubbles. */
public record BubbleSettingsPayload(boolean throughWalls) implements CustomPacketPayload {
	public static final Type<BubbleSettingsPayload> TYPE =
			new Type<>(LaborCraft.id("bubble_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BubbleSettingsPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeBoolean(payload.throughWalls()),
			buffer -> new BubbleSettingsPayload(buffer.readBoolean())
	);

	public static void send(ServerPlayer player) {
		if (player != null && ServerPlayNetworking.canSend(player, TYPE)) {
			ServerPlayNetworking.send(player, new BubbleSettingsPayload(
					LaborCraftConfig.get().speech.bubbleThroughWalls
			));
		}
	}

	public static void sendAll(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
		}
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

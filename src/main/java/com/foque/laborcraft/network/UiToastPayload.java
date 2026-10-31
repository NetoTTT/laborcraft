package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Lightweight UI feedback: shown as a Quill toast when a screen is open, chat remains the fallback. */
public record UiToastPayload(Component message, boolean error) implements CustomPacketPayload {
	public static final Type<UiToastPayload> TYPE = new Type<>(LaborCraft.id("ui_toast"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UiToastPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				ComponentSerialization.STREAM_CODEC.encode(buffer, payload.message());
				buffer.writeBoolean(payload.error());
			},
			buffer -> new UiToastPayload(
					ComponentSerialization.STREAM_CODEC.decode(buffer),
					buffer.readBoolean()
			)
	);

	public static void send(ServerPlayer player, Component message, boolean error) {
		if (player != null && ServerPlayNetworking.canSend(player, TYPE)) {
			ServerPlayNetworking.send(player, new UiToastPayload(message, error));
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

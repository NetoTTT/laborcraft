package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetLanguagePayload(String languageCode) implements CustomPacketPayload {
	public static final Type<SetLanguagePayload> TYPE = new Type<>(LaborCraft.id("set_language"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetLanguagePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeUtf(payload.languageCode()),
			buffer -> new SetLanguagePayload(buffer.readUtf())
	);

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

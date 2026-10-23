package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenPlayersPayload() implements CustomPacketPayload {
	public static final Type<OpenPlayersPayload> TYPE = new Type<>(LaborCraft.id("open_players"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlayersPayload> CODEC =
			StreamCodec.unit(new OpenPlayersPayload());

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

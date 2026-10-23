package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenConfigPayload() implements CustomPacketPayload {
	public static final Type<OpenConfigPayload> TYPE = new Type<>(LaborCraft.id("open_config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigPayload> CODEC =
			StreamCodec.unit(new OpenConfigPayload());

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

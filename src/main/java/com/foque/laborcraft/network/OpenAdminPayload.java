package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenAdminPayload() implements CustomPacketPayload {
	public static final Type<OpenAdminPayload> TYPE = new Type<>(LaborCraft.id("open_admin"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminPayload> CODEC =
			StreamCodec.unit(new OpenAdminPayload());

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

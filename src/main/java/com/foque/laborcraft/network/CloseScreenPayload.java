package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fecha a tela atual do cliente sem abrir outra. Usado quando a dispensa remove o trabalhador da
 * tela de Empregado: nao faz sentido continuar mostrando a entidade que nao existe mais.
 */
public record CloseScreenPayload() implements CustomPacketPayload {
	public static final Type<CloseScreenPayload> TYPE = new Type<>(LaborCraft.id("close_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CloseScreenPayload> CODEC =
			StreamCodec.unit(new CloseScreenPayload());

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	public static boolean open(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		ServerPlayNetworking.send(player, new CloseScreenPayload());
		return true;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

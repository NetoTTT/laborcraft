package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * O cliente avisa quando a tela do trabalhador abre ou fecha. Enquanto ela estiver aberta, o
 * trabalhador fica parado e olhando para o jogador ({@link LaborCraftWorker#beginAttending}).
 * Sem isso, em servidor, o NPC continuaria andando com o jogador dentro da interface.
 */
public record WorkerScreenStatePayload(UUID workerId, boolean open) implements CustomPacketPayload {
	public static final Type<WorkerScreenStatePayload> TYPE = new Type<>(LaborCraft.id("worker_screen_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkerScreenStatePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.workerId());
				buffer.writeBoolean(payload.open());
			},
			buffer -> new WorkerScreenStatePayload(buffer.readUUID(), buffer.readBoolean())
	);

	public static void bootstrap() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
	}

	public static void registerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				LaborCraftWorker worker = LaborCraftWorker.find(player.level().getServer(), payload.workerId());
				if (worker == null || !worker.isOwnedBy(player.getUUID())) {
					return;
				}

				if (payload.open()) {
					worker.beginAttending(player);
				} else {
					worker.endAttending();
				}
			});
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

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

public record OpenWorkerPayload(
		UUID workerId,
		String name,
		String jobId,
		String state,
		float health,
		float maxHealth,
		int hunger,
		int mood,
		String taskKey,
		boolean vacant
) implements CustomPacketPayload {
	public static final Type<OpenWorkerPayload> TYPE = new Type<>(LaborCraft.id("open_worker"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenWorkerPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.workerId());
				buffer.writeUtf(payload.name(), 256);
				buffer.writeUtf(payload.jobId(), 128);
				buffer.writeUtf(payload.state(), 32);
				buffer.writeFloat(payload.health());
				buffer.writeFloat(payload.maxHealth());
				buffer.writeVarInt(payload.hunger());
				buffer.writeVarInt(payload.mood());
				buffer.writeUtf(payload.taskKey(), 128);
				buffer.writeBoolean(payload.vacant());
			},
			buffer -> new OpenWorkerPayload(
					buffer.readUUID(),
					buffer.readUtf(256),
					buffer.readUtf(128),
					buffer.readUtf(32),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readUtf(128),
					buffer.readBoolean()
			)
	);

	public static OpenWorkerPayload of(LaborCraftWorker worker) {
		return new OpenWorkerPayload(
				worker.getUUID(),
				worker.getName().getString(),
				worker.jobId(),
				worker.workerState().name(),
				worker.getHealth(),
				worker.getMaxHealth(),
				worker.hunger(),
				worker.mood(),
				worker.taskKey(),
				!worker.hasOwner()
		);
	}

	public static boolean open(ServerPlayer player, LaborCraftWorker worker) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		ServerPlayNetworking.send(player, of(worker));
		return true;
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

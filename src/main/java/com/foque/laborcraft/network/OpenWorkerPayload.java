package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.Mastery;
import com.foque.laborcraft.worker.WorkerBoost;
import java.util.ArrayList;
import java.util.List;
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
		String rankId,
		String rarityId,
		List<String> boosts,
		int aptitude,
		int masteryLevel,
		long masteryXp,
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
				buffer.writeUtf(payload.rankId(), 32);
				buffer.writeUtf(payload.rarityId(), 32);
				buffer.writeVarInt(payload.boosts().size());
				for (String boost : payload.boosts()) {
					buffer.writeUtf(boost, 64);
				}
				buffer.writeVarInt(payload.aptitude());
				buffer.writeVarInt(payload.masteryLevel());
				buffer.writeVarLong(payload.masteryXp());
				buffer.writeBoolean(payload.vacant());
			},
			buffer -> {
				UUID workerId = buffer.readUUID();
				String name = buffer.readUtf(256);
				String jobId = buffer.readUtf(128);
				String state = buffer.readUtf(32);
				float health = buffer.readFloat();
				float maxHealth = buffer.readFloat();
				int hunger = buffer.readVarInt();
				int mood = buffer.readVarInt();
				String taskKey = buffer.readUtf(128);
				String rankId = buffer.readUtf(32);
				String rarityId = buffer.readUtf(32);
				int boostCount = buffer.readVarInt();
				List<String> boosts = new ArrayList<>(boostCount);
				for (int index = 0; index < boostCount; index++) {
					boosts.add(buffer.readUtf(64));
				}
				return new OpenWorkerPayload(
						workerId,
						name,
						jobId,
						state,
						health,
						maxHealth,
						hunger,
						mood,
						taskKey,
						rankId,
						rarityId,
						List.copyOf(boosts),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarLong(),
						buffer.readBoolean()
				);
			}
	);

	public static OpenWorkerPayload of(LaborCraftWorker worker) {
		long masteryXp = worker.hasJob() ? worker.masteryFor(worker.jobId()) : 0L;
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
				worker.rank().id(),
				worker.rarity().id(),
				worker.boosts().stream().map(WorkerBoost::serialize).toList(),
				worker.hasJob() ? worker.aptitudeFor(worker.jobId()) : 0,
				worker.hasJob() ? Mastery.levelFor(masteryXp) : 0,
				masteryXp,
				!worker.hasOwner()
		);
	}

	public static boolean open(ServerPlayer player, LaborCraftWorker worker) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		// Congela já na abertura; o cliente confirma abre/fecha depois com WorkerScreenStatePayload.
		worker.beginAttending(player);
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

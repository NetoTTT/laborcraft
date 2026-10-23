package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record OpenProfilePayload(
		String playerName,
		String balanceText,
		int workers,
		int limit,
		List<WorkerSummary> summaries
) implements CustomPacketPayload {
	public static final Type<OpenProfilePayload> TYPE = new Type<>(LaborCraft.id("open_profile"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenProfilePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUtf(payload.playerName(), 128);
				buffer.writeUtf(payload.balanceText(), 64);
				buffer.writeVarInt(payload.workers());
				buffer.writeVarInt(payload.limit());
				buffer.writeVarInt(payload.summaries().size());
				for (WorkerSummary summary : payload.summaries()) {
					buffer.writeUUID(summary.id());
					buffer.writeUtf(summary.name(), 256);
					buffer.writeUtf(summary.jobId(), 128);
					buffer.writeUtf(summary.state(), 32);
					buffer.writeUtf(summary.taskKey(), 128);
					buffer.writeFloat(summary.health());
					buffer.writeFloat(summary.maxHealth());
					buffer.writeVarInt(summary.hunger());
					buffer.writeVarInt(summary.mood());
				}
			},
			buffer -> {
				String playerName = buffer.readUtf(128);
				String balanceText = buffer.readUtf(64);
				int workers = buffer.readVarInt();
				int limit = buffer.readVarInt();
				int summaryCount = buffer.readVarInt();
				List<WorkerSummary> summaries = new ArrayList<>(summaryCount);
				for (int index = 0; index < summaryCount; index++) {
					summaries.add(new WorkerSummary(
							buffer.readUUID(),
							buffer.readUtf(256),
							buffer.readUtf(128),
							buffer.readUtf(32),
							buffer.readUtf(128),
							buffer.readFloat(),
							buffer.readFloat(),
							buffer.readVarInt(),
							buffer.readVarInt()
					));
				}
				return new OpenProfilePayload(playerName, balanceText, workers, limit, List.copyOf(summaries));
			}
	);

	public static OpenProfilePayload of(ServerPlayer player) {
		CurrencyProvider provider = CurrencyProviders.active();
		List<WorkerSummary> summaries = new ArrayList<>();
		for (LaborCraftWorker worker : WorkerOwnership.ownedWorkerList(player)) {
			summaries.add(new WorkerSummary(
					worker.getUUID(),
					worker.getName().getString(),
					worker.jobId(),
					worker.workerState().name(),
					worker.taskKey(),
					worker.getHealth(),
					worker.getMaxHealth(),
					worker.hunger(),
					worker.mood()
			));
		}

		return new OpenProfilePayload(
				player.getName().getString(),
				provider.format(provider.balance(player.getUUID())),
				summaries.size(),
				LaborCraftConfig.get().worker.maxWorkersPerPlayer,
				List.copyOf(summaries)
		);
	}

	public static boolean open(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		ServerPlayNetworking.send(player, of(player));
		return true;
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public record WorkerSummary(
			UUID id,
			String name,
			String jobId,
			String state,
			String taskKey,
			float health,
			float maxHealth,
			int hunger,
			int mood
	) {
	}
}

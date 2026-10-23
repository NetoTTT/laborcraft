package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tela do Posto de Trabalho: vagas do posto (hoje 1), trabalhador vinculado e a lista de
 * empregados do jogador para vincular.
 */
public record OpenStationPayload(
		BlockPos pos,
		String mode,
		ZoneView zone,
		Summary bound,
		List<Summary> workers
) implements CustomPacketPayload {
	public static final Type<OpenStationPayload> TYPE = new Type<>(LaborCraft.id("open_station"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenStationPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeBlockPos(payload.pos());
				buffer.writeUtf(payload.mode(), 16);
				buffer.writeBoolean(payload.zone() != null);
				if (payload.zone() != null) {
					buffer.writeBlockPos(payload.zone().from());
					buffer.writeBlockPos(payload.zone().to());
				}
				buffer.writeBoolean(payload.bound() != null);
				if (payload.bound() != null) {
					writeSummary(buffer, payload.bound());
				}
				buffer.writeVarInt(payload.workers().size());
				for (Summary summary : payload.workers()) {
					writeSummary(buffer, summary);
				}
			},
			buffer -> {
				BlockPos pos = buffer.readBlockPos();
				String mode = buffer.readUtf(16);
				ZoneView zone = buffer.readBoolean()
						? new ZoneView(buffer.readBlockPos(), buffer.readBlockPos())
						: null;
				Summary bound = buffer.readBoolean() ? readSummary(buffer) : null;
				int count = buffer.readVarInt();
				List<Summary> workers = new ArrayList<>(count);
				for (int index = 0; index < count; index++) {
					workers.add(readSummary(buffer));
				}
				return new OpenStationPayload(pos, mode, zone, bound, List.copyOf(workers));
			}
	);

	public static OpenStationPayload of(ServerPlayer player, StationRegistry.Station station) {
		List<Summary> workers = new ArrayList<>();
		Summary bound = null;

		for (LaborCraftWorker worker : WorkerOwnership.ownedWorkerList(player)) {
			StationRegistry.Station other = StationRegistry.byWorker(player.level().getServer(), worker.getUUID());
			boolean elsewhere = other != null && !other.pos().equals(station.pos());
			Summary summary = new Summary(
					worker.getUUID(),
					worker.getName().getString(),
					worker.rank().id(),
					worker.jobId(),
					worker.workerState().name(),
					worker.taskKey(),
					elsewhere
			);
			workers.add(summary);

			if (worker.getUUID().equals(station.worker())) {
				bound = summary;
			}
		}

		ZoneView zone = station.zones().isEmpty()
				? null
				: new ZoneView(station.zones().getFirst().from(), station.zones().getFirst().to());

		return new OpenStationPayload(
				station.pos(),
				station.mode().name(),
				zone,
				bound,
				List.copyOf(workers)
		);
	}

	public static boolean open(ServerPlayer player, StationRegistry.Station station) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		ServerPlayNetworking.send(player, of(player, station));
		return true;
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void writeSummary(RegistryFriendlyByteBuf buffer, Summary summary) {
		buffer.writeUUID(summary.id());
		buffer.writeUtf(summary.name(), 96);
		buffer.writeUtf(summary.rankId(), 32);
		buffer.writeUtf(summary.jobId(), 128);
		buffer.writeUtf(summary.state(), 32);
		buffer.writeUtf(summary.taskKey(), 128);
		buffer.writeBoolean(summary.boundElsewhere());
	}

	private static Summary readSummary(RegistryFriendlyByteBuf buffer) {
		return new Summary(
				buffer.readUUID(),
				buffer.readUtf(96),
				buffer.readUtf(32),
				buffer.readUtf(128),
				buffer.readUtf(32),
				buffer.readUtf(128),
				buffer.readBoolean()
		);
	}

	public record ZoneView(BlockPos from, BlockPos to) {
		public int width() {
			return Math.abs(to.getX() - from.getX()) + 1;
		}

		public int depth() {
			return Math.abs(to.getZ() - from.getZ()) + 1;
		}
	}

	public record Summary(
			UUID id,
			String name,
			String rankId,
			String jobId,
			String state,
			String taskKey,
			boolean boundElsewhere
	) {
		public boolean idle() {
			return "PAUSED".equals(state);
		}
	}
}

package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.StationAreas;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.ArrayList;
import java.util.Comparator;
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
		int maxRadius,
		int verticalRadius,
		Summary bound,
		List<Summary> workers,
		List<AreaView> ownAreas,
		List<AreaView> areas,
		List<BlockPos> otherPosts
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
				buffer.writeVarInt(payload.maxRadius());
				buffer.writeVarInt(payload.verticalRadius());
				buffer.writeBoolean(payload.bound() != null);
				if (payload.bound() != null) {
					writeSummary(buffer, payload.bound());
				}
				buffer.writeVarInt(payload.workers().size());
				for (Summary summary : payload.workers()) {
					writeSummary(buffer, summary);
				}
				buffer.writeVarInt(payload.ownAreas().size());
				for (AreaView area : payload.ownAreas()) {
					buffer.writeUtf(area.kind(), 16);
					buffer.writeBlockPos(area.from());
					buffer.writeBlockPos(area.to());
					buffer.writeBoolean(area.blocksDraft());
				}
				buffer.writeVarInt(payload.areas().size());
				for (AreaView area : payload.areas()) {
					buffer.writeUtf(area.kind(), 16);
					buffer.writeBlockPos(area.from());
					buffer.writeBlockPos(area.to());
					buffer.writeBoolean(area.blocksDraft());
				}
				buffer.writeVarInt(payload.otherPosts().size());
				for (BlockPos post : payload.otherPosts()) {
					buffer.writeBlockPos(post);
				}
			},
			buffer -> {
				BlockPos pos = buffer.readBlockPos();
				String mode = buffer.readUtf(16);
				ZoneView zone = buffer.readBoolean()
						? new ZoneView(buffer.readBlockPos(), buffer.readBlockPos())
						: null;
				int maxRadius = buffer.readVarInt();
				int verticalRadius = buffer.readVarInt();
				Summary bound = buffer.readBoolean() ? readSummary(buffer) : null;
				int count = buffer.readVarInt();
				List<Summary> workers = new ArrayList<>(count);
				for (int index = 0; index < count; index++) {
					workers.add(readSummary(buffer));
				}
				int ownAreaCount = buffer.readVarInt();
				List<AreaView> ownAreas = new ArrayList<>(ownAreaCount);
				for (int index = 0; index < ownAreaCount; index++) {
					ownAreas.add(new AreaView(
							buffer.readUtf(16),
							buffer.readBlockPos(),
							buffer.readBlockPos(),
							buffer.readBoolean()
					));
				}
				int areaCount = buffer.readVarInt();
				List<AreaView> areas = new ArrayList<>(areaCount);
				for (int index = 0; index < areaCount; index++) {
					areas.add(new AreaView(
							buffer.readUtf(16),
							buffer.readBlockPos(),
							buffer.readBlockPos(),
							buffer.readBoolean()
					));
				}
				int postCount = buffer.readVarInt();
				List<BlockPos> otherPosts = new ArrayList<>(postCount);
				for (int index = 0; index < postCount; index++) {
					otherPosts.add(buffer.readBlockPos());
				}
				return new OpenStationPayload(
						pos, mode, zone, maxRadius, verticalRadius, bound,
						List.copyOf(workers), List.copyOf(ownAreas), List.copyOf(areas),
						List.copyOf(otherPosts)
				);
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
		int maxRadius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		List<StationRegistry.Station> all = StationRegistry.all(player.level().getServer());
		List<StationRegistry.Station> nearby = nearbyStations(station, all);

		return new OpenStationPayload(
				station.pos(),
				station.mode().name(),
				zone,
				maxRadius,
				Math.max(1, LaborCraftConfig.get().area.verticalRadius),
				bound,
				List.copyOf(workers),
				toAreaViews(station, StationAreas.effectiveSpans(station, all, maxRadius), false),
				nearbyAreas(station, nearby, all, maxRadius),
				nearby.stream().map(StationRegistry.Station::pos).toList()
		);
	}

	private static List<StationRegistry.Station> nearbyStations(
			StationRegistry.Station station,
			List<StationRegistry.Station> all
	) {
		List<StationRegistry.Station> nearby = new ArrayList<>();
		for (StationRegistry.Station other : all) {
			if (other.pos().equals(station.pos()) || !other.dimension().equals(station.dimension())) {
				continue;
			}

			int dx = other.pos().getX() - station.pos().getX();
			int dz = other.pos().getZ() - station.pos().getZ();
			if (Math.abs(dx) > 256 || Math.abs(dz) > 256) {
				continue;
			}

			nearby.add(other);
		}

		nearby.sort(Comparator.comparingInt((StationRegistry.Station other) -> {
			int dx = other.pos().getX() - station.pos().getX();
			int dz = other.pos().getZ() - station.pos().getZ();
			return dx * dx + dz * dz;
		}));

		return nearby.size() > 64 ? List.copyOf(nearby.subList(0, 64)) : List.copyOf(nearby);
	}

	private static List<AreaView> nearbyAreas(
			StationRegistry.Station station,
			List<StationRegistry.Station> nearby,
			List<StationRegistry.Station> all,
			int maxRadius
	) {
		List<AreaView> areas = new ArrayList<>();
		for (StationRegistry.Station other : nearby) {
			areas.addAll(toAreaViews(
					other,
					StationAreas.effectiveSpans(other, all, maxRadius),
					StationAreas.isHigherPriority(other, station)
			));
		}

		return List.copyOf(areas);
	}

	private static List<AreaView> toAreaViews(
			StationRegistry.Station station,
			List<StationAreas.Span> spans,
			boolean blocksDraft
	) {
		List<AreaView> areas = new ArrayList<>(spans.size());
		int y = station.pos().getY();
		for (StationAreas.Span span : spans) {
			areas.add(new AreaView(
					"effective",
					new BlockPos(span.x0(), y, span.z()),
					new BlockPos(span.x1(), y, span.z()),
					blocksDraft
			));
		}
		return List.copyOf(areas);
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

	/**
	 * Effective horizontal span of a post area. The map combines spans into the clipped shape.
	 * {@code blocksDraft} is true when this station is older than the open station, so a new
	 * draft selection must be clipped by it.
	 */
	public record AreaView(String kind, BlockPos from, BlockPos to, boolean blocksDraft) {
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

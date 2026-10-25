package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.worker.Mastery;
import com.foque.laborcraft.worker.WorkerBoost;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
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
		boolean vacant,
		boolean hasHome,
		int homeX,
		int homeY,
		int homeZ,
		boolean homeValid,
		String homeFailure,
		String homeName,
		List<HouseSummary> houses
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
				buffer.writeBoolean(payload.hasHome());
				buffer.writeVarInt(payload.homeX());
				buffer.writeVarInt(payload.homeY());
				buffer.writeVarInt(payload.homeZ());
				buffer.writeBoolean(payload.homeValid());
				buffer.writeUtf(payload.homeFailure(), 32);
				buffer.writeUtf(payload.homeName(), 48);
				buffer.writeVarInt(payload.houses().size());
				for (HouseSummary house : payload.houses()) {
					buffer.writeUtf(house.name(), 48);
					buffer.writeVarInt(house.x());
					buffer.writeVarInt(house.y());
					buffer.writeVarInt(house.z());
					buffer.writeVarInt(house.slots());
					buffer.writeVarInt(house.free());
					buffer.writeBoolean(house.valid());
					buffer.writeBoolean(house.current());
					buffer.writeUtf(house.failure(), 32);
					buffer.writeVarInt(house.freeBedX());
					buffer.writeVarInt(house.freeBedY());
					buffer.writeVarInt(house.freeBedZ());
				}
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
						buffer.readBoolean(),
						buffer.readBoolean(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readBoolean(),
						buffer.readUtf(32),
						buffer.readUtf(48),
						readHouses(buffer)
				);
			}
	);

	private static List<HouseSummary> readHouses(RegistryFriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<HouseSummary> houses = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			houses.add(new HouseSummary(
					buffer.readUtf(48),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readUtf(32),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt()
			));
		}
		return List.copyOf(houses);
	}

	public static OpenWorkerPayload of(LaborCraftWorker worker) {
		long masteryXp = worker.hasJob() ? worker.masteryFor(worker.jobId()) : 0L;
		HomeRegistry.Home home = worker.level().getServer() == null
				? null
				: HomeRegistry.byWorker(worker.level().getServer(), worker.getUUID());
		List<HouseSummary> houses = new ArrayList<>();
		if (worker.level().getServer() != null && worker.owner() != null) {
			String dimension = worker.level().dimension().identifier().toString();
			HomeRegistry.byOwner(worker.level().getServer(), worker.owner()).stream()
					.filter(candidate -> candidate.dimension().equals(dimension))
					.sorted(Comparator.comparing(HomeRegistry.Home::name, String.CASE_INSENSITIVE_ORDER))
					.forEach(candidate -> {
						BlockPos freeBed = candidate.firstFreeBed();
						houses.add(new HouseSummary(
								candidate.name(),
								candidate.bed().getX(),
								candidate.bed().getY(),
								candidate.bed().getZ(),
								candidate.slots(),
								candidate.freeBeds().size(),
								candidate.valid(),
								home != null && home.id().equals(candidate.id()),
								candidate.failure(),
								freeBed == null ? 0 : freeBed.getX(),
								freeBed == null ? 0 : freeBed.getY(),
								freeBed == null ? 0 : freeBed.getZ()
						));
					});
		}

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
				!worker.hasOwner(),
				home != null,
				home == null ? 0 : home.bed().getX(),
				home == null ? 0 : home.bed().getY(),
				home == null ? 0 : home.bed().getZ(),
				home != null && home.valid(),
				home == null ? "" : home.failure(),
				home == null ? "" : home.name(),
				List.copyOf(houses)
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

	public record HouseSummary(
			String name,
			int x,
			int y,
			int z,
			int slots,
			int free,
			boolean valid,
			boolean current,
			String failure,
			int freeBedX,
			int freeBedY,
			int freeBedZ
	) {
		public HouseSummary {
			failure = failure == null ? "" : failure;
		}
	}
}

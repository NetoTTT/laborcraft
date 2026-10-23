package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOffers;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record OpenHiringPayload(
		List<Offer> offers,
		long balance,
		String balanceText,
		int owned,
		int pending,
		int limit,
		String currencyId
) implements CustomPacketPayload {
	public static final Type<OpenHiringPayload> TYPE = new Type<>(LaborCraft.id("open_hiring"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenHiringPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.offers().size());
				for (Offer offer : payload.offers()) {
					buffer.writeUtf(offer.id(), 128);
					buffer.writeUtf(offer.jobId(), 128);
					buffer.writeUtf(offer.nameKey(), 128);
					buffer.writeUtf(offer.descriptionKey(), 128);
					buffer.writeUtf(offer.boostKey(), 128);
					buffer.writeLong(offer.price());
					buffer.writeUtf(offer.priceText(), 64);
					buffer.writeLong(offer.upkeep());
					buffer.writeUtf(offer.upkeepText(), 64);
				}
				buffer.writeLong(payload.balance());
				buffer.writeUtf(payload.balanceText(), 64);
				buffer.writeVarInt(payload.owned());
				buffer.writeVarInt(payload.pending());
				buffer.writeVarInt(payload.limit());
				buffer.writeUtf(payload.currencyId(), 128);
			},
			buffer -> {
				int offerCount = buffer.readVarInt();
				List<Offer> offers = new ArrayList<>(offerCount);
				for (int index = 0; index < offerCount; index++) {
					offers.add(new Offer(
							buffer.readUtf(128),
							buffer.readUtf(128),
							buffer.readUtf(128),
							buffer.readUtf(128),
							buffer.readUtf(128),
							buffer.readLong(),
							buffer.readUtf(64),
							buffer.readLong(),
							buffer.readUtf(64)
					));
				}
				return new OpenHiringPayload(
						List.copyOf(offers),
						buffer.readLong(),
						buffer.readUtf(64),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readVarInt(),
						buffer.readUtf(128)
				);
			}
	);

	public static OpenHiringPayload of(ServerPlayer player) {
		LaborCraftConfig config = LaborCraftConfig.get();
		CurrencyProvider provider = CurrencyProviders.active();
		List<Offer> offers = new ArrayList<>();
		for (WorkerOffer offer : WorkerOffers.available()) {
			offers.add(new Offer(
					offer.id(),
					offer.jobId(),
					offer.nameKey(),
					offer.descriptionKey(),
					offer.boostKey(),
					offer.price(),
					offer.free() ? "" : provider.format(offer.price()),
					offer.upkeep(),
					offer.upkeep() <= 0L ? "" : provider.format(offer.upkeep())
			));
		}

		long balance = provider.balance(player.getUUID());
		return new OpenHiringPayload(
				List.copyOf(offers),
				balance,
				provider.format(balance),
				WorkerOwnership.ownedWorkers(player),
				WorkerOwnership.pendingContracts(player),
				config.worker.maxWorkersPerPlayer,
				provider.id()
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

	public boolean atLimit() {
		return limit > 0 && owned + pending >= limit;
	}

	public boolean canAfford(Offer offer) {
		return offer != null && (offer.price() <= 0L || balance >= offer.price());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public record Offer(
			String id,
			String jobId,
			String nameKey,
			String descriptionKey,
			String boostKey,
			long price,
			String priceText,
			long upkeep,
			String upkeepText
	) {
		public boolean free() {
			return price <= 0L;
		}

		public boolean hasBoost() {
			return boostKey != null && !boostKey.isBlank();
		}
	}
}

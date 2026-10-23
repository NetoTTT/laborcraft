package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.worker.WorkerBoost;
import com.foque.laborcraft.worker.WorkerMarket;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loja publica de contratacao. As ofertas vem do mercado do servidor (global e compartilhado),
 * nao do jogador.
 */
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
					buffer.writeUtf(offer.id(), 160);
					buffer.writeUtf(offer.name(), 96);
					buffer.writeUtf(offer.rankId(), 32);
					buffer.writeUtf(offer.rarityId(), 32);
					buffer.writeVarInt(offer.boosts().size());
					for (String boost : offer.boosts()) {
						buffer.writeUtf(boost, 64);
					}
					buffer.writeVarInt(offer.aptitude());
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
					String id = buffer.readUtf(160);
					String name = buffer.readUtf(96);
					String rankId = buffer.readUtf(32);
					String rarityId = buffer.readUtf(32);
					int boostCount = buffer.readVarInt();
					List<String> boosts = new ArrayList<>(boostCount);
					for (int boostIndex = 0; boostIndex < boostCount; boostIndex++) {
						boosts.add(buffer.readUtf(64));
					}
					offers.add(new Offer(
							id,
							name,
							rankId,
							rarityId,
							List.copyOf(boosts),
							buffer.readVarInt(),
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
		for (WorkerOffer offer : WorkerMarket.offers(player.level().getServer())) {
			offers.add(new Offer(
					offer.id(),
					offer.name(),
					offer.rank().id(),
					offer.rarity().id(),
					offer.boosts().stream().map(WorkerBoost::serialize).toList(),
					offer.aptitudeFor(com.foque.laborcraft.job.JobTypes.FARMER.id()),
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
			String name,
			String rankId,
			String rarityId,
			List<String> boosts,
			int aptitude,
			long price,
			String priceText,
			long upkeep,
			String upkeepText
	) {
		public boolean free() {
			return price <= 0L;
		}

		public boolean hasBoost() {
			return !boosts.isEmpty();
		}
	}
}

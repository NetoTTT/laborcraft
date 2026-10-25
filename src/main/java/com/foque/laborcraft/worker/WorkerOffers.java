package com.foque.laborcraft.worker;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

/**
 * Gerador deterministico das ofertas do servidor. O mercado e global (todos os jogadores veem a
 * mesma loja) e gira com o tempo do mundo; a partir de (seed do mundo + rotacao) as ofertas sao
 * sempre as mesmas, entao nao precisam ser guardadas para serem reproduzidas.
 */
public final class WorkerOffers {
	public static final long TICKS_PER_DAY = 24000L;
	private static final long SEED_SALT = 0x9E3779B97F4A7C15L;

	private WorkerOffers() {
	}

	public static long rotation(MinecraftServer server) {
		LaborCraftConfig config = LaborCraftConfig.get();
		int days = Math.max(1, config.offers.rotationDays);
		return server.overworld().getGameTime() / Math.max(1L, TICKS_PER_DAY * days);
	}

	public static List<WorkerOffer> generate(MinecraftServer server, long rotation) {
		LaborCraftConfig config = LaborCraftConfig.get();
		int count = Math.clamp(config.offers.offerCount, 1, 20);
		RandomSource random = RandomSource.create(seed(server, rotation));
		List<WorkerOffer> offers = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			WorkerRank rank = WorkerRank.roll(random);
			WorkerRarity rarity = WorkerRarity.roll(random);
			List<WorkerBoost> boosts = rollBoosts(random, rarity);
			String id = "laborcraft:offer/" + rotation + "/" + index;

			offers.add(new WorkerOffer(
					id,
					WorkerNames.random(random),
					rank,
					rarity,
					boosts,
					WorkerAptitudes.roll(random),
					price(config, rank, rarity),
					SalaryTable.periodWage(config, config.farmer, rank, rarity, boosts)
			));
		}

		return List.copyOf(offers);
	}

	public static long seed(MinecraftServer server, long rotation) {
		return server.overworld().getSeed() * 31L + rotation * SEED_SALT;
	}

	private static List<WorkerBoost> rollBoosts(RandomSource random, WorkerRarity rarity) {
		List<WorkerBoost> boosts = new ArrayList<>(rarity.boostCount());
		List<WorkerBoostType> pool = new ArrayList<>(List.of(WorkerBoostType.values()));

		for (int index = 0; index < rarity.boostCount() && !pool.isEmpty(); index++) {
			WorkerBoostType type = pool.remove(random.nextInt(pool.size()));
			boosts.add(WorkerBoost.of(type, index < rarity.strongBoosts()));
		}

		return List.copyOf(boosts);
	}

	private static long price(LaborCraftConfig config, WorkerRank rank, WorkerRarity rarity) {
		if ("free".equalsIgnoreCase(config.acquisition.mode)) {
			return 0L;
		}

		long base = Math.max(0L, config.acquisition.price);
		return Math.round(base * rank.priceMultiplier() * rarity.priceMultiplier());
	}

}

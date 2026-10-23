package com.foque.laborcraft.worker;

import com.foque.laborcraft.LaborCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Mercado de trabalho do servidor. Uma loja por mundo, igual para todos os jogadores, com estoque
 * compartilhado: quem compra primeiro leva. Fica salva em {@code <mundo>/data/laborcraft/market.json}
 * e gira sozinha quando o dia do mundo avanca; as ofertas nao vendidas sao substituidas na virada.
 */
public final class WorkerMarket {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static WorkerMarket instance;

	private long rotation = Long.MIN_VALUE;
	private List<WorkerOffer> offers = new ArrayList<>();
	private Path file;

	private WorkerMarket() {
	}

	public static List<WorkerOffer> offers(MinecraftServer server) {
		if (server == null) {
			return List.of();
		}

		WorkerMarket market = load(server);
		return List.copyOf(market.current(server));
	}

	public static int remaining(MinecraftServer server) {
		return offers(server).size();
	}

	/**
	 * Retira a oferta do estoque e devolve a pessoa comprada. Retorna {@code null} quando a oferta
	 * nao existe mais (vendida ou de uma rotacao anterior).
	 */
	public static WorkerOffer take(MinecraftServer server, String offerId) {
		if (offerId == null || offerId.isBlank()) {
			return null;
		}

		WorkerMarket market = load(server);
		market.current(server);

		for (int index = 0; index < market.offers.size(); index++) {
			if (market.offers.get(index).id().equals(offerId)) {
				WorkerOffer offer = market.offers.remove(index);
				market.save();
				return offer;
			}
		}

		return null;
	}

	/** Devolve uma oferta ao estoque (compra cancelada depois de retirada). */
	public static void restore(MinecraftServer server, WorkerOffer offer) {
		if (server == null || offer == null) {
			return;
		}

		WorkerMarket market = load(server);
		market.current(server);
		if (market.offers.stream().noneMatch(entry -> entry.id().equals(offer.id()))) {
			market.offers.add(offer);
			market.save();
		}
	}

	/** Forca uma nova rotacao (usado por admin/teste). */
	public static void refresh(MinecraftServer server) {
		WorkerMarket market = load(server);
		market.rotation = Long.MIN_VALUE;
		market.offers = new ArrayList<>();
		market.current(server);
		market.save();
	}

	public static void clear() {
		instance = null;
	}

	private static WorkerMarket load(MinecraftServer server) {
		Path path = marketFile(server);
		if (instance != null && path.equals(instance.file)) {
			return instance;
		}

		WorkerMarket market = new WorkerMarket();
		market.file = path;
		market.read();
		instance = market;
		return market;
	}

	private static Path marketFile(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("laborcraft").resolve("market.json");
	}

	private List<WorkerOffer> current(MinecraftServer server) {
		long currentRotation = WorkerOffers.rotation(server);
		if (rotation != currentRotation) {
			rotation = currentRotation;
			offers = new ArrayList<>(WorkerOffers.generate(server, currentRotation));
			save();
		}

		return offers;
	}

	private void read() {
		if (file == null || !Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			MarketData data = GSON.fromJson(reader, MarketData.class);
			if (data == null || data.offers == null) {
				return;
			}

			rotation = data.rotation;
			offers = new ArrayList<>(data.offers.size());
			for (OfferData entry : data.offers) {
				if (entry == null || entry.id == null || entry.id.isBlank()) {
					continue;
				}

				offers.add(new WorkerOffer(
						entry.id,
						entry.name == null ? "" : entry.name,
						WorkerRank.of(entry.rank),
						WorkerRarity.of(entry.rarity),
						WorkerBoost.split(entry.boosts),
						WorkerAptitudes.parse(entry.aptitudes),
						Math.max(0L, entry.price),
						Math.max(0L, entry.upkeep)
				));
			}
		} catch (IOException | RuntimeException exception) {
			LaborCraft.LOGGER.error("Falha ao ler o mercado {}", file, exception);
		}
	}

	private void save() {
		if (file == null) {
			return;
		}

		MarketData data = new MarketData();
		data.rotation = rotation;
		data.offers = new ArrayList<>(offers.size());
		for (WorkerOffer offer : offers) {
			OfferData entry = new OfferData();
			entry.id = offer.id();
			entry.name = offer.name();
			entry.rank = offer.rank().id();
			entry.rarity = offer.rarity().id();
			entry.boosts = offer.boostSerialize();
			entry.aptitudes = offer.aptitudeSerialize();
			entry.price = offer.price();
			entry.upkeep = offer.upkeep();
			data.offers.add(entry);
		}

		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException exception) {
			LaborCraft.LOGGER.error("Falha ao salvar o mercado {}", file, exception);
		}
	}

	private static final class MarketData {
		private long rotation = Long.MIN_VALUE;
		private List<OfferData> offers = new ArrayList<>();
	}

	private static final class OfferData {
		private String id;
		private String name;
		private String rank;
		private String rarity;
		private String boosts;
		private String aptitudes;
		private long price;
		private long upkeep;
	}
}

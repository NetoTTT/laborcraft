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
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Registro dos postos de trabalho do mundo. Cada posto guarda o dono, o trabalhador vinculado
 * (uma vaga por enquanto), o modo de area (raio livre ou zona desenhada) e a zona.
 *
 * <p>Fica em {@code <mundo>/data/laborcraft/stations.json}. Um posto quebrado nao apaga o registro:
 * se o bloco voltar na mesma posicao, o vinculo continua.</p>
 */
public final class StationRegistry {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static StationRegistry instance;

	private final List<Station> stations = new ArrayList<>();
	private Path file;

	private StationRegistry() {
	}

	public static Station ensure(MinecraftServer server, String dimension, BlockPos pos, UUID owner) {
		StationRegistry registry = load(server);
		Station existing = registry.find(dimension, pos);
		if (existing != null) {
			return existing;
		}

		Station station = new Station(
				"laborcraft:station/%d_%d_%d".formatted(pos.getX(), pos.getY(), pos.getZ()),
				dimension,
				pos.immutable(),
				owner,
				null,
				StationMode.FREE,
				List.of(),
				null,
				System.currentTimeMillis()
		);
		registry.stations.add(station);
		registry.save();
		return station;
	}

	public static Station get(MinecraftServer server, String dimension, BlockPos pos) {
		return load(server).find(dimension, pos);
	}

	public static List<Station> all(MinecraftServer server) {
		return List.copyOf(load(server).stations);
	}

	public static Station byWorker(MinecraftServer server, UUID worker) {
		if (worker == null) {
			return null;
		}

		for (Station station : load(server).stations) {
			if (worker.equals(station.worker())) {
				return station;
			}
		}

		return null;
	}

	public static boolean bind(MinecraftServer server, Station station, UUID worker) {
		if (station == null || worker == null || station.worker() != null) {
			return false;
		}

		StationRegistry registry = load(server);
		if (registry.stations.stream().anyMatch(other -> worker.equals(other.worker()))) {
			return false;
		}

		boolean replaced = registry.replace(station.withWorker(worker));
		if (replaced) {
			registry.save();
		}

		return replaced;
	}

	public static boolean unbind(MinecraftServer server, Station station) {
		if (station == null || station.worker() == null) {
			return false;
		}

		StationRegistry registry = load(server);
		boolean replaced = registry.replace(station.withWorker(null));
		if (replaced) {
			registry.save();
		}

		return replaced;
	}

	/** Assume um posto sem dono (bloco colocado por /setblock ou anterior a este recurso). */
	public static Station claim(MinecraftServer server, Station station, UUID owner) {
		if (station == null || station.owner() != null) {
			return station;
		}

		StationRegistry registry = load(server);
		Station claimed = new Station(
				station.id(),
				station.dimension(),
				station.pos(),
				owner,
				station.worker(),
				station.mode(),
				station.zones(),
				station.outputChest(),
				station.createdAt()
		);
		registry.replace(claimed);
		registry.save();
		return claimed;
	}

	public static void setMode(MinecraftServer server, Station station, StationMode mode) {		StationRegistry registry = load(server);
		if (registry.replace(station.withMode(mode))) {
			registry.save();
		}
	}

	public static void setZone(MinecraftServer server, Station station, BlockPos from, BlockPos to) {
		BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
		StationRegistry registry = load(server);
		boolean replaced = registry.replace(station
				.withZones(List.of(new Zone(min, max)))
				.withMode(StationMode.ZONES));
		if (replaced) {
			registry.save();
		}
	}

	public static void clearZone(MinecraftServer server, Station station) {
		StationRegistry registry = load(server);
		if (registry.replace(station.withZones(List.of()).withMode(StationMode.FREE))) {
			registry.save();
		}
	}

	public static void clear() {
		instance = null;
	}

	private Station find(String dimension, BlockPos pos) {
		for (Station station : stations) {
			if (station.dimension().equals(dimension) && station.pos().equals(pos)) {
				return station;
			}
		}

		return null;
	}

	private boolean replace(Station updated) {
		for (int index = 0; index < stations.size(); index++) {
			Station current = stations.get(index);
			if (current.dimension().equals(updated.dimension()) && current.pos().equals(updated.pos())) {
				stations.set(index, updated);
				return true;
			}
		}

		return false;
	}

	private static StationRegistry load(MinecraftServer server) {
		Path path = file(server);
		if (instance != null && path.equals(instance.file)) {
			return instance;
		}

		StationRegistry registry = new StationRegistry();
		registry.file = path;
		registry.read();
		instance = registry;
		return registry;
	}

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("laborcraft").resolve("stations.json");
	}

	private void read() {
		if (file == null || !Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			StationFile data = GSON.fromJson(reader, StationFile.class);
			if (data == null || data.stations == null) {
				return;
			}

			stations.clear();
			for (StationData entry : data.stations) {
				if (entry == null || entry.dimension == null) {
					continue;
				}

				stations.add(new Station(
						entry.id == null ? "laborcraft:station/?" : entry.id,
						entry.dimension,
						new BlockPos(entry.x, entry.y, entry.z),
						parseUuid(entry.owner),
						parseUuid(entry.worker),
						StationMode.of(entry.mode),
						readZones(entry.zones),
						entry.chest == null ? null : new BlockPos(entry.chest[0], entry.chest[1], entry.chest[2]),
						entry.createdAt
				));
			}
		} catch (IOException | RuntimeException exception) {
			LaborCraft.LOGGER.error("Falha ao ler os postos {}", file, exception);
		}
	}

	private void save() {
		if (file == null) {
			return;
		}

		StationFile data = new StationFile();
		data.stations = new ArrayList<>(stations.size());
		for (Station station : stations) {
			StationData entry = new StationData();
			entry.id = station.id();
			entry.dimension = station.dimension();
			entry.x = station.pos().getX();
			entry.y = station.pos().getY();
			entry.z = station.pos().getZ();
			entry.owner = station.owner() == null ? null : station.owner().toString();
			entry.worker = station.worker() == null ? null : station.worker().toString();
			entry.mode = station.mode().name();
			entry.zones = new ArrayList<>(station.zones().size());
			for (Zone zone : station.zones()) {
				entry.zones.add(new int[]{
						zone.from().getX(), zone.from().getY(), zone.from().getZ(),
						zone.to().getX(), zone.to().getY(), zone.to().getZ()
				});
			}
			entry.chest = station.outputChest() == null
					? null
					: new int[]{station.outputChest().getX(), station.outputChest().getY(), station.outputChest().getZ()};
			entry.createdAt = station.createdAt();
			data.stations.add(entry);
		}

		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException exception) {
			LaborCraft.LOGGER.error("Falha ao salvar os postos {}", file, exception);
		}
	}

	private static List<Zone> readZones(List<int[]> raw) {
		if (raw == null) {
			return List.of();
		}

		List<Zone> zones = new ArrayList<>(raw.size());
		for (int[] value : raw) {
			if (value != null && value.length == 6) {
				zones.add(new Zone(
						new BlockPos(value[0], value[1], value[2]),
						new BlockPos(value[3], value[4], value[5])
				));
			}
		}

		return List.copyOf(zones);
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public enum StationMode {
		FREE,
		ZONES;

		public static StationMode of(String value) {
			if (value == null) {
				return FREE;
			}

			try {
				return valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				return FREE;
			}
		}
	}

	public record Zone(BlockPos from, BlockPos to) {
		public int width() {
			return Math.abs(to.getX() - from.getX()) + 1;
		}

		public int height() {
			return Math.abs(to.getY() - from.getY()) + 1;
		}

		public int depth() {
			return Math.abs(to.getZ() - from.getZ()) + 1;
		}

		public int volume() {
			return width() * height() * depth();
		}
	}

	public record Station(
			String id,
			String dimension,
			BlockPos pos,
			UUID owner,
			UUID worker,
			StationMode mode,
			List<Zone> zones,
			BlockPos outputChest,
			long createdAt
	) {
		public Station {
			zones = List.copyOf(zones);
		}

		public Station withWorker(UUID newWorker) {
			return new Station(id, dimension, pos, owner, newWorker, mode, zones, outputChest, createdAt);
		}

		public boolean isOwner(UUID candidate) {
			return owner != null && owner.equals(candidate);
		}

		public Station withMode(StationMode newMode) {
			return new Station(id, dimension, pos, owner, worker, newMode, zones, outputChest, createdAt);
		}

		public Station withZones(List<Zone> newZones) {
			return new Station(id, dimension, pos, owner, worker, mode, newZones, outputChest, createdAt);
		}

		public boolean hasZone() {
			return !zones.isEmpty();
		}
	}

	private static final class StationFile {
		private List<StationData> stations = new ArrayList<>();
	}

	private static final class StationData {
		private String id;
		private String dimension;
		private int x;
		private int y;
		private int z;
		private String owner;
		private String worker;
		private String mode;
		private List<int[]> zones;
		private int[] chest;
		private long createdAt;
	}
}

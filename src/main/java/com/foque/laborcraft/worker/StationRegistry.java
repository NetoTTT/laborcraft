package com.foque.laborcraft.worker;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureSpan;
import com.foque.laborcraft.block.LaborCraftBlocks;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
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
import net.minecraft.server.level.ServerLevel;
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
	private static final int MAX_ZONE_RECTANGLES = 32;
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
				List.of(),
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

	/**
	 * A station record can survive its block (so replacing the block reattaches). Area conflicts
	 * and map overlays must ignore those orphan records until the block is placed again.
	 */
	public static boolean isActive(MinecraftServer server, Station station) {
		if (server == null || station == null) {
			return false;
		}

		ServerLevel level = level(server, station.dimension());
		return level != null
				&& level.isLoaded(station.pos())
				&& level.getBlockState(station.pos()).is(LaborCraftBlocks.WORKSTATION);
	}

	private static ServerLevel level(MinecraftServer server, String dimension) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.dimension().identifier().toString().equals(dimension)) {
				return level;
			}
		}
		return null;
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
		if (station == null || worker == null) {
			return false;
		}

		// A dismissed worker leaves a stale UUID behind; allow the owner to replace that ghost.
		if (station.worker() != null && LaborCraftWorker.find(server, station.worker()) != null) {
			return false;
		}

		StationRegistry registry = load(server);
		if (registry.stations.stream().anyMatch(other -> worker.equals(other.worker()))) {
			return false;
		}

		LaborCraftWorker entity = LaborCraftWorker.find(server, worker);
		String workerJob = entity == null ? "" : entity.jobId();
		if (!station.jobId().isBlank()
				&& !workerJob.isBlank()
				&& !station.jobId().equals(workerJob)) {
			return false;
		}

		Station updated = station.withWorker(worker);
		if (station.jobId().isBlank() && !workerJob.isBlank()) {
			updated = updated.withJob(workerJob);
		}
		boolean replaced = registry.replace(updated);
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

	/** Clears any station pointing at this worker (dismiss/remove/auto-dismiss). */
	public static void unbindWorker(MinecraftServer server, UUID worker) {
		Station station = byWorker(server, worker);
		if (station != null) {
			unbind(server, station);
		}
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
				station.storage(),
				station.createdAt(),
				station.jobId(),
				station.enclosure()
		);
		registry.replace(claimed);
		registry.save();
		return claimed;
	}

	/** Valida uma zona e devolve o motivo da recusa (chave curta) ou {@code null} se estiver ok. */
	public static String validateZone(MinecraftServer server, Station station, BlockPos from, BlockPos to) {
		int width = Math.abs(to.getX() - from.getX()) + 1;
		int depth = Math.abs(to.getZ() - from.getZ()) + 1;
		int maxSize = 32;
		if (width > maxSize || depth > maxSize) {
			return "too_large";
		}

		int maxRadius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		if (Math.abs(from.getX() - station.pos().getX()) > maxRadius
				|| Math.abs(from.getZ() - station.pos().getZ()) > maxRadius
				|| Math.abs(to.getX() - station.pos().getX()) > maxRadius
				|| Math.abs(to.getZ() - station.pos().getZ()) > maxRadius) {
			return "too_far";
		}

		int minX = Math.min(from.getX(), to.getX());
		int maxX = Math.max(from.getX(), to.getX());
		int minZ = Math.min(from.getZ(), to.getZ());
		int maxZ = Math.max(from.getZ(), to.getZ());
		List<Station> all = load(server).stations;
		for (Station other : all) {
			if (other.pos().equals(station.pos()) || !other.dimension().equals(station.dimension())
					|| !isActive(server, other)) {
				continue;
			}
			// Only drawn zones are exclusive; maximum areas may overlap.
			if (StationAreas.zoneOverlaps(other, minX, maxX, minZ, maxZ)) {
				return "overlap";
			}
			if (insideHorizontal(from, to, other.pos())) {
				return "overlap";
			}
		}

		return null;
	}

	public static String validateNewPlacement(MinecraftServer server, String dimension, BlockPos pos) {
		List<Station> all = load(server).stations;
		for (Station other : all) {
			if (!other.dimension().equals(dimension) || other.pos().equals(pos)
					|| !isActive(server, other)) {
				continue;
			}
			// A new post may sit inside another post's maximum area, never inside its drawn zone.
			if (StationAreas.zoneContains(other, pos.getX(), pos.getZ())) {
				return "occupied";
			}
		}
		return null;
	}

	public static String validateClaim(MinecraftServer server, Station station) {
		List<Station> all = load(server).stations;
		for (Station other : all) {
			if (!other.dimension().equals(station.dimension()) || other.pos().equals(station.pos())
					|| !isActive(server, other)) {
				continue;
			}
			if (StationAreas.zoneContains(other, station.pos().getX(), station.pos().getZ())) {
				return "occupied";
			}
		}
		return null;
	}

	private static boolean insideHorizontal(BlockPos from, BlockPos to, BlockPos pos) {
		return pos.getX() >= Math.min(from.getX(), to.getX())
				&& pos.getX() <= Math.max(from.getX(), to.getX())
				&& pos.getZ() >= Math.min(from.getZ(), to.getZ())
				&& pos.getZ() <= Math.max(from.getZ(), to.getZ());
	}

	public static void setMode(MinecraftServer server, Station station, StationMode mode) {		StationRegistry registry = load(server);
		if (registry.replace(station.withMode(mode))) {
			registry.save();
		}
	}

	public static void setZone(MinecraftServer server, Station station, BlockPos from, BlockPos to) {
		BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
		setZones(server, station, List.of(new Zone(min, max)));
	}

	public static void setZones(MinecraftServer server, Station station, List<Zone> zones) {
		StationRegistry registry = load(server);
		boolean hasArea = !zones.isEmpty() || station.enclosure() != null;
		Station updated = station.withZones(List.copyOf(zones))
				.withMode(hasArea ? StationMode.ZONES : StationMode.FREE);
		if (registry.replace(updated)) {
			registry.save();
		}
	}

	public static void setJob(MinecraftServer server, Station station, String jobId) {
		StationRegistry registry = load(server);
		if (registry.replace(station.withJob(jobId == null ? "" : jobId))) {
			registry.save();
		}
	}

	public static void setEnclosure(MinecraftServer server, Station station, EnclosureDescriptor enclosure) {
		StationRegistry registry = load(server);
		if (registry.replace(station.withEnclosure(enclosure))) {
			registry.save();
		}
	}

	public static void clearEnclosure(MinecraftServer server, Station station) {
		setEnclosure(server, station, null);
	}

	/**
	 * Clips the station's maximum square against nearby posts/zones and stores the remaining free
	 * area as non-overlapping rectangles. Returns the rectangle count, -1 when nothing is free, or
	 * -2 when the decomposition exceeds {@link #MAX_ZONE_RECTANGLES}.
	 */
	public static int applyMaxZone(MinecraftServer server, Station station) {
		int maxRadius = Math.min(15, Math.max(4, LaborCraftConfig.get().area.maxRadius));
		BlockPos center = station.pos();
		return applyClippedArea(
				server,
				station,
				center.getX() - maxRadius,
				center.getX() + maxRadius,
				center.getZ() - maxRadius,
				center.getZ() + maxRadius
		);
	}

	/**
	 * Clips an arbitrary drawn rectangle against nearby posts/zones and stores the free area.
	 * Returns the rectangle count, -1 (no free area), -2 (too many rectangles), -3 (too large) or
	 * -4 (outside the maximum radius).
	 */
	public static int applyZone(MinecraftServer server, Station station, BlockPos from, BlockPos to) {
		int minX = Math.min(from.getX(), to.getX());
		int maxX = Math.max(from.getX(), to.getX());
		int minZ = Math.min(from.getZ(), to.getZ());
		int maxZ = Math.max(from.getZ(), to.getZ());
		if (maxX - minX + 1 > 32 || maxZ - minZ + 1 > 32) {
			return -3;
		}

		int maxRadius = Math.max(4, LaborCraftConfig.get().area.maxRadius);
		if (Math.abs(from.getX() - station.pos().getX()) > maxRadius
				|| Math.abs(to.getX() - station.pos().getX()) > maxRadius
				|| Math.abs(from.getZ() - station.pos().getZ()) > maxRadius
				|| Math.abs(to.getZ() - station.pos().getZ()) > maxRadius) {
			return -4;
		}

		return applyClippedArea(server, station, minX, maxX, minZ, maxZ);
	}

	private static int applyClippedArea(
			MinecraftServer server,
			Station station,
			int minX,
			int maxX,
			int minZ,
			int maxZ
	) {
		int vertical = Math.max(1, LaborCraftConfig.get().area.verticalRadius);
		int width = maxX - minX + 1;
		int depth = maxZ - minZ + 1;
		int y0 = station.pos().getY() - vertical;
		int y1 = station.pos().getY() + vertical;
		boolean[][] blocked = new boolean[depth][width];

		List<Station> all = load(server).stations;
		for (Station other : all) {
			if (other.pos().equals(station.pos()) || !other.dimension().equals(station.dimension())
					|| !isActive(server, other)) {
				continue;
			}

			for (Zone zone : other.zones()) {
				int zoneMinX = Math.min(zone.from().getX(), zone.to().getX());
				int zoneMaxX = Math.max(zone.from().getX(), zone.to().getX());
				int zoneMinZ = Math.min(zone.from().getZ(), zone.to().getZ());
				int zoneMaxZ = Math.max(zone.from().getZ(), zone.to().getZ());
				int x0 = Math.max(minX, zoneMinX);
				int x1 = Math.min(maxX, zoneMaxX);
				int z0 = Math.max(minZ, zoneMinZ);
				int z1 = Math.min(maxZ, zoneMaxZ);
				for (int z = z0; z <= z1; z++) {
					for (int x = x0; x <= x1; x++) {
						blocked[z - minZ][x - minX] = true;
					}
				}
			}

			BlockPos otherPos = other.pos();
			if (otherPos.getX() >= minX && otherPos.getX() <= maxX
					&& otherPos.getZ() >= minZ && otherPos.getZ() <= maxZ) {
				blocked[otherPos.getZ() - minZ][otherPos.getX() - minX] = true;
			}
		}

		boolean[][] used = new boolean[depth][width];
		List<Zone> zones = new ArrayList<>();
		for (int z = 0; z < depth; z++) {
			for (int x = 0; x < width; x++) {
				if (blocked[z][x] || used[z][x]) {
					continue;
				}

				int x1 = x;
				while (x1 + 1 < width && !blocked[z][x1 + 1] && !used[z][x1 + 1]) {
					x1++;
				}

				int z1 = z;
				boolean canGrow = true;
				while (z1 + 1 < depth && canGrow) {
					for (int xx = x; xx <= x1; xx++) {
						if (blocked[z1 + 1][xx] || used[z1 + 1][xx]) {
							canGrow = false;
							break;
						}
					}
					if (canGrow) {
						z1++;
					}
				}

				for (int zz = z; zz <= z1; zz++) {
					for (int xx = x; xx <= x1; xx++) {
						used[zz][xx] = true;
					}
				}
				zones.add(new Zone(
						new BlockPos(minX + x, y0, minZ + z),
						new BlockPos(minX + x1, y1, minZ + z1)
				));
				if (zones.size() > MAX_ZONE_RECTANGLES) {
					return -2;
				}
			}
		}

		if (zones.isEmpty()) {
			return -1;
		}

		setZones(server, station, zones);
		return zones.size();
	}

	public static void clearZone(MinecraftServer server, Station station) {
		StationRegistry registry = load(server);
		if (registry.replace(station.withZones(List.of()).withMode(StationMode.FREE))) {
			registry.save();
		}
	}

	/**
	 * Replaces the storage links of a station. Container validation belongs to the command layer,
	 * which knows the level the player is in.
	 */
	public static Station setStorage(MinecraftServer server, Station station, List<StorageLink> storage) {
		Station updated = station.withStorage(storage);
		StationRegistry registry = load(server);
		if (registry.replace(updated)) {
			registry.save();
			return updated;
		}
		return station;
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
						readStorage(entry),
						entry.createdAt,
						entry.jobId == null ? "" : entry.jobId,
						readEnclosure(entry.dimension, entry.enclosure)
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
			entry.chest = null;
			entry.storage = new ArrayList<>(station.storage().size());
			for (StorageLink link : station.storage()) {
				StorageData linkData = new StorageData();
				linkData.x = link.pos().getX();
				linkData.y = link.pos().getY();
				linkData.z = link.pos().getZ();
				linkData.role = link.role().id();
				entry.storage.add(linkData);
			}
			entry.createdAt = station.createdAt();
			entry.jobId = station.jobId();
			entry.enclosure = writeEnclosure(station.enclosure());
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

	private static EnclosureDescriptor readEnclosure(String dimension, EnclosureData data) {
		if (data == null) {
			return null;
		}

		List<EnclosureSpan> spans = new ArrayList<>();
		if (data.spans != null) {
			for (int[] span : data.spans) {
				if (span != null && span.length == 3) {
					spans.add(new EnclosureSpan(span[0], span[1], span[2]));
				}
			}
		}

		List<BlockPos> gates = new ArrayList<>();
		if (data.gates != null) {
			for (int[] gate : data.gates) {
				if (gate != null && gate.length == 3) {
					gates.add(new BlockPos(gate[0], gate[1], gate[2]));
				}
			}
		}

		return new EnclosureDescriptor(
				dimension,
				new BlockPos(data.seedX, data.seedY, data.seedZ),
				new BlockPos(data.minX, data.minY, data.minZ),
				new BlockPos(data.maxX, data.maxY, data.maxZ),
				data.area,
				spans,
				gates
		);
	}

	private static EnclosureData writeEnclosure(EnclosureDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}

		EnclosureData data = new EnclosureData();
		data.seedX = descriptor.seed().getX();
		data.seedY = descriptor.seed().getY();
		data.seedZ = descriptor.seed().getZ();
		data.minX = descriptor.min().getX();
		data.minY = descriptor.min().getY();
		data.minZ = descriptor.min().getZ();
		data.maxX = descriptor.max().getX();
		data.maxY = descriptor.max().getY();
		data.maxZ = descriptor.max().getZ();
		data.area = descriptor.area();
		data.spans = new ArrayList<>(descriptor.spans().size());
		for (EnclosureSpan span : descriptor.spans()) {
			data.spans.add(new int[]{span.z(), span.minX(), span.maxX()});
		}
		data.gates = new ArrayList<>(descriptor.gates().size());
		for (BlockPos gate : descriptor.gates()) {
			data.gates.add(new int[]{gate.getX(), gate.getY(), gate.getZ()});
		}
		return data;
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

	private static List<StorageLink> readStorage(StationData entry) {
		List<StorageLink> links = new ArrayList<>();
		if (entry.storage != null) {
			for (StorageData value : entry.storage) {
				if (value != null) {
					links.add(new StorageLink(
							new BlockPos(value.x, value.y, value.z),
							StorageRole.of(value.role)
					));
				}
			}
		}

		// Migration: the old single output chest becomes a MIXED link.
		if (links.isEmpty() && entry.chest != null && entry.chest.length == 3) {
			links.add(new StorageLink(
					new BlockPos(entry.chest[0], entry.chest[1], entry.chest[2]),
					StorageRole.MIXED
			));
		}

		return List.copyOf(links);
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

	public enum StorageRole {
		MIXED,
		OUTPUT,
		INPUT,
		TOOLS;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public StorageRole next() {
			StorageRole[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static StorageRole of(String value) {
			if (value == null) {
				return MIXED;
			}
			try {
				return valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				return MIXED;
			}
		}

		/** Returns null when the value is not a valid role (used by commands to reject typos). */
		public static StorageRole parse(String value) {
			if (value == null) {
				return null;
			}
			for (StorageRole role : values()) {
				if (role.id().equalsIgnoreCase(value)) {
					return role;
				}
			}
			return null;
		}
	}

	public record StorageLink(BlockPos pos, StorageRole role) {
		public StorageLink {
			pos = pos.immutable();
			role = role == null ? StorageRole.MIXED : role;
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
			List<StorageLink> storage,
			long createdAt,
			String jobId,
			EnclosureDescriptor enclosure
	) {
		public Station {
			zones = List.copyOf(zones);
			storage = List.copyOf(storage);
			jobId = jobId == null ? "" : jobId;
		}

		/** Compatibility constructor for stations created before jobs/enclosures existed. */
		public Station(
				String id,
				String dimension,
				BlockPos pos,
				UUID owner,
				UUID worker,
				StationMode mode,
				List<Zone> zones,
				List<StorageLink> storage,
				long createdAt
		) {
			this(id, dimension, pos, owner, worker, mode, zones, storage, createdAt, "", null);
		}

		public Station withWorker(UUID newWorker) {
			return new Station(
					id, dimension, pos, owner, newWorker, mode, zones, storage, createdAt, jobId, enclosure
			);
		}

		public boolean isOwner(UUID candidate) {
			return owner != null && owner.equals(candidate);
		}

		public Station withMode(StationMode newMode) {
			return new Station(
					id, dimension, pos, owner, worker, newMode, zones, storage, createdAt, jobId, enclosure
			);
		}

		public Station withZones(List<Zone> newZones) {
			return new Station(
					id, dimension, pos, owner, worker, mode, newZones, storage, createdAt, jobId, enclosure
			);
		}

		public Station withStorage(List<StorageLink> newStorage) {
			return new Station(
					id, dimension, pos, owner, worker, mode, zones, newStorage, createdAt, jobId, enclosure
			);
		}

		public Station withJob(String newJobId) {
			return new Station(
					id, dimension, pos, owner, worker, mode, zones, storage, createdAt, newJobId, enclosure
			);
		}

		public Station withEnclosure(EnclosureDescriptor newEnclosure) {
			return new Station(
					id, dimension, pos, owner, worker, mode, zones, storage, createdAt, jobId, newEnclosure
			);
		}

		public boolean hasZone() {
			return !zones.isEmpty();
		}

		public boolean hasEnclosure() {
			return enclosure != null;
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
		private List<StorageData> storage;
		private int[] chest;
		private long createdAt;
		private String jobId;
		private EnclosureData enclosure;
	}

	private static final class EnclosureData {
		private int seedX;
		private int seedY;
		private int seedZ;
		private int minX;
		private int minY;
		private int minZ;
		private int maxX;
		private int maxY;
		private int maxZ;
		private int area;
		private List<int[]> spans;
		private List<int[]> gates;
	}

	private static final class StorageData {
		private int x;
		private int y;
		private int z;
		private String role;
	}
}

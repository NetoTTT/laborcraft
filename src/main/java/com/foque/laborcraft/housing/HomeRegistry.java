package com.foque.laborcraft.housing;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
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
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Persistent registry of worker houses.
 *
 * <p>A house is a valid room anchored to a bed. The first player to register the bed owns the
 * room; the house stores the beds found (slots) and which worker occupies each bed. The worker
 * limit is {@code worker.maxWorkersPerPlayer} plus the slots of valid houses.</p>
 *
 * <p>Stored at {@code <world>/data/laborcraft/homes.json}. A broken house is not deleted: it
 * becomes invalid during revalidation and recovers when the room is repaired.</p>
 */
public final class HomeRegistry {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static HomeRegistry instance;

	private final List<Home> homes = new ArrayList<>();
	private Path file;

	private HomeRegistry() {
	}

	public static ClaimResult claim(MinecraftServer server, ServerLevel level, BlockPos bedPos, UUID owner, String name) {
		if (server == null || level == null || bedPos == null || owner == null) {
			return new ClaimResult(null, "no_space");
		}

		BlockPos anchor = HousingValidator.canonicalBed(level, bedPos);
		HousingReport report = HousingValidator.validate(level, anchor, LaborCraftConfig.get());
		if (!report.valid()) {
			return new ClaimResult(null, report.failureReason());
		}

		String cleanName = cleanName(name, server, owner);
		String dimension = level.dimension().identifier().toString();
		HomeRegistry registry = load(server);
		Home existing = registry.findOverlapping(dimension, report.min(), report.max());
		if (existing != null) {
			if (!existing.isOwner(owner)) {
				return new ClaimResult(existing, "claimed");
			}
			if (existing.valid()) {
				return new ClaimResult(existing, "already_owner");
			}

			// The owner repaired/re-anchored a broken house: refresh it instead of blocking.
			List<BlockPos> slots = slotBeds(report.bedList(), report.capacity());
			Home repaired = new Home(
					homeId(anchor),
					cleanName,
					dimension,
					owner,
					anchor.immutable(),
					report.min(),
					report.max(),
					report.capacity(),
					report.bedList(),
					existing.residents().stream()
							.filter(resident -> slots.contains(resident.bed()))
							.toList(),
					existing.createdAt(),
					report.checkedAtTick(),
					true,
					""
			);
			registry.homes.removeIf(candidate -> candidate.id().equals(existing.id()));
			registry.homes.add(repaired);
			registry.save();
			return new ClaimResult(repaired, null);
		}

		Home home = new Home(
				homeId(anchor),
				cleanName,
				dimension,
				owner,
				anchor.immutable(),
				report.min(),
				report.max(),
				report.capacity(),
				report.bedList(),
				List.of(),
				System.currentTimeMillis(),
				report.checkedAtTick(),
				true,
				""
		);
		registry.homes.add(home);
		registry.save();
		return new ClaimResult(home, null);
	}

	public static Home get(MinecraftServer server, String dimension, BlockPos bed) {
		if (server == null || dimension == null || bed == null) {
			return null;
		}

		ServerLevel level = level(server, dimension);
		if (level == null || !level.getBlockState(bed).is(BlockTags.BEDS)) {
			return null;
		}

		return findAt(server, dimension, bed);
	}

	/**
	 * Finds a house by anchor/bed position even when the bed is gone (used to remove an invalid
	 * house after its anchor was broken).
	 */
	public static Home findAt(MinecraftServer server, String dimension, BlockPos pos) {
		if (server == null || dimension == null || pos == null) {
			return null;
		}

		for (Home home : load(server).homes) {
			if (home.dimension().equals(dimension)
					&& (home.bed().equals(pos) || home.containsBed(pos))) {
				return home;
			}
		}

		return null;
	}

	public static Home byWorker(MinecraftServer server, UUID worker) {
		if (server == null || worker == null) {
			return null;
		}

		for (Home home : load(server).homes) {
			if (home.resident(worker) != null) {
				return home;
			}
		}

		return null;
	}

	public static List<Home> byOwner(MinecraftServer server, UUID owner) {
		if (server == null || owner == null) {
			return List.of();
		}

		return load(server).homes.stream()
				.filter(home -> home.isOwner(owner))
				.toList();
	}

	public static int slots(MinecraftServer server, UUID owner) {
		if (server == null || owner == null) {
			return 0;
		}

		int total = 0;
		for (Home home : byOwner(server, owner)) {
			total += home.slots();
		}
		return total;
	}

	/**
	 * Moves the worker to a house/bed. {@code bed == null} picks the first free bed. If the worker
	 * already lived in another house, they are moved.
	 */
	public static AssignResult assign(MinecraftServer server, Home home, UUID worker, BlockPos bed) {
		if (server == null || home == null || worker == null) {
			return new AssignResult(null, null, "no_home", false);
		}

		HomeRegistry registry = load(server);
		Home actual = registry.find(home);
		if (actual == null) {
			return new AssignResult(null, null, "no_home", false);
		}

		if (!actual.valid()) {
			return new AssignResult(actual, null, "invalid", false);
		}

		Home.Resident existing = actual.resident(worker);
		if (bed == null && existing != null) {
			return new AssignResult(actual, existing.bed(), "already", false);
		}

		BlockPos target = bed == null ? actual.firstFreeBed() : actual.matchSlotBed(bed);
		if (target == null) {
			return new AssignResult(actual, null, bed == null ? "no_free_bed" : "bed_not_free", false);
		}

		Home.Resident occupant = actual.residentAt(target);
		if (occupant != null && !occupant.worker().equals(worker)) {
			return new AssignResult(actual, target, "bed_occupied", false);
		}

		if (existing != null && existing.bed().equals(target)) {
			return new AssignResult(actual, target, "already", false);
		}

		boolean moved = false;
		// Remove the worker from any other house (one house per worker).
		for (int index = 0; index < registry.homes.size(); index++) {
			Home candidate = registry.homes.get(index);
			if (candidate.resident(worker) == null) {
				continue;
			}
			registry.homes.set(index, candidate.withResidents(candidate.residents().stream()
					.filter(resident -> !resident.worker().equals(worker))
					.toList()));
			moved = true;
		}

		Home targetHome = registry.find(home);
		if (targetHome == null) {
			return new AssignResult(null, null, "no_home", false);
		}

		List<Home.Resident> residents = new ArrayList<>(targetHome.residents());
		residents.add(new Home.Resident(worker, target));
		registry.replace(targetHome.withResidents(residents));
		registry.save();
		return new AssignResult(registry.find(targetHome), target, null, moved);
	}

	/** Removes the worker from any house and returns the previous house, if any. */
	public static Home unassign(MinecraftServer server, UUID worker) {
		if (server == null || worker == null) {
			return null;
		}

		HomeRegistry registry = load(server);
		Home previous = null;
		boolean changed = false;
		for (int index = 0; index < registry.homes.size(); index++) {
			Home home = registry.homes.get(index);
			if (home.resident(worker) == null) {
				continue;
			}

			previous = home;
			registry.homes.set(index, home.withResidents(home.residents().stream()
					.filter(resident -> !resident.worker().equals(worker))
					.toList()));
			changed = true;
		}

		if (changed) {
			registry.save();
		}
		return previous;
	}

	public static Home rename(MinecraftServer server, Home home, String name) {
		if (server == null || home == null) {
			return null;
		}

		HomeRegistry registry = load(server);
		Home actual = registry.find(home);
		if (actual == null) {
			return null;
		}

		String value = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").strip();
		if (value.isBlank()) {
			value = actual.name();
		}
		if (value.length() > 32) {
			value = value.substring(0, 32);
		}

		Home renamed = actual.withName(value);
		registry.replace(renamed);
		registry.save();
		return renamed;
	}

	public static boolean remove(MinecraftServer server, Home home) {
		if (server == null || home == null) {
			return false;
		}

		HomeRegistry registry = load(server);
		boolean removed = registry.homes.removeIf(candidate -> candidate.id().equals(home.id()));
		if (removed) {
			registry.save();
		}
		return removed;
	}

	/**
	 * Revalidates the houses and returns how many changed state. The owner is notified only when
	 * the state changes, to avoid chat spam every minute.
	 */
	public static int revalidateAll(MinecraftServer server) {
		if (server == null) {
			return 0;
		}

		HomeRegistry registry = load(server);
		int changed = 0;
		boolean dirty = false;
		for (int index = 0; index < registry.homes.size(); index++) {
			Home home = registry.homes.get(index);
			ServerLevel level = level(server, home.dimension());
			if (level == null || !level.isLoaded(home.bed())) {
				continue;
			}

			HousingReport report = HousingValidator.validate(level, home.bed(), LaborCraftConfig.get());
			boolean wasValid = home.valid();
			Home updated;
			if (report.valid()) {
				List<BlockPos> beds = report.bedList();
				List<BlockPos> slots = slotBeds(beds, report.capacity());
				List<Home.Resident> residents = home.residents().stream()
						.filter(resident -> slots.contains(resident.bed()))
						.toList();
				updated = home.withRevalidation(
						true,
						"",
						report.min(),
						report.max(),
						report.capacity(),
						beds,
						report.checkedAtTick()
				).withResidents(residents);
			} else {
				updated = home.withRevalidation(
						false,
						report.failureReason(),
						home.min(),
						home.max(),
						home.capacity(),
						home.beds(),
						report.checkedAtTick()
				);
			}

			boolean stateChanged = wasValid != updated.valid()
					|| !updated.failure().equals(home.failure());
			boolean residentsChanged = !updated.residents().equals(home.residents());
			if (!updated.equals(home)) {
				registry.homes.set(index, updated);
				dirty = true;
			}

			if (stateChanged) {
				changed++;
				notifyOwner(server, updated);
			} else if (residentsChanged && updated.valid()) {
				notifyResidentsChanged(server, updated);
			}
		}

		if (dirty) {
			registry.save();
		}
		return changed;
	}

	public static void clear() {
		instance = null;
	}

	private static void notifyOwner(MinecraftServer server, Home home) {
		if (home.owner() == null) {
			return;
		}

		ServerPlayer owner = server.getPlayerList().getPlayer(home.owner());
		if (owner == null) {
			return;
		}

		if (home.valid()) {
			owner.sendSystemMessage(Component.translatable(
					"laborcraft.command.home.revalidated",
					home.name(),
					home.bed().getX(),
					home.bed().getY(),
					home.bed().getZ()
			));
			return;
		}

		Component reason = Component.translatable(
				"laborcraft.housing.reason." + (home.failure().isBlank() ? "invalid" : home.failure())
		);
		owner.sendSystemMessage(Component.translatable(
				"laborcraft.command.home.invalidated",
				home.name(),
				home.bed().getX(),
				home.bed().getY(),
				home.bed().getZ(),
				reason
		));
	}

	private static void notifyResidentsChanged(MinecraftServer server, Home home) {
		if (home.owner() == null) {
			return;
		}

		ServerPlayer owner = server.getPlayerList().getPlayer(home.owner());
		if (owner == null) {
			return;
		}

		owner.sendSystemMessage(Component.translatable(
				"laborcraft.command.home.residents_changed",
				home.name(),
				home.bed().getX(),
				home.bed().getY(),
				home.bed().getZ()
		));
	}

	private static ServerLevel level(MinecraftServer server, String dimension) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.dimension().identifier().toString().equals(dimension)) {
				return level;
			}
		}
		return null;
	}

	private Home find(Home target) {
		for (Home home : homes) {
			if (home.id().equals(target.id())) {
				return home;
			}
		}
		return null;
	}

	private Home findOverlapping(String dimension, BlockPos min, BlockPos max) {
		for (Home home : homes) {
			if (home.dimension().equals(dimension) && home.overlaps(min, max)) {
				return home;
			}
		}
		return null;
	}

	private boolean replace(Home updated) {
		for (int index = 0; index < homes.size(); index++) {
			if (homes.get(index).id().equals(updated.id())) {
				homes.set(index, updated);
				return true;
			}
		}
		return false;
	}

	private static HomeRegistry load(MinecraftServer server) {
		Path path = file(server);
		if (instance != null && path.equals(instance.file)) {
			return instance;
		}

		HomeRegistry registry = new HomeRegistry();
		registry.file = path;
		registry.read();
		instance = registry;
		return registry;
	}

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("laborcraft").resolve("homes.json");
	}

	private static String homeId(BlockPos bed) {
		return "laborcraft:home/%d_%d_%d".formatted(bed.getX(), bed.getY(), bed.getZ());
	}

	public static String suggestedName(MinecraftServer server, UUID owner) {
		return "Casa " + (byOwner(server, owner).size() + 1);
	}

	private static String cleanName(String raw, MinecraftServer server, UUID owner) {
		String value = raw == null ? "" : raw.replaceAll("\\p{Cntrl}", "").strip();
		if (value.isBlank()) {
			value = suggestedName(server, owner);
		}
		return value.length() > 32 ? value.substring(0, 32) : value;
	}

	private static List<BlockPos> slotBeds(List<BlockPos> beds, int capacity) {
		int limit = Math.min(Math.max(0, capacity), beds.size());
		return limit <= 0 ? List.of() : beds.subList(0, limit);
	}

	private void read() {
		if (file == null || !Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			HomeFile data = GSON.fromJson(reader, HomeFile.class);
			if (data == null || data.homes == null) {
				return;
			}

			homes.clear();
			for (HomeData entry : data.homes) {
				if (entry == null || entry.dimension == null) {
					continue;
				}

				homes.add(new Home(
						entry.id == null ? "laborcraft:home/?" : entry.id,
						entry.name == null || entry.name.isBlank() ? "Casa" : entry.name,
						entry.dimension,
						parseUuid(entry.owner),
						new BlockPos(entry.x, entry.y, entry.z),
						new BlockPos(entry.minX, entry.minY, entry.minZ),
						new BlockPos(entry.maxX, entry.maxY, entry.maxZ),
						entry.capacity,
						readBeds(entry.beds),
						readResidents(entry.residents),
						entry.createdAt,
						entry.checkedAt,
						entry.valid,
						entry.failure
				));
			}
		} catch (IOException | RuntimeException exception) {
			LaborCraft.LOGGER.error("Falha ao ler as casas {}", file, exception);
		}
	}

	private void save() {
		if (file == null) {
			return;
		}

		HomeFile data = new HomeFile();
		data.homes = new ArrayList<>(homes.size());
		for (Home home : homes) {
			HomeData entry = new HomeData();
			entry.id = home.id();
			entry.name = home.name();
			entry.dimension = home.dimension();
			entry.owner = home.owner() == null ? null : home.owner().toString();
			entry.x = home.bed().getX();
			entry.y = home.bed().getY();
			entry.z = home.bed().getZ();
			entry.minX = home.min().getX();
			entry.minY = home.min().getY();
			entry.minZ = home.min().getZ();
			entry.maxX = home.max().getX();
			entry.maxY = home.max().getY();
			entry.maxZ = home.max().getZ();
			entry.capacity = home.capacity();
			entry.beds = new ArrayList<>(home.beds().size());
			for (BlockPos bed : home.beds()) {
				entry.beds.add(new int[]{bed.getX(), bed.getY(), bed.getZ()});
			}
			entry.residents = new ArrayList<>(home.residents().size());
			for (Home.Resident resident : home.residents()) {
				ResidentData residentData = new ResidentData();
				residentData.worker = resident.worker().toString();
				residentData.x = resident.bed().getX();
				residentData.y = resident.bed().getY();
				residentData.z = resident.bed().getZ();
				entry.residents.add(residentData);
			}
			entry.createdAt = home.createdAt();
			entry.checkedAt = home.checkedAt();
			entry.valid = home.valid();
			entry.failure = home.failure();
			data.homes.add(entry);
		}

		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException exception) {
			LaborCraft.LOGGER.error("Falha ao salvar as casas {}", file, exception);
		}
	}

	private static List<BlockPos> readBeds(List<int[]> raw) {
		if (raw == null) {
			return List.of();
		}

		List<BlockPos> beds = new ArrayList<>(raw.size());
		for (int[] value : raw) {
			if (value != null && value.length == 3) {
				beds.add(new BlockPos(value[0], value[1], value[2]));
			}
		}
		beds.sort((left, right) -> {
			int byX = Integer.compare(left.getX(), right.getX());
			if (byX != 0) {
				return byX;
			}
			int byZ = Integer.compare(left.getZ(), right.getZ());
			return byZ != 0 ? byZ : Integer.compare(left.getY(), right.getY());
		});
		return List.copyOf(beds);
	}

	private static List<Home.Resident> readResidents(List<ResidentData> raw) {
		if (raw == null) {
			return List.of();
		}

		List<Home.Resident> residents = new ArrayList<>(raw.size());
		for (ResidentData value : raw) {
			if (value == null) {
				continue;
			}
			UUID worker = parseUuid(value.worker);
			if (worker != null) {
				residents.add(new Home.Resident(worker, new BlockPos(value.x, value.y, value.z)));
			}
		}
		return List.copyOf(residents);
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

	public record ClaimResult(Home home, String failure) {
		public boolean ok() {
			return home != null && failure == null;
		}

		public boolean alreadyOwner() {
			return "already_owner".equals(failure);
		}
	}

	public record AssignResult(Home home, BlockPos bed, String failure, boolean moved) {
		public boolean ok() {
			return home != null && failure == null;
		}
	}

	public record Home(
			String id,
			String name,
			String dimension,
			UUID owner,
			BlockPos bed,
			BlockPos min,
			BlockPos max,
			int capacity,
			List<BlockPos> beds,
			List<Resident> residents,
			long createdAt,
			long checkedAt,
			boolean valid,
			String failure
	) {
		public Home {
			bed = bed.immutable();
			min = min.immutable();
			max = max.immutable();
			beds = List.copyOf(beds);
			residents = List.copyOf(residents);
			failure = failure == null ? "" : failure;
		}

		public boolean isOwner(UUID candidate) {
			return owner != null && owner.equals(candidate);
		}

		public boolean containsBed(BlockPos pos) {
			if (bed.equals(pos)) {
				return true;
			}
			for (BlockPos candidate : beds) {
				if (candidate.equals(pos)) {
					return true;
				}
				// Accept the other half of the bed (head/foot).
				if (candidate.getY() == pos.getY()
						&& Math.abs(candidate.getX() - pos.getX()) + Math.abs(candidate.getZ() - pos.getZ()) == 1) {
					return true;
				}
			}
			return false;
		}

		/** Beds that count as slots: at most the measured room capacity. */
		public List<BlockPos> slotBeds() {
			return HomeRegistry.slotBeds(beds, capacity);
		}

		public int slots() {
			return valid ? slotBeds().size() : 0;
		}

		public BlockPos matchSlotBed(BlockPos pos) {
			for (BlockPos candidate : slotBeds()) {
				if (candidate.equals(pos)) {
					return candidate;
				}
				if (candidate.getY() == pos.getY()
						&& Math.abs(candidate.getX() - pos.getX()) + Math.abs(candidate.getZ() - pos.getZ()) == 1) {
					return candidate;
				}
			}
			return null;
		}

		public BlockPos firstFreeBed() {
			for (BlockPos candidate : slotBeds()) {
				if (residentAt(candidate) == null) {
					return candidate;
				}
			}
			return null;
		}

		public List<BlockPos> freeBeds() {
			List<BlockPos> free = new ArrayList<>();
			for (BlockPos candidate : slotBeds()) {
				if (residentAt(candidate) == null) {
					free.add(candidate);
				}
			}
			return free;
		}

		public Resident resident(UUID worker) {
			for (Resident resident : residents) {
				if (resident.worker().equals(worker)) {
					return resident;
				}
			}
			return null;
		}

		public Resident residentAt(BlockPos bedPos) {
			for (Resident resident : residents) {
				if (resident.bed().equals(bedPos)) {
					return resident;
				}
			}
			return null;
		}

		public boolean overlaps(BlockPos otherMin, BlockPos otherMax) {
			return min.getX() <= otherMax.getX() && max.getX() >= otherMin.getX()
					&& min.getY() <= otherMax.getY() && max.getY() >= otherMin.getY()
					&& min.getZ() <= otherMax.getZ() && max.getZ() >= otherMin.getZ();
		}

		public Home withName(String newName) {
			return new Home(
					id,
					newName,
					dimension,
					owner,
					bed,
					min,
					max,
					capacity,
					beds,
					residents,
					createdAt,
					checkedAt,
					valid,
					failure
			);
		}

		public Home withResidents(List<Resident> newResidents) {
			return new Home(
					id,
					name,
					dimension,
					owner,
					bed,
					min,
					max,
					capacity,
					beds,
					newResidents,
					createdAt,
					checkedAt,
					valid,
					failure
			);
		}

		public Home withRevalidation(
				boolean newValid,
				String newFailure,
				BlockPos newMin,
				BlockPos newMax,
				int newCapacity,
				List<BlockPos> newBeds,
				long newCheckedAt
		) {
			return new Home(
					id,
					name,
					dimension,
					owner,
					bed,
					newMin,
					newMax,
					newCapacity,
					newBeds,
					residents,
					createdAt,
					newCheckedAt,
					newValid,
					newFailure
			);
		}

		public record Resident(UUID worker, BlockPos bed) {
			public Resident {
				bed = bed.immutable();
			}
		}
	}

	private static final class HomeFile {
		private List<HomeData> homes = new ArrayList<>();
	}

	private static final class HomeData {
		private String id;
		private String name;
		private String dimension;
		private String owner;
		private int x;
		private int y;
		private int z;
		private int minX;
		private int minY;
		private int minZ;
		private int maxX;
		private int maxY;
		private int maxZ;
		private int capacity;
		private List<int[]> beds;
		private List<ResidentData> residents;
		private long createdAt;
		private long checkedAt;
		private boolean valid;
		private String failure;
	}

	private static final class ResidentData {
		private String worker;
		private int x;
		private int y;
		private int z;
	}
}

package com.foque.laborcraft.config;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LaborCraftConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path FILE = Path.of("config", "laborcraft.json");

	private static LaborCraftConfig instance = new LaborCraftConfig();

	public int schemaVersion = 2;
	public String currencyProvider = "laborcraft:ledger";
	public boolean allowOwnerOverrides = true;

	public WorkerSettings worker = new WorkerSettings();
	public OfferSettings offers = new OfferSettings();
	public HousingSettings housing = new HousingSettings();
	public AcquisitionSettings acquisition = new AcquisitionSettings();
	public AreaSettings area = new AreaSettings();
	public StorageSettings storage = new StorageSettings();
	public ScheduleSettings schedule = new ScheduleSettings();
	public PathingSettings pathing = new PathingSettings();
	public SpeechSettings speech = new SpeechSettings();
	public CompatibilitySettings compatibility = new CompatibilitySettings();
	public BillingSettings billing = new BillingSettings();
	public EnclosureSettings enclosure = new EnclosureSettings();
	public JobSettings farmer = JobSettings.farmer();
	public Map<String, JobSettings> jobs = new LinkedHashMap<>();

	public LaborCraftConfig() {
		normalize();
	}

	/** Fills the per-job table and keeps the legacy {@code farmer} field pointing at the farmer entry. */
	public void normalize() {
		if (jobs == null) {
			jobs = new LinkedHashMap<>();
		}
		if (farmer == null) {
			farmer = JobSettings.farmer();
		}
		jobs.putIfAbsent("laborcraft:farmer", farmer);
		jobs.putIfAbsent("laborcraft:herder", JobSettings.herder());
		farmer = jobs.get("laborcraft:farmer");
	}

	/** Settings for one job; unknown jobs fall back to the default/farmer table. */
	public JobSettings job(String jobId) {
		if (jobId == null || jobId.isBlank()) {
			return farmer;
		}
		JobSettings settings = jobs.get(jobId);
		return settings == null ? farmer : settings;
	}

	public JobSettings defaultJob() {
		return job("laborcraft:farmer");
	}

	public static LaborCraftConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				LaborCraftConfig loaded = GSON.fromJson(reader, LaborCraftConfig.class);
				if (loaded != null) {
					instance = loaded;
					instance.normalize();
				}
			} catch (IOException | RuntimeException exception) {
				LaborCraft.LOGGER.error("Falha ao ler {}, usando padroes", FILE, exception);
			}
		}

		instance.save();
	}

	public void save() {
		normalize();
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			LaborCraft.LOGGER.error("Falha ao salvar {}", FILE, exception);
		}
	}

	public static final class WorkerSettings {
		public long[] masteryLevelThresholds = {0L, 100L, 300L, 700L, 1500L};
		public int masteryXpPerHarvest = 1;
		public boolean allowReassignment = true;
		public long reassignmentFee = 0L;
		public int reassignmentCooldownDays = 0;
		public boolean allowPickup = true;
		public boolean pickupRequiresEmptyInventory = true;
		public int maxWorkersPerPlayer = 1;

		public WorkerSettings() {
		}
	}

	public static final class OfferSettings {
		/** Quantas pessoas ficam disponiveis por rotacao na loja do servidor. */
		public int offerCount = 5;
		/** De quantos em quantos dias do mundo a loja troca o estoque. */
		public int rotationDays = 1;

		public OfferSettings() {
		}
	}

	public static final class HousingSettings {
		public boolean required = false;
		public boolean requireBed = true;
		public boolean requireRoof = true;
		public boolean requireDoor = true;
		public boolean requireEnclosure = true;
		/** Requires at least one light-emitting block (torch, lantern, glowstone...) inside the house. */
		public boolean requireLight = true;
		// Casas compartilhadas ficam para uma etapa futura; o primeiro build valida apenas casa separada.
		public boolean allowSharedHouse = false;
		public boolean oneWorkerPerBed = true;
		public int maxDistanceFromAnchor = 32;
		public int maxVolume = 512;
		/** Lado minimo do interior (5 = casa externa 7x7 com parede de 1 bloco). */
		public int minInteriorSize = 5;
		/** Teto de camas por casa; a capacidade real sai da area interna. */
		public int maxBedsPerHouse = 4;
		public int recheckSeconds = 60;
		public int bonusRadius = 2;
		public double bonusSpeed = 0.10D;
		public int bonusMaxWorkers = 1;
		public double speedPenaltyWithoutHome = 0.25D;
		public List<String> comfortBlocks = new ArrayList<>(List.of(
				"minecraft:chest",
				"minecraft:crafting_table",
				"minecraft:furnace",
				"minecraft:bookshelf",
				"minecraft:torch",
				"minecraft:flower_pot"
		));
		public double comfortBonusPerBlock = 0.02D;
		public double comfortBonusMax = 0.10D;

		public HousingSettings() {
		}
	}

	public static final class AcquisitionSettings {
		public String mode = "purchase";
		public long price = 64L;
		public String revenueAccount = "server";
		public boolean contractTradeable = false;
		public boolean keepProfileBackup = true;

		public AcquisitionSettings() {
		}
	}

	public static final class AreaSettings {
		public String shape = "square";
		public int[] radiusByTier = {4, 6, 8, 9};
		public int verticalRadius = 4;
		public int maxRadius = 12;
		public boolean useHouseBonus = true;
		public boolean previewParticles = true;

		public AreaSettings() {
		}
	}

	public static final class StorageSettings {
		public int maxDistance = 8;
		public int maxContainers = 3;
		public boolean equipFromChest = false;
		public int workerSlots = 9;

		public StorageSettings() {
		}
	}

	public static final class ScheduleSettings {
		public String mode = "daylight";
		public int startTick = 0;
		public int endTick = 12000;
		public boolean workInRain = true;
		public boolean pauseBillingOffDuty = false;
		public int idleCheckTicks = 100;

		public ScheduleSettings() {
		}
	}

	public static final class PathingSettings {
		public int maxPathLength = 64;
		public int maxPathFailures = 5;
		public boolean allowLadders = false;
		public boolean allowDoors = true;

		public PathingSettings() {
		}
	}

	public static final class CompatibilitySettings {
		/** MineLab scoreboard objective used by minelab-ponte/minelab-bancos. */
		public String mineLabObjective = "plims";
		/** Format used for MineLab balances; %d/%,d are accepted. */
		public String mineLabFormat = "%,d plims";
		/** If the MineLab objective/name is unavailable, use the internal ledger instead. */
		public boolean mineLabFallbackToLedger = true;

		public CompatibilitySettings() {
		}
	}

	public static final class SpeechSettings {
		/** False by default: bubbles are hidden when a wall blocks the line of sight. */
		public boolean bubbleThroughWalls = false;

		public SpeechSettings() {
		}
	}

	public static final class BillingSettings {
		public boolean enabled = true;
		public int checkTicks = 100;
		public boolean useSchedule = true;
		public boolean notifyOwner = true;
		public boolean allowDebt = true;
		/** Missed payments still worked; the next one after this pauses the worker. */
		public int gracePeriods = 1;
		/** 0 disables breaking the contract. */
		public int dismissAfterMissed = 0;
		public int missedMoodPenalty = 10;
		public boolean useSalaryTable = true;
		public int minimumDaily = 0;
		public boolean useRankMultiplier = true;
		public boolean useRarityMultiplier = true;
		public boolean useBoostUpkeep = true;
		/** Total multiplier cap (rank * rarity + boosts). */
		public double boostCap = 2.5D;
		/** Indexed by WorkerRank ordinal: COMUM, JUNIOR, PLENO, SENIOR. */
		public double[] rankMultipliers = {1.00D, 1.25D, 1.60D, 2.00D};
		/** Indexed by WorkerRarity ordinal: COMUM, INCOMUM, RARO, EPICO. */
		public double[] rarityMultipliers = {1.00D, 1.05D, 1.10D, 1.20D};

		public BillingSettings() {
		}
	}

	public static final class EnclosureSettings {
		/** How far from the clicked seed the pen scan may walk before giving up. */
		public int maxScanRadius = 64;
		/** Safety cap for the interior area; a huge field is not a pen. */
		public int maxArea = 4096;
		public int minArea = 9;

		public EnclosureSettings() {
		}
	}
	public static final class JobSettings {
		public boolean enabled = true;
		public int baseWage = 24;
		public int maxTier = 4;
		public int workRadius = 4;
		public int billingPeriodDays = 1;
		public boolean useBoneMeal = false;
		public boolean useComposter = false;
		public int seedSurplusForCompost = 64;
		public String waterPolicy = "prefer";

		public JobSettings() {
		}

		public static JobSettings herder() {
			JobSettings settings = new JobSettings();
			settings.baseWage = 20;
			settings.maxTier = 4;
			settings.workRadius = 8;
			settings.billingPeriodDays = 1;
			settings.useBoneMeal = false;
			settings.useComposter = false;
			return settings;
		}

		public static JobSettings farmer() {
			JobSettings settings = new JobSettings();
			settings.baseWage = 24;
			settings.maxTier = 4;
			settings.workRadius = 4;
			settings.billingPeriodDays = 1;
			settings.useBoneMeal = true;
			settings.useComposter = true;
			return settings;
		}
	}
}

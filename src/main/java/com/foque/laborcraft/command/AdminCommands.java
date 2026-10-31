package com.foque.laborcraft.command;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.area.EnclosureScan;
import com.foque.laborcraft.area.EnclosureScanner;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.BubbleSettingsPayload;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.worker.WorkerMarket;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** `/laborcraft admin ...`: recarregar config, status, mercado e ajustes de economia. */
final class AdminCommands {
	private AdminCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("admin")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("reload").executes(context -> reload(context.getSource())))
				.then(Commands.literal("status").executes(context -> status(context.getSource())))
				.then(Commands.literal("market")
						.then(Commands.literal("refresh")
								.executes(context -> refreshMarket(context.getSource()))))
				.then(Commands.literal("set")
						.then(Commands.literal("wage")
								.then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
										.executes(context -> setWage(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "value")
										))))
						.then(Commands.literal("provider")
								.then(Commands.argument("provider", StringArgumentType.greedyString())
										.suggests((context, builder) -> {
											CurrencyProviders.all()
													.forEach(provider -> builder.suggest(provider.id()));
											return builder.buildFuture();
										})
										.executes(context -> setProvider(
												context.getSource(),
												StringArgumentType.getString(context, "provider")
										))))
						.then(Commands.literal("balance")
								.then(Commands.argument("value", LongArgumentType.longArg(0L))
										.executes(context -> setBalance(
												context.getSource(),
												LongArgumentType.getLong(context, "value")
										))))
						.then(Commands.literal("bubble-walls")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(context -> setBubbleWalls(
												context.getSource(),
												BoolArgumentType.getBool(context, "value")
										))))
						.then(Commands.literal("max-workers")
								.then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
										.executes(context -> setMaxWorkers(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "value")
										)))))
				.then(Commands.literal("pen")
						.then(Commands.literal("scan")
								.executes(context -> scanPen(context.getSource()))))
				.then(Commands.literal("config")
						.then(Commands.argument("field", StringArgumentType.word())
								.suggests((context, builder) -> {
									CONFIG_FIELDS.forEach(builder::suggest);
									return builder.buildFuture();
								})
								.then(Commands.argument("value", StringArgumentType.greedyString())
										.executes(context -> setConfig(
												context.getSource(),
												StringArgumentType.getString(context, "field"),
												StringArgumentType.getString(context, "value")
										)))));
	}

	private static final java.util.List<String> CONFIG_FIELDS = java.util.List.of(
			"schedule.mode",
			"schedule.start-tick",
			"schedule.end-tick",
			"schedule.work-in-rain",
			"schedule.idle-check-ticks",
			"offers.offer-count",
			"offers.rotation-days",
			"worker.max-workers-per-player",
			"area.shape",
			"area.vertical-radius",
			"area.max-radius",
			"area.use-house-bonus",
			"area.preview-particles",
			"housing.required",
			"housing.require-bed",
			"housing.require-roof",
			"housing.require-door",
			"housing.require-enclosure",
			"housing.require-light",
			"housing.max-distance",
			"housing.max-volume",
			"housing.min-interior",
			"housing.max-beds",
			"housing.recheck-seconds"
	);

	private static int refreshMarket(CommandSourceStack source) {
		if (source.getServer() == null) {
			return 0;
		}

		WorkerMarket.refresh(source.getServer());
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.admin.market.refresh",
						WorkerMarket.remaining(source.getServer())
				),
				true
		);
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			OpenAdminPayload.open(player);
		}
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		LaborCraftConfig.load();
		LaborCraftConfig config = LaborCraftConfig.get();
		int jobs = JobTypes.all().size();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.reload", config.currencyProvider, jobs),
				true
		);
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			OpenAdminPayload.open(player);
		}
		return 1;
	}

	private static int status(CommandSourceStack source) {
		String version = FabricLoader.getInstance()
				.getModContainer(LaborCraft.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
		String provider = CurrencyProviders.active().id();
		String jobs = JobTypes.all().stream()
				.map(JobType::shortName)
				.collect(Collectors.joining(", "));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.status", version, provider, jobs),
				false
		);
		return 1;
	}

	private static int setMaxWorkers(CommandSourceStack source, int value) {
		LaborCraftConfig config = LaborCraftConfig.get();
		config.worker.maxWorkersPerPlayer = value;
		config.save();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.set.max_workers", value),
				true
		);
		return 1;
	}

	private static int scanPen(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.translatable("laborcraft.command.player_only"));
			return 0;
		}

		EnclosureScan scan = EnclosureScanner.scan(
				source.getLevel(),
				player.blockPosition(),
				LaborCraftConfig.get()
		);
		if (scan.valid()) {
			source.sendSuccess(
					() -> Component.translatable(
							"laborcraft.command.admin.pen.valid",
							scan.area(),
							scan.width(),
							scan.depth(),
							scan.gatePositions().size(),
							scan.boundaryBlocks()
					),
					false
			);
			return 1;
		}

		source.sendFailure(Component.translatable(
				"laborcraft.command.admin.pen.invalid",
				Component.translatable("laborcraft.pen.reason." + scan.failureReason())
		));
		return 0;
	}
	private static int setWage(CommandSourceStack source, int value) {
		LaborCraftConfig config = LaborCraftConfig.get();
		config.farmer.baseWage = value;
		config.save();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.set.wage", value),
				true
		);
		return 1;
	}

	private static int setBalance(CommandSourceStack source, long value)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		CurrencyProvider provider = CurrencyProviders.active();
		long current = provider.balance(player.getUUID());
		if (value > current) {
			provider.deposit(player.getUUID(), value - current);
		} else if (value < current && !provider.withdraw(player.getUUID(), current - value)) {
			source.sendFailure(Component.translatable(
					"laborcraft.command.admin.set.balance.failed",
					provider.format(current)
			));
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.set.balance", provider.format(value)),
				true
		);
		return 1;
	}

	private static int setProvider(CommandSourceStack source, String id) {
		CurrencyProvider provider = CurrencyProviders.get(id);
		if (provider == null) {
			String available = CurrencyProviders.all().stream()
					.map(CurrencyProvider::id)
					.collect(Collectors.joining(", "));
			source.sendFailure(
					Component.translatable("laborcraft.command.admin.set.provider.unknown", id, available)
			);
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		config.currencyProvider = provider.id();
		config.save();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.set.provider", provider.id()),
				true
		);
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			OpenAdminPayload.open(player);
		}
		return 1;
	}

	private static int setConfig(CommandSourceStack source, String field, String value) {
		LaborCraftConfig config = LaborCraftConfig.get();
		String key = field.toLowerCase(java.util.Locale.ROOT);
		boolean refreshMarket = false;

		try {
			switch (key) {
				case "schedule.mode" -> {
					String mode = value.toLowerCase(java.util.Locale.ROOT);
					if (!java.util.Set.of("daylight", "night", "custom", "always").contains(mode)) {
						throw new IllegalArgumentException(value);
					}
					config.schedule.mode = mode;
				}
				case "schedule.start-tick" -> config.schedule.startTick = clamp(parseInt(value), 0, 23_999);
				case "schedule.end-tick" -> config.schedule.endTick = clamp(parseInt(value), 0, 23_999);
				case "schedule.work-in-rain" -> config.schedule.workInRain = parseBoolean(value);
				case "schedule.idle-check-ticks" ->
						config.schedule.idleCheckTicks = clamp(parseInt(value), 20, 24_000);
				case "offers.offer-count" -> {
					config.offers.offerCount = clamp(parseInt(value), 1, 20);
					refreshMarket = true;
				}
				case "offers.rotation-days" -> {
					config.offers.rotationDays = clamp(parseInt(value), 1, 365);
					refreshMarket = true;
				}
				case "worker.max-workers-per-player" ->
						config.worker.maxWorkersPerPlayer = clamp(parseInt(value), 0, 100);
				case "area.shape" -> {
					String shape = value.toLowerCase(java.util.Locale.ROOT);
					if (!java.util.Set.of("square", "circle").contains(shape)) {
						throw new IllegalArgumentException(value);
					}
					config.area.shape = shape;
				}
				case "area.vertical-radius" -> config.area.verticalRadius = clamp(parseInt(value), 1, 32);
				case "area.max-radius" -> config.area.maxRadius = clamp(parseInt(value), 1, 64);
				case "area.use-house-bonus" -> config.area.useHouseBonus = parseBoolean(value);
				case "area.preview-particles" -> config.area.previewParticles = parseBoolean(value);
				case "housing.required" -> config.housing.required = parseBoolean(value);
				case "housing.require-bed" -> config.housing.requireBed = parseBoolean(value);
				case "housing.require-roof" -> config.housing.requireRoof = parseBoolean(value);
				case "housing.require-door" -> config.housing.requireDoor = parseBoolean(value);
				case "housing.require-enclosure" -> config.housing.requireEnclosure = parseBoolean(value);
				case "housing.require-light" -> config.housing.requireLight = parseBoolean(value);
				case "housing.max-distance" -> config.housing.maxDistanceFromAnchor = clamp(parseInt(value), 1, 256);
				case "housing.max-volume" -> config.housing.maxVolume = clamp(parseInt(value), 1, 100000);
				case "housing.min-interior" -> config.housing.minInteriorSize = clamp(parseInt(value), 1, 64);
				case "housing.max-beds" -> config.housing.maxBedsPerHouse = clamp(parseInt(value), 1, 64);
				case "housing.recheck-seconds" -> config.housing.recheckSeconds = clamp(parseInt(value), 1, 3600);
				default -> {
					source.sendFailure(Component.translatable("laborcraft.command.admin.config.unknown", field));
					return 0;
				}
			}
		} catch (IllegalArgumentException exception) {
			source.sendFailure(Component.translatable("laborcraft.command.admin.config.invalid", field, value));
			return 0;
		}

		config.save();
		if (refreshMarket) {
			WorkerMarket.refresh(source.getServer());
		}
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.config.set", field, value),
				true
		);
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			OpenAdminPayload.open(player);
		}
		return 1;
	}

	private static boolean parseBoolean(String value) {
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)) {
			return false;
		}
		throw new IllegalArgumentException(value);
	}

	private static int parseInt(String value) {
		return Integer.parseInt(value);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int setBubbleWalls(CommandSourceStack source, boolean value) {
		LaborCraftConfig config = LaborCraftConfig.get();
		config.speech.bubbleThroughWalls = value;
		config.save();
		BubbleSettingsPayload.sendAll(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.set.bubble_walls", value),
				true
		);
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			OpenAdminPayload.open(player);
		}
		return 1;
	}
}

package com.foque.laborcraft.command;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.item.LaborCraftItems;
import com.foque.laborcraft.item.WorkerContractData;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.network.OpenConfigPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenItemTestPayload;
import com.foque.laborcraft.network.OpenPlayersPayload;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.SetLanguagePayload;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOffers;
import com.foque.laborcraft.worker.WorkerOwnership;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;

public final class LaborCraftCommands {
	public static final String UI = "laborcraft ui";
	public static final String CONFIG = "laborcraft config";
	public static final String TEST_ITEMS = "laborcraft test items";
	public static final String PLAYERS = "laborcraft players";
	public static final String PROFILE = "laborcraft profile";
	public static final String HIRE = "laborcraft hire";
	public static final String WORKER = "laborcraft worker";
	public static final String WORKER_SPAWN = WORKER + " spawn";
	public static final String WORKER_INFO = WORKER + " info";
	public static final String WORKER_REMOVE = WORKER + " remove";
	public static final String WORKER_BUY = WORKER + " buy";
	public static final String WORKER_ASSIGN = WORKER + " assign";
	public static final String WORKER_DISMISS = WORKER + " dismiss";
	public static final String LANGUAGE = "laborcraft language";
	public static final String LANGUAGE_PT_BR = LANGUAGE + " pt_br";
	public static final String LANGUAGE_EN_US = LANGUAGE + " en_us";
	public static final String ADMIN_RELOAD = "laborcraft admin reload";
	public static final String ADMIN_STATUS = "laborcraft admin status";
	public static final String ADMIN_SET_WAGE = "laborcraft admin set wage";
	public static final String ADMIN_SET_PROVIDER = "laborcraft admin set provider";
	public static final String ADMIN_SET_BALANCE = "laborcraft admin set balance";

	private LaborCraftCommands() {
	}

	public static void bootstrap() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("laborcraft")
						.then(Commands.literal("ui").executes(context -> openUi(context.getSource())))
						.then(Commands.literal("config").executes(context -> openConfig(context.getSource())))
						.then(Commands.literal("test")
								.then(Commands.literal("items").executes(context -> openItemTest(context.getSource()))))
						.then(Commands.literal("players").executes(context -> openPlayers(context.getSource())))
						.then(Commands.literal("profile").executes(context -> openProfile(context.getSource())))
						.then(Commands.literal("hire").executes(context -> openHiring(context.getSource())))
						.then(Commands.literal("worker")
								.then(Commands.literal("spawn")
										.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
										.executes(context -> spawnWorker(context.getSource(), null))
										.then(Commands.argument("name", StringArgumentType.greedyString())
												.executes(context -> spawnWorker(
														context.getSource(),
														StringArgumentType.getString(context, "name")
												))))
								.then(Commands.literal("info").executes(context -> workerInfo(context.getSource())))
								.then(Commands.literal("remove")
										.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
										.executes(context -> removeWorker(context.getSource())))
								.then(Commands.literal("buy")
										.executes(context -> buyWorker(
												context.getSource(),
												WorkerOffers.standard().id()
										))
										.then(Commands.argument("offer", StringArgumentType.word())
												.suggests((context, builder) -> {
													WorkerOffers.available()
															.forEach(offer -> builder.suggest(offer.id()));
													return builder.buildFuture();
												})
												.executes(context -> buyWorker(
														context.getSource(),
														StringArgumentType.getString(context, "offer")
												))))
								.then(Commands.literal("assign")
										.then(Commands.argument("worker", StringArgumentType.word())
												.then(Commands.argument("job", IdentifierArgument.id())
														.suggests((context, builder) -> {
															JobTypes.all().forEach(job -> builder.suggest(job.id()));
															return builder.buildFuture();
														})
														.executes(context -> assignWorker(
																context.getSource(),
																StringArgumentType.getString(context, "worker"),
																IdentifierArgument.getId(context, "job")
														)))))
								.then(Commands.literal("dismiss")
										.then(Commands.argument("worker", StringArgumentType.word())
												.executes(context -> dismissWorker(
														context.getSource(),
														StringArgumentType.getString(context, "worker")
												)))))
						.then(Commands.literal("language")
								.then(Commands.argument("code", StringArgumentType.word())
										.suggests((context, builder) -> {
											builder.suggest("pt_br");
											builder.suggest("en_us");
											return builder.buildFuture();
										})
										.executes(context -> setLanguage(
												context.getSource(),
												StringArgumentType.getString(context, "code")
										))))
						.then(Commands.literal("admin")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.literal("reload").executes(context -> reload(context.getSource())))
								.then(Commands.literal("status").executes(context -> status(context.getSource())))
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
								)
						)
				)
		);
	}

	private static int openUi(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenAdminPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenAdminPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openConfig(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenConfigPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenConfigPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenItemTestPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenItemTestPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openPlayers(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenPlayersPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenPlayersPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openProfile(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (OpenProfilePayload.open(player)) {
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openHiring(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (OpenHiringPayload.open(player)) {
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int spawnWorker(CommandSourceStack source, String name)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		source.getPlayerOrException();
		BlockPos position = BlockPos.containing(source.getPosition());
		LaborCraftWorker worker = LaborCraftEntities.WORKER.spawn(
				source.getLevel(),
				position,
				EntitySpawnReason.COMMAND
		);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.spawn_failed"));
			return 0;
		}

		Component displayName = name == null || name.isBlank()
				? Component.translatable("entity.laborcraft.worker")
				: Component.literal(name);
		worker.setWorkerName(displayName);

		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.spawn",
						worker.getName(),
						position.getX(),
						position.getY(),
						position.getZ()
				),
				false
		);
		return 1;
	}

	private static int buyWorker(CommandSourceStack source, String offerId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		WorkerOffer offer = WorkerOffers.get(offerId);
		if (offer == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.unknown_offer", offerId));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		int limit = config.worker.maxWorkersPerPlayer;
		int total = WorkerOwnership.total(player);
		if (limit > 0 && total >= limit) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.limit", limit));
			return 0;
		}

		CurrencyProvider provider = CurrencyProviders.active();
		long price = offer.price();
		long balance = provider.balance(player.getUUID());
		if (price > 0L && !provider.withdraw(player.getUUID(), price)) {
			source.sendFailure(Component.translatable(
					"laborcraft.command.worker.buy.no_money",
					provider.format(balance),
					provider.format(price)
			));
			return 0;
		}

		ItemStack contract = LaborCraftItems.newContract(
				player.getUUID(),
				WorkerContractData.fromOffer(offer)
		);
		if (!player.getInventory().add(contract)) {
			if (price > 0L) {
				provider.deposit(player.getUUID(), price);
			}
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.inventory_full"));
			return 0;
		}

		if (price > 0L) {
			creditRevenue(provider, config.acquisition.revenueAccount, price);
		}

		OpenHiringPayload.open(player);
		Component priceLabel = price <= 0L
				? Component.translatable("laborcraft.ui.hire.free")
				: Component.literal(provider.format(price));
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.buy.contract",
						Component.translatable(offer.nameKey()),
						priceLabel
				),
				false
		);
		return 1;
	}

	private static void creditRevenue(CurrencyProvider provider, String account, long price) {
		if (account == null || account.isBlank() || "server".equalsIgnoreCase(account)) {
			provider.deposit(serverAccount(), price);
			return;
		}

		try {
			provider.deposit(UUID.fromString(account), price);
		} catch (IllegalArgumentException exception) {
			provider.deposit(serverAccount(), price);
		}
	}

	private static UUID serverAccount() {
		return UUID.nameUUIDFromBytes("laborcraft:server".getBytes(StandardCharsets.UTF_8));
	}

	private static int assignWorker(CommandSourceStack source, String workerId, Identifier jobArgument)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = findWorker(source, workerId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}

		String jobId = normalizeJobId(jobArgument);
		JobType job = JobTypes.get(jobId);
		if (job == null) {
			String available = JobTypes.all().stream()
					.map(JobType::id)
					.collect(Collectors.joining(", "));
			source.sendFailure(Component.translatable("laborcraft.command.worker.unknown_job", jobId, available));
			return 0;
		}

		worker.setJobId(job.id());
		OpenWorkerPayload.open(player, worker);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.assign.success", job.id(), worker.getName()),
				false
		);
		return 1;
	}

	private static int dismissWorker(CommandSourceStack source, String workerId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = findWorker(source, workerId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		WorkerContractData data = WorkerContractData.fromWorker(worker);
		Component workerName = worker.getName();
		worker.discard();

		UUID contractOwner = config.acquisition.contractTradeable ? null : player.getUUID();
		ItemStack contract = LaborCraftItems.newContract(contractOwner, data);
		if (!player.getInventory().add(contract)) {
			player.drop(contract, false, Prediction.SERVER_ONLY);
		}

		OpenHiringPayload.open(player);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.dismiss.contract", workerName),
				false
		);
		return 1;
	}

	private static String normalizeJobId(Identifier jobArgument) {
		return jobArgument.getNamespace().equals("minecraft")
				? LaborCraft.id(jobArgument.getPath()).toString()
				: jobArgument.toString();
	}

	private static LaborCraftWorker findWorker(CommandSourceStack source, String rawId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		UUID workerId;
		try {
			workerId = UUID.fromString(rawId);
		} catch (IllegalArgumentException exception) {
			return null;
		}

		ServerPlayer player = source.getPlayerOrException();
		return source.getLevel().getEntitiesOfClass(
						LaborCraftWorker.class,
						player.getBoundingBox().inflate(16.0D),
						worker -> worker.getUUID().equals(workerId)
				)
				.stream()
				.findFirst()
				.orElse(null);
	}

	private static int workerInfo(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		LaborCraftWorker worker = nearestWorker(source, true);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.none"));
			return 0;
		}

		BlockPos position = worker.blockPosition();
		JobType job = JobTypes.get(worker.jobId());
		Component jobLabel;
		if (!worker.hasJob()) {
			jobLabel = Component.translatable("laborcraft.ui.worker.job.none");
		} else if (job == null) {
			jobLabel = Component.translatable("laborcraft.ui.worker.job.unknown", worker.jobId());
		} else {
			jobLabel = Component.translatable(job.translationKey());
		}
		Component stateLabel = Component.translatable(
				"laborcraft.ui.worker.state." + worker.workerState().name().toLowerCase(Locale.ROOT)
		);
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.info",
						worker.getName(),
						jobLabel,
						stateLabel,
						(int) worker.getHealth(),
						position.getX(),
						position.getY(),
						position.getZ()
				),
				false
		);
		return 1;
	}

	private static int removeWorker(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		LaborCraftWorker worker = nearestWorker(source, false);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.none"));
			return 0;
		}

		Component name = worker.getName();
		worker.discard();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.removed", name),
				false
		);
		return 1;
	}

	private static LaborCraftWorker nearestWorker(CommandSourceStack source, boolean ownedOnly)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		List<LaborCraftWorker> workers = source.getLevel().getEntitiesOfClass(
				LaborCraftWorker.class,
				player.getBoundingBox().inflate(16.0D),
				worker -> !ownedOnly || worker.isOwnedBy(player.getUUID())
		);
		return workers.stream()
				.min(Comparator.comparingDouble(worker -> worker.position().distanceToSqr(player.position())))
				.orElse(null);
	}

	private static int setLanguage(CommandSourceStack source, String code) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String normalized = code.toLowerCase(Locale.ROOT);
		if (!normalized.equals("pt_br") && !normalized.equals("en_us")) {
			source.sendFailure(
					Component.translatable("laborcraft.command.language.unknown", code, "pt_br, en_us")
			);
			return 0;
		}

		ServerPlayer player = source.getPlayerOrException();
		if (!ServerPlayNetworking.canSend(player, SetLanguagePayload.TYPE)) {
			source.sendFailure(Component.translatable("laborcraft.command.language.unavailable"));
			return 0;
		}

		ServerPlayNetworking.send(player, new SetLanguagePayload(normalized));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.language.set", normalized),
				false
		);
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
		return 1;
	}
}

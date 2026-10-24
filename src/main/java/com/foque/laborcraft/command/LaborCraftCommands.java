package com.foque.laborcraft.command;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HousingMessages;
import com.foque.laborcraft.housing.HousingReport;
import com.foque.laborcraft.housing.HousingValidator;
import com.foque.laborcraft.item.LaborCraftItems;
import com.foque.laborcraft.item.WorkerContractData;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.CloseScreenPayload;
import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.network.OpenConfigPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenItemTestPayload;
import com.foque.laborcraft.network.OpenPlayersPayload;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.foque.laborcraft.network.OpenStationPayload;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.SetLanguagePayload;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerMarket;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOwnership;
import com.foque.laborcraft.worker.WorkerState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
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
	public static final String STATION = "laborcraft station";
	public static final String STATION_BIND = STATION + " bind";
	public static final String STATION_UNBIND = STATION + " unbind";
	public static final String STATION_RECALL = STATION + " recall";
	public static final String STATION_RESUME = STATION + " resume";
	public static final String STATION_ZONE_SET = STATION + " zone set";
	public static final String STATION_ZONE_CLEAR = STATION + " zone clear";
	public static final String STATION_STORAGE_LINK = STATION + " storage link";
	public static final String STATION_STORAGE_UNLINK = STATION + " storage unlink";
	public static final String STATION_STORAGE_ROLE = STATION + " storage role";
	public static final String STATION_STORAGE_SCAN = STATION + " storage scan";
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
										.executes(context -> buyWorker(context.getSource(), null))
										.then(Commands.argument("offer", IdentifierArgument.id())
												.suggests((context, builder) -> {
													WorkerMarket.offers(context.getSource().getServer())
															.forEach(offer -> builder.suggest(offer.id()));
													return builder.buildFuture();
												})
												.executes(context -> buyWorker(
														context.getSource(),
														IdentifierArgument.getId(context, "offer").toString()
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
						.then(Commands.literal("station")
								.then(Commands.literal("bind")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.then(Commands.argument("worker", StringArgumentType.word())
														.suggests((context, builder) -> {
															ServerPlayer player = context.getSource().getPlayer();
															if (player != null) {
																WorkerOwnership.ownedWorkerList(player)
																		.forEach(worker -> builder.suggest(worker.getUUID().toString()));
															}
															return builder.buildFuture();
														})
														.executes(context -> stationBind(
																context.getSource(),
																BlockPosArgument.getLoadedBlockPos(context, "pos"),
																StringArgumentType.getString(context, "worker")
														)))))
								.then(Commands.literal("unbind")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationUnbind(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
												))))
								.then(Commands.literal("recall")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationRecall(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														true
												))))
								.then(Commands.literal("pause")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationRecall(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														false
												))))
								.then(Commands.literal("resume")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationResume(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
												))))
								.then(Commands.literal("open")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationOpen(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
												))))
								.then(Commands.literal("info")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> stationInfo(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
												))))
								.then(Commands.literal("zone")
									.then(Commands.literal("set")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.then(Commands.argument("from", BlockPosArgument.blockPos())
												.then(Commands.argument("to", BlockPosArgument.blockPos())
													.executes(context -> stationZoneSet(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														BlockPosArgument.getLoadedBlockPos(context, "from"),
														BlockPosArgument.getLoadedBlockPos(context, "to")
													))))))
									.then(Commands.literal("clear")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.executes(context -> stationZoneClear(
												context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos")
											)))))
								.then(Commands.literal("storage")
									.then(Commands.literal("link")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.then(Commands.argument("chest", BlockPosArgument.blockPos())
												.executes(context -> stationStorageLink(
													context.getSource(),
													BlockPosArgument.getLoadedBlockPos(context, "pos"),
													BlockPosArgument.getLoadedBlockPos(context, "chest"),
													"mixed"
												))
												.then(Commands.argument("role", StringArgumentType.word())
													.suggests((context, builder) -> {
														for (StationRegistry.StorageRole role : StationRegistry.StorageRole.values()) {
															builder.suggest(role.id());
														}
														return builder.buildFuture();
													})
													.executes(context -> stationStorageLink(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														BlockPosArgument.getLoadedBlockPos(context, "chest"),
														StringArgumentType.getString(context, "role")
													))))))
									.then(Commands.literal("unlink")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.then(Commands.argument("chest", BlockPosArgument.blockPos())
												.executes(context -> stationStorageUnlink(
													context.getSource(),
													BlockPosArgument.getLoadedBlockPos(context, "pos"),
													BlockPosArgument.getLoadedBlockPos(context, "chest")
												)))))
									.then(Commands.literal("role")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.then(Commands.argument("chest", BlockPosArgument.blockPos())
												.then(Commands.argument("role", StringArgumentType.word())
													.suggests((context, builder) -> {
														for (StationRegistry.StorageRole role : StationRegistry.StorageRole.values()) {
															builder.suggest(role.id());
														}
														return builder.buildFuture();
													})
													.executes(context -> stationStorageRole(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														BlockPosArgument.getLoadedBlockPos(context, "chest"),
														StringArgumentType.getString(context, "role")
													))))))
									.then(Commands.literal("scan")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.executes(context -> stationStorageScan(
												context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos")
											))))
									.then(Commands.literal("list")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
											.executes(context -> stationStorageList(
												context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos")
											))))))
						.then(Commands.literal("home")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.literal("scan")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> scanHome(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
												))))
								.then(Commands.literal("measure")
										.then(Commands.argument("pos", BlockPosArgument.blockPos())
												.executes(context -> measureHome(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos")
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
		if (player.level().getServer() == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.no_market"));
			return 0;
		}

		List<WorkerOffer> offers = WorkerMarket.offers(player.level().getServer());
		if (offers.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.no_offers"));
			return 0;
		}

		WorkerOffer offer = offerId == null || offerId.isBlank()
				? offers.getFirst()
				: offers.stream().filter(candidate -> candidate.id().equals(offerId)).findFirst().orElse(null);
		if (offer == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.unknown_offer", String.valueOf(offerId)));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		int limit = config.worker.maxWorkersPerPlayer;
		int total = WorkerOwnership.total(player);
		if (limit > 0 && total >= limit) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.limit", limit));
			return 0;
		}

		// No criativo o Inventory.add() descarta o que nao couber; checar antes evita perder o contrato.
		if (player.getInventory().getFreeSlot() < 0) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.inventory_full"));
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

		WorkerOffer taken = WorkerMarket.take(player.level().getServer(), offer.id());
		if (taken == null) {
			if (price > 0L) {
				provider.deposit(player.getUUID(), price);
			}
			source.sendFailure(Component.translatable("laborcraft.command.worker.buy.sold_out", offer.name()));
			return 0;
		}

		ItemStack contract = LaborCraftItems.newContract(
				player.getUUID(),
				WorkerContractData.fromOffer(taken)
		);
		if (!player.getInventory().add(contract)) {
			WorkerMarket.restore(player.level().getServer(), taken);
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
		Component workerName = Component.literal(taken.name());
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.buy.contract",
						workerName,
						priceLabel
				),
				false
		);
		return 1;
	}

	private static int refreshMarket(CommandSourceStack source) {
		if (source.getServer() == null) {
			return 0;
		}

		WorkerMarket.refresh(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.admin.market.refresh", WorkerMarket.remaining(source.getServer())),
				true
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

		UUID contractOwner = config.acquisition.contractTradeable ? null : player.getUUID();
		ItemStack contract = LaborCraftItems.newContract(contractOwner, data);
		// Copia intacta: no criativo o Inventory.add() descarta o que nao couber e ainda devolve true.
		ItemStack fallback = contract.copy();
		boolean hasRoom = player.getInventory().getFreeSlot() >= 0;

		worker.discard();
		boolean stored = hasRoom && player.getInventory().add(contract);

		if (!stored) {
			if (player.level() instanceof ServerLevel serverLevel) {
				player.spawnAtLocation(serverLevel, fallback);
			}

			source.sendSystemMessage(Component.translatable("laborcraft.command.worker.dismiss.dropped"));
		}

		// Nada de reabrir a loja: a dispensa sÃ³ fecha a tela de Empregado.
		CloseScreenPayload.open(player);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.dismiss.contract", workerName),
				false
		);
		return 1;
	}

	private static int scanHome(CommandSourceStack source, BlockPos pos) {
		HousingReport report = HousingValidator.validate(source.getLevel(), pos, LaborCraftConfig.get());
		sendHousingReport(source, pos, report);
		return report.valid() ? 1 : 0;
	}

	private static int measureHome(CommandSourceStack source, BlockPos pos) {
		HousingReport report = HousingValidator.measure(source.getLevel(), pos, LaborCraftConfig.get());
		sendHousingReport(source, pos, report);
		return report.valid() ? 1 : 0;
	}

	private static void sendHousingReport(CommandSourceStack source, BlockPos pos, HousingReport report) {
		for (Component line : HousingMessages.describe(pos, report)) {
			source.sendSuccess(() -> line, false);
		}
	}

	private static StationRegistry.Station stationAt(CommandSourceStack source, BlockPos pos) {
		return StationRegistry.get(
				source.getServer(),
				source.getLevel().dimension().identifier().toString(),
				pos
		);
	}

	private static LaborCraftWorker ownedWorker(ServerPlayer player, UUID workerId) {
		return WorkerOwnership.ownedWorkerList(player).stream()
				.filter(worker -> worker.getUUID().equals(workerId))
				.findFirst()
				.orElse(null);
	}

	private static UUID parseUuid(String raw) {
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static int stationBind(CommandSourceStack source, BlockPos pos, String rawWorker)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		UUID workerId = parseUuid(rawWorker);
		LaborCraftWorker worker = workerId == null ? null : ownedWorker(player, workerId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		if (station.worker() != null) {
			source.sendFailure(Component.translatable("laborcraft.station.full"));
			return 0;
		}

		if (StationRegistry.byWorker(source.getServer(), workerId) != null) {
			source.sendFailure(Component.translatable("laborcraft.station.worker_bound"));
			return 0;
		}

		if (!StationRegistry.bind(source.getServer(), station, workerId)) {
			source.sendFailure(Component.translatable("laborcraft.station.bind_failed"));
			return 0;
		}

		OpenStationPayload.open(player, stationAt(source, pos));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.bound", worker.getName()),
				false
		);
		return 1;
	}

	private static int stationUnbind(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		if (!StationRegistry.unbind(source.getServer(), station)) {
			source.sendFailure(Component.translatable("laborcraft.station.empty"));
			return 0;
		}

		OpenStationPayload.open(player, stationAt(source, pos));
		source.sendSuccess(() -> Component.translatable("laborcraft.station.unbound"), false);
		return 1;
	}

	private static int stationRecall(CommandSourceStack source, BlockPos pos, boolean walk)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null || station.worker() == null) {
			source.sendFailure(Component.translatable("laborcraft.station.empty"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		LaborCraftWorker worker = LaborCraftWorker.find(source.getServer(), station.worker());
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		worker.setWorkerState(WorkerState.PAUSED);
		if (walk) {
			worker.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 1.0D);
		}

		OpenStationPayload.open(player, station);
		source.sendSuccess(
				() -> Component.translatable(
						walk ? "laborcraft.station.recalled" : "laborcraft.station.paused",
						worker.getName()
				),
				false
		);
		return 1;
	}

	private static int stationResume(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null || station.worker() == null) {
			source.sendFailure(Component.translatable("laborcraft.station.empty"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		LaborCraftWorker worker = LaborCraftWorker.find(source.getServer(), station.worker());
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		worker.setWorkerState(WorkerState.ACTIVE);
		OpenStationPayload.open(player, station);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.resumed", worker.getName()),
				false
		);
		return 1;
	}

	private static int stationZoneClear(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}
		if (!station.hasZone()) {
			source.sendFailure(Component.translatable("laborcraft.station.zone.none"));
			return 0;
		}

		StationRegistry.clearZone(source.getServer(), station);
		OpenStationPayload.open(player, stationAt(source, pos));
		source.sendSuccess(() -> Component.translatable("laborcraft.station.zone.cleared"), false);
		return 1;
	}

	private static int stationZoneSet(CommandSourceStack source, BlockPos pos, BlockPos from, BlockPos to)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		String error = StationRegistry.validateZone(source.getServer(), station, from, to);
		if (error != null) {
			source.sendFailure(Component.translatable("laborcraft.station.zone.invalid." + error));
			return 0;
		}

		int vertical = Math.max(1, LaborCraftConfig.get().area.verticalRadius);
		BlockPos zoneFrom = new BlockPos(from.getX(), station.pos().getY() - vertical, from.getZ());
		BlockPos zoneTo = new BlockPos(to.getX(), station.pos().getY() + vertical, to.getZ());
		StationRegistry.setZone(source.getServer(), station, zoneFrom, zoneTo);
		OpenStationPayload.open(player, stationAt(source, pos));
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.stake.applied",
						Math.abs(to.getX() - from.getX()) + 1,
						Math.abs(to.getZ() - from.getZ()) + 1
				),
				false
		);
		return 1;
	}

	private static int stationStorageLink(CommandSourceStack source, BlockPos pos, BlockPos chest, String roleId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		StationRegistry.StorageRole role = StationRegistry.StorageRole.parse(roleId);
		if (role == null) {
			source.sendFailure(Component.translatable("laborcraft.storage.invalid.role"));
			return 0;
		}
		if (chest.equals(station.pos())) {
			source.sendFailure(Component.translatable("laborcraft.storage.invalid.self"));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		int maxDistance = Math.max(1, config.storage.maxDistance);
		if (station.pos().distSqr(chest) > (double) maxDistance * maxDistance) {
			source.sendFailure(Component.translatable("laborcraft.storage.invalid.distance", maxDistance));
			return 0;
		}
		if (!(source.getLevel().getBlockEntity(chest) instanceof Container)) {
			source.sendFailure(Component.translatable("laborcraft.storage.invalid.container"));
			return 0;
		}

		boolean existing = station.storage().stream().anyMatch(link -> link.pos().equals(chest));
		int maxContainers = Math.max(1, config.storage.maxContainers);
		if (!existing && station.storage().size() >= maxContainers) {
			source.sendFailure(Component.translatable("laborcraft.storage.full", maxContainers));
			return 0;
		}

		List<StationRegistry.StorageLink> links = new ArrayList<>(station.storage());
		links.removeIf(link -> link.pos().equals(chest));
		links.add(new StationRegistry.StorageLink(chest, role));
		StationRegistry.Station updated = StationRegistry.setStorage(source.getServer(), station, links);
		OpenStationPayload.open(player, updated);
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.storage.linked",
						chest.getX(),
						chest.getY(),
						chest.getZ(),
						Component.translatable("laborcraft.storage.role." + role.id())
				),
				false
		);
		return 1;
	}

	private static int stationStorageUnlink(CommandSourceStack source, BlockPos pos, BlockPos chest)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		List<StationRegistry.StorageLink> links = new ArrayList<>(station.storage());
		if (!links.removeIf(link -> link.pos().equals(chest))) {
			source.sendFailure(Component.translatable("laborcraft.storage.not_linked"));
			return 0;
		}

		StationRegistry.Station updated = StationRegistry.setStorage(source.getServer(), station, links);
		OpenStationPayload.open(player, updated);
		source.sendSuccess(() -> Component.translatable("laborcraft.storage.unlinked"), false);
		return 1;
	}

	private static int stationStorageRole(CommandSourceStack source, BlockPos pos, BlockPos chest, String roleId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		StationRegistry.StorageRole role = StationRegistry.StorageRole.parse(roleId);
		if (role == null) {
			source.sendFailure(Component.translatable("laborcraft.storage.invalid.role"));
			return 0;
		}

		List<StationRegistry.StorageLink> links = new ArrayList<>(station.storage());
		boolean found = false;
		for (int index = 0; index < links.size(); index++) {
			if (links.get(index).pos().equals(chest)) {
				links.set(index, new StationRegistry.StorageLink(chest, role));
				found = true;
				break;
			}
		}
		if (!found) {
			source.sendFailure(Component.translatable("laborcraft.storage.not_linked"));
			return 0;
		}

		StationRegistry.Station updated = StationRegistry.setStorage(source.getServer(), station, links);
		OpenStationPayload.open(player, updated);
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.storage.role_set",
						Component.translatable("laborcraft.storage.role." + role.id())
				),
				false
		);
		return 1;
	}

	private static int stationStorageScan(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		int maxContainers = Math.max(1, config.storage.maxContainers);
		if (station.storage().size() >= maxContainers) {
			source.sendFailure(Component.translatable("laborcraft.storage.full", maxContainers));
			return 0;
		}

		int radius = Math.max(1, config.storage.maxDistance);
		BlockPos center = station.pos();
		ServerLevel level = source.getLevel();
		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos candidate = center.offset(dx, dy, dz);
					if (candidate.equals(center) || center.distSqr(candidate) > (double) radius * radius) {
						continue;
					}
					if (!(level.getBlockEntity(candidate) instanceof Container)) {
						continue;
					}
					BlockPos immutable = candidate.immutable();
					if (station.storage().stream().anyMatch(link -> link.pos().equals(immutable))) {
						continue;
					}
					candidates.add(immutable);
				}
			}
		}

		if (candidates.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.storage.scan.none", radius));
			return 0;
		}

		candidates.sort(Comparator.comparingDouble(candidate -> candidate.distSqr(center)));
		BlockPos best = candidates.getFirst();
		List<StationRegistry.StorageLink> links = new ArrayList<>(station.storage());
		links.add(new StationRegistry.StorageLink(best, StationRegistry.StorageRole.MIXED));
		StationRegistry.Station updated = StationRegistry.setStorage(source.getServer(), station, links);
		OpenStationPayload.open(player, updated);
		int distance = (int) Math.round(Math.sqrt(best.distSqr(center)));
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.storage.scan.linked",
						best.getX(),
						best.getY(),
						best.getZ(),
						distance
				),
				false
		);
		return 1;
	}

	private static int stationStorageList(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		if (station.storage().isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.storage.empty"));
			return 0;
		}

		for (StationRegistry.StorageLink link : station.storage()) {
			BlockPos chest = link.pos();
			int distance = (int) Math.round(Math.sqrt(chest.distSqr(station.pos())));
			source.sendSuccess(
					() -> Component.translatable(
							"laborcraft.storage.list.entry",
							Component.translatable("laborcraft.storage.role." + link.role().id()),
							chest.getX(),
							chest.getY(),
							chest.getZ(),
							distance
					),
					false
			);
		}
		return station.storage().size();
	}

	private static int stationOpen(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		if (!OpenStationPayload.open(player, station)) {
			source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
			return 0;
		}

		return 1;
	}

	private static int stationInfo(CommandSourceStack source, BlockPos pos) {
		StationRegistry.Station station = stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		LaborCraftWorker worker = station.worker() == null
				? null
				: LaborCraftWorker.find(source.getServer(), station.worker());
		Component workerLabel = worker == null
				? Component.translatable("laborcraft.station.vacant")
				: worker.getName();
		String zone = station.zones().isEmpty()
				? "-"
				: station.zones().getFirst().width() + "x" + station.zones().getFirst().depth();

		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.station.info",
						pos.getX(),
						pos.getY(),
						pos.getZ(),
						station.mode().name().toLowerCase(Locale.ROOT),
						zone,
						workerLabel
				),
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
		Component rankLabel = Component.translatable(worker.rank().translationKey());
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.info",
						worker.getName(),
						rankLabel,
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

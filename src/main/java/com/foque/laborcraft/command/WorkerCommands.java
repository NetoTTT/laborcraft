package com.foque.laborcraft.command;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.item.LaborCraftItems;
import com.foque.laborcraft.item.WorkerContractData;
import com.foque.laborcraft.job.JobEquipment;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.CloseScreenPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenWorkerInventoryPayload;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.worker.FarmerStorage;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerMarket;
import com.foque.laborcraft.worker.WorkerOffer;
import com.foque.laborcraft.worker.WorkerOwnership;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;

/** `/laborcraft worker ...`: contratação, atribuição, dispensa e mochila do trabalhador. */
final class WorkerCommands {
	private WorkerCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("worker")
				.then(Commands.literal("spawn")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.executes(context -> spawnWorker(context.getSource(), null))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(context -> spawnWorker(
										context.getSource(),
										StringArgumentType.getString(context, "name")
								))))
				.then(Commands.literal("info").executes(context -> workerInfo(context.getSource())))
				.then(Commands.literal("open")
						.then(Commands.argument("worker", StringArgumentType.word())
								.suggests(CommandSupport.ownedWorkerSuggestions())
								.executes(context -> openWorker(
										context.getSource(),
										StringArgumentType.getString(context, "worker")
								))))
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
								))))
				.then(Commands.literal("inv")
						.then(Commands.literal("open")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.executes(context -> openWorkerInventory(
												context.getSource(),
												StringArgumentType.getString(context, "worker")
										))))
						.then(Commands.literal("take")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.then(Commands.argument("slot", IntegerArgumentType.integer(0, LaborCraftWorker.SLOT_COUNT - 1))
												.executes(context -> workerInventoryTake(
														context.getSource(),
														StringArgumentType.getString(context, "worker"),
														IntegerArgumentType.getInteger(context, "slot")
												)))))
						.then(Commands.literal("give")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.then(Commands.argument("slot", IntegerArgumentType.integer(0, LaborCraftWorker.SLOT_COUNT - 1))
												.executes(context -> workerInventoryGive(
														context.getSource(),
														StringArgumentType.getString(context, "worker"),
														IntegerArgumentType.getInteger(context, "slot")
												)))))
						.then(Commands.literal("swap")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.then(Commands.argument("workerSlot", IntegerArgumentType.integer(0, LaborCraftWorker.SLOT_COUNT - 1))
												.then(Commands.argument("playerSlot", IntegerArgumentType.integer(0, 35))
														.executes(context -> workerInventorySwap(
																context.getSource(),
																StringArgumentType.getString(context, "worker"),
																IntegerArgumentType.getInteger(context, "workerSlot"),
																IntegerArgumentType.getInteger(context, "playerSlot")
														))))))
						.then(Commands.literal("deposit")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.then(Commands.argument("workerSlot", IntegerArgumentType.integer(0, LaborCraftWorker.SLOT_COUNT - 1))
												.then(Commands.argument("playerSlot", IntegerArgumentType.integer(0, 35))
														.then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
																.executes(context -> workerInventoryDeposit(
																		context.getSource(),
																		StringArgumentType.getString(context, "worker"),
																		IntegerArgumentType.getInteger(context, "workerSlot"),
																		IntegerArgumentType.getInteger(context, "playerSlot"),
																		IntegerArgumentType.getInteger(context, "amount")
																)))))))
						.then(Commands.literal("withdraw")
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
										.then(Commands.argument("workerSlot", IntegerArgumentType.integer(0, LaborCraftWorker.SLOT_COUNT - 1))
												.then(Commands.argument("playerSlot", IntegerArgumentType.integer(0, 35))
														.then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
																.executes(context -> workerInventoryWithdraw(
																		context.getSource(),
																		StringArgumentType.getString(context, "worker"),
																		IntegerArgumentType.getInteger(context, "workerSlot"),
																		IntegerArgumentType.getInteger(context, "playerSlot"),
																		IntegerArgumentType.getInteger(context, "amount")
																))))))))
				.then(Commands.literal("storage")
						.then(Commands.literal("deposit")
								.then(Commands.argument("worker", StringArgumentType.greedyString())
										.suggests(CommandSupport.ownedWorkerNameSuggestions())
										.executes(context -> workerStorageDeposit(
												context.getSource(),
												StringArgumentType.getString(context, "worker")
										))))
						.then(Commands.literal("withdraw")
								.then(Commands.argument("worker", StringArgumentType.greedyString())
										.suggests(CommandSupport.ownedWorkerNameSuggestions())
										.executes(context -> workerStorageWithdraw(
												context.getSource(),
												StringArgumentType.getString(context, "worker")
										)))));
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
		int limit = WorkerOwnership.limit(player);
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
		LaborCraftWorker worker = CommandSupport.findWorker(source, workerId);
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
		LaborCraftWorker worker = CommandSupport.findWorker(source, workerId);
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

		HomeRegistry.unassign(player.level().getServer(), worker.getUUID());
		worker.discard();
		boolean stored = hasRoom && player.getInventory().add(contract);

		if (!stored) {
			if (player.level() instanceof ServerLevel serverLevel) {
				player.spawnAtLocation(serverLevel, fallback);
			}

			source.sendSystemMessage(Component.translatable("laborcraft.command.worker.dismiss.dropped"));
		}

		// Nada de reabrir a loja: a dispensa so fecha a tela de Empregado.
		CloseScreenPayload.open(player);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.dismiss.contract", workerName),
				false
		);
		return 1;
	}

	private static int workerInfo(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		LaborCraftWorker worker = CommandSupport.nearestWorker(source, true);
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
		LaborCraftWorker worker = CommandSupport.nearestWorker(source, false);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.none"));
			return 0;
		}

		Component name = worker.getName();
		HomeRegistry.unassign(source.getServer(), worker.getUUID());
		worker.discard();
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.removed", name),
				false
		);
		return 1;
	}

	private static int openWorker(CommandSourceStack source, String rawId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (!OpenWorkerPayload.open(player, worker)) {
			source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
			return 0;
		}
		return 1;
	}

	private static int openWorkerInventory(CommandSourceStack source, String rawId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (!OpenWorkerInventoryPayload.open(player, worker)) {
			source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
			return 0;
		}
		return 1;
	}

	private static int workerInventorySwap(CommandSourceStack source, String rawId, int workerSlot, int playerSlot)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (workerSlot < 0 || workerSlot >= LaborCraftWorker.SLOT_COUNT || playerSlot < 0 || playerSlot >= 36) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.invalid_slot"));
			return 0;
		}

		ItemStack workerStack = worker.inventoryItem(workerSlot);
		ItemStack playerStack = player.getInventory().getItem(playerSlot);
		if (workerSlot == LaborCraftWorker.TOOL_SLOT
				&& !playerStack.isEmpty()
				&& !JobEquipment.validTool(worker.jobId(), playerStack)) {
			source.sendFailure(Component.translatable(
					"laborcraft.command.worker.inv.tool.invalid",
					JobEquipment.toolName(worker.jobId())
			));
			return 0;
		}
		if (workerStack.isEmpty() && playerStack.isEmpty()) {
			return 0;
		}

		worker.setInventoryItem(workerSlot, playerStack.copy());
		player.getInventory().setItem(playerSlot, workerStack.copy());
		OpenWorkerInventoryPayload.open(player, worker);
		return 1;
	}

	private static int workerInventoryDeposit(
			CommandSourceStack source,
			String rawId,
			int workerSlot,
			int playerSlot,
			int amount
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (workerSlot < 0 || workerSlot >= LaborCraftWorker.SLOT_COUNT || playerSlot < 0 || playerSlot >= 36) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.invalid_slot"));
			return 0;
		}

		ItemStack from = player.getInventory().getItem(playerSlot);
		if (from.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_empty"));
			return 0;
		}
		if (workerSlot == LaborCraftWorker.TOOL_SLOT && !JobEquipment.validTool(worker.jobId(), from)) {
			source.sendFailure(Component.translatable(
					"laborcraft.command.worker.inv.tool.invalid",
					JobEquipment.toolName(worker.jobId())
			));
			return 0;
		}

		ItemStack target = worker.inventoryItem(workerSlot);
		if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, from)) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_occupied"));
			return 0;
		}

		int room = target.isEmpty()
				? from.getMaxStackSize()
				: target.getMaxStackSize() - target.getCount();
		int moved = Math.min(amount, Math.min(from.getCount(), room));
		if (moved <= 0) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_full"));
			return 0;
		}

		if (target.isEmpty()) {
			worker.setInventoryItem(workerSlot, from.copyWithCount(moved));
		} else {
			target.grow(moved);
		}
		from.shrink(moved);
		OpenWorkerInventoryPayload.open(player, worker);
		return 1;
	}

	private static int workerInventoryWithdraw(
			CommandSourceStack source,
			String rawId,
			int workerSlot,
			int playerSlot,
			int amount
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (workerSlot < 0 || workerSlot >= LaborCraftWorker.SLOT_COUNT || playerSlot < 0 || playerSlot >= 36) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.invalid_slot"));
			return 0;
		}

		ItemStack from = worker.inventoryItem(workerSlot);
		if (from.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_empty"));
			return 0;
		}

		ItemStack target = player.getInventory().getItem(playerSlot);
		if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, from)) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_occupied"));
			return 0;
		}

		int room = target.isEmpty()
				? from.getMaxStackSize()
				: target.getMaxStackSize() - target.getCount();
		int moved = Math.min(amount, Math.min(from.getCount(), room));
		if (moved <= 0) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_full"));
			return 0;
		}

		if (target.isEmpty()) {
			player.getInventory().setItem(playerSlot, from.copyWithCount(moved));
		} else {
			target.grow(moved);
		}
		from.shrink(moved);
		OpenWorkerInventoryPayload.open(player, worker);
		return 1;
	}

	private static int workerStorageDeposit(CommandSourceStack source, String rawId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		StationRegistry.Station station = StationRegistry.byWorker(source.getServer(), worker.getUUID());
		if (station == null || !(worker.level() instanceof ServerLevel level)) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.storage.no_station"));
			return 0;
		}

		int moved = FarmerStorage.deposit(level, station, worker);
		OpenWorkerInventoryPayload.open(player, worker);
		if (moved <= 0) {
			source.sendSuccess(
					() -> Component.translatable("laborcraft.command.worker.storage.nothing"),
					false
			);
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.storage.deposited", moved),
				false
		);
		return 1;
	}

	private static int workerStorageWithdraw(CommandSourceStack source, String rawId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		StationRegistry.Station station = StationRegistry.byWorker(source.getServer(), worker.getUUID());
		if (station == null || !(worker.level() instanceof ServerLevel level)) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.storage.no_station"));
			return 0;
		}

		int moved = FarmerStorage.withdraw(
				level,
				station,
				worker,
				LaborCraftConfig.get().farmer.useBoneMeal
		);
		OpenWorkerInventoryPayload.open(player, worker);
		if (moved <= 0) {
			source.sendSuccess(
					() -> Component.translatable("laborcraft.command.worker.storage.nothing"),
					false
			);
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.storage.withdrawn", moved),
				false
		);
		return 1;
	}

	private static int workerInventoryTake(CommandSourceStack source, String rawId, int slot)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (slot < 0 || slot >= LaborCraftWorker.SLOT_COUNT) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.invalid_slot"));
			return 0;
		}

		ItemStack stack = worker.inventoryItem(slot);
		if (stack.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_empty"));
			return 0;
		}

		ItemStack moving = stack.copy();
		int before = moving.getCount();
		worker.setInventoryItem(slot, ItemStack.EMPTY);
		player.getInventory().add(moving);
		if (!moving.isEmpty()) {
			worker.setInventoryItem(slot, moving);
		}

		OpenWorkerInventoryPayload.open(player, worker);
		if (moving.getCount() == before) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.full"));
			return 0;
		}

		int moved = before - moving.getCount();
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.worker.inv.taken",
						stack.getHoverName(),
						moved
				),
				false
		);
		return 1;
	}

	private static int workerInventoryGive(CommandSourceStack source, String rawId, int slot)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LaborCraftWorker worker = CommandSupport.findWorker(source, rawId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}
		if (!worker.isOwnedBy(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_owner"));
			return 0;
		}
		if (slot < 0 || slot >= LaborCraftWorker.SLOT_COUNT) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.invalid_slot"));
			return 0;
		}

		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.hand_empty"));
			return 0;
		}
		if (slot == LaborCraftWorker.TOOL_SLOT && !JobEquipment.validTool(worker.jobId(), held)) {
			source.sendFailure(Component.translatable(
					"laborcraft.command.worker.inv.tool.invalid",
					JobEquipment.toolName(worker.jobId())
			));
			return 0;
		}

		ItemStack target = worker.inventoryItem(slot);
		if (target.isEmpty()) {
			worker.setInventoryItem(slot, held.copy());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		} else if (ItemStack.isSameItemSameComponents(target, held)) {
			int room = target.getMaxStackSize() - target.getCount();
			if (room <= 0) {
				source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_full"));
				return 0;
			}
			int moved = Math.min(room, held.getCount());
			target.grow(moved);
			held.shrink(moved);
		} else {
			source.sendFailure(Component.translatable("laborcraft.command.worker.inv.slot_occupied"));
			return 0;
		}

		OpenWorkerInventoryPayload.open(player, worker);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.worker.inv.given", worker.getName()),
				false
		);
		return 1;
	}

	private static String normalizeJobId(Identifier jobArgument) {
		return jobArgument.getNamespace().equals("minecraft")
				? LaborCraft.id(jobArgument.getPath()).toString()
				: jobArgument.toString();
	}
}

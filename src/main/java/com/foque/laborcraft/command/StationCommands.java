package com.foque.laborcraft.command;

import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureScanner;
import com.foque.laborcraft.area.EnclosureScan;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.network.OpenStationPayload;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerOwnership;
import com.foque.laborcraft.worker.WorkerState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

/** `/laborcraft station ...`: posto, vínculo, zona e armazenamento. */
final class StationCommands {
	private StationCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("station")
				.then(Commands.literal("bind")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("worker", StringArgumentType.word())
										.suggests(CommandSupport.ownedWorkerSuggestions())
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
				.then(Commands.literal("enclosure")
						.then(Commands.literal("set")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.then(Commands.argument("seed", BlockPosArgument.blockPos())
												.executes(context -> stationEnclosureSet(
														context.getSource(),
														BlockPosArgument.getLoadedBlockPos(context, "pos"),
														BlockPosArgument.getLoadedBlockPos(context, "seed")
												)))))
						.then(Commands.literal("clear")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> stationEnclosureClear(
												context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos")
										)))))				.then(Commands.literal("maxarea")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> stationZoneMax(
										context.getSource(),
										BlockPosArgument.getLoadedBlockPos(context, "pos")
								))))
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
										)))));
	}

	private static int stationBind(CommandSourceStack source, BlockPos pos, String rawWorker)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}

		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		UUID workerId = CommandSupport.parseUuid(rawWorker);
		LaborCraftWorker worker = workerId == null ? null : CommandSupport.ownedWorker(player, workerId);
		if (worker == null) {
			source.sendFailure(Component.translatable("laborcraft.command.worker.not_found"));
			return 0;
		}

		if (station.worker() != null && LaborCraftWorker.find(source.getServer(), station.worker()) != null) {
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

		OpenStationPayload.open(player, CommandSupport.stationAt(source, pos));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.bound", worker.getName()),
				false
		);
		return 1;
	}

	private static int stationUnbind(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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

		OpenStationPayload.open(player, CommandSupport.stationAt(source, pos));
		source.sendSuccess(() -> Component.translatable("laborcraft.station.unbound"), false);
		return 1;
	}

	private static int stationRecall(CommandSourceStack source, BlockPos pos, boolean walk)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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

		worker.setWorkNotice("");
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		OpenStationPayload.open(player, CommandSupport.stationAt(source, pos));
		source.sendSuccess(() -> Component.translatable("laborcraft.station.zone.cleared"), false);
		return 1;
	}

	private static int stationZoneMax(CommandSourceStack source, BlockPos pos)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		int result = StationRegistry.applyMaxZone(source.getServer(), station);
		if (result == -1) {
			source.sendFailure(Component.translatable("laborcraft.station.zone.max.blocked"));
			return 0;
		}
		if (result == -2) {
			source.sendFailure(Component.translatable("laborcraft.station.zone.max.too_complex"));
			return 0;
		}

		OpenStationPayload.open(player, CommandSupport.stationAt(source, pos));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.zone.max.applied", result),
				false
		);
		return 1;
	}

	private static int stationEnclosureSet(
			CommandSourceStack source, BlockPos pos, BlockPos seed
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		EnclosureScan scan = EnclosureScanner.scan(source.getLevel(), seed, LaborCraftConfig.get());
		if (!scan.valid()) {
			source.sendFailure(Component.translatable(
					"laborcraft.station.enclosure.invalid",
					Component.translatable("laborcraft.pen.reason." + scan.failureReason())
			));
			return 0;
		}

		StationRegistry.setEnclosure(
				source.getServer(),
				station,
				EnclosureDescriptor.from(scan, station.dimension())
		);
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.station.enclosure.set",
						scan.area(),
						scan.gatePositions().size()
				),
				true
		);
		return 1;
	}

	private static int stationEnclosureClear(
			CommandSourceStack source, BlockPos pos
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		StationRegistry.clearEnclosure(source.getServer(), station);
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.enclosure.cleared"),
				true
		);
		return 1;
	}
	private static int stationZoneSet(CommandSourceStack source, BlockPos pos, BlockPos from, BlockPos to)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
		if (station == null) {
			source.sendFailure(Component.translatable("laborcraft.station.not_found"));
			return 0;
		}
		if (!station.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.station.not_owner"));
			return 0;
		}

		int result = StationRegistry.applyZone(source.getServer(), station, from, to);
		if (result < 0) {
			source.sendFailure(Component.translatable(zoneClipErrorKey(result)));
			return 0;
		}

		OpenStationPayload.open(player, CommandSupport.stationAt(source, pos));
		source.sendSuccess(
				() -> Component.translatable("laborcraft.station.zone.applied", result),
				false
		);
		return 1;
	}

	private static String zoneClipErrorKey(int result) {
		return switch (result) {
			case -2 -> "laborcraft.station.zone.clip.too_complex";
			case -3 -> "laborcraft.station.zone.invalid.too_large";
			case -4 -> "laborcraft.station.zone.invalid.too_far";
			default -> "laborcraft.station.zone.clip.blocked";
		};
	}

	private static int stationStorageLink(CommandSourceStack source, BlockPos pos, BlockPos chest, String roleId)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
		StationRegistry.Station station = CommandSupport.stationAt(source, pos);
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
}

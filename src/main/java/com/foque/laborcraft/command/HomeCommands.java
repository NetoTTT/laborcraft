package com.foque.laborcraft.command;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeActions;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.housing.HousingMessages;
import com.foque.laborcraft.housing.HousingReport;
import com.foque.laborcraft.housing.HousingValidator;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * `/laborcraft home ...`: house diagnostics (gamemaster), claim/assignment for players and
 * house-driven worker limit.
 */
final class HomeCommands {
	private HomeCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("home")
				.then(Commands.literal("set")
						.executes(context -> setHome(context.getSource(), null, null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> setHome(
										context.getSource(),
										BlockPosArgument.getLoadedBlockPos(context, "pos"),
										null
								))
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(context -> setHome(
												context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos"),
												StringArgumentType.getString(context, "name")
										)))))
				.then(Commands.literal("remove")
						.executes(context -> removeHome(context.getSource(), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> removeHome(
										context.getSource(),
										BlockPosArgument.getBlockPos(context, "pos")
								))))
				.then(Commands.literal("rename")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(context -> renameHome(
												context.getSource(),
												BlockPosArgument.getBlockPos(context, "pos"),
												StringArgumentType.getString(context, "name")
										))))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(context -> renameHome(
										context.getSource(),
										null,
										StringArgumentType.getString(context, "name")
								))))
				.then(Commands.literal("assign")
						.then(Commands.argument("worker", StringArgumentType.word())
								.suggests(CommandSupport.ownedWorkerSuggestions())
								.executes(context -> assignHome(
										context.getSource(),
										StringArgumentType.getString(context, "worker"),
										null
								))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> assignHome(
												context.getSource(),
												StringArgumentType.getString(context, "worker"),
												BlockPosArgument.getBlockPos(context, "pos")
										)))))
				.then(Commands.literal("unassign")
						.then(Commands.argument("worker", StringArgumentType.word())
								.suggests(CommandSupport.ownedWorkerSuggestions())
								.executes(context -> unassignHome(
										context.getSource(),
										StringArgumentType.getString(context, "worker")
								))))
				.then(Commands.literal("list")
						.executes(context -> listHomes(context.getSource())))
				.then(Commands.literal("info")
						.executes(context -> infoHome(context.getSource(), null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> infoHome(
										context.getSource(),
										BlockPosArgument.getLoadedBlockPos(context, "pos")
								))))
				.then(Commands.literal("scan")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> scanHome(
										context.getSource(),
										BlockPosArgument.getLoadedBlockPos(context, "pos")
								))))
				.then(Commands.literal("measure")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(context -> measureHome(
										context.getSource(),
										BlockPosArgument.getLoadedBlockPos(context, "pos")
								))))
				.then(Commands.literal("revalidate")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.executes(context -> revalidateHomes(context.getSource())));
	}

	private static int setHome(CommandSourceStack source, BlockPos pos, String name)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BlockPos target = pos == null ? lookedAt(player) : pos;
		if (target == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.look_at_bed"));
			return 0;
		}

		boolean created = HomeActions.claim(player, source.getLevel(), target, name);
		return created ? 1 : 0;
	}

	private static int removeHome(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BlockPos target = pos == null ? lookedAt(player) : pos;
		if (target == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.look_at_bed"));
			return 0;
		}

		HomeRegistry.Home home = homeAt(source, target, false);
		if (home == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_claimed"));
			return 0;
		}
		if (!home.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_owner"));
			return 0;
		}

		int slots = home.slots();
		if (!HomeRegistry.remove(source.getServer(), home)) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_claimed"));
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.home.removed",
						home.name(),
						home.bed().getX(),
						home.bed().getY(),
						home.bed().getZ(),
						slots
				),
				false
		);
		return 1;
	}

	private static int renameHome(CommandSourceStack source, BlockPos pos, String name)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BlockPos target = pos == null ? lookedAt(player) : pos;
		if (target == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.look_at_bed"));
			return 0;
		}

		HomeRegistry.Home home = homeAt(source, target, false);
		if (home == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_claimed"));
			return 0;
		}
		if (!home.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_owner"));
			return 0;
		}

		HomeRegistry.Home renamed = HomeRegistry.rename(source.getServer(), home, name);
		if (renamed == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_claimed"));
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.home.renamed", renamed.name()),
				false
		);
		return 1;
	}

	private static int assignHome(CommandSourceStack source, String workerId, BlockPos pos)
			throws CommandSyntaxException {
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

		HomeRegistry.Home home;
		BlockPos bed = pos;
		if (pos != null) {
			home = HomeRegistry.findAt(
					source.getServer(),
					source.getLevel().dimension().identifier().toString(),
					pos
			);
			if (home == null) {
				source.sendFailure(Component.translatable("laborcraft.command.home.assign.not_claimed"));
				return 0;
			}
			if (!home.isOwner(player.getUUID())) {
				source.sendFailure(Component.translatable("laborcraft.command.home.not_owner"));
				return 0;
			}
		} else {
			home = nearestFreeHome(source.getServer(), player, worker);
			if (home == null) {
				source.sendFailure(Component.translatable("laborcraft.command.home.assign.no_home"));
				return 0;
			}
			bed = home.firstFreeBed();
		}

		HomeRegistry.AssignResult result = HomeRegistry.assign(
				source.getServer(),
				home,
				worker.getUUID(),
				bed
		);
		if (!result.ok()) {
			String failure = result.failure() == null ? "no_home" : result.failure();
			Component reason = Component.translatable(
					"laborcraft.command.home.assign.reason." + failure,
					worker.getName()
			);
			source.sendFailure(Component.translatable("laborcraft.command.home.assign.failed", reason));
			return 0;
		}

		Component workerName = worker.getName();
		Component homeName = Component.literal(result.home().name());
		BlockPos assigned = result.bed();
		if (result.moved()) {
			source.sendSuccess(
					() -> Component.translatable(
							"laborcraft.command.home.assign.moved",
							workerName,
							homeName,
							assigned.getX(),
							assigned.getY(),
							assigned.getZ()
					),
					false
			);
		} else {
			source.sendSuccess(
					() -> Component.translatable(
							"laborcraft.command.home.assign.success",
							workerName,
							homeName,
							assigned.getX(),
							assigned.getY(),
							assigned.getZ()
					),
					false
			);
		}

		if (worker.isAttending()) {
			OpenWorkerPayload.open(player, worker);
		}
		return 1;
	}

	private static int unassignHome(CommandSourceStack source, String workerId)
			throws CommandSyntaxException {
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

		HomeRegistry.Home before = HomeRegistry.byWorker(source.getServer(), worker.getUUID());
		BlockPos bed = before != null && before.resident(worker.getUUID()) != null
				? before.resident(worker.getUUID()).bed()
				: null;
		HomeRegistry.Home previous = HomeRegistry.unassign(source.getServer(), worker.getUUID());
		if (previous == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.unassign.none", worker.getName()));
			return 0;
		}

		BlockPos reported = bed == null ? previous.bed() : bed;
		Component homeName = Component.literal(previous.name());
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.home.unassign.done",
						worker.getName(),
						homeName,
						reported.getX(),
						reported.getY(),
						reported.getZ()
				),
				false
		);

		if (worker.isAttending()) {
			OpenWorkerPayload.open(player, worker);
		}
		return 1;
	}

	private static int listHomes(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		List<HomeRegistry.Home> homes = HomeRegistry.byOwner(source.getServer(), player.getUUID());
		int homeSlots = HomeRegistry.slots(source.getServer(), player.getUUID());
		int base = LaborCraftConfig.get().worker.maxWorkersPerPlayer;
		int limit = base <= 0 ? 0 : base + homeSlots;

		if (homes.isEmpty()) {
			source.sendFailure(Component.translatable("laborcraft.command.home.list.none"));
		} else {
			source.sendSuccess(
					() -> Component.translatable("laborcraft.command.home.list.header", homes.size()),
					false
			);
			for (HomeRegistry.Home home : homes) {
				Component suffix = home.valid()
						? Component.empty()
						: Component.translatable(
								"laborcraft.command.home.list.entry.invalid",
								Component.translatable(
										"laborcraft.housing.reason."
												+ (home.failure().isBlank() ? "invalid" : home.failure())
								)
						);
				source.sendSuccess(
						() -> Component.translatable(
								"laborcraft.command.home.list.entry",
								home.name(),
								home.bed().getX(),
								home.bed().getY(),
								home.bed().getZ(),
								home.residents().size(),
								home.slots(),
								suffix
						),
						false
				);
			}
		}

		if (base <= 0) {
			source.sendSuccess(
					() -> Component.translatable("laborcraft.command.home.list.unlimited"),
					false
			);
		} else {
			source.sendSuccess(
					() -> Component.translatable(
							"laborcraft.command.home.list.limit",
							limit,
							base,
							homeSlots
					),
					false
			);
		}
		return homes.size();
	}

	private static int infoHome(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BlockPos target = pos == null ? lookedAt(player) : pos;
		if (target == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.look_at_bed"));
			return 0;
		}

		HomeRegistry.Home home = homeAt(source, target, true);
		if (home == null) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_claimed"));
			return 0;
		}
		if (!home.isOwner(player.getUUID())) {
			source.sendFailure(Component.translatable("laborcraft.command.home.not_owner"));
			return 0;
		}

		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.home.info.header",
						home.name(),
						home.bed().getX(),
						home.bed().getY(),
						home.bed().getZ(),
						home.residents().size(),
						home.slots()
				),
				false
		);
		source.sendSuccess(
				() -> Component.translatable(
						"laborcraft.command.home.info.residents",
						residentNames(source, home)
				),
				false
		);
		HousingReport report = HousingValidator.validate(source.getLevel(), home.bed(), LaborCraftConfig.get());
		for (Component line : HousingMessages.describe(home.bed(), report)) {
			source.sendSuccess(() -> line, false);
		}
		return 1;
	}

	private static Component residentNames(CommandSourceStack source, HomeRegistry.Home home) {
		if (home.residents().isEmpty()) {
			return Component.translatable("laborcraft.command.home.info.none");
		}

		List<Component> names = new ArrayList<>(home.residents().size());
		for (HomeRegistry.Home.Resident resident : home.residents()) {
			LaborCraftWorker worker = LaborCraftWorker.find(source.getServer(), resident.worker());
			names.add(worker == null
					? Component.literal(resident.worker().toString().substring(0, 8))
					: worker.getName());
		}
		return Component.literal(names.stream()
				.map(Component::getString)
				.reduce((left, right) -> left + ", " + right)
				.orElse(""));
	}

	private static HomeRegistry.Home nearestFreeHome(
			net.minecraft.server.MinecraftServer server,
			ServerPlayer player,
			LaborCraftWorker worker
	) {
		String workerDimension = worker.level().dimension().identifier().toString();
		List<HomeRegistry.Home> candidates = HomeRegistry.byOwner(server, player.getUUID()).stream()
				.filter(HomeRegistry.Home::valid)
				.filter(home -> home.firstFreeBed() != null)
				.filter(home -> home.dimension().equals(workerDimension))
				.toList();

		HomeRegistry.Home best = null;
		double bestDistance = Double.MAX_VALUE;
		for (HomeRegistry.Home candidate : candidates) {
			BlockPos bed = candidate.firstFreeBed();
			double distance = worker.position().distanceToSqr(Vec3.atCenterOf(bed));
			if (best == null || distance < bestDistance) {
				best = candidate;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static HomeRegistry.Home homeAt(CommandSourceStack source, BlockPos pos, boolean requireBed) {
		String dimension = source.getLevel().dimension().identifier().toString();
		return requireBed
				? HomeRegistry.get(source.getServer(), dimension, pos)
				: HomeRegistry.findAt(source.getServer(), dimension, pos);
	}

	private static BlockPos lookedAt(ServerPlayer player) {
		HitResult hit = player.pick(5.0D, 0.0F, false);
		return hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit
				? blockHit.getBlockPos()
				: null;
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

	private static int revalidateHomes(CommandSourceStack source) {
		int changed = HomeRegistry.revalidateAll(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.home.revalidate.done", changed),
				false
		);
		return changed;
	}

	private static void sendHousingReport(CommandSourceStack source, BlockPos pos, HousingReport report) {
		for (Component line : HousingMessages.describe(pos, report)) {
			source.sendSuccess(() -> line, false);
		}
	}
}

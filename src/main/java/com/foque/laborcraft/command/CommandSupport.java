package com.foque.laborcraft.command;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.worker.StationRegistry;
import com.foque.laborcraft.worker.WorkerOwnership;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Helpers shared by the command modules (worker/station lookup, suggestions). */
final class CommandSupport {
	private CommandSupport() {
	}

	static StationRegistry.Station stationAt(CommandSourceStack source, BlockPos pos) {
		return StationRegistry.get(
				source.getServer(),
				source.getLevel().dimension().identifier().toString(),
				pos
		);
	}

	static LaborCraftWorker ownedWorker(ServerPlayer player, UUID workerId) {
		return WorkerOwnership.ownedWorkerList(player).stream()
				.filter(worker -> worker.getUUID().equals(workerId))
				.findFirst()
				.orElse(null);
	}

	static UUID parseUuid(String raw) {
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	static LaborCraftWorker findWorker(CommandSourceStack source, String rawId) throws CommandSyntaxException {
		UUID workerId = parseUuid(rawId);
		if (workerId == null) {
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

	static LaborCraftWorker nearestWorker(CommandSourceStack source, boolean ownedOnly) throws CommandSyntaxException {
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

	static SuggestionProvider<CommandSourceStack> ownedWorkerSuggestions() {
		return (context, builder) -> {
			ServerPlayer player = context.getSource().getPlayer();
			if (player != null) {
				WorkerOwnership.ownedWorkerList(player)
						.forEach(worker -> builder.suggest(worker.getUUID().toString()));
			}
			return builder.buildFuture();
		};
	}
}

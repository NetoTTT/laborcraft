package com.foque.laborcraft.command;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.housing.HousingMessages;
import com.foque.laborcraft.housing.HousingReport;
import com.foque.laborcraft.housing.HousingValidator;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/** `/laborcraft home ...`: relatório de casa (scan/measure), só para gamemaster/console. */
final class HomeCommands {
	private HomeCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("home")
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
								))));
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
}

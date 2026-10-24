package com.foque.laborcraft.command;

import com.foque.laborcraft.network.OpenAdminPayload;
import com.foque.laborcraft.network.OpenConfigPayload;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.foque.laborcraft.network.OpenItemTestPayload;
import com.foque.laborcraft.network.OpenPlayersPayload;
import com.foque.laborcraft.network.OpenProfilePayload;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Root-level screens: ui, config, test items, players, profile and hire. */
final class UiCommands {
	private UiCommands() {
	}

	static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("ui").executes(context -> openUi(context.getSource())))
				.then(Commands.literal("config").executes(context -> openConfig(context.getSource())))
				.then(Commands.literal("test")
						.then(Commands.literal("items").executes(context -> openItemTest(context.getSource()))))
				.then(Commands.literal("players").executes(context -> openPlayers(context.getSource())))
				.then(Commands.literal("profile").executes(context -> openProfile(context.getSource())))
				.then(Commands.literal("hire").executes(context -> openHiring(context.getSource())));
	}

	private static int openUi(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenAdminPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenAdminPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openConfig(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenConfigPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenConfigPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openItemTest(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenItemTestPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenItemTestPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openPlayers(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (ServerPlayNetworking.canSend(player, OpenPlayersPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenPlayersPayload());
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openProfile(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (OpenProfilePayload.open(player)) {
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}

	private static int openHiring(CommandSourceStack source)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (OpenHiringPayload.open(player)) {
			return 1;
		}

		source.sendFailure(Component.translatable("laborcraft.command.ui.unavailable"));
		return 0;
	}
}

package com.foque.laborcraft.command;

import com.foque.laborcraft.network.SetLanguagePayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** `/laborcraft language <pt_br|en_us>`. */
final class LanguageCommands {
	private LanguageCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("language")
				.then(Commands.argument("code", StringArgumentType.word())
						.suggests((context, builder) -> {
							builder.suggest("pt_br");
							builder.suggest("en_us");
							return builder.buildFuture();
						})
						.executes(context -> setLanguage(
								context.getSource(),
								StringArgumentType.getString(context, "code")
						)));
	}

	private static int setLanguage(CommandSourceStack source, String code)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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
}

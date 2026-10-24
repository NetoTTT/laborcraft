package com.foque.laborcraft.command;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.economy.CurrencyProvider;
import com.foque.laborcraft.economy.CurrencyProviders;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.worker.WorkerMarket;
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
										)))));
	}

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

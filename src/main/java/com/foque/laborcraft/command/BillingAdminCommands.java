package com.foque.laborcraft.command;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.config.LaborCraftConfig.BillingSettings;
import com.foque.laborcraft.contract.BillingManager;
import com.foque.laborcraft.worker.WorkerMarket;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/** `/laborcraft billing ...`: configuração live do salário e da cobrança. */
final class BillingAdminCommands {
	private static final List<String> FIELDS = List.of(
			"enabled",
			"check-ticks",
			"use-schedule",
			"pause-off-duty",
			"notify",
			"debt",
			"grace",
			"dismiss-after",
			"mood-penalty",
			"use-table",
			"minimum-daily",
			"use-rank",
			"use-rarity",
			"use-boost",
			"boost-cap",
			"base-wage",
			"price",
			"period-days"
	);
	private static final Set<String> OFFER_FIELDS = Set.of(
			"use-table",
			"minimum-daily",
			"use-rank",
			"use-rarity",
			"use-boost",
			"boost-cap",
			"base-wage",
			"price",
			"period-days"
	);

	private BillingAdminCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("billing")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("set")
						.then(Commands.argument("field", com.mojang.brigadier.arguments.StringArgumentType.word())
								.suggests((context, builder) -> {
									FIELDS.forEach(builder::suggest);
									return builder.buildFuture();
								})
								.then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.word())
										.executes(context -> setField(
												context,
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "field"),
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "value")
										)))))
				.then(Commands.literal("rank")
						.then(Commands.argument("rank", com.mojang.brigadier.arguments.StringArgumentType.word())
								.suggests((context, builder) -> {
									for (WorkerRank rank : WorkerRank.values()) {
										builder.suggest(rank.id());
									}
									return builder.buildFuture();
								})
								.then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01D, 100.0D))
										.executes(context -> setRank(
												context.getSource(),
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rank"),
												DoubleArgumentType.getDouble(context, "value")
										)))))
				.then(Commands.literal("rarity")
						.then(Commands.argument("rarity", com.mojang.brigadier.arguments.StringArgumentType.word())
								.suggests((context, builder) -> {
									for (WorkerRarity rarity : WorkerRarity.values()) {
										builder.suggest(rarity.id());
									}
									return builder.buildFuture();
								})
								.then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01D, 100.0D))
										.executes(context -> setRarity(
												context.getSource(),
												com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rarity"),
												DoubleArgumentType.getDouble(context, "value")
										)))))
				.then(Commands.literal("force")
						.executes(context -> force(context.getSource())))
				.then(Commands.literal("apply-salary")
						.executes(context -> applySalary(context.getSource())));
	}

	private static int setField(CommandContext<CommandSourceStack> context, String field, String value) {
		CommandSourceStack source = context.getSource();
		LaborCraftConfig config = LaborCraftConfig.get();
		BillingSettings billing = config.billing;
		String key = field.toLowerCase(Locale.ROOT);

		try {
			switch (key) {
				case "enabled" -> billing.enabled = parseBoolean(value);
				case "check-ticks" -> billing.checkTicks = clamp(parseInt(value), 20, 24_000);
				case "use-schedule" -> billing.useSchedule = parseBoolean(value);
				case "pause-off-duty" -> config.schedule.pauseBillingOffDuty = parseBoolean(value);
				case "notify" -> billing.notifyOwner = parseBoolean(value);
				case "debt" -> billing.allowDebt = parseBoolean(value);
				case "grace" -> billing.gracePeriods = clamp(parseInt(value), 0, 1000);
				case "dismiss-after" -> billing.dismissAfterMissed = clamp(parseInt(value), 0, 1000);
				case "mood-penalty" -> billing.missedMoodPenalty = clamp(parseInt(value), 0, 100);
				case "use-table" -> billing.useSalaryTable = parseBoolean(value);
				case "minimum-daily" -> billing.minimumDaily = clamp(parseInt(value), 0, 1_000_000);
				case "use-rank" -> billing.useRankMultiplier = parseBoolean(value);
				case "use-rarity" -> billing.useRarityMultiplier = parseBoolean(value);
				case "use-boost" -> billing.useBoostUpkeep = parseBoolean(value);
				case "boost-cap" -> billing.boostCap = clamp(parseDouble(value), 0.1D, 100.0D);
				case "base-wage" -> config.farmer.baseWage = clamp(parseInt(value), 0, 1_000_000);
				case "price" -> config.acquisition.price = clamp(parseInt(value), 0, 1_000_000);
				case "period-days" -> config.farmer.billingPeriodDays = clamp(parseInt(value), 1, 365);
				default -> {
					source.sendFailure(Component.translatable("laborcraft.command.billing.unknown_field", field));
					return 0;
				}
			}
		} catch (IllegalArgumentException exception) {
			source.sendFailure(Component.translatable("laborcraft.command.billing.invalid_value", field, value));
			return 0;
		}

		config.save();
		if (OFFER_FIELDS.contains(key)) {
			WorkerMarket.refresh(source.getServer());
		}
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.billing.set", field, value),
				true
		);
		return 1;
	}

	private static int setRank(CommandSourceStack source, String rawRank, double value) {
		WorkerRank rank = findByRank(rawRank);
		if (rank == null) {
			source.sendFailure(Component.translatable("laborcraft.command.billing.unknown_rank", rawRank));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		ensureRanks(config.billing);
		config.billing.rankMultipliers[rank.ordinal()] = value;
		config.save();
		WorkerMarket.refresh(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.billing.rank", rank.id(), value),
				true
		);
		return 1;
	}

	private static int setRarity(CommandSourceStack source, String rawRarity, double value) {
		WorkerRarity rarity = findByRarity(rawRarity);
		if (rarity == null) {
			source.sendFailure(Component.translatable("laborcraft.command.billing.unknown_rarity", rawRarity));
			return 0;
		}

		LaborCraftConfig config = LaborCraftConfig.get();
		ensureRarities(config.billing);
		config.billing.rarityMultipliers[rarity.ordinal()] = value;
		config.save();
		WorkerMarket.refresh(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.billing.rarity", rarity.id(), value),
				true
		);
		return 1;
	}

	private static int force(CommandSourceStack source) {
		int count = BillingManager.forceDue(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.billing.force", count),
				true
		);
		return count;
	}

	private static int applySalary(CommandSourceStack source) {
		int changed = BillingManager.applySalaryToExisting(source.getServer());
		source.sendSuccess(
				() -> Component.translatable("laborcraft.command.billing.apply", changed),
				true
		);
		return changed;
	}

	private static boolean parseBoolean(String value) {
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)) {
			return false;
		}
		throw new IllegalArgumentException(value);
	}

	private static int parseInt(String value) {
		return Integer.parseInt(value);
	}

	private static double parseDouble(String value) {
		return Double.parseDouble(value.replace(',', '.'));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static WorkerRank findByRank(String value) {
		for (WorkerRank rank : WorkerRank.values()) {
			if (rank.id().equalsIgnoreCase(value)) {
				return rank;
			}
		}
		return null;
	}

	private static WorkerRarity findByRarity(String value) {
		for (WorkerRarity rarity : WorkerRarity.values()) {
			if (rarity.id().equalsIgnoreCase(value)) {
				return rarity;
			}
		}
		return null;
	}

	private static void ensureRanks(BillingSettings billing) {
		if (billing.rankMultipliers == null || billing.rankMultipliers.length < WorkerRank.values().length) {
			billing.rankMultipliers = new double[]{1.0D, 1.25D, 1.6D, 2.0D};
		}
	}

	private static void ensureRarities(BillingSettings billing) {
		if (billing.rarityMultipliers == null || billing.rarityMultipliers.length < WorkerRarity.values().length) {
			billing.rarityMultipliers = new double[]{1.0D, 1.05D, 1.10D, 1.20D};
		}
	}
}

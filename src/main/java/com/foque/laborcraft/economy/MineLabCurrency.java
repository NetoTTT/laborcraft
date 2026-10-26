package com.foque.laborcraft.economy;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

/**
 * MineLab's money is the {@code plims} scoreboard objective. This adapter touches only vanilla
 * scoreboard API and never references MineLab classes, so their mod can change internally without
 * forcing changes here. If the objective/player cannot be resolved, the optional ledger fallback
 * keeps LaborCraft operational.
 */
public final class MineLabCurrency implements CurrencyProvider {
	public static final String ID = "minelab:plims";

	private static final MineLabCurrency INSTANCE = new MineLabCurrency();
	private static volatile MinecraftServer server;
	private static boolean fallbackWarned;

	static {
		ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
		ServerLifecycleEvents.SERVER_STOPPING.register(stopped -> server = null);
	}

	private MineLabCurrency() {
	}

	public static MineLabCurrency get() {
		return INSTANCE;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public long balance(UUID account) {
		ScoreAccess access = score(account);
		if (access == null) {
			return fallback().balance(account);
		}
		return access.get();
	}

	@Override
	public boolean withdraw(UUID account, long amount) {
		if (amount < 0L) {
			throw new IllegalArgumentException("amount must be >= 0");
		}
		if (amount > Integer.MAX_VALUE) {
			return false;
		}

		ScoreAccess access = score(account);
		if (access == null) {
			return fallback().withdraw(account, amount);
		}

		int current = access.get();
		if (current < amount) {
			return false;
		}
		access.set(current - (int) amount);
		return true;
	}

	@Override
	public void deposit(UUID account, long amount) {
		if (amount < 0L) {
			throw new IllegalArgumentException("amount must be >= 0");
		}

		ScoreAccess access = score(account);
		if (access == null) {
			fallback().deposit(account, amount);
			return;
		}

		long next = (long) access.get() + amount;
		if (next > Integer.MAX_VALUE) {
			next = Integer.MAX_VALUE;
			LaborCraft.LOGGER.warn("MineLab: saldo de {} excedeu o limite do scoreboard; truncado", account);
		}
		access.set((int) next);
	}

	@Override
	public String format(long amount) {
		String pattern = LaborCraftConfig.get().compatibility.mineLabFormat;
		if (pattern == null || pattern.isBlank()) {
			return Long.toString(amount);
		}
		try {
			return String.format(Locale.forLanguageTag("pt-BR"), pattern, amount);
		} catch (RuntimeException exception) {
			return Long.toString(amount);
		}
	}

	private static ScoreAccess score(UUID account) {
		MinecraftServer current = server;
		if (current == null || account == null) {
			return null;
		}

		Scoreboard scoreboard = current.getScoreboard();
		String objectiveName = LaborCraftConfig.get().compatibility.mineLabObjective;
		Objective objective = scoreboard.getObjective(objectiveName == null ? "plims" : objectiveName);
		if (objective == null) {
			warnFallback("objective MineLab ausente");
			return null;
		}

		ScoreHolder holder = holder(current, account);
		if (holder == null) {
			warnFallback("jogador sem nome no cache");
			return null;
		}
		return scoreboard.getOrCreatePlayerScore(holder, objective);
	}

	private static ScoreHolder holder(MinecraftServer current, UUID account) {
		ServerPlayer online = current.getPlayerList().getPlayer(account);
		if (online != null) {
			return online;
		}

		Optional<NameAndId> cached = current.services().nameToIdCache().get(account);
		return cached.map(profile -> ScoreHolder.forNameOnly(profile.name())).orElse(null);
	}

	private static CurrencyProvider fallback() {
		if (LaborCraftConfig.get().compatibility.mineLabFallbackToLedger) {
			return LedgerCurrency.get();
		}
		return EMPTY;
	}

	private static void warnFallback(String reason) {
		if (!fallbackWarned && LaborCraftConfig.get().compatibility.mineLabFallbackToLedger) {
			fallbackWarned = true;
			LaborCraft.LOGGER.warn("MineLab indisponivel ({}); usando ledger de fallback", reason);
		}
	}

	private static final CurrencyProvider EMPTY = new CurrencyProvider() {
		@Override
		public String id() {
			return ID;
		}

		@Override
		public long balance(UUID account) {
			return 0L;
		}

		@Override
		public boolean withdraw(UUID account, long amount) {
			return false;
		}

		@Override
		public void deposit(UUID account, long amount) {
		}

		@Override
		public String format(long amount) {
			return Long.toString(amount);
		}
	};
}

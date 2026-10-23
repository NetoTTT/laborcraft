package com.foque.laborcraft.economy;

import com.foque.laborcraft.LaborCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class LedgerCurrency implements CurrencyProvider {
	public static final String ID = "laborcraft:ledger";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Long>>() {
	}.getType();
	private static final Path FILE = Path.of("config", "laborcraft-ledger.json");

	private static final LedgerCurrency INSTANCE = new LedgerCurrency();

	private final Map<UUID, Long> balances = new LinkedHashMap<>();

	private LedgerCurrency() {
	}

	public static LedgerCurrency get() {
		return INSTANCE;
	}

	public void load() {
		if (!Files.exists(FILE)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			Map<String, Long> raw = GSON.fromJson(reader, MAP_TYPE);
			if (raw == null) {
				return;
			}

			balances.clear();
			raw.forEach((key, value) -> {
				try {
					balances.put(UUID.fromString(key), value == null ? 0L : value);
				} catch (IllegalArgumentException exception) {
					LaborCraft.LOGGER.warn("Conta invalida no ledger: {}", key);
				}
			});
		} catch (IOException | RuntimeException exception) {
			LaborCraft.LOGGER.error("Falha ao ler {}", FILE, exception);
		}
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Map<String, Long> raw = new LinkedHashMap<>();
			balances.forEach((key, value) -> raw.put(key.toString(), value));
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(raw, writer);
			}
		} catch (IOException exception) {
			LaborCraft.LOGGER.error("Falha ao salvar {}", FILE, exception);
		}
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public synchronized long balance(UUID account) {
		return balances.getOrDefault(account, 0L);
	}

	@Override
	public synchronized boolean withdraw(UUID account, long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount must be >= 0");
		}

		long current = balance(account);
		if (current < amount) {
			return false;
		}

		balances.put(account, current - amount);
		save();
		return true;
	}

	@Override
	public synchronized void deposit(UUID account, long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount must be >= 0");
		}

		balances.put(account, balance(account) + amount);
		save();
	}

	@Override
	public String format(long amount) {
		return Long.toString(amount);
	}
}

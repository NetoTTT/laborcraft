package com.foque.laborcraft.economy;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CurrencyProviders {
	private static final Map<String, CurrencyProvider> PROVIDERS = new LinkedHashMap<>();

	private CurrencyProviders() {
	}

	public static void register(CurrencyProvider provider) {
		CurrencyProvider previous = PROVIDERS.put(provider.id(), provider);
		if (previous != null) {
			LaborCraft.LOGGER.warn("Provedor de dinheiro '{}' substituido", provider.id());
		}
	}

	public static CurrencyProvider get(String id) {
		return PROVIDERS.get(id);
	}

	public static CurrencyProvider active() {
		String configured = LaborCraftConfig.get().currencyProvider;
		CurrencyProvider provider = PROVIDERS.get(configured);
		if (provider == null) {
			provider = PROVIDERS.get(LedgerCurrency.ID);
			LaborCraft.LOGGER.warn(
					"Provedor '{}' nao encontrado, usando '{}'",
					configured,
					LedgerCurrency.ID
			);
		}
		return provider;
	}

	public static Collection<CurrencyProvider> all() {
		return PROVIDERS.values();
	}

	public static void bootstrap() {
		LedgerCurrency.get().load();
		register(LedgerCurrency.get());
		register(MineLabCurrency.get());
	}
}

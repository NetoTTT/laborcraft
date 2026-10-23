package com.foque.laborcraft.economy;

import java.util.UUID;

public interface CurrencyProvider {
	String id();

	long balance(UUID account);

	boolean withdraw(UUID account, long amount);

	void deposit(UUID account, long amount);

	String format(long amount);
}

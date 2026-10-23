package com.foque.laborcraft.contract;

public enum BillingPeriod {
	DAILY(1),
	WEEKLY(7),
	MONTHLY(30);

	private static final int TICKS_PER_DAY = 24000;

	private final int days;

	BillingPeriod(int days) {
		this.days = days;
	}

	public int days() {
		return days;
	}

	public long ticks() {
		return (long) days * TICKS_PER_DAY;
	}
}

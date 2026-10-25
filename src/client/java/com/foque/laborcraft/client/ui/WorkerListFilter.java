package com.foque.laborcraft.client.ui;

/** Shared filter for worker lists in the Posto and Profile screens. */
public enum WorkerListFilter {
	ALL("laborcraft.ui.filter.all"),
	DEBT("laborcraft.ui.filter.debt"),
	STRIKE("laborcraft.ui.filter.strike"),
	PAUSED("laborcraft.ui.filter.paused");

	private final String labelKey;

	WorkerListFilter(String labelKey) {
		this.labelKey = labelKey;
	}

	public String labelKey() {
		return labelKey;
	}

	public WorkerListFilter next() {
		WorkerListFilter[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public boolean matches(String state, boolean hasDebt) {
		return switch (this) {
			case ALL -> true;
			case DEBT -> hasDebt;
			case STRIKE -> "STRIKE".equalsIgnoreCase(state);
			case PAUSED -> "PAUSED".equalsIgnoreCase(state);
		};
	}
}

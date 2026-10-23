package com.foque.laborcraft.worker;

public record WorkerOffer(
		String id,
		String jobId,
		String nameKey,
		String descriptionKey,
		String boostKey,
		long price,
		long upkeep
) {
	public boolean free() {
		return price <= 0L;
	}

	public boolean hasBoost() {
		return boostKey != null && !boostKey.isBlank();
	}
}

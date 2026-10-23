package com.foque.laborcraft.contract;

import java.util.UUID;

public record Contract(
		UUID id,
		UUID owner,
		UUID worker,
		String jobId,
		long wagePerPeriod,
		BillingPeriod period,
		boolean autoRenew,
		int missedPayments
) {
	public Contract withMissedPayments(int value) {
		return new Contract(id, owner, worker, jobId, wagePerPeriod, period, autoRenew, value);
	}

	public Contract withAutoRenew(boolean value) {
		return new Contract(id, owner, worker, jobId, wagePerPeriod, period, value, missedPayments);
	}
}

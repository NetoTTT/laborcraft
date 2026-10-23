package com.foque.laborcraft.housing;

public record HousingReport(
		boolean valid,
		boolean enclosed,
		int volume,
		int doors,
		int beds,
		int comfortBlocks,
		double comfortBonus,
		long checkedAtTick,
		String failureReason
) {
	public static HousingReport invalid(String reason, long checkedAtTick) {
		return new HousingReport(false, false, 0, 0, 0, 0, 0.0D, checkedAtTick, reason);
	}
}

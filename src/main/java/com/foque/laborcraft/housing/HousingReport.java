package com.foque.laborcraft.housing;

/**
 * Resultado da validacao de uma casa a partir da cama. {@code failureReason} e um codigo curto
 * (traduzido na UI/comando) e {@code capacity} e quantas camas a casa aceita.
 */
public record HousingReport(
		boolean valid,
		boolean enclosed,
		int volume,
		int width,
		int depth,
		int doors,
		int beds,
		int capacity,
		int comfortBlocks,
		double comfortBonus,
		long checkedAtTick,
		String failureReason
) {
	public static HousingReport invalid(String reason, long checkedAtTick) {
		return new HousingReport(false, false, 0, 0, 0, 0, 0, 0, 0, 0.0D, checkedAtTick, reason);
	}

	public boolean failed(String reason) {
		return reason != null && reason.equals(failureReason);
	}
}

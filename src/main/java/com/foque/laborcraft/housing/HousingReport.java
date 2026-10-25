package com.foque.laborcraft.housing;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Resultado da validacao de uma casa a partir da cama. {@code failureReason} e um codigo curto
 * (traduzido na UI/comando) e {@code capacity} e quantas camas a casa aceita.
 *
 * <p>{@code min}/{@code max} bound the interior floor (the two measured layers) and {@code bedList}
 * holds one canonical position per bed (the bed foot), so bed slots do not count the two halves of
 * one bed as two beds.</p>
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
		String failureReason,
		BlockPos min,
		BlockPos max,
		List<BlockPos> bedList
) {
	public HousingReport {
		bedList = List.copyOf(bedList);
	}

	public static HousingReport invalid(String reason, long checkedAtTick) {
		return new HousingReport(
				false,
				false,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0.0D,
				checkedAtTick,
				reason,
				BlockPos.ZERO,
				BlockPos.ZERO,
				List.of()
		);
	}

	public boolean failed(String reason) {
		return reason != null && reason.equals(failureReason);
	}
}

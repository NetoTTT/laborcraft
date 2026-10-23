package com.foque.laborcraft.housing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Formata o relatorio de uma casa para o chat (comando e Planta de Casa usam o mesmo texto). */
public final class HousingMessages {
	private HousingMessages() {
	}

	public static List<Component> describe(BlockPos pos, HousingReport report) {
		List<Component> lines = new ArrayList<>(3);

		if (report.valid()) {
			lines.add(Component.translatable("laborcraft.housing.report.valid", pos.getX(), pos.getY(), pos.getZ()));
		} else {
			lines.add(Component.translatable(
					"laborcraft.housing.report.invalid",
					pos.getX(),
					pos.getY(),
					pos.getZ(),
					Component.translatable("laborcraft.housing.reason." + report.failureReason())
			));
		}

		if (report.width() > 0 || report.volume() > 0) {
			lines.add(Component.translatable(
					"laborcraft.housing.report.size",
					report.width(),
					report.depth(),
					report.width() * report.depth(),
					report.volume(),
					report.doors(),
					report.beds(),
					report.capacity()
			));
		}

		if (report.valid()) {
			lines.add(Component.translatable(
					"laborcraft.housing.report.comfort",
					report.comfortBlocks(),
					Math.round(report.comfortBonus() * 100.0D)
			));
		}

		return lines;
	}
}

package com.foque.laborcraft.item;

import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.housing.HousingMessages;
import com.foque.laborcraft.housing.HousingReport;
import com.foque.laborcraft.housing.HousingValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Planta de Casa. Ao usar no chao, mede o comodo como se houvesse uma cama no bloco clicado:
 * valida a casa (fechada, porta, tamanho) e diz quantas camas cabem ali. Nao registra nada e nao
 * consome o item — e uma trena.
 */
public class HousePlanItem extends Item {
	public HousePlanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		BlockPos bedPos = context.getClickedPos().above();
		HousingReport report = HousingValidator.measure(serverLevel, bedPos, LaborCraftConfig.get());
		Player player = context.getPlayer();
		if (player instanceof ServerPlayer serverPlayer) {
			for (Component line : HousingMessages.describe(bedPos, report)) {
				serverPlayer.sendSystemMessage(line);
			}
		}

		return InteractionResult.SUCCESS_SERVER;
	}
}

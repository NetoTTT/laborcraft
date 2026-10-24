package com.foque.laborcraft.block;

import com.foque.laborcraft.worker.StationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class WorkstationBlockItem extends BlockItem {
	public WorkstationBlockItem(Block block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		if (context.getLevel() instanceof ServerLevel serverLevel) {
			BlockPos pos = context.getClickedPos();
			String error = StationRegistry.validateNewPlacement(
					serverLevel.getServer(),
					serverLevel.dimension().identifier().toString(),
					pos
			);
			if (error != null) {
				if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(
							Component.translatable("laborcraft.station.placement." + error)
					);
				}
				return InteractionResult.FAIL;
			}
		}

		return super.place(context);
	}
}

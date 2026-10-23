package com.foque.laborcraft.block;

import com.foque.laborcraft.network.OpenStationPayload;
import com.foque.laborcraft.worker.StationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Posto de Trabalho. Nao gera area sozinho: e o ponto de referencia do trabalhador e a mesa onde o
 * dono configura o vinculo, a area e o bau de saida. Interagir abre a tela do posto.
 */
public class WorkstationBlock extends Block {
	public WorkstationBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level instanceof ServerLevel serverLevel && placer instanceof ServerPlayer player) {
			StationRegistry.ensure(
					serverLevel.getServer(),
					serverLevel.dimension().identifier().toString(),
					pos,
					player.getUUID()
			);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);

		// Bloco colocado por /setblock ou vindo de versao antiga: fica sem dono ate alguem usar.
		if (level instanceof ServerLevel serverLevel) {
			StationRegistry.ensure(
					serverLevel.getServer(),
					serverLevel.dimension().identifier().toString(),
					pos,
					null
			);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hit
	) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		String dimension = serverLevel.dimension().identifier().toString();
		StationRegistry.Station station = StationRegistry.ensure(
				serverLevel.getServer(),
				dimension,
				pos,
				null
		);

		if (station.owner() == null) {
			station = StationRegistry.claim(serverLevel.getServer(), station, serverPlayer.getUUID());
		}

		if (!station.isOwner(serverPlayer.getUUID())) {
			serverPlayer.sendSystemMessage(Component.translatable("laborcraft.station.not_owner"));
			return InteractionResult.SUCCESS_SERVER;
		}

		OpenStationPayload.open(serverPlayer, station);
		return InteractionResult.SUCCESS_SERVER;
	}
}

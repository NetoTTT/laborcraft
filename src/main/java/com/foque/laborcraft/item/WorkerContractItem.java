package com.foque.laborcraft.item;

import com.foque.laborcraft.entity.LaborCraftEntities;
import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.worker.WorkerOwnership;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WorkerContractItem extends Item {
	public WorkerContractItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		WorkerContractData data = stack.getOrDefault(LaborCraftItems.CONTRACT_DATA, WorkerContractData.empty());
		UUID owner = stack.get(LaborCraftItems.CONTRACT_OWNER);
		if (owner != null && !owner.equals(player.getUUID())) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"laborcraft.command.worker.contract.not_owner"
			));
			return InteractionResult.FAIL;
		}

		int limit = WorkerOwnership.limit(serverPlayer);
		int owned = WorkerOwnership.ownedWorkers(serverPlayer);
		if (limit > 0 && owned >= limit) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"laborcraft.command.worker.contract.limit",
					limit
			));
			return InteractionResult.FAIL;
		}

		LaborCraftWorker worker = LaborCraftEntities.WORKER.spawn(
				serverPlayer.level(),
				player.blockPosition(),
				EntitySpawnReason.SPAWN_ITEM_USE
		);
		if (worker == null) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"laborcraft.command.worker.contract.spawn_failed"
			));
			return InteractionResult.FAIL;
		}

		worker.hire(player.getUUID());
		worker.setWorkerName(data.displayName());
		worker.setJobId(data.jobId());
		worker.setOfferId(data.offerId());
		worker.applyTraits(data.rank(), data.rarity(), data.boosts(), data.aptitudes(), data.mastery());
		worker.setUpkeep(data.upkeep());
		worker.setHunger(data.hunger());
		worker.setMood(data.mood());

		// O contrato gera uma entidade: consumir sempre evita duplicar trabalhador no criativo,
		// onde itens nao sao gastos por padrao (e o limite de trabalhadores seria furado).
		stack.shrink(1);

		OpenWorkerPayload.open(serverPlayer, worker);
		serverPlayer.sendSystemMessage(Component.translatable(
				"laborcraft.command.worker.contract.spawn",
				worker.getName()
		));
		return InteractionResult.SUCCESS_SERVER;
	}
}

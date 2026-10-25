package com.foque.laborcraft.housing;

import com.foque.laborcraft.network.OpenHomeRegisterPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Sneak + right-click a bed with an empty hand to register its room as the player's house.
 * The same gesture would otherwise try to sleep, so the server cancels the vanilla interaction.
 */
public final class HousingInteractions {
	private HousingInteractions() {
	}

	public static void bootstrap() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (hand != InteractionHand.MAIN_HAND
					|| !player.isShiftKeyDown()
					|| !player.getItemInHand(hand).isEmpty()
					|| hitResult.getType() != HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}

			if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			BlockHitResult blockHit = (BlockHitResult) hitResult;
			BlockPos pos = blockHit.getBlockPos();
			if (!level.getBlockState(pos).is(BlockTags.BEDS)) {
				return InteractionResult.PASS;
			}

			if (!OpenHomeRegisterPayload.open(serverPlayer, serverLevel, pos)) {
				HomeActions.claim(serverPlayer, serverLevel, pos, null);
			}
			return InteractionResult.SUCCESS_SERVER;
		});
	}
}

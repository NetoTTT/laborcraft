package com.foque.laborcraft.mixin;

import com.foque.laborcraft.entity.LaborCraftWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla classifies a closed fence gate as {@code PathType.FENCE} (malus -1), so no path can ever
 * cross it, even for mobs that can open doors. Workers treat a closed fence gate exactly like a
 * closed wooden door: {@code WALKABLE_DOOR} (malus 0). {@code OpenFenceGateGoal} then opens the
 * gate only while the path steps on it and closes it right after the worker crosses.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
	@Inject(method = "getPathTypeOfMob", at = @At("HEAD"), cancellable = true)
	private void laborcraft$pathThroughFenceGate(
			PathfindingContext context,
			int x,
			int y,
			int z,
			Mob mob,
			CallbackInfoReturnable<PathType> callback
	) {
		if (!(mob instanceof LaborCraftWorker)) {
			return;
		}

		BlockState state = context.getBlockState(new BlockPos(x, y, z));
		if (state.getBlock() instanceof FenceGateBlock
				&& state.hasProperty(FenceGateBlock.OPEN)
				&& !state.getValue(FenceGateBlock.OPEN)) {
			callback.setReturnValue(PathType.WALKABLE_DOOR);
		}
	}
}

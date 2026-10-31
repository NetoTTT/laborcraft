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
 * Vanilla treats a closed fence gate as {@code PathType.FENCE}, even when the mob can open doors.
 * Workers treat it as a closed wooden door so the path can go through it; {@code OpenFenceGateGoal}
 * opens and closes the gate.
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
			callback.setReturnValue(PathType.DOOR_WOOD_CLOSED);
		}
	}
}

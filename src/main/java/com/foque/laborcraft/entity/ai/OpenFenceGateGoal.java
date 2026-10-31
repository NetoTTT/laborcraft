package com.foque.laborcraft.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Opens a closed fence gate that the current path goes through, then closes it after the worker
 * moves away. Complements vanilla {@code OpenDoorGoal}, which only handles {@code DoorBlock}.
 */
public final class OpenFenceGateGoal extends Goal {
	private static final double SEARCH_RADIUS = 1.0D;
	private static final double USE_DISTANCE_SQR = 9.0D;
	private static final double CLOSE_DISTANCE_SQR = 12.25D;

	private final Mob mob;
	private final boolean closeDoor;
	private BlockPos gate;

	public OpenFenceGateGoal(Mob mob, boolean closeDoor) {
		this.mob = mob;
		this.closeDoor = closeDoor;
	}

	@Override
	public boolean canUse() {
		if (mob.level().isClientSide()) {
			return false;
		}

		Path path = mob.getNavigation().getPath();
		if (path == null || path.isDone()) {
			return false;
		}

		BlockPos next = path.getNextNodePos();
		BlockPos min = next.offset(-(int) SEARCH_RADIUS, -1, -(int) SEARCH_RADIUS);
		BlockPos max = next.offset((int) SEARCH_RADIUS, 1, (int) SEARCH_RADIUS);
		for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
			BlockState state = mob.level().getBlockState(candidate);
			if (state.getBlock() instanceof FenceGateBlock
					&& state.hasProperty(FenceGateBlock.OPEN)
					&& !state.getValue(FenceGateBlock.OPEN)
					&& mob.blockPosition().distSqr(candidate) <= 36.0D) {
				gate = candidate.immutable();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return gate != null
				&& mob.level().getBlockState(gate).getBlock() instanceof FenceGateBlock
				&& mob.distanceToSqr(
						gate.getX() + 0.5D,
						gate.getY() + 0.5D,
						gate.getZ() + 0.5D
				) <= CLOSE_DISTANCE_SQR + 4.0D;
	}

	@Override
	public void start() {
		setOpen(true);
	}

	@Override
	public void tick() {
		if (gate == null) {
			return;
		}

		double distance = mob.distanceToSqr(
				gate.getX() + 0.5D,
				gate.getY() + 0.5D,
				gate.getZ() + 0.5D
		);
		if (distance <= USE_DISTANCE_SQR) {
			setOpen(true);
		}
		else if (closeDoor && distance > CLOSE_DISTANCE_SQR) {
			setOpen(false);
		}
	}

	@Override
	public void stop() {
		if (closeDoor) {
			setOpen(false);
		}
		gate = null;
	}

	private void setOpen(boolean open) {
		if (gate == null) {
			return;
		}

		BlockState state = mob.level().getBlockState(gate);
		if (!(state.getBlock() instanceof FenceGateBlock)
				|| !state.hasProperty(FenceGateBlock.OPEN)
				|| state.getValue(FenceGateBlock.OPEN) == open) {
			return;
		}

		mob.level().setBlock(gate, state.setValue(FenceGateBlock.OPEN, open), 10);
		mob.level().playSound(
				null,
				gate,
				open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);
	}
}

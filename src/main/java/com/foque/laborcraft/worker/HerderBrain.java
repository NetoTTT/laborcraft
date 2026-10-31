package com.foque.laborcraft.worker;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureSpan;
import com.foque.laborcraft.entity.LaborCraftWorker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Herder test brain: while on duty, the worker walks into the pen through its gate, wanders the
 * interior, then leaves again and repeats.
 *
 * <p>Pathfinding is only used where it is reliable: outside up to the gate, and inside the pen.
 * The two-block passage through the gate is driven directly with the move control while the brain
 * holds the gate open, because vanilla pathfinding treats a closed fence gate as a fence even for
 * door-capable mobs. The gate is closed again as soon as the worker is fully through.</p>
 */
public final class HerderBrain extends AbstractWorkerBrain {
	private static final int CHECK_INTERVAL = 10;
	private static final int PATH_TIMEOUT = 200;
	private static final int GRAZE_TICKS = 240;
	private static final int OUTSIDE_TICKS = 100;
	private static final int CROSSING_TIMEOUT = 80;
	private static final int RETRY_TICKS = 200;
	private static final int NOTICE_COOLDOWN = 600;
	private static final double GATE_REACH_SQR = 6.25D;
	private static final Direction[] HORIZONTAL = {
			Direction.NORTH,
			Direction.SOUTH,
			Direction.EAST,
			Direction.WEST
	};

	private enum Phase {
		ENTER,
		GRAZE,
		EXIT,
		WAIT_OUTSIDE
	}

	private Phase phase = Phase.ENTER;
	private int phaseTicks;
	private int pathTicks;
	private int failedPaths;
	private int crossingTicks;
	private long retryAt;
	private long nextNoticeAt;
	private BlockPos target;
	private BlockPos openedGate;
	private String debugInfo = "";
	private int lastNodeIndex = -1;
	private int nodeStuckTicks;

	public HerderBrain(LaborCraftWorker worker) {
		super(worker, CHECK_INTERVAL);
	}

	@Override
	protected boolean hasWorkArea(StationRegistry.Station station) {
		return station.hasEnclosure();
	}

	@Override
	protected String areaNotice() {
		return "laborcraft.ui.worker.task.need_pen";
	}

	@Override
	protected boolean needsTool() {
		return false;
	}

	@Override
	protected boolean canResumeJob(ServerLevel level, StationRegistry.Station station) {
		return station.hasEnclosure();
	}

	@Override
	protected void tickJob(ServerLevel level, StationRegistry.Station station) {
		if (level.getGameTime() < retryAt) {
			worker.getNavigation().stop();
			return;
		}

		EnclosureDescriptor pen = station.enclosure();
		if (pen == null || pen.spans().isEmpty() || pen.gates().isEmpty()) {
			pause(level, "laborcraft.ui.worker.task.need_pen");
			return;
		}

		boolean inside = pen.contains(worker.blockPosition());
		BlockPos gate = nearestGate(pen, worker.blockPosition());
		Direction inward = insideDirection(pen, gate);
		debugInfo = "pos=" + shortPos(worker.blockPosition())
				+ " inside=" + inside
				+ " spans=" + pen.spans().size()
				+ " bbox=" + shortPos(pen.min()) + ".." + shortPos(pen.max())
				+ " gate=" + shortPos(gate)
				+ " inward=" + (inward == null ? "none" : inward.getName())
				+ " path=" + (worker.getNavigation().isDone() ? "done" : "moving");
		if (inward == null) {
			notifyOnce(level, "laborcraft.ui.worker.task.pen_unreachable");
			retryAt = level.getGameTime() + RETRY_TICKS;
			return;
		}

		switch (phase) {
			case EXIT -> tickExit(level, pen, gate, inward, inside);
			case WAIT_OUTSIDE -> {
				phaseTicks -= CHECK_INTERVAL;
				worker.getNavigation().stop();
				closeGate(level, gate);
				if (phaseTicks <= 0) {
					resetAttempt();
					phase = Phase.ENTER;
				}
			}
			case ENTER -> tickEnter(level, pen, gate, inward, inside);
			case GRAZE -> tickGraze(level, pen, gate, inside);
		}
	}

	private void tickEnter(
			ServerLevel level,
			EnclosureDescriptor pen,
			BlockPos gate,
			Direction inward,
			boolean inside
	) {
		if (inside) {
			finishCrossing(level, gate);
			phase = Phase.GRAZE;
			phaseTicks = GRAZE_TICKS;
			return;
		}

		if (nearGate(gate)) {
			cross(level, pen, gate, inward, true, 2);
			return;
		}

		BlockPos approach = outsideStand(level, pen, gate, inward);
		if (approach == null) {
			fail(level);
			return;
		}
		closeGate(level, gate);
		follow(level, approach);
	}

	private void tickExit(
			ServerLevel level,
			EnclosureDescriptor pen,
			BlockPos gate,
			Direction inward,
			boolean inside
	) {
		if (!inside) {
			finishCrossing(level, gate);
			phase = Phase.WAIT_OUTSIDE;
			phaseTicks = OUTSIDE_TICKS;
			return;
		}

		if (nearGate(gate)) {
			cross(level, pen, gate, inward, false, 2);
			return;
		}

		BlockPos insideStand = standAt(level, gate.relative(inward));
		if (insideStand == null) {
			fail(level);
			return;
		}
		follow(level, insideStand);
	}

	private void tickGraze(ServerLevel level, EnclosureDescriptor pen, BlockPos gate, boolean inside) {
		if (!inside) {
			resetAttempt();
			phase = Phase.ENTER;
			return;
		}

		phaseTicks -= CHECK_INTERVAL;
		if (phaseTicks <= 0) {
			resetAttempt();
			phase = Phase.EXIT;
			return;
		}

		if (worker.position().distanceToSqr(Vec3.atCenterOf(gate)) > 16.0D) {
			closeGate(level, gate);
		}

		if (target == null || worker.getNavigation().isDone()) {
			target = randomInteriorThread(level, pen);
			pathTicks = PATH_TIMEOUT;
		}
		if (target != null) {
			follow(level, target);
		}
	}

	/**
	 * Drives the worker straight through the gate while holding it open. Navigation is stopped so
	 * there is no path conflict; vanilla pathfinding is not asked to cross a closed gate.
	 */
	private void cross(
			ServerLevel level,
			EnclosureDescriptor pen,
			BlockPos gate,
			Direction inward,
			boolean entering,
			int depth
	) {
		Direction direction = entering ? inward : inward.getOpposite();
		BlockPos rawStand = gate.relative(direction, depth);
		BlockPos stand = standAt(level, rawStand);
		if (stand == null) {
			stand = standAt(level, gate.relative(direction));
		}
		if (stand == null) {
			fail(level);
			return;
		}

		openGate(level, gate);
		worker.getNavigation().stop();
		worker.getMoveControl().setWantedPosition(
				stand.getX() + 0.5D,
				stand.getY(),
				stand.getZ() + 0.5D,
				1.0D
		);

		crossingTicks += CHECK_INTERVAL;
		if (crossingTicks >= 20) {
			worker.getJumpControl().jump();
		}
		if (crossingTicks >= CROSSING_TIMEOUT) {
			fail(level);
			return;
		}

		boolean passed = entering ? pen.contains(worker.blockPosition()) : !pen.contains(worker.blockPosition());
		if (passed) {
			finishCrossing(level, gate);
			if (entering) {
				phase = Phase.GRAZE;
				phaseTicks = GRAZE_TICKS;
			}
			else {
				phase = Phase.WAIT_OUTSIDE;
				phaseTicks = OUTSIDE_TICKS;
			}
		}
	}

	private boolean nearGate(BlockPos gate) {
		return worker.position().distanceToSqr(Vec3.atCenterOf(gate)) <= GATE_REACH_SQR;
	}

	private void finishCrossing(ServerLevel level, BlockPos gate) {
		closeGate(level, gate);
		crossingTicks = 0;
		failedPaths = 0;
		pathTicks = 0;
		target = null;
	}

	private void resetAttempt() {
		crossingTicks = 0;
		lastNodeIndex = -1;
		nodeStuckTicks = 0;
		failedPaths = 0;
		pathTicks = 0;
		target = null;
	}

	/** Retries after a cooldown and reports the problem at most once per 30 s, never per tick. */
	private void fail(ServerLevel level) {
		closeGate(level, openedGate);
		openedGate = null;
		worker.getNavigation().stop();
		resetAttempt();
		retryAt = level.getGameTime() + RETRY_TICKS;
		notifyOnce(level, "laborcraft.ui.worker.task.pen_unreachable");
	}

	private void notifyOnce(ServerLevel level, String key) {
		long now = level.getGameTime();
		if (now < nextNoticeAt) {
			return;
		}
		nextNoticeAt = now + NOTICE_COOLDOWN;
		worker.announce(key, 10);
		ServerPlayer owner = worker.owner() == null
				? null
				: level.getServer().getPlayerList().getPlayer(worker.owner());
		if (owner != null) {
			owner.sendSystemMessage(WorkerChat.say(worker, Component.translatable(key)));
			owner.sendSystemMessage(Component.literal("[LC path] " + debugInfo).withStyle(ChatFormatting.GRAY));
		}
		LaborCraft.LOGGER.info("[herder] {} " + debugInfo, key);
	}

	/** Walks to {@code stand} with vanilla navigation and reports a failure after a timeout. */
	private void follow(ServerLevel level, BlockPos stand) {
		if (worker.blockPosition().distSqr(stand) <= 2.25D) {
			worker.getNavigation().stop();
			pathTicks = 0;
			return;
		}

		if (worker.getNavigation().isDone() || pathTicks <= 0) {
			boolean moved = worker.getNavigation().moveTo(
					stand.getX() + 0.5D,
					stand.getY(),
					stand.getZ() + 0.5D,
					1.0D
			);
			pathTicks = moved ? PATH_TIMEOUT : 0;
			if (!moved) {
				failedPaths++;
			}
		}
		else {
			pathTicks -= CHECK_INTERVAL;
			if (pathTicks <= 0) {
				failedPaths++;
			}
		}

		Path path = worker.getNavigation().getPath();
		int index = path == null || path.isDone() ? -1 : path.getNextNodeIndex();
		if (index < 0) {
			lastNodeIndex = -1;
			nodeStuckTicks = 0;
		}
		else if (index == lastNodeIndex) {
			nodeStuckTicks += CHECK_INTERVAL;
			if (nodeStuckTicks >= 60) {
				nodeStuckTicks = 0;
				failedPaths++;
				worker.getNavigation().stop();
				pathTicks = 0;
			}
		}
		else {
			lastNodeIndex = index;
			nodeStuckTicks = 0;
		}

		if (failedPaths >= 3) {
			fail(level);
		}
	}

	private static String shortPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static Direction insideDirection(EnclosureDescriptor pen, BlockPos gate) {
		for (Direction direction : HORIZONTAL) {
			if (pen.contains(gate.relative(direction))) {
				return direction;
			}
		}
		return null;
	}

	private static BlockPos nearestGate(EnclosureDescriptor pen, BlockPos from) {
		BlockPos best = pen.gates().getFirst();
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos gate : pen.gates()) {
			double distance = gate.distSqr(from);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = gate;
			}
		}
		return best;
	}

	private BlockPos outsideStand(
			ServerLevel level, EnclosureDescriptor pen, BlockPos gate, Direction inward
	) {
		for (int distance = 2; distance >= 1; distance--) {
			BlockPos stand = standAt(level, gate.relative(inward.getOpposite(), distance));
			if (stand != null) {
				return stand;
			}
		}
		return null;
	}

	/** Picks a walkable column inside the pen. Spans are interior-only, so this never leaves it. */
	private BlockPos randomInteriorThread(ServerLevel level, EnclosureDescriptor pen) {
		if (pen.spans().isEmpty()) {
			return null;
		}

		for (int attempt = 0; attempt < 16; attempt++) {
			EnclosureSpan span = pen.spans().get(worker.getRandom().nextInt(pen.spans().size()));
			int x = span.minX() + worker.getRandom().nextInt(Math.max(1, span.maxX() - span.minX() + 1));
			BlockPos stand = standAt(level, new BlockPos(x, pen.seed().getY(), span.z()));
			if (stand != null) {
				return stand;
			}
		}
		return null;
	}

	private static BlockPos standAt(ServerLevel level, BlockPos origin) {
		for (int dy = 6; dy >= -8; dy--) {
			BlockPos candidate = origin.above(dy);
			if (!level.hasChunkAt(candidate)) {
				continue;
			}
			BlockState feet = level.getBlockState(candidate);
			BlockState head = level.getBlockState(candidate.above());
			BlockState below = level.getBlockState(candidate.below());
			if (feet.getCollisionShape(level, candidate).isEmpty()
					&& head.getCollisionShape(level, candidate.above()).isEmpty()
					&& !below.getCollisionShape(level, candidate.below()).isEmpty()) {
				return candidate;
			}
		}
		return null;
	}

	private void openGate(ServerLevel level, BlockPos gate) {
		setGate(level, gate, true);
		openedGate = gate;
	}

	private void closeGate(ServerLevel level, BlockPos gate) {
		if (gate != null) {
			setGate(level, gate, false);
			if (gate.equals(openedGate)) {
				openedGate = null;
			}
		}
	}

	private static void setGate(ServerLevel level, BlockPos gate, boolean open) {
		BlockState state = level.getBlockState(gate);
		if (!(state.getBlock() instanceof FenceGateBlock)
				|| !state.hasProperty(FenceGateBlock.OPEN)
				|| state.getValue(FenceGateBlock.OPEN) == open) {
			return;
		}
		level.setBlock(gate, state.setValue(FenceGateBlock.OPEN, open), 10);
		level.playSound(
				null,
				gate,
				open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);
	}

	@Override
	protected void onPaused() {
		if (openedGate != null && worker.level() instanceof ServerLevel level) {
			setGate(level, openedGate, false);
		}
		openedGate = null;
		target = null;
		pathTicks = 0;
		failedPaths = 0;
		crossingTicks = 0;
		lastNodeIndex = -1;
		nodeStuckTicks = 0;
	}
}

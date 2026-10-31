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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Herder test brain: while on duty, the worker walks into the pen through its gate, wanders the
 * interior, then leaves again and repeats.
 *
 * <p>Pathfinding crosses the closed gate because {@code WalkNodeEvaluatorMixin} maps it to
 * {@code WALKABLE_DOOR}. {@code OpenFenceGateGoal} opens the gate only while the path steps on it
 * and closes it two blocks after crossing, so a worker never opens a gate just by walking near.</p>
 */
public final class HerderBrain extends AbstractWorkerBrain {
	private static final int CHECK_INTERVAL = 10;
	private static final int PATH_TIMEOUT = 200;
	private static final int GRAZE_TICKS = 240;
	private static final int OUTSIDE_TICKS = 100;
	private static final int RETRY_TICKS = 200;
	private static final int NOTICE_COOLDOWN = 600;
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
	private int nodeStuckTicks;
	private int lastNodeIndex = -1;
	private long retryAt;
	private long nextNoticeAt;
	private BlockPos target;
	private String debugInfo = "";

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
				+ " inward=" + (inward == null ? "none" : inward.getName());

		if (inward == null) {
			fail(level);
			return;
		}

		switch (phase) {
			case EXIT -> tickExit(level, pen, gate, inward, inside);
			case WAIT_OUTSIDE -> {
				phaseTicks -= CHECK_INTERVAL;
				worker.getNavigation().stop();
				if (phaseTicks <= 0) {
					resetAttempt();
					phase = Phase.ENTER;
				}
			}
			case ENTER -> tickEnter(level, pen, gate, inside);
			case GRAZE -> tickGraze(level, pen, inside);
		}
	}

	private void tickEnter(ServerLevel level, EnclosureDescriptor pen, BlockPos gate, boolean inside) {
		if (inside) {
			phase = Phase.GRAZE;
			phaseTicks = GRAZE_TICKS;
			resetAttempt();
			return;
		}

		// Aim deep inside the pen, so the path goes through the gate instead of stopping next to it.
		BlockPos entry = randomInteriorThread(level, pen);
		if (entry == null) {
			entry = insideStand(level, pen, gate);
		}
		if (entry == null) {
			fail(level);
			return;
		}
		follow(level, entry);
		if (pen.contains(worker.blockPosition())) {
			phase = Phase.GRAZE;
			phaseTicks = GRAZE_TICKS;
			resetAttempt();
		}
	}

	private void tickExit(
			ServerLevel level, EnclosureDescriptor pen, BlockPos gate, Direction inward, boolean inside
	) {
		if (!inside) {
			phase = Phase.WAIT_OUTSIDE;
			phaseTicks = OUTSIDE_TICKS;
			resetAttempt();
			return;
		}

		BlockPos outside = outsideStand(level, pen, gate, inward);
		if (outside == null) {
			fail(level);
			return;
		}
		follow(level, outside);
		if (!pen.contains(worker.blockPosition())) {
			phase = Phase.WAIT_OUTSIDE;
			phaseTicks = OUTSIDE_TICKS;
			resetAttempt();
		}
	}

	private void tickGraze(ServerLevel level, EnclosureDescriptor pen, boolean inside) {
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

		if (target == null || worker.getNavigation().isDone()) {
			target = randomInteriorThread(level, pen);
			pathTicks = PATH_TIMEOUT;
		}
		if (target != null) {
			follow(level, target);
		}
	}

	private void resetAttempt() {
		failedPaths = 0;
		pathTicks = 0;
		nodeStuckTicks = 0;
		lastNodeIndex = -1;
		target = null;
	}

	/** Retries after a cooldown and reports the problem at most once per 30 s, never per tick. */
	private void fail(ServerLevel level) {
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
		LaborCraft.LOGGER.info("[herder] {} {}", key, debugInfo);
	}

	/**
	 * Walks to {@code stand} with vanilla navigation. Stuck detection uses the path node index (the
	 * MineColonies approach): if the next node never advances, the worker is pushing a wall.
	 */
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

	private static BlockPos insideStand(ServerLevel level, EnclosureDescriptor pen, BlockPos gate) {
		for (Direction direction : HORIZONTAL) {
			BlockPos adjacent = gate.relative(direction);
			if (!pen.contains(adjacent)) {
				continue;
			}
			BlockPos stand = standAt(level, adjacent);
			if (stand != null) {
				return stand;
			}
		}
		return null;
	}

	/** Two blocks outside the gate, so the path keeps moving until the worker is fully through. */
	private static BlockPos outsideStand(
			ServerLevel level, EnclosureDescriptor pen, BlockPos gate, Direction inward
	) {
		if (!pen.contains(gate.relative(inward))) {
			return null;
		}
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

	private static String shortPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	@Override
	protected void onPaused() {
		target = null;
		pathTicks = 0;
		failedPaths = 0;
		nodeStuckTicks = 0;
		lastNodeIndex = -1;
	}
}

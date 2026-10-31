package com.foque.laborcraft.worker;

import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureSpan;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Herder test brain: while on duty, the worker walks into the pen through a gate, wanders the
 * interior, then leaves again and repeats. Gate opening/closing is handled by
 * {@link com.foque.laborcraft.entity.ai.OpenFenceGateGoal} as the navigation path crosses it;
 * this brain only picks gate-side stands and interior points.
 */
public final class HerderBrain extends AbstractWorkerBrain {
	private static final int CHECK_INTERVAL = 10;
	private static final int PATH_TIMEOUT = 200;
	private static final int GRAZE_TICKS = 240;
	private static final int OUTSIDE_TICKS = 100;
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
	private BlockPos target;

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
		String notice = worker.workNotice();
		return switch (notice == null ? "" : notice) {
			case "laborcraft.ui.worker.task.need_pen",
					"laborcraft.ui.worker.task.pen_unreachable" -> station.hasEnclosure();
			default -> false;
		};
	}

	@Override
	protected void tickJob(ServerLevel level, StationRegistry.Station station) {
		EnclosureDescriptor pen = station.enclosure();
		if (pen == null || pen.spans().isEmpty() || pen.gates().isEmpty()) {
			pause(level, "laborcraft.ui.worker.task.need_pen");
			return;
		}

		boolean inside = pen.contains(worker.blockPosition());
		BlockPos gate = nearestGate(pen, worker.blockPosition());

		switch (phase) {
			case EXIT -> {
				BlockPos outside = outsideStand(level, pen, gate);
				if (outside == null) {
					pause(level, "laborcraft.ui.worker.task.pen_unreachable");
					return;
				}
				if (!inside && worker.blockPosition().distSqr(outside) <= 4.0D) {
					phase = Phase.WAIT_OUTSIDE;
					phaseTicks = OUTSIDE_TICKS;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
					return;
				}
				if (!inside && worker.blockPosition().distSqr(outside) > 4.0D) {
					follow(level, outside, () -> phase = Phase.WAIT_OUTSIDE);
					return;
				}
				follow(level, outside, () -> phase = Phase.WAIT_OUTSIDE);
			}
			case WAIT_OUTSIDE -> {
				phaseTicks -= CHECK_INTERVAL;
				worker.getNavigation().stop();
				if (phaseTicks <= 0) {
					phase = Phase.ENTER;
					target = null;
					failedPaths = 0;
				}
			}
			case ENTER -> {
				if (inside) {
					phase = Phase.GRAZE;
					phaseTicks = GRAZE_TICKS;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
					return;
				}
				BlockPos gateInside = insideStand(level, pen, gate);
				if (gateInside == null) {
					pause(level, "laborcraft.ui.worker.task.pen_unreachable");
					return;
				}
				follow(level, gateInside, () -> {
					// reached the gate-side stand; the actual entry is the next interior step
				});
				if (pen.contains(worker.blockPosition())) {
					phase = Phase.GRAZE;
					phaseTicks = GRAZE_TICKS;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
				}
			}
			case GRAZE -> {
				if (!inside) {
					// pushed out or gate left open: walk back in
					phase = Phase.ENTER;
					target = null;
					pathTicks = 0;
					return;
				}
				phaseTicks -= CHECK_INTERVAL;
				if (phaseTicks <= 0) {
					phase = Phase.EXIT;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
					return;
				}
				if (target == null || worker.getNavigation().isDone()) {
					target = randomInteriorThread(level, pen);
					pathTicks = PATH_TIMEOUT;
				}
				if (target == null) {
					return;
				}
				BlockPos walk = target;
				follow(level, walk, () -> {
				});
			}
		}
	}

	/**
	 * Walks to {@code stand}, retrying on path timeouts and pausing after repeated failures so the
	 * worker does not silently spin in front of a closed or unreachable pen.
	 */
	private void follow(ServerLevel level, BlockPos stand, Runnable onArrive) {
		double speed = 1.0D;
		if (worker.blockPosition().distSqr(stand) <= 2.25D) {
			worker.getNavigation().stop();
			pathTicks = 0;
			onArrive.run();
			return;
		}

		if (worker.getNavigation().isDone() || pathTicks <= 0) {
			boolean moved = worker.getNavigation().moveTo(
					stand.getX() + 0.5D,
					stand.getY(),
					stand.getZ() + 0.5D,
					speed
			);
			if (!moved) {
				pathTicks = 0;
				failedPaths++;
			}
			else {
				pathTicks = PATH_TIMEOUT;
			}
		}
		else {
			pathTicks -= CHECK_INTERVAL;
			if (pathTicks <= 0) {
				failedPaths++;
			}
		}

		if (failedPaths >= 3) {
			failedPaths = 0;
			target = null;
			pause(level, "laborcraft.ui.worker.task.pen_unreachable");
		}
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

	private BlockPos insideStand(ServerLevel level, EnclosureDescriptor pen, BlockPos gate) {
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
		return randomInteriorThread(level, pen);
	}

	private BlockPos outsideStand(ServerLevel level, EnclosureDescriptor pen, BlockPos gate) {
		for (Direction direction : HORIZONTAL) {
			BlockPos adjacent = gate.relative(direction);
			if (pen.contains(adjacent)) {
				BlockPos outside = gate.relative(direction.getOpposite());
				BlockPos stand = standAt(level, outside);
				if (stand != null) {
					return stand;
				}
			}
		}
		return standAt(level, gate.relative(Direction.NORTH));
	}

	/** Picks a walkable column inside the pen. Spans are interior-only, so this never leaves it. */
	private BlockPos randomInteriorThread(ServerLevel level, EnclosureDescriptor pen) {
		List<EnclosureSpan> spans = pen.spans();
		if (spans.isEmpty()) {
			return null;
		}

		for (int attempt = 0; attempt < 16; attempt++) {
			EnclosureSpan span = spans.get(worker.getRandom().nextInt(spans.size()));
			int x = span.minX() + worker.getRandom().nextInt(Math.max(1, span.maxX() - span.minX() + 1));
			BlockPos stand = standAt(level, new BlockPos(x, pen.seed().getY(), span.z()));
			if (stand != null) {
				return stand;
			}
		}
		return null;
	}

	/** Finds a spot with two free blocks and solid ground within a small vertical window. */
	private static BlockPos standAt(ServerLevel level, BlockPos origin) {
		for (int dy = 2; dy >= -3; dy--) {
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

	@Override
	protected void onPaused() {
		target = null;
		pathTicks = 0;
		failedPaths = 0;
	}
}

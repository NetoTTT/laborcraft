package com.foque.laborcraft.worker;

import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureSpan;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.FenceGateBlock;
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
	private BlockPos openedGate;

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
				applyGate(level, gate, true);
				follow(level, outside, () -> {
				});
				if (!pen.contains(worker.blockPosition())) {
					applyGate(level, gate, false);
					phase = Phase.WAIT_OUTSIDE;
					phaseTicks = OUTSIDE_TICKS;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
				}
			}
			case WAIT_OUTSIDE -> {
				phaseTicks -= CHECK_INTERVAL;
				worker.getNavigation().stop();
				applyGate(level, gate, false);
				if (phaseTicks <= 0) {
					phase = Phase.ENTER;
					target = null;
					failedPaths = 0;
				}
			}
			case ENTER -> {
				if (inside) {
					applyGate(level, gate, false);
					phase = Phase.GRAZE;
					phaseTicks = GRAZE_TICKS;
					target = null;
					pathTicks = 0;
					failedPaths = 0;
					return;
				}

				BlockPos gateInside = insideStand(level, pen, gate);
				BlockPos gateOutside = outsideStand(level, pen, gate);
				if (gateInside == null || gateOutside == null) {
					pause(level, "laborcraft.ui.worker.task.pen_unreachable");
					return;
				}

				if (worker.blockPosition().distSqr(gateOutside) > 6.25D) {
					// Approach the gate from the outside while it stays closed.
					applyGate(level, gate, false);
					follow(level, gateOutside, () -> {
					});
				}
				else {
					applyGate(level, gate, true);
					follow(level, gateInside, () -> {
					});
				}

				if (pen.contains(worker.blockPosition())) {
					applyGate(level, gate, false);
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
				if (worker.blockPosition().distSqr(gate) > 16.0D) {
					applyGate(level, gate, false);
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

	/** Opens/closes the gate directly, so entry and exit are visible even if pathing skips it. */
	private void applyGate(ServerLevel level, BlockPos gate, boolean open) {
		setGate(level, gate, open);
		if (open) {
			openedGate = gate;
		}
		else if (gate.equals(openedGate)) {
			openedGate = null;
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

	@Override
	protected void onPaused() {
		if (openedGate != null && worker.level() instanceof ServerLevel level) {
			setGate(level, openedGate, false);
		}
		openedGate = null;
		target = null;
		pathTicks = 0;
		failedPaths = 0;
	}
}

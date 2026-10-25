package com.foque.laborcraft.worker;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.schedule.WorkSchedule;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Off-duty routine: walk home, sleep in the assigned bed, and wait at the post when the house is
 * unloaded/unreachable. The farmer brain only runs while on duty.
 */
public final class WorkerRest {
	private static final int CHECK_INTERVAL = 10;
	private static final int PATH_TIMEOUT = 200;
	private static final double BED_REACH_SQR = 9.0D;
	private static final double POST_NEAR_SQR = 25.0D;

	private final LaborCraftWorker worker;
	private int cooldown;
	private int pathTicks;
	private BlockPos bed;
	private Vec3 stand;
	private String notice = "";

	public WorkerRest(LaborCraftWorker worker) {
		this.worker = worker;
	}

	/** Returns true when the farmer brain must stay idle (off duty/resting). */
	public boolean tick() {
		if (worker.level().isClientSide() || !(worker.level() instanceof ServerLevel level)) {
			return false;
		}

		WorkerState state = worker.workerState();
		if (--cooldown > 0) {
			return state == WorkerState.OFF_DUTY || worker.isSleeping();
		}
		cooldown = CHECK_INTERVAL;

		boolean working = WorkSchedule.isWorking(worker.jobId(), level.getOverworldClockTime());
		if (working) {
			stopRest();
			return false;
		}

		if (state == WorkerState.STRIKE
				|| state == WorkerState.PAUSED
				|| state == WorkerState.STORED
				|| state == WorkerState.QUIT) {
			return false;
		}

		if (state != WorkerState.OFF_DUTY) {
			worker.setWorkerState(WorkerState.OFF_DUTY);
			worker.getNavigation().stop();
			reset();
		}

		HomeRegistry.Home home = HomeRegistry.byWorker(level.getServer(), worker.getUUID());
		HomeRegistry.Home.Resident resident = home == null ? null : home.resident(worker.getUUID());
		if (home == null || resident == null || !home.valid()) {
			if (worker.isSleeping()) {
				worker.stopSleeping();
			}
			waitAtPost(level, "laborcraft.ui.worker.task.off_duty_no_home");
			return true;
		}

		BlockPos targetBed = resident.bed();
		if (!home.dimension().equals(level.dimension().identifier().toString())) {
			if (worker.isSleeping()) {
				worker.stopSleeping();
			}
			waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
			return true;
		}

		if (!level.isLoaded(targetBed)) {
			if (worker.isSleeping()) {
				worker.stopSleeping();
			}
			waitAtPost(level, "laborcraft.ui.worker.task.home_unloaded");
			return true;
		}

		if (!level.getBlockState(targetBed).is(BlockTags.BEDS)) {
			if (worker.isSleeping()) {
				worker.stopSleeping();
			}
			waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
			return true;
		}

		if (worker.isSleeping()) {
			BlockPos sleeping = worker.getSleepingPos().orElse(null);
			if (sleeping != null && sleeping.equals(targetBed)) {
				setNotice("laborcraft.ui.worker.task.sleeping");
				return true;
			}
			worker.stopSleeping();
		}

		if (worker.distanceToSqr(Vec3.atCenterOf(targetBed)) <= BED_REACH_SQR) {
			if (worker.startSleeping(targetBed)) {
				worker.getNavigation().stop();
				setNotice("laborcraft.ui.worker.task.sleeping");
				return true;
			}
			waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
			return true;
		}

		if (!targetBed.equals(bed)) {
			bed = targetBed;
			stand = bedStand(level, targetBed);
			pathTicks = 0;
		}

		if (stand == null) {
			waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
			return true;
		}

		if (worker.getNavigation().isDone() || pathTicks <= 0) {
			boolean moved = worker.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.0D);
			if (!moved) {
				waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
				return true;
			}
			pathTicks = PATH_TIMEOUT;
		} else {
			pathTicks -= CHECK_INTERVAL;
			if (pathTicks <= 0) {
				waitAtPost(level, "laborcraft.ui.worker.task.home_unreachable");
				return true;
			}
		}

		setNotice("laborcraft.ui.worker.task.going_home");
		return true;
	}

	private void stopRest() {
		if (worker.isSleeping()) {
			worker.stopSleeping();
		}
		if (worker.workerState() == WorkerState.OFF_DUTY) {
			worker.setWorkerState(WorkerState.ACTIVE);
		}
		worker.setWorkNotice("");
		reset();
	}

	private void waitAtPost(ServerLevel level, String noticeKey) {
		setNotice(noticeKey);
		StationRegistry.Station station = StationRegistry.byWorker(level.getServer(), worker.getUUID());
		if (station == null || !StationRegistry.isActive(level.getServer(), station)) {
			worker.getNavigation().stop();
			return;
		}

		BlockPos post = station.pos();
		if (worker.distanceToSqr(Vec3.atCenterOf(post)) <= POST_NEAR_SQR) {
			worker.getNavigation().stop();
			return;
		}

		BlockPos postStand = standNear(level, post);
		if (postStand == null) {
			worker.getNavigation().stop();
			return;
		}

		if (worker.getNavigation().isDone()) {
			worker.getNavigation().moveTo(
					postStand.getX() + 0.5D,
					postStand.getY(),
					postStand.getZ() + 0.5D,
					1.0D
			);
		}
	}

	private Vec3 bedStand(ServerLevel level, BlockPos bedPos) {
		Optional<Vec3> found = AbstractBedBlock.findStandUpPosition(
				worker.getType(),
				level,
				bedPos,
				BedBlock.getBedOrientation(level, bedPos),
				worker.getYRot()
		);
		if (found.isPresent()) {
			return found.get();
		}

		BlockPos fallback = standNear(level, bedPos);
		return fallback == null ? null : Vec3.atBottomCenterOf(fallback);
	}

	private static BlockPos standNear(ServerLevel level, BlockPos pos) {
		BlockPos[] candidates = {
				pos.north(),
				pos.south(),
				pos.east(),
				pos.west(),
				pos.above()
		};
		for (BlockPos candidate : candidates) {
			if (level.getBlockState(candidate).isAir()
					&& level.getBlockState(candidate.above()).isAir()
					&& level.getBlockState(candidate.below()).canOcclude()) {
				return candidate;
			}
		}
		return null;
	}

	private void setNotice(String key) {
		if (!notice.equals(key)) {
			notice = key;
			worker.setWorkNotice(key);
		}
	}

	private void reset() {
		bed = null;
		stand = null;
		pathTicks = 0;
		notice = "";
	}
}

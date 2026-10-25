package com.foque.laborcraft.schedule;

import com.foque.laborcraft.config.LaborCraftConfig;
import java.util.Locale;

public final class WorkSchedule {
	private static final long TICKS_PER_DAY = 24000L;
	private static final long NIGHT_START = 12000L;
	private static final long NIGHT_END = 24000L;

	private WorkSchedule() {
	}

	/**
	 * Per-job schedules will override this later; for now the farmer follows the global config
	 * (default daylight), which already supports custom and wrapping windows.
	 */
	public static boolean isWorking(String jobId, long dayTime) {
		return isWorking(dayTime);
	}

	public static boolean isWorking(long dayTime) {
		LaborCraftConfig.ScheduleSettings settings = LaborCraftConfig.get().schedule;
		long time = Math.floorMod(dayTime, TICKS_PER_DAY);

		return switch (settings.mode == null ? "always" : settings.mode.toLowerCase(Locale.ROOT)) {
			case "daylight" -> time >= 0L && time < NIGHT_START;
			case "night" -> time >= NIGHT_START && time < NIGHT_END;
			case "custom" -> inWindow(time, settings.startTick, settings.endTick);
			default -> true;
		};
	}

	public static boolean isNight(long dayTime) {
		long time = Math.floorMod(dayTime, TICKS_PER_DAY);
		return time >= NIGHT_START && time < NIGHT_END;
	}

	public static boolean inWindow(long time, int startTick, int endTick) {
		long start = Math.floorMod(startTick, TICKS_PER_DAY);
		long end = Math.floorMod(endTick, TICKS_PER_DAY);

		if (start == end) {
			return true;
		}

		if (start < end) {
			return time >= start && time < end;
		}

		return time >= start || time < end;
	}
}

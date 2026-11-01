package com.foque.laborcraft.worker;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.area.EnclosureSpan;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Herder brain: keeps the worker inside the pen while on duty, breeds the herd, collects drops and
 * only leaves to deliver produce or when the shift ends. Entering and leaving use the gate through
 * vanilla navigation (the mixin maps closed gates to {@code WALKABLE_DOOR}); the gate is opened by
 * {@code OpenFenceGateGoal} only while the path steps on it.
 *
 * <p>Gates are kept closed by a watchdog. The worker stays inside instead of looping in and out,
 * because frequent gate openings let animals escape.</p>
 */
public final class HerderBrain extends AbstractWorkerBrain {
	private static final int CHECK_INTERVAL = 10;
	private static final int PATH_TIMEOUT = 200;
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
		GRAZE
	}

	private Phase phase = Phase.ENTER;
	private int pathTicks;
	private int failedPaths;
	private int nodeStuckTicks;
	private int lastNodeIndex = -1;
	private long retryAt;
	private long nextNoticeAt;
	private BlockPos target;
	private boolean depositing;
	private String debugInfo = "";
	private final Map<UUID, Long> fedUntil = new HashMap<>();

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
		EnclosureDescriptor pen = station.enclosure();
		if (pen == null || pen.spans().isEmpty() || pen.gates().isEmpty()) {
			pause(level, "laborcraft.ui.worker.task.need_pen");
			return;
		}

		closeIdleGates(level, pen);

		if (level.getGameTime() < retryAt) {
			worker.getNavigation().stop();
			return;
		}

		boolean inside = pen.contains(worker.blockPosition());
		BlockPos gate = nearestGate(pen, worker.blockPosition());
		Direction inward = insideDirection(pen, gate);
		debugInfo = "pos=" + shortPos(worker.blockPosition())
				+ " inside=" + inside
				+ " spans=" + pen.spans().size()
				+ " gate=" + shortPos(gate)
				+ " inward=" + (inward == null ? "none" : inward.getName());
		if (inward == null) {
			fail(level);
			return;
		}

		Map<HerderAnimals.Species, List<Animal>> herd = HerderAnimals.snapshot(level, pen);
		ensureTool(level, station, herd);

		switch (phase) {
			case ENTER -> tickEnter(level, pen, gate, inside);
			case GRAZE -> tickGraze(level, pen, station, inside, herd);
		}
	}

	private void tickEnter(ServerLevel level, EnclosureDescriptor pen, BlockPos gate, boolean inside) {
		if (inside) {
			phase = Phase.GRAZE;
			resetAttempt();
			return;
		}

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
			resetAttempt();
		}
	}

	private void tickGraze(
			ServerLevel level,
			EnclosureDescriptor pen,
			StationRegistry.Station station,
			boolean inside,
			Map<HerderAnimals.Species, List<Animal>> herd
	) {
		if (!inside && !depositing) {
			phase = Phase.ENTER;
			resetAttempt();
			return;
		}

		if (depositing) {
			if (depositProduce(level, station)) {
				depositing = false;
				target = null;
			}
			return;
		}

		if (LaborCraftConfig.get().herder.collectDrops && shouldDeliver()) {
			depositing = true;
			return;
		}

		if (LaborCraftConfig.get().herder.breeding && tryBreed(level, pen, station, herd)) {
			return;
		}

		if (LaborCraftConfig.get().herder.shearSheep && tryShear(level, herd)) {
			return;
		}

		if (LaborCraftConfig.get().herder.collectDrops) {
			ItemEntity drop = findPickup(level, pen);
			if (drop != null) {
				collectItem(drop);
				return;
			}
		}

		if (target == null || worker.getNavigation().isDone()) {
			target = randomInteriorThread(level, pen);
			pathTicks = PATH_TIMEOUT;
		}
		if (target != null) {
			follow(level, target);
		}
	}

	private boolean shouldDeliver() {
		int produce = HerderStorage.countProduce(worker);
		if (produce >= Math.max(1, LaborCraftConfig.get().herder.produceDeliverThreshold)) {
			return true;
		}
		return produce > 0 && findEmptySlot() < 0;
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
		depositing = false;
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

	/** Shears the nearest woolly adult if the tool slot already holds shears. */
	private boolean tryShear(ServerLevel level, Map<HerderAnimals.Species, List<Animal>> herd) {
		ItemStack tool = worker.toolItem();
		boolean hasWoolly = false;
		for (Animal animal : herd.getOrDefault(HerderAnimals.Species.SHEEP, List.of())) {
			if (animal instanceof Sheep sheep && sheep.readyForShearing()) {
				hasWoolly = true;
				break;
			}
		}
		if (!tool.is(Items.SHEARS)) {
			if (hasWoolly) {
				notifyOnce(level, "laborcraft.ui.worker.task.need_shears");
			}
			return false;
		}
		if (!hasWoolly) {
			return false;
		}

		Sheep target = null;
		double bestDistance = Double.MAX_VALUE;
		for (Animal animal : herd.getOrDefault(HerderAnimals.Species.SHEEP, List.of())) {
			if (!(animal instanceof Sheep sheep) || !sheep.readyForShearing()) {
				continue;
			}
			double distance = worker.position().distanceToSqr(sheep.position());
			if (distance < bestDistance) {
				bestDistance = distance;
				target = sheep;
			}
		}
		if (target == null) {
			return false;
		}

		if (worker.position().distanceToSqr(target.position()) > 2.25D) {
			if (worker.getNavigation().isDone() || pathTicks <= 0) {
				worker.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0D);
				pathTicks = PATH_TIMEOUT;
			}
			return true;
		}

		target.shear(level, SoundSource.PLAYERS, tool);
		if (tool.isDamageableItem()) {
			tool.hurtAndBreak(1, worker, EquipmentSlot.MAINHAND);
		}
		worker.getNavigation().stop();
		return true;
	}

	/** Feeds one adult when its species is below the cap and at least two adults can breed. */
	private boolean tryBreed(
			ServerLevel level,
			EnclosureDescriptor pen,
			StationRegistry.Station station,
			Map<HerderAnimals.Species, List<Animal>> herd
	) {
		LaborCraftConfig.HerderSettings settings = LaborCraftConfig.get().herder;
		for (HerderAnimals.Species species : HerderAnimals.Species.values()) {
			int population = HerderAnimals.count(herd, species);
			if (population < 2 || population >= settings.maxPerSpecies) {
				continue;
			}
			if (HerderAnimals.adultCount(herd, species) < 2) {
				continue;
			}

			long now = level.getGameTime();
			List<Animal> candidates = new ArrayList<>();
			for (Animal adult : HerderAnimals.breedableAdults(herd, species)) {
				if (fedUntil.getOrDefault(adult.getUUID(), 0L) <= now) {
					candidates.add(adult);
				}
			}
			if (candidates.isEmpty()) {
				continue;
			}
			pruneFed(now);

			if (!hasFood(species)) {
				withdrawFood(level, station, species);
				if (!hasFood(species)) {
					continue;
				}
			}

			Animal target = nearest(candidates);
			if (target == null) {
				continue;
			}
			if (worker.position().distanceToSqr(target.position()) > 2.25D) {
				if (worker.getNavigation().isDone() || pathTicks <= 0) {
					worker.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0D);
					pathTicks = PATH_TIMEOUT;
				}
				return true;
			}
			if (consumeFood(species)) {
				target.setInLove(null);
				fedUntil.put(target.getUUID(), now + 6000L);
				worker.getNavigation().stop();
				return true;
			}
		}
		return false;
	}

	private void pruneFed(long now) {
		if (fedUntil.size() < 64) {
			return;
		}
		fedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
	}

	private Animal nearest(List<Animal> animals) {
		Animal best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Animal animal : animals) {
			double distance = worker.position().distanceToSqr(animal.position());
			if (distance < bestDistance) {
				bestDistance = distance;
				best = animal;
			}
		}
		return best;
	}

	private boolean hasFood(HerderAnimals.Species species) {
		return findFoodSlot(species) >= 0;
	}

	private int findFoodSlot(HerderAnimals.Species species) {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			if (species.isFood(worker.inventoryItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	private boolean consumeFood(HerderAnimals.Species species) {
		int slot = findFoodSlot(species);
		if (slot < 0) {
			return false;
		}
		worker.inventoryItem(slot).shrink(1);
		return true;
	}

	private void withdrawFood(
			ServerLevel level, StationRegistry.Station station, HerderAnimals.Species species
	) {
		var got = WorkerStorage.extract(level, station, species::isFood, 16);
		if (got.isEmpty()) {
			return;
		}
		var leftover = FarmerStorage.addToWorker(worker, got);
		if (!leftover.isEmpty()) {
			WorkerStorage.insert(level, station, leftover);
		}
	}

	/** Swaps the tool slot to what the pen needs, using the backpack first and TOOLS chests after. */
	private void ensureTool(
			ServerLevel level,
			StationRegistry.Station station,
			Map<HerderAnimals.Species, List<Animal>> herd
	) {
		if (!LaborCraftConfig.get().herder.autoTools) {
			return;
		}
		ItemStack required = HerderTools.requiredFor(herd);
		if (required.isEmpty() || HerderTools.matches(worker.toolItem(), required)) {
			return;
		}

		int slot = findToolSlot(required);
		if (slot >= 0) {
			ItemStack stored = worker.inventoryItem(slot);
			ItemStack next = stored.copyWithCount(1);
			if (equipTool(level, station, next)) {
				stored.shrink(1);
				if (stored.isEmpty()) {
					worker.setInventoryItem(slot, ItemStack.EMPTY);
				}
			}
			return;
		}

		ItemStack pulled = WorkerStorage.extractTool(
				level, station, stack -> HerderTools.matches(stack, required), 1
		);
		if (pulled.isEmpty()) {
			// Fall back to INPUT/MIXED chests, which are common for tools too.
			pulled = WorkerStorage.extract(
					level, station, stack -> HerderTools.matches(stack, required), 1
			);
		}
		if (pulled.isEmpty()) {
			return;
		}
		if (!equipTool(level, station, pulled)) {
			WorkerStorage.insertTool(level, station, pulled);
			notifyOnce(level, "laborcraft.ui.worker.task.need_tool_space");
		}
	}

	private int findToolSlot(ItemStack required) {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			if (HerderTools.matches(worker.inventoryItem(slot), required)) {
				return slot;
			}
		}
		return -1;
	}

	/** Puts {@code next} in the tool slot, stashing the old tool in the backpack or TOOLS chest. */
	private boolean equipTool(
			ServerLevel level, StationRegistry.Station station, ItemStack next
	) {
		ItemStack current = worker.toolItem();
		if (current.isEmpty()) {
			worker.setInventoryItem(LaborCraftWorker.TOOL_SLOT, next);
			return true;
		}

		int empty = findEmptySlot();
		if (empty >= 0) {
			worker.setInventoryItem(empty, current.copy());
			worker.setInventoryItem(LaborCraftWorker.TOOL_SLOT, next);
			return true;
		}

		ItemStack leftover = WorkerStorage.insertTool(level, station, current.copy());
		if (!leftover.isEmpty()) {
			return false;
		}
		worker.setInventoryItem(LaborCraftWorker.TOOL_SLOT, next);
		return true;
	}

	private int findEmptySlot() {
		for (int slot = 0; slot < LaborCraftWorker.STORAGE_SLOTS; slot++) {
			if (worker.inventoryItem(slot).isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	private ItemEntity findPickup(ServerLevel level, EnclosureDescriptor pen) {
		int margin = Math.max(2, LaborCraftConfig.get().herder.eggScanRadius);
		BlockPos min = pen.min();
		BlockPos max = pen.max();
		int y = pen.seed().getY();
		AABB box = new AABB(
				min.getX() - margin,
				y - 8,
				min.getZ() - margin,
				max.getX() + 1 + margin,
				y + 8,
				max.getZ() + 1 + margin
		);
		ItemEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			if (!item.isAlive() || !HerderStorage.isPickup(item.getItem())) {
				continue;
			}
			if (!pen.contains(item.blockPosition())) {
				continue;
			}
			double distance = worker.position().distanceToSqr(item.position());
			if (distance < bestDistance) {
				bestDistance = distance;
				best = item;
			}
		}
		return best;
	}

	private void collectItem(ItemEntity item) {
		if (worker.position().distanceToSqr(item.position()) > 2.25D) {
			if (worker.getNavigation().isDone() || pathTicks <= 0) {
				worker.getNavigation().moveTo(item.getX(), item.getY(), item.getZ(), 1.0D);
				pathTicks = PATH_TIMEOUT;
			}
			return;
		}

		ItemStack stack = item.getItem();
		ItemStack leftover = FarmerStorage.addToWorker(worker, stack.copy());
		int moved = stack.getCount() - leftover.getCount();
		if (moved <= 0) {
			return;
		}
		stack.shrink(moved);
		if (stack.isEmpty()) {
			item.discard();
		}
		else {
			item.setItem(stack);
		}
	}

	private boolean depositProduce(ServerLevel level, StationRegistry.Station station) {
		BlockPos chest = nearestChest(level, station);
		if (chest == null) {
			notifyOnce(level, "laborcraft.ui.worker.task.no_chest");
			return true;
		}

		if (worker.position().distanceToSqr(Vec3.atCenterOf(chest)) > 2.25D) {
			if (worker.getNavigation().isDone() || pathTicks <= 0) {
				worker.getNavigation().moveTo(
						chest.getX() + 0.5D,
						chest.getY(),
						chest.getZ() + 0.5D,
						1.0D
				);
				pathTicks = PATH_TIMEOUT;
			}
			else {
				pathTicks -= CHECK_INTERVAL;
			}
			return false;
		}

		HerderStorage.deposit(level, station, worker);
		return true;
	}

	private static BlockPos nearestChest(ServerLevel level, StationRegistry.Station station) {
		for (StationRegistry.StorageLink link : station.storage()) {
			if (link.role() == StationRegistry.StorageRole.TOOLS
					|| link.role() == StationRegistry.StorageRole.INPUT) {
				continue;
			}
			if (!(level.getBlockEntity(link.pos()) instanceof net.minecraft.world.Container)) {
				continue;
			}
			return link.pos();
		}
		return null;
	}

	/** Closes pen gates nobody is using. A gate is in use only while a worker's next node is it. */
	static void closeIdleGates(ServerLevel level, EnclosureDescriptor pen) {
		if (pen == null) {
			return;
		}
		for (BlockPos gate : pen.gates()) {
			BlockState state = level.getBlockState(gate);
			if (!(state.getBlock() instanceof FenceGateBlock)
					|| !state.hasProperty(FenceGateBlock.OPEN)
					|| !state.getValue(FenceGateBlock.OPEN)) {
				continue;
			}
			if (gateInUse(level, gate)) {
				continue;
			}
			level.setBlock(gate, state.setValue(FenceGateBlock.OPEN, false), 10);
			level.playSound(
					null,
					gate,
					SoundEvents.FENCE_GATE_CLOSE,
					SoundSource.BLOCKS,
					1.0F,
					1.0F
			);
		}
	}

	private static boolean gateInUse(ServerLevel level, BlockPos gate) {
		AABB box = new AABB(gate).inflate(3.0D);
		for (LaborCraftWorker other : level.getEntitiesOfClass(LaborCraftWorker.class, box)) {
			Path path = other.getNavigation().getPath();
			if (path != null && !path.isDone() && path.getNextNodePos().equals(gate)) {
				return true;
			}
		}
		return false;
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
		depositing = false;
	}
}

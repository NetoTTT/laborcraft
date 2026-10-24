package com.foque.laborcraft.entity;

import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.worker.WorkerAptitudes;
import com.foque.laborcraft.worker.WorkerBoost;
import com.foque.laborcraft.worker.WorkerMastery;
import com.foque.laborcraft.worker.WorkerRank;
import com.foque.laborcraft.worker.WorkerRarity;
import com.foque.laborcraft.worker.WorkerState;
import com.foque.laborcraft.worker.WorkerTraits;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LaborCraftWorker extends PathfinderMob {
	public static final int STORAGE_SLOTS = 9;
	public static final int TOOL_SLOT = 9;
	public static final int SLOT_COUNT = STORAGE_SLOTS + 1;

	private UUID owner;
	private String jobId = "";
	private String offerId = "";
	private WorkerRank rank = WorkerRank.COMUM;
	private WorkerRarity rarity = WorkerRarity.COMUM;
	private List<WorkerBoost> boosts = List.of();
	private Map<String, Integer> aptitudes = Map.of();
	private Map<String, Long> mastery = Map.of();
	private long upkeep = 0L;
	private int hunger = 20;
	private int mood = 100;
	private WorkerState state = WorkerState.ACTIVE;
	private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	/**
	 * Jogador que esta com a tela deste trabalhador aberta. Enquanto isso o trabalhador fica parado,
	 * olhando para ele, sem executar nenhuma acao. Nao e persistido.
	 */
	private ServerPlayer attendingPlayer;

	public LaborCraftWorker(EntityType<? extends LaborCraftWorker> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.30D)
				.add(Attributes.FOLLOW_RANGE, 24.0D)
				.add(Attributes.STEP_HEIGHT, 0.6D);
	}

	@Override
	protected void registerGoals() {
		// O trabalhador fica parado no lugar ate existir a IA de trabalho (area/casa/pathing).
		// Nada de passeio aleatorio: so flutuar na agua, olhar para quem esta perto e olhar em volta.
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	public void setWorkerName(Component displayName) {
		setCustomName(displayName);
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	public UUID owner() {
		return owner;
	}

	public boolean hasOwner() {
		return owner != null;
	}

	public boolean isOwnedBy(UUID playerId) {
		return owner != null && owner.equals(playerId);
	}

	public void hire(UUID newOwner) {
		this.owner = newOwner;
		this.state = WorkerState.ACTIVE;
		setPersistenceRequired();
	}

	public void release() {
		endAttending();
		this.owner = null;
		this.jobId = "";
		this.state = WorkerState.ACTIVE;
	}

	public boolean isAttending() {
		return attendingPlayer != null;
	}

	/** Congela o trabalhador: para a navegacao e faz ele olhar para o jogador da tela. */
	public void beginAttending(ServerPlayer player) {
		this.attendingPlayer = player;
		if (getNavigation() != null) {
			getNavigation().stop();
		}

		setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
	}

	public void endAttending() {
		this.attendingPlayer = null;
	}

	public static LaborCraftWorker find(MinecraftServer server, UUID id) {
		if (server == null || id == null) {
			return null;
		}

		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (entity instanceof LaborCraftWorker worker) {
				return worker;
			}
		}

		return null;
	}

	@Override
	public void tick() {
		super.tick();

		if (level().isClientSide() || attendingPlayer == null) {
			return;
		}

		if (attendingPlayer.isRemoved()
				|| attendingPlayer.level() != level()
				|| attendingPlayer.distanceToSqr(this) > 64.0D) {
			endAttending();
			return;
		}

		getNavigation().stop();
		getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
		setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
		getLookControl().setLookAt(attendingPlayer, 30.0F, 30.0F);
	}

	public String jobId() {
		return jobId;
	}

	public NonNullList<ItemStack> inventory() {
		return inventory;
	}

	public ItemStack inventoryItem(int slot) {
		return slot >= 0 && slot < SLOT_COUNT ? inventory.get(slot) : ItemStack.EMPTY;
	}

	public void setInventoryItem(int slot, ItemStack stack) {
		if (slot >= 0 && slot < SLOT_COUNT) {
			inventory.set(slot, stack == null ? ItemStack.EMPTY : stack);
		}
	}

	public ItemStack toolItem() {
		return inventoryItem(TOOL_SLOT);
	}

	public boolean hasJob() {
		return jobId != null && !jobId.isBlank();
	}

	public void setJobId(String jobId) {
		this.jobId = jobId == null ? "" : jobId;
	}

	public String offerId() {
		return offerId;
	}

	public void setOfferId(String offerId) {
		this.offerId = offerId == null ? "" : offerId;
	}

	public String boostId() {
		return WorkerBoost.join(boosts);
	}

	public WorkerRank rank() {
		return rank;
	}

	public WorkerRarity rarity() {
		return rarity;
	}

	public List<WorkerBoost> boosts() {
		return boosts;
	}

	public Map<String, Integer> aptitudes() {
		return aptitudes;
	}

	public Map<String, Long> mastery() {
		return mastery;
	}

	public int aptitudeFor(String jobId) {
		return WorkerAptitudes.starsWithBoosts(aptitudes, jobId, WorkerTraits.aptitudeBonus(boosts));
	}

	public long masteryFor(String jobId) {
		return mastery.getOrDefault(jobId, 0L);
	}

	public void addMastery(String jobId, long xp) {
		if (jobId == null || jobId.isBlank() || xp <= 0L) {
			return;
		}

		Map<String, Long> updated = new java.util.LinkedHashMap<>(mastery);
		updated.put(jobId, Math.max(0L, masteryFor(jobId) + Math.round(xp * WorkerTraits.learning(rank, boosts))));
		mastery = Map.copyOf(updated);
	}

	public void applyTraits(
			WorkerRank rank,
			WorkerRarity rarity,
			List<WorkerBoost> boosts,
			Map<String, Integer> aptitudes,
			Map<String, Long> mastery
	) {
		this.rank = rank == null ? WorkerRank.COMUM : rank;
		this.rarity = rarity == null ? WorkerRarity.COMUM : rarity;
		this.boosts = List.copyOf(boosts);
		this.aptitudes = Map.copyOf(aptitudes);
		this.mastery = Map.copyOf(mastery);
		applyMovementSpeed();
	}

	private void applyMovementSpeed() {
		if (getAttribute(Attributes.MOVEMENT_SPEED) == null) {
			return;
		}

		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30D * WorkerTraits.speed(rank, boosts));
	}

	public long upkeep() {
		return upkeep;
	}

	public void setUpkeep(long upkeep) {
		this.upkeep = Math.max(0L, upkeep);
	}

	public int hunger() {
		return hunger;
	}

	public void setHunger(int hunger) {
		this.hunger = Math.clamp(hunger, 0, 20);
	}

	public int mood() {
		return mood;
	}

	public void setMood(int mood) {
		this.mood = Math.clamp(mood, 0, 100);
	}

	public String taskKey() {
		if (!hasJob()) {
			return "laborcraft.ui.worker.task.none";
		}

		if (state == WorkerState.PAUSED) {
			return "laborcraft.ui.worker.task.paused";
		}

		if (state == WorkerState.STORED) {
			return "laborcraft.ui.worker.task.stored";
		}

		if (state == WorkerState.QUIT) {
			return "laborcraft.ui.worker.task.quit";
		}

		return "laborcraft.ui.worker.task." + jobId.substring(jobId.indexOf(':') + 1);
	}

	public WorkerState workerState() {
		return state;
	}

	public void setWorkerState(WorkerState state) {
		this.state = state;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (!hasOwner()) {
			serverPlayer.sendSystemMessage(Component.translatable("laborcraft.worker.interact.unowned"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!isOwnedBy(player.getUUID())) {
			serverPlayer.sendSystemMessage(Component.translatable("laborcraft.worker.interact.not_owner"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!OpenWorkerPayload.open(serverPlayer, this)) {
			serverPlayer.sendSystemMessage(Component.translatable("laborcraft.command.ui.unavailable"));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		if (owner != null) {
			output.putString("Owner", owner.toString());
		}

		output.putString("JobId", jobId);
		output.putString("OfferId", offerId);
		output.putString("Rank", rank.id());
		output.putString("Rarity", rarity.id());
		output.putString("Boosts", WorkerBoost.join(boosts));
		output.putString("Aptitudes", WorkerAptitudes.serialize(aptitudes));
		output.putString("Mastery", WorkerMastery.join(mastery));
		output.putLong("Upkeep", upkeep);
		output.putInt("Hunger", hunger);
		output.putInt("Mood", mood);
		output.putString("WorkerState", state.name());
		output.store("Inventory", ItemStack.OPTIONAL_CODEC.listOf(), inventory);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);

		owner = input.getString("Owner")
				.map(value -> {
					try {
						return UUID.fromString(value);
					} catch (IllegalArgumentException exception) {
						return null;
					}
				})
				.orElse(null);

		jobId = input.getStringOr("JobId", "");
		offerId = input.getStringOr("OfferId", "");
		rank = WorkerRank.of(input.getStringOr("Rank", "comum"));
		rarity = WorkerRarity.of(input.getStringOr("Rarity", "comum"));
		boosts = WorkerBoost.split(input.getStringOr("Boosts", ""));
		aptitudes = WorkerAptitudes.parse(input.getStringOr("Aptitudes", ""));
		mastery = WorkerMastery.parse(input.getStringOr("Mastery", ""));
		upkeep = input.getLongOr("Upkeep", 0L);
		hunger = Math.clamp(input.getIntOr("Hunger", 20), 0, 20);
		mood = Math.clamp(input.getIntOr("Mood", 100), 0, 100);

		String storedState = input.getStringOr("WorkerState", WorkerState.ACTIVE.name());
		try {
			state = WorkerState.valueOf(storedState);
		} catch (IllegalArgumentException exception) {
			state = WorkerState.ACTIVE;
		}

		input.read("Inventory", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(stacks -> {
			for (int index = 0; index < Math.min(stacks.size(), SLOT_COUNT); index++) {
				inventory.set(index, stacks.get(index));
			}
		});

		applyMovementSpeed();
	}
}

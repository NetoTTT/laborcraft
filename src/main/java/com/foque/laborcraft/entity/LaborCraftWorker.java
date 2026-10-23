package com.foque.laborcraft.entity;

import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.worker.WorkerState;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LaborCraftWorker extends PathfinderMob {
	private UUID owner;
	private String jobId = "";
	private String offerId = "";
	private String boostId = "";
	private long upkeep = 0L;
	private int hunger = 20;
	private int mood = 100;
	private WorkerState state = WorkerState.ACTIVE;

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
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
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
		this.owner = null;
		this.jobId = "";
		this.state = WorkerState.ACTIVE;
	}

	public String jobId() {
		return jobId;
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
		return boostId;
	}

	public void setBoostId(String boostId) {
		this.boostId = boostId == null ? "" : boostId;
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
		output.putString("BoostId", boostId);
		output.putLong("Upkeep", upkeep);
		output.putInt("Hunger", hunger);
		output.putInt("Mood", mood);
		output.putString("WorkerState", state.name());
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
		boostId = input.getStringOr("BoostId", "");
		upkeep = input.getLongOr("Upkeep", 0L);
		hunger = Math.clamp(input.getIntOr("Hunger", 20), 0, 20);
		mood = Math.clamp(input.getIntOr("Mood", 100), 0, 100);

		String storedState = input.getStringOr("WorkerState", WorkerState.ACTIVE.name());
		try {
			state = WorkerState.valueOf(storedState);
		} catch (IllegalArgumentException exception) {
			state = WorkerState.ACTIVE;
		}
	}
}

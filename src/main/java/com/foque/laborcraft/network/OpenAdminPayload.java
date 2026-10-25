package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.config.LaborCraftConfig.BillingSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Snapshot of the economy settings shown by the admin panel. */
public record OpenAdminPayload(
		int baseWage,
		int periodDays,
		long price,
		int minimumDaily,
		int gracePeriods,
		int dismissAfterMissed,
		int missedMoodPenalty,
		int checkTicks,
		boolean enabled,
		boolean allowDebt,
		boolean notifyOwner,
		boolean useSchedule,
		boolean pauseOffDuty,
		boolean useSalaryTable,
		boolean useRankMultiplier,
		boolean useRarityMultiplier,
		boolean useBoostUpkeep,
		boolean bubbleThroughWalls,
		double boostCap,
		double[] rankMultipliers,
		double[] rarityMultipliers
) implements CustomPacketPayload {
	public static final Type<OpenAdminPayload> TYPE = new Type<>(LaborCraft.id("open_admin"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.baseWage());
				buffer.writeVarInt(payload.periodDays());
				buffer.writeVarLong(payload.price());
				buffer.writeVarInt(payload.minimumDaily());
				buffer.writeVarInt(payload.gracePeriods());
				buffer.writeVarInt(payload.dismissAfterMissed());
				buffer.writeVarInt(payload.missedMoodPenalty());
				buffer.writeVarInt(payload.checkTicks());
				buffer.writeBoolean(payload.enabled());
				buffer.writeBoolean(payload.allowDebt());
				buffer.writeBoolean(payload.notifyOwner());
				buffer.writeBoolean(payload.useSchedule());
				buffer.writeBoolean(payload.pauseOffDuty());
				buffer.writeBoolean(payload.useSalaryTable());
				buffer.writeBoolean(payload.useRankMultiplier());
				buffer.writeBoolean(payload.useRarityMultiplier());
				buffer.writeBoolean(payload.useBoostUpkeep());
				buffer.writeBoolean(payload.bubbleThroughWalls());
				buffer.writeDouble(payload.boostCap());
				writeMultipliers(buffer, payload.rankMultipliers());
				writeMultipliers(buffer, payload.rarityMultipliers());
			},
			buffer -> new OpenAdminPayload(
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarLong(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readDouble(),
					readMultipliers(buffer),
					readMultipliers(buffer)
			)
	);

	public static OpenAdminPayload of() {
		LaborCraftConfig config = LaborCraftConfig.get();
		BillingSettings billing = config.billing;
		return new OpenAdminPayload(
				config.farmer.baseWage,
				config.farmer.billingPeriodDays,
				config.acquisition.price,
				billing.minimumDaily,
				billing.gracePeriods,
				billing.dismissAfterMissed,
				billing.missedMoodPenalty,
				billing.checkTicks,
				billing.enabled,
				billing.allowDebt,
				billing.notifyOwner,
				billing.useSchedule,
				config.schedule.pauseBillingOffDuty,
				billing.useSalaryTable,
				billing.useRankMultiplier,
				billing.useRarityMultiplier,
				billing.useBoostUpkeep,
				config.speech.bubbleThroughWalls,
				billing.boostCap,
				multipliers(
						billing.rankMultipliers,
						new double[]{1.00D, 1.25D, 1.60D, 2.00D}
				),
				multipliers(
						billing.rarityMultipliers,
						new double[]{1.00D, 1.05D, 1.10D, 1.20D}
				)
		);
	}

	public static boolean open(ServerPlayer player) {
		if (player == null || !ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}
		ServerPlayNetworking.send(player, of());
		return true;
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	private static void writeMultipliers(RegistryFriendlyByteBuf buffer, double[] values) {
		buffer.writeVarInt(values.length);
		for (double value : values) {
			buffer.writeDouble(value);
		}
	}

	private static double[] readMultipliers(RegistryFriendlyByteBuf buffer) {
		int length = buffer.readVarInt();
		double[] values = new double[length];
		for (int index = 0; index < length; index++) {
			values[index] = buffer.readDouble();
		}
		return values;
	}

	private static double[] multipliers(double[] values, double[] fallback) {
		double[] result = fallback.clone();
		if (values != null) {
			for (int index = 0; index < Math.min(result.length, values.length); index++) {
				if (values[index] > 0.0D) {
					result[index] = values[index];
				}
			}
		}
		return result;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

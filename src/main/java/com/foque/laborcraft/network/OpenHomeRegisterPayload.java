package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.config.LaborCraftConfig;
import com.foque.laborcraft.housing.HomeRegistry;
import com.foque.laborcraft.housing.HousingReport;
import com.foque.laborcraft.housing.HousingValidator;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server preview for the house registration modal. The house is not claimed until the client
 * confirms through {@code /laborcraft home set ... <name>}.
 */
public record OpenHomeRegisterPayload(
		int x,
		int y,
		int z,
		boolean valid,
		boolean canRegister,
		String failure,
		int width,
		int depth,
		int doors,
		int beds,
		int slots,
		int capacity,
		String suggestedName,
		boolean claimed,
		boolean alreadyOwner,
		String ownerName,
		String existingName
) implements CustomPacketPayload {
	public static final Type<OpenHomeRegisterPayload> TYPE = new Type<>(LaborCraft.id("open_home_register"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenHomeRegisterPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.x());
				buffer.writeVarInt(payload.y());
				buffer.writeVarInt(payload.z());
				buffer.writeBoolean(payload.valid());
				buffer.writeBoolean(payload.canRegister());
				buffer.writeUtf(payload.failure(), 32);
				buffer.writeVarInt(payload.width());
				buffer.writeVarInt(payload.depth());
				buffer.writeVarInt(payload.doors());
				buffer.writeVarInt(payload.beds());
				buffer.writeVarInt(payload.slots());
				buffer.writeVarInt(payload.capacity());
				buffer.writeUtf(payload.suggestedName(), 48);
				buffer.writeBoolean(payload.claimed());
				buffer.writeBoolean(payload.alreadyOwner());
				buffer.writeUtf(payload.ownerName(), 64);
				buffer.writeUtf(payload.existingName(), 48);
			},
			buffer -> new OpenHomeRegisterPayload(
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readUtf(32),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readVarInt(),
					buffer.readUtf(48),
					buffer.readBoolean(),
					buffer.readBoolean(),
					buffer.readUtf(64),
					buffer.readUtf(48)
			)
	);

	public static boolean open(ServerPlayer player, ServerLevel level, BlockPos bedPos) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		BlockPos anchor = HousingValidator.canonicalBed(level, bedPos);
		HousingReport report = HousingValidator.validate(level, anchor, LaborCraftConfig.get());
		String dimension = level.dimension().identifier().toString();
		HomeRegistry.Home existing = HomeRegistry.get(level.getServer(), dimension, anchor);
		boolean claimed = existing != null;
		boolean alreadyOwner = existing != null && existing.isOwner(player.getUUID());
		boolean canRegister = report.valid() && (!claimed || (alreadyOwner && !existing.valid()));
		String suggested = existing != null && !existing.name().isBlank()
				? existing.name()
				: HomeRegistry.suggestedName(level.getServer(), player.getUUID());
		int slots = report.valid() ? Math.min(report.beds(), report.capacity()) : 0;

		String ownerName = "";
		if (claimed && !alreadyOwner && existing.owner() != null) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(existing.owner());
			ownerName = owner == null ? existing.owner().toString() : owner.getName().getString();
		}

		ServerPlayNetworking.send(player, new OpenHomeRegisterPayload(
				anchor.getX(),
				anchor.getY(),
				anchor.getZ(),
				report.valid(),
				canRegister,
				report.failureReason(),
				report.width(),
				report.depth(),
				report.doors(),
				report.beds(),
				slots,
				report.capacity(),
				suggested,
				claimed,
				alreadyOwner,
				ownerName,
				existing == null ? "" : existing.name()
		));
		return true;
	}

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

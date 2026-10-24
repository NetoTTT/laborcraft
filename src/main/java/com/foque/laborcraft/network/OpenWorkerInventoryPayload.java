package com.foque.laborcraft.network;

import com.foque.laborcraft.LaborCraft;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Worker backpack screen: nine general slots plus the tool slot. Sent again after every take/give
 * so the screen stays authoritative and the client does not have to simulate moves.
 */
public record OpenWorkerInventoryPayload(
		UUID workerId,
		String name,
		String jobId,
		List<ItemStack> slots,
		ItemStack tool
) implements CustomPacketPayload {
	public static final Type<OpenWorkerInventoryPayload> TYPE = new Type<>(LaborCraft.id("open_worker_inventory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenWorkerInventoryPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.workerId());
				buffer.writeUtf(payload.name(), 256);
				buffer.writeUtf(payload.jobId(), 128);
				for (int index = 0; index < LaborCraftWorker.STORAGE_SLOTS; index++) {
					ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.slots().get(index));
				}
				ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.tool());
			},
			buffer -> {
				UUID workerId = buffer.readUUID();
				String name = buffer.readUtf(256);
				String jobId = buffer.readUtf(128);
				List<ItemStack> slots = new ArrayList<>(LaborCraftWorker.STORAGE_SLOTS);
				for (int index = 0; index < LaborCraftWorker.STORAGE_SLOTS; index++) {
					slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
				}
				ItemStack tool = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
				return new OpenWorkerInventoryPayload(workerId, name, jobId, List.copyOf(slots), tool);
			}
	);

	public static OpenWorkerInventoryPayload of(LaborCraftWorker worker) {
		List<ItemStack> slots = new ArrayList<>(LaborCraftWorker.STORAGE_SLOTS);
		for (int index = 0; index < LaborCraftWorker.STORAGE_SLOTS; index++) {
			slots.add(worker.inventoryItem(index).copy());
		}
		return new OpenWorkerInventoryPayload(
				worker.getUUID(),
				worker.getName().getString(),
				worker.jobId(),
				List.copyOf(slots),
				worker.toolItem().copy()
		);
	}

	public static boolean open(ServerPlayer player, LaborCraftWorker worker) {
		if (!ServerPlayNetworking.canSend(player, TYPE)) {
			return false;
		}

		ServerPlayNetworking.send(player, of(worker));
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

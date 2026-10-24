package com.foque.laborcraft.item;

import com.foque.laborcraft.LaborCraft;
import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class LaborCraftItems {
	public static final ResourceKey<Item> WORKER_CONTRACT_KEY = ResourceKey.create(
			Registries.ITEM,
			LaborCraft.id("worker_contract")
	);

	public static final DataComponentType<UUID> CONTRACT_OWNER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			LaborCraft.id("contract_owner"),
			DataComponentType.<UUID>builder()
					.persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString))
					.build()
	);

	public static final DataComponentType<WorkerContractData> CONTRACT_DATA = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			LaborCraft.id("contract_data"),
			DataComponentType.<WorkerContractData>builder()
					.persistent(WorkerContractData.CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(WorkerContractData.CODEC))
					.build()
	);

	public static final Item WORKER_CONTRACT = new WorkerContractItem(new Item.Properties()
			.setId(WORKER_CONTRACT_KEY)
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON));

	public static final ResourceKey<Item> HOUSE_PLAN_KEY = ResourceKey.create(
			Registries.ITEM,
			LaborCraft.id("house_plan")
	);

	public static final Item HOUSE_PLAN = new HousePlanItem(new Item.Properties()
			.setId(HOUSE_PLAN_KEY)
			.stacksTo(1));

	public static final ResourceKey<Item> SURVEY_STAKE_KEY = ResourceKey.create(
			Registries.ITEM,
			LaborCraft.id("survey_stake")
	);

	public static final Item SURVEY_STAKE = new SurveyStakeItem(new Item.Properties()
			.setId(SURVEY_STAKE_KEY)
			.stacksTo(1));

	private LaborCraftItems() {
	}

	public static ItemStack newContract(UUID owner) {
		return newContract(owner, WorkerContractData.empty());
	}

	public static ItemStack newContract(UUID owner, WorkerContractData data) {
		ItemStack stack = new ItemStack(WORKER_CONTRACT);
		if (owner != null) {
			stack.set(CONTRACT_OWNER, owner);
		}

		stack.set(CONTRACT_DATA, data);
		if (!data.name().isBlank() || !data.legacyNameKey().isBlank()) {
			stack.set(
					DataComponents.CUSTOM_NAME,
					Component.translatable("laborcraft.item.worker_contract.named", data.displayName())
			);
		}

		return stack;
	}

	public static void bootstrap() {
		Registry.register(BuiltInRegistries.ITEM, LaborCraft.id("worker_contract"), WORKER_CONTRACT);
		Registry.register(BuiltInRegistries.ITEM, LaborCraft.id("house_plan"), HOUSE_PLAN);
		Registry.register(BuiltInRegistries.ITEM, LaborCraft.id("survey_stake"), SURVEY_STAKE);
	}
}
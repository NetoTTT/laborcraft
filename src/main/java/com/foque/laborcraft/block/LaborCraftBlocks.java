package com.foque.laborcraft.block;

import com.foque.laborcraft.LaborCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class LaborCraftBlocks {
	public static final ResourceKey<Block> WORKSTATION_KEY = ResourceKey.create(
			Registries.BLOCK,
			LaborCraft.id("workstation")
	);
	public static final ResourceKey<Item> WORKSTATION_ITEM_KEY = ResourceKey.create(
			Registries.ITEM,
			LaborCraft.id("workstation")
	);

	public static final Block WORKSTATION = new Block(BlockBehaviour.Properties.of()
			.setId(WORKSTATION_KEY)
			.mapColor(MapColor.WOOD)
			.strength(2.5F)
			.sound(SoundType.WOOD));

	public static final Item WORKSTATION_ITEM = new BlockItem(
			WORKSTATION,
			new Item.Properties().setId(WORKSTATION_ITEM_KEY).useBlockDescriptionPrefix()
	);

	private LaborCraftBlocks() {
	}

	public static void bootstrap() {
		Registry.register(BuiltInRegistries.BLOCK, LaborCraft.id("workstation"), WORKSTATION);
		Registry.register(BuiltInRegistries.ITEM, LaborCraft.id("workstation"), WORKSTATION_ITEM);
	}
}

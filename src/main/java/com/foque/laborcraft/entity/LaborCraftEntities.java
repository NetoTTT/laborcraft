package com.foque.laborcraft.entity;

import com.foque.laborcraft.LaborCraft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class LaborCraftEntities {
	public static final ResourceKey<EntityType<?>> WORKER_KEY = ResourceKey.create(
			Registries.ENTITY_TYPE,
			LaborCraft.id("worker")
	);

	public static final EntityType<LaborCraftWorker> WORKER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			LaborCraft.id("worker"),
			EntityType.Builder.of(LaborCraftWorker::new, MobCategory.CREATURE)
					.sized(0.6F, 1.95F)
					.clientTrackingRange(10)
					.build(WORKER_KEY)
	);

	private LaborCraftEntities() {
	}

	public static void bootstrap() {
		FabricDefaultAttributeRegistry.register(WORKER, LaborCraftWorker.createAttributes());
	}
}

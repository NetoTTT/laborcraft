package com.foque.laborcraft.client.render;

import com.foque.laborcraft.entity.LaborCraftWorker;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public final class LaborCraftWorkerRenderer
		extends MobRenderer<LaborCraftWorker, VillagerRenderState, VillagerModel> {
	private static final Identifier TEXTURE =
			Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

	public LaborCraftWorkerRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}
}

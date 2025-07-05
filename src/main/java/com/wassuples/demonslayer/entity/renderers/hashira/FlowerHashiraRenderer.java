package com.wassuples.demonslayer.entity.renderers.hashira;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.hashira.FlowerHashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FlowerHashiraRenderer extends HumanoidMobRenderer<FlowerHashiraEntity, HashiraModel<FlowerHashiraEntity>> {
    private static final ResourceLocation KANAE_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/kanae.png");

    public FlowerHashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(FlowerHashiraEntity pEntity) {
        return KANAE_TEXTURE;
    }
}

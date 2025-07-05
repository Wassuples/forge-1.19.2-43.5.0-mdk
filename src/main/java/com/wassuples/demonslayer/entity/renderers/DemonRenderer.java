package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.entity.models.DemonModel;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

// This is the base renderer for all generic demons.
public class DemonRenderer extends HumanoidMobRenderer<DemonEntity, DemonModel<DemonEntity>> {
    public DemonRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(DemonEntity pEntity) {
        // A default placeholder texture for demons that don't have a specific one.
        return new ResourceLocation("minecraft:textures/entity/zombie/zombie.png");
    }
}
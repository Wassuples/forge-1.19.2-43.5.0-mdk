package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.entity.models.HashiraModel;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Hashira entity.
 */
public class HashiraRenderer extends HumanoidMobRenderer<HashiraEntity, HashiraModel<HashiraEntity>> {
    public HashiraRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HashiraModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HashiraEntity pEntity) {
        return new ResourceLocation("minecraft:textures/entity/steve.png");
    }
}
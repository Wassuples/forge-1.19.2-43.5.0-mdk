package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.DemonModel;
import com.wassuples.demonslayer.entity.custom.demon.LowlyDemonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side GeckoLib renderer for Lowly Demon entity.
 */
public class LowlyDemonRenderer extends HumanoidMobRenderer<LowlyDemonEntity, DemonModel<LowlyDemonEntity>> {

    private static final ResourceLocation LOWLY_DEMON_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/lowly_demon01.png");

    public LowlyDemonRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(LowlyDemonEntity pEntity) {
        return LOWLY_DEMON_TEXTURE;
    }
}
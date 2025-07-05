package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.models.DemonModel;
import com.wassuples.demonslayer.entity.custom.demon.FodderDemonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FodderDemonRenderer extends HumanoidMobRenderer<FodderDemonEntity, DemonModel<FodderDemonEntity>> {

    private static final ResourceLocation FODDER_DEMON_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/fodder_demon01.png");

    public FodderDemonRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(FodderDemonEntity pEntity) {
        return FODDER_DEMON_TEXTURE;
    }
}
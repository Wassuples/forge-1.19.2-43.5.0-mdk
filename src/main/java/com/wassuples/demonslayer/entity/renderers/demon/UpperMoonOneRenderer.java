package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.demon.UpperMoonOneEntity;
import com.wassuples.demonslayer.entity.custom.demon.UpperMoonTwoEntity;
import com.wassuples.demonslayer.entity.models.DemonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UpperMoonOneRenderer extends HumanoidMobRenderer<UpperMoonOneEntity, DemonModel<UpperMoonOneEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_one.png");

    public UpperMoonOneRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(UpperMoonOneEntity pEntity) {
        return TEXTURE;
    }
}
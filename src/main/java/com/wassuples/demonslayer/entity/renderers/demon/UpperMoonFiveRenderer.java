package com.wassuples.demonslayer.entity.renderers.demon;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.demon.LowerMoonFourEntity;
import com.wassuples.demonslayer.entity.custom.demon.UpperMoonFiveEntity;
import com.wassuples.demonslayer.entity.models.DemonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UpperMoonFiveRenderer extends HumanoidMobRenderer<UpperMoonFiveEntity, DemonModel<UpperMoonFiveEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/upper_moon_five.png");

    public UpperMoonFiveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DemonModel<>(pContext.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(UpperMoonFiveEntity pEntity) {
        return TEXTURE;
    }
}
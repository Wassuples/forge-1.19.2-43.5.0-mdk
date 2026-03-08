package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Cold White Princess entity.
 */
public class ColdWhitePrincessModel extends AnimatedGeoModel<ColdWhitePrincessEntity> {
    @Override
    public ResourceLocation getModelResource(ColdWhitePrincessEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/cold_white_princess.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ColdWhitePrincessEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/cold_white_princess.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ColdWhitePrincessEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/cold_white_princess.animation.json");
    }
}
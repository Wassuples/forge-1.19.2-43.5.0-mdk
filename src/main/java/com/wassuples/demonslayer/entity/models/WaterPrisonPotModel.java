package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WaterPrisonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Water Prison Pot entity.
 */
public class WaterPrisonPotModel extends AnimatedGeoModel<WaterPrisonEntity> {
    @Override
    public ResourceLocation getModelResource(WaterPrisonEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/water_prison.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(WaterPrisonEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/water_prison.png");
    }
    @Override
    public ResourceLocation getAnimationResource(WaterPrisonEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/water_prison.animation.json");
    }
}
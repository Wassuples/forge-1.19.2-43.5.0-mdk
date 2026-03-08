package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.MotherEyeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Mother Eye entity.
 */
public class MotherEyeModel extends AnimatedGeoModel<MotherEyeEntity> {
    @Override
    public ResourceLocation getModelResource(MotherEyeEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/mother_eye.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MotherEyeEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/mother_eye.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MotherEyeEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/mother_eye.animation.json");
    }
}
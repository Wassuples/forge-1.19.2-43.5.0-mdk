package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.PoisonNeedleProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class PoisonNeedleModel extends AnimatedGeoModel<PoisonNeedleProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(PoisonNeedleProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/poison_needle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PoisonNeedleProjectileEntity object) {
        // Assumes your texture is named this. Change if necessary.
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/poison_needle.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PoisonNeedleProjectileEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/poison_needle.animation.json");
    }
}
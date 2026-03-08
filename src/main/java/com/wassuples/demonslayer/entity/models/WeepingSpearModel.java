package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.WeepingSpearProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Weeping Spear entity.
 */
public class WeepingSpearModel extends AnimatedGeoModel<WeepingSpearProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(WeepingSpearProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/weeping_spear.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(WeepingSpearProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/weeping_spear.png");
    }
    @Override
    public ResourceLocation getAnimationResource(WeepingSpearProjectileEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/weeping_spear.animation.json");
    }
}
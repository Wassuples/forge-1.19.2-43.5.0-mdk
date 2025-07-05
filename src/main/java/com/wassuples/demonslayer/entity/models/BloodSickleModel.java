package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BloodSickleProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class BloodSickleModel extends AnimatedGeoModel<BloodSickleProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(BloodSickleProjectileEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/blood_sickle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloodSickleProjectileEntity object) {
        // Using the white.png texture as requested.
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloodSickleProjectileEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/blood_sickle.animation.json");
    }
}
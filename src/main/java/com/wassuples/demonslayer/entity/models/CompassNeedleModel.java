package com.wassuples.demonslayer.entity.models; // Or your model package

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CompassNeedleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Compass Needle entity.
 */
public class CompassNeedleModel extends AnimatedGeoModel<CompassNeedleEntity> {
    @Override
    public ResourceLocation getModelResource(CompassNeedleEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/compass_needle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CompassNeedleEntity object) {
        // This should be your existing texture
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/effect/compass_needle.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CompassNeedleEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/compass_needle.animation.json");
    }
}
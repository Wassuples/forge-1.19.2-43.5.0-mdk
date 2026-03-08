package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ThrownNichirinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Thrown Nichirin entity.
 */
public class ThrownNichirinModel extends AnimatedGeoModel<ThrownNichirinEntity> {
    @Override
    public ResourceLocation getModelResource(ThrownNichirinEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/thrown_nichirin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ThrownNichirinEntity object) {
        // You will need to create this texture file
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ThrownNichirinEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/thrown_nichirin.animation.json");
    }
}
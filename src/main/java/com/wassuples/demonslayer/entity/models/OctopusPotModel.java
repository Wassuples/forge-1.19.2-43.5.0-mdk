package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.OctopusPotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class OctopusPotModel extends AnimatedGeoModel<OctopusPotEntity> {
    @Override
    public ResourceLocation getModelResource(OctopusPotEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/octopus_pot.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OctopusPotEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/octopus_pot.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OctopusPotEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/octopus_pot.animation.json");
    }
}
package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.KillerGoldfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class KillerGoldfishModel extends AnimatedGeoModel<KillerGoldfishEntity> {
    @Override
    public ResourceLocation getModelResource(KillerGoldfishEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/killer_fish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KillerGoldfishEntity object) {
        // Assumes your texture is named this. Change if necessary.
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KillerGoldfishEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/killer_fish.animation.json");
    }
}
package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CrowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class CrowModel extends AnimatedGeoModel<CrowEntity> {
    @Override
    public ResourceLocation getModelResource(CrowEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/crow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrowEntity object) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrowEntity animatable) {
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/crow.animation.json");
    }
}

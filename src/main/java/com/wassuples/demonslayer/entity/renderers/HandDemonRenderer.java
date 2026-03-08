package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * Client-side GeckoLib renderer for Hand Demon entity.
 */
public class HandDemonRenderer extends GeoEntityRenderer<HandDemonEntity> {
    public HandDemonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnimatedGeoModel<HandDemonEntity>() {
            @Override
            public ResourceLocation getModelResource(HandDemonEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/hand_demon.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(HandDemonEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
            }

            @Override
            public ResourceLocation getAnimationResource(HandDemonEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/hand_demon.animation.json");
            }
        });
        this.shadowRadius = 1.5F;
    }
}
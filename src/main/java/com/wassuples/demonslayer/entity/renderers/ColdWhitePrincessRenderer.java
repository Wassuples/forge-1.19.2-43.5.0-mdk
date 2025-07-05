package com.wassuples.demonslayer.entity.renderers;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ColdWhitePrincessEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class ColdWhitePrincessRenderer extends GeoEntityRenderer<ColdWhitePrincessEntity> {
    public ColdWhitePrincessRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(ColdWhitePrincessEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/cold_white_princess.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(ColdWhitePrincessEntity object) {
                // You will need to create this texture file
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/cold_white_princess.png");
            }

            @Override
            public ResourceLocation getAnimationResource(ColdWhitePrincessEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/cold_white_princess.animation.json");
            }
        });
        this.shadowRadius = 0.5F;
    }
}
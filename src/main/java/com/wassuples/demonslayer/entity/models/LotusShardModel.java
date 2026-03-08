package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.LotusShardProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib animated model for the Lotus Shard entity.
 */
public class LotusShardModel extends AnimatedGeoModel<LotusShardProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(LotusShardProjectileEntity object) {
        // This will point to the placeholder model file below
        return new ResourceLocation(DemonSlayer.MOD_ID, "geo/lotus_shard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LotusShardProjectileEntity object) {
        // You can create a simple 16x16 transparent texture for this
        return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LotusShardProjectileEntity animatable) {
        // This will point to the placeholder animation file below
        return new ResourceLocation(DemonSlayer.MOD_ID, "animations/lotus_shard.animation.json");
    }
}
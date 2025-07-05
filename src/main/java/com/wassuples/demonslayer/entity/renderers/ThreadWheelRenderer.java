package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ThreadWheelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

public class ThreadWheelRenderer extends GeoEntityRenderer<ThreadWheelEntity> {
    public ThreadWheelRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(ThreadWheelEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/thread_wheel.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(ThreadWheelEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
            }

            @Override
            public ResourceLocation getAnimationResource(ThreadWheelEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/thread_wheel.animation.json");
            }
        });
        this.shadowRadius = 0.0F;
    }

    @Override
    public RenderType getRenderType(ThreadWheelEntity animatable, float partialTicks, PoseStack stack, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
}
package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CuttingThreadCageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Cutting Thread Cage entity.
 */
public class CuttingThreadCageRenderer extends GeoEntityRenderer<CuttingThreadCageEntity> {
    public CuttingThreadCageRenderer(EntityRendererProvider.Context renderManager) {
        // The Model class is defined inside the renderer for simplicity
        super(renderManager, new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(CuttingThreadCageEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/cutting_thread_cage.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(CuttingThreadCageEntity object) {
                // A simple white texture works best for applying color effects or transparency
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
            }

            @Override
            public ResourceLocation getAnimationResource(CuttingThreadCageEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/cutting_thread_cage.animation.json");
            }
        });
        this.shadowRadius = 0.0F; // The cage should not cast a shadow
    }

    // This method handles rendering the entity, including its shrinking scale
    @Override
    public void render(CuttingThreadCageEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        float scale = entity.getScale();
        stack.pushPose();
        // The model itself is a 1x1 cube, so we scale it to its proper size here
        stack.scale(scale * 8.0F, scale * 8.0F, scale * 8.0F);
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }

    @Override
    public RenderType getRenderType(CuttingThreadCageEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return  RenderType.entityCutoutNoCull(textureLocation);
    }
}
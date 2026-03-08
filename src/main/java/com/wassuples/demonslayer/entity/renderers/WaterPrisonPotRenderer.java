package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.WaterPrisonEntity;
import com.wassuples.demonslayer.entity.models.WaterPrisonPotModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Water Prison Pot entity.
 */
public class WaterPrisonPotRenderer extends GeoEntityRenderer<WaterPrisonEntity> {
    public WaterPrisonPotRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WaterPrisonPotModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(WaterPrisonEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        // Use a translucent render type to make it look like water
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(WaterPrisonEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();

        float scale = 3.0F;
        stack.scale(scale, scale, scale);

        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }
}
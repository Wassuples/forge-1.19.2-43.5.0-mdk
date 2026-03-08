package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.entity.custom.MotherEyeEntity;
import com.wassuples.demonslayer.entity.models.MotherEyeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Mother Eye entity.
 */
public class MotherEyeRenderer extends GeoEntityRenderer<MotherEyeEntity> {
    public MotherEyeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MotherEyeModel());
        this.shadowRadius = 2.4f;
    }

    @Override
    public RenderType getRenderType(MotherEyeEntity animatable, float partialTicks, PoseStack stack,
                                    @Nullable MultiBufferSource renderTypeBuffer,
                                    @Nullable VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(MotherEyeEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();
        float scale = 3.0F;
        stack.scale(scale, scale, scale);

        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }
}
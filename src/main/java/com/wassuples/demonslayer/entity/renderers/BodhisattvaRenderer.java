package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * Client-side GeckoLib renderer for Bodhisattva entity.
 */
public class BodhisattvaRenderer extends GeoEntityRenderer<BodhisattvaEntity> {

    public BodhisattvaRenderer(EntityRendererProvider.Context renderManager) {
        // We define the model class inline here for simplicity
        super(renderManager, new AnimatedGeoModel<BodhisattvaEntity>() {
            @Override
            public ResourceLocation getModelResource(BodhisattvaEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/bodhisattva.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(BodhisattvaEntity object) {
                // You will need to create this texture file
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
            }

            @Override
            public ResourceLocation getAnimationResource(BodhisattvaEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/bodhisattva.animation.json");
            }
        });
        this.shadowRadius = 4.0F; // It's huge, so it needs a big shadow
    }

    // This makes the entity render with a semi-transparent, icy look
    @Override
    public RenderType getRenderType(BodhisattvaEntity animatable, float partialTicks, PoseStack stack, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    // We override the main render method to apply a color tint
    @Override
    public void render(BodhisattvaEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        // This is necessary to get the correct lighting from different angles
        stack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(-entityYaw));

        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
    }

    @Override
    public void renderEarly(BodhisattvaEntity animatable, PoseStack stackIn, float ticks, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        // This is where we can apply the color tint.
        // We will make it a cold, icy blue.
        super.renderEarly(animatable, stackIn, ticks, renderTypeBuffer, vertexBuilder, packedLightIn, packedOverlayIn, 0.7f, 0.9f, 1.0f, 0.85f);
    }
}
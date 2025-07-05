package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.CompassNeedleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class CompassNeedleRenderer extends GeoEntityRenderer<CompassNeedleEntity> {

    public CompassNeedleRenderer(EntityRendererProvider.Context renderManager) {
        // We define the model directly inside the renderer, just like the working example.
        super(renderManager, new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(CompassNeedleEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "geo/compass_needle.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(CompassNeedleEntity object) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "textures/effect/compass_needle.png");
            }

            @Override
            public ResourceLocation getAnimationResource(CompassNeedleEntity animatable) {
                return new ResourceLocation(DemonSlayer.MOD_ID, "animations/compass_needle.animation.json");
            }
        });
        this.shadowRadius = 0.0F; // The effect should not cast a shadow.
    }

    @Override
    public void render(CompassNeedleEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       net.minecraft.client.renderer.MultiBufferSource bufferIn, int packedLightIn) {
        stack.pushPose();

        float scale = 10.0F;
        stack.scale(scale, scale, scale);
        stack.mulPose(Vector3f.YP.rotationDegrees(entity.getYRot() + 180F));

        super.render(entity, 0, partialTicks, stack, bufferIn, packedLightIn);
        stack.popPose();
    }
}

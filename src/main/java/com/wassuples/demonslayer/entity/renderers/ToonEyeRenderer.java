package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A clean camera-facing cartoon eyeball that cannot suffer cube UV distortion. */
public class ToonEyeRenderer extends EntityRenderer<ToonEyeProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonEyeProjectileEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float pulse = 1.0f + 0.12f * (float) Math.sin((entity.tickCount + partialTick) * 0.8f);
        pose.scale(-pulse, -pulse, pulse);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF16121A, 0.42f, 0.34f, 0.00f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFFFFF8DD, 0.36f, 0.28f, 0.01f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF42CFF5, 0.16f, 0.23f, 0.02f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF101018, 0.08f, 0.15f, 0.03f);
        pose.translate(-0.025, -0.065, 0);
        ToonRenderHelper.ellipse(pose, buffers, 0xFFFFFFFF, 0.025f, 0.045f, 0.04f);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonEyeProjectileEntity entity) {
        return TEXTURE;
    }
}

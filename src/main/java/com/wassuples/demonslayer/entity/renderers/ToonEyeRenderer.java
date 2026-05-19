package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ToonEyeRenderer extends EntityRenderer<ToonEyeProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonEyeProjectileEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float wobble = 1.0f + Mth.sin((entity.tickCount + partialTicks) * 0.75f) * 0.08f;
        stack.scale(1.05f * wobble, 0.82f / wobble, 1.0f);

        ToonRenderHelper.quad(stack, buffer, 0xAA111111, -1.35f, -0.06f, -0.55f, 0.06f, -0.08f);
        ToonRenderHelper.quad(stack, buffer, 0xAA111111, -1.62f, 0.22f, -0.72f, 0.34f, -0.08f);
        ToonRenderHelper.quad(stack, buffer, 0xAA111111, -1.62f, -0.34f, -0.72f, -0.22f, -0.08f);

        ToonRenderHelper.ellipse(stack, buffer, 0xFF080808, 0.78f, 0.58f, -0.04f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFFFFFFFF, 0.68f, 0.50f, -0.03f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFF38C9FF, 0.30f, 0.32f, -0.02f);
        ToonRenderHelper.ellipse(stack, buffer, 0xFF050505, 0.15f, 0.18f, -0.01f);
        ToonRenderHelper.ellipse(stack, buffer, 0xEEFFFFFF, 0.055f, 0.07f, 0.0f);

        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonEyeProjectileEntity entity) {
        return TEXTURE;
    }
}

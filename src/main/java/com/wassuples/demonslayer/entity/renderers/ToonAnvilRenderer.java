package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ToonAnvilRenderer extends EntityRenderer<ToonAnvilEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonAnvilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
    }

    @Override
    public void render(ToonAnvilEntity entity, float entityYaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float squash = entity.getDeltaMovement().y < -0.25
                ? 1.0f + Mth.sin((entity.tickCount + partialTicks) * 0.9f) * 0.08f
                : 1.0f;
        stack.scale(1.45f * squash, 1.45f / squash, 1.45f);

        ToonRenderHelper.box(stack, buffer, 0xFF121212, -0.74f, -0.58f, -0.18f, 0.74f, -0.42f, 0.18f);
        ToonRenderHelper.box(stack, buffer, 0xFF6B6F76, -0.68f, -0.52f, -0.16f, 0.68f, -0.34f, 0.16f);
        ToonRenderHelper.box(stack, buffer, 0xFF0F0F0F, -0.45f, -0.36f, -0.15f, 0.45f, 0.18f, 0.15f);
        ToonRenderHelper.box(stack, buffer, 0xFF81858D, -0.38f, -0.32f, -0.13f, 0.38f, 0.16f, 0.13f);
        ToonRenderHelper.box(stack, buffer, 0xFF0F0F0F, -0.84f, 0.10f, -0.16f, 0.84f, 0.36f, 0.16f);
        ToonRenderHelper.box(stack, buffer, 0xFF8D929A, -0.78f, 0.16f, -0.14f, 0.78f, 0.34f, 0.14f);
        ToonRenderHelper.box(stack, buffer, 0xFF555A62, -0.92f, 0.20f, -0.13f, -0.58f, 0.30f, 0.13f);
        ToonRenderHelper.box(stack, buffer, 0xFF555A62, 0.58f, 0.20f, -0.13f, 0.92f, 0.30f, 0.13f);
        ToonRenderHelper.box(stack, buffer, 0xCCFFFFFF, -0.60f, 0.25f, -0.17f, 0.22f, 0.31f, -0.15f);

        stack.pushPose();
        stack.translate(0.0f, -0.16f, -0.18f);
        stack.scale(0.018f, -0.018f, 0.018f);
        ToonRenderHelper.text(this.getFont(), stack, buffer, Component.literal("ACME"), 0xFFE93636);
        stack.popPose();

        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonAnvilEntity entity) {
        return TEXTURE;
    }
}

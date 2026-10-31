package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonAnvilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A deliberately flat, camera-facing squash-and-stretch ACME prop rather than a vanilla anvil model. */
public class ToonAnvilRenderer extends EntityRenderer<ToonAnvilEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonAnvilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonAnvilEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.45, 0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float squash = entity.isOnGround() ? 1.25f : 1.0f + 0.08f * (float) Math.sin((entity.tickCount + partialTick) * 0.8f);
        pose.scale(-squash, -1.0f / squash, squash);

        // Thick ink outline, silver face, horn and pedestal; only 1/16 block deep visually.
        ToonRenderHelper.quad(pose, buffers, 0xFF111015, -1.22f, -0.72f, 1.22f, 0.72f, 0.02f);
        ToonRenderHelper.quad(pose, buffers, 0xFF7E91A6, -1.08f, -0.58f, 1.08f, 0.58f, 0.03f);
        ToonRenderHelper.quad(pose, buffers, 0xFF111015, -1.75f, -0.52f, 1.35f, -0.05f, 0.04f);
        ToonRenderHelper.quad(pose, buffers, 0xFFAFC0CE, -1.6f, -0.4f, 1.18f, -0.16f, 0.05f);
        ToonRenderHelper.quad(pose, buffers, 0xFF111015, -0.62f, 0.48f, 0.62f, 1.05f, 0.04f);
        ToonRenderHelper.quad(pose, buffers, 0xFF66798C, -0.49f, 0.51f, 0.49f, 0.92f, 0.05f);
        pose.translate(0, -0.15, 0.07);
        pose.scale(0.012f, 0.012f, 0.012f);
        ToonRenderHelper.text(this.getFont(), pose, buffers, Component.literal("ACME"), 0xFFFFE45C);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonAnvilEntity entity) {
        return TEXTURE;
    }
}

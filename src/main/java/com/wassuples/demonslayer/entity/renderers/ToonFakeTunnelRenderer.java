package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonFakeTunnelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A paper-thin painted tunnel: concentric ink rings make the lie readable at a glance. */
public class ToonFakeTunnelRenderer extends EntityRenderer<ToonFakeTunnelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonFakeTunnelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonFakeTunnelEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 1.45, 0);
        // Billboard keeps a paper-thin decal visible and avoids ever resembling a real portal.
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float pop = Math.min(1.0f, (entity.tickCount + partialTick) / 5.0f);
        pose.scale(-pop, -pop, pop);
        ToonRenderHelper.ellipse(pose, buffers, 0xFFF2E6C9, 1.18f, 1.55f, 0.00f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF16141B, 1.05f, 1.42f, 0.01f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF34303C, 0.76f, 1.10f, 0.02f);
        ToonRenderHelper.ellipse(pose, buffers, 0xFF08080A, 0.58f, 0.90f, 0.03f);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonFakeTunnelEntity entity) {
        return TEXTURE;
    }
}

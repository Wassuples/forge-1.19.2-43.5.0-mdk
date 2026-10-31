package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import static net.minecraft.client.gui.components.toasts.Toast.TEXTURE;

/** Uses the authored transparent peel texture directly, avoiding tint/depth artifacts. */
public class ToonBananaPeelRenderer extends EntityRenderer<ToonBananaPeelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/toon/toon_banana_peel.png");

    public ToonBananaPeelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.08f;
    }

    @Override
    public void render(ToonBananaPeelEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.12, 0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float wobble = 1.0f + 0.06f * (float) Math.sin((entity.tickCount + partialTick) * 0.55f);
        pose.scale(-wobble, -wobble, wobble);
        ToonRenderHelper.texturedQuad(pose, buffers, TEXTURE, -0.38f, -0.18f, 0.38f, 0.38f, 0.0f);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ToonBananaPeelEntity entity) {
        return TEXTURE;
    }
}

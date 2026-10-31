package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonShadowAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Vector3f;
import net.minecraft.world.phys.Vec3;

/** Renders a flat humanoid shadow that conforms to the floor/wall it strikes. */
public class ToonShadowAnchorRenderer extends EntityRenderer<ToonShadowAnchorEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    public ToonShadowAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonShadowAnchorEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (entity.isStuck()) {
            orientToSurface(pose, entity.getStuckNormal());
        } else {
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        }
        float wave = 0.12f * (float) Math.sin((entity.tickCount + partialTick) * 0.65f);
        float scale = entity.isStuck() ? 1.45f : 1.0f;
        pose.scale(-scale, -scale, scale);
        int ink = 0xDC08070C;
        ToonRenderHelper.ellipse(pose, buffers, ink, 0.34f, 0.35f, 0.01f); // head
        pose.translate(0, 0.55 + wave, 0);
        ToonRenderHelper.ellipse(pose, buffers, ink, 0.54f, 0.78f, 0.01f); // body
        ToonRenderHelper.quad(pose, buffers, ink, -1.12f, -0.15f, 1.12f, 0.18f, 0.01f); // stretched arms
        ToonRenderHelper.quad(pose, buffers, ink, -0.48f, 0.48f, -0.08f, 1.35f, 0.01f); // leg
        ToonRenderHelper.quad(pose, buffers, ink, 0.08f, 0.48f, 0.48f, 1.35f, 0.01f); // leg
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void orientToSurface(PoseStack pose, Vec3 normal) {
        if (normal.y > 0.5) {
            pose.mulPose(Vector3f.XP.rotationDegrees(90.0f));
        } else if (normal.y < -0.5) {
            pose.mulPose(Vector3f.XP.rotationDegrees(-90.0f));
        } else {
            float yaw = (float) Math.toDegrees(Math.atan2(normal.x, normal.z));
            pose.mulPose(Vector3f.YP.rotationDegrees(yaw));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonShadowAnchorEntity entity) {
        return TEXTURE;
    }
}

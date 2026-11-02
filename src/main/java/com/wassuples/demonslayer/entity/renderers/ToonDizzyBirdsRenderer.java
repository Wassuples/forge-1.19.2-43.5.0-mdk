package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.toon.ToonFallCurve;
import com.wassuples.demonslayer.entity.custom.ToonDizzyBirdsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Three little cartoon birds orbiting the victim's head. Follows the head down when a player is knocked flat. */
public class ToonDizzyBirdsRenderer extends EntityRenderer<ToonDizzyBirdsEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int[] BODY_COLORS = {0xFF4FB3FF, 0xFFFFD84A, 0xFFFF8FC8};
    private static final int OUTLINE = 0xFF1B1530;

    public ToonDizzyBirdsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(ToonDizzyBirdsEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        Entity target = entity.getTarget();
        if (target == null) return;

        float age = entity.tickCount + partialTick;
        float fadeIn = Mth.clamp(age / 6.0f, 0.0f, 1.0f);
        float fadeOut = Mth.clamp((entity.getDuration() - age) / 8.0f, 0.0f, 1.0f);
        float appear = Math.min(fadeIn, fadeOut);
        if (appear <= 0.0f) return;

        // Ring centre relative to this entity's render origin (which already sits at the target's head height).
        Vec3 centre = Vec3.ZERO;
        if (target instanceof Player player) {
            // Birds were spawned together with the knock-down, so their age is the animation time.
            float[][] keys = ToonFallCurve.TUNNEL_SLAM;
            if (age < ToonFallCurve.length(keys)) {
                float headTop = player.getBbHeight();
                float h = ToonFallCurve.pointHeight(keys, age, headTop);
                float back = ToonFallCurve.pointBackOffset(keys, age, headTop) * ToonFallCurve.BACK;
                float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
                Vec3 backDir = new Vec3(Mth.sin(bodyYaw), 0.0, -Mth.cos(bodyYaw)); // opposite of facing
                centre = backDir.scale(back).add(0.0, h - headTop, 0.0);
            }
        }

        Vector3f left = this.entityRenderDispatcher.camera.getLeftVector();
        float radius = target instanceof LivingEntity living ? Math.max(0.35f, living.getBbWidth() * 0.75f) : 0.4f;

        for (int i = 0; i < 3; i++) {
            float angle = age * 0.22f + i * (Mth.TWO_PI / 3.0f);
            float bob = Mth.sin(age * 0.5f + i * 2.0f) * 0.05f;
            double bx = centre.x + Mth.cos(angle) * radius;
            double bz = centre.z + Mth.sin(angle) * radius;
            double by = centre.y + 0.22 + bob;

            // Tangent of the orbit decides whether this bird faces left or right on screen.
            float tx = -Mth.sin(angle);
            float tz = Mth.cos(angle);
            boolean facingLeft = tx * left.x() + tz * left.z() > 0.0f;

            pose.pushPose();
            pose.translate(bx, by, bz);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float s = 0.012f * appear;
            pose.scale(facingLeft ? s : -s, -s, s);
            drawBird(pose, buffers, BODY_COLORS[i], age + i * 3.7f);
            pose.popPose();
        }
    }

    /** Bird in "pixel" units, Y down, facing +X (mirrored by the caller). */
    private static void drawBird(PoseStack pose, MultiBufferSource buffers, int body, float age) {
        float flap = Mth.sin(age * 1.6f);
        // outline pass (slightly bigger, behind)
        ellipseAt(pose, buffers, OUTLINE, 0, 0, 8.6f, 6.6f, 0.6f);
        ToonRenderHelper.disc(pose, buffers, OUTLINE, 6.5f, -4.5f, 5.4f, 14, 0.6f);
        // tail
        ToonRenderHelper.tri(pose, buffers, OUTLINE, -7.0f, -1.5f, -13.5f, -5.0f, -12.5f, 2.5f, 0.6f);
        ToonRenderHelper.tri(pose, buffers, body, -7.0f, -0.8f, -12.0f, -3.6f, -11.3f, 1.4f, 0.3f);
        // body + head
        ellipseAt(pose, buffers, body, 0, 0, 7.5f, 5.5f, 0.3f);
        ToonRenderHelper.disc(pose, buffers, body, 6.5f, -4.5f, 4.3f, 14, 0.3f);
        // belly highlight
        ellipseAt(pose, buffers, 0xFFFFFFFF, 1.0f, 2.0f, 4.0f, 2.2f, 0.2f);
        // beak
        ToonRenderHelper.tri(pose, buffers, OUTLINE, 10.0f, -6.2f, 15.2f, -4.0f, 10.0f, -2.4f, 0.25f);
        ToonRenderHelper.tri(pose, buffers, 0xFFFF9A1F, 10.4f, -5.6f, 14.2f, -4.0f, 10.4f, -3.0f, 0.1f);
        // dizzy eye: white with a spinning pupil
        ToonRenderHelper.disc(pose, buffers, 0xFFFFFFFF, 7.6f, -5.4f, 1.9f, 10, 0.1f);
        float pupil = age * 0.9f;
        ToonRenderHelper.disc(pose, buffers, OUTLINE, 7.6f + Mth.cos(pupil) * 0.8f, -5.4f + Mth.sin(pupil) * 0.8f, 0.9f, 8, 0.0f);
        // flapping wing (front)
        pose.pushPose();
        pose.translate(-1.0f, -1.0f, -0.2f);
        pose.scale(1.0f, 0.35f + 0.65f * Math.abs(flap), 1.0f);
        pose.translate(0.0f, flap > 0 ? -3.0f : 3.0f, 0.0f);
        ellipseAt(pose, buffers, OUTLINE, 0, 0, 4.8f, 3.6f, 0.1f);
        ellipseAt(pose, buffers, ToonRenderHelper.shade(body, 0.8f), 0, 0, 3.8f, 2.6f, 0.0f);
        pose.popPose();
    }

    private static void ellipseAt(PoseStack pose, MultiBufferSource buffers, int color, float x, float y,
                                  float rx, float ry, float z) {
        pose.pushPose();
        pose.translate(x, y, 0.0f);
        ToonRenderHelper.ellipse(pose, buffers, color, rx, ry, z);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ToonDizzyBirdsEntity entity) {
        return TEXTURE;
    }
}

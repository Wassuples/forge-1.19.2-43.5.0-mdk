package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonEyeProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A 3D cartoon eyeball: white sphere with red veins, a cyan iris with a dark ring, a big pupil and a glint,
 * outlined in ink, staring in the direction it flies and trailing a wiggly pink optic nerve. Squashes on bounces.
 *
 * <p>(The old version stacked flat ellipses with the black outline nearest the camera, so all you ever saw was
 * the outline: a black oval.)
 */
public class ToonEyeRenderer extends EntityRenderer<ToonEyeProjectileEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final float RADIUS = 0.22f;
    private static final int SCLERA = 0xFFFFFBF0;
    private static final int VEIN = 0xFFE0464E;
    private static final int IRIS = 0xFF3FC8F2;
    private static final int IRIS_DARK = 0xFF1C6FA8;
    private static final int PUPIL = 0xFF0E0D14;
    private static final int GLINT = 0xFFFFFFFF;
    private static final int NERVE = 0xFFF28CA5;
    private static final int NERVE_DARK = 0xFFB4516B;

    public ToonEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(ToonEyeProjectileEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-4) velocity = new Vec3(0, 0, 1);
        Vec3 dir = velocity.normalize();
        float flyYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float flyPitch = (float) (Math.asin(Mth.clamp(-dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);

        pose.pushPose();
        pose.translate(0.0, 0.25, 0.0);
        // Local +Z = direction of travel (the pupil faces +Z).
        pose.mulPose(Vector3f.YP.rotationDegrees(-flyYaw));
        pose.mulPose(Vector3f.XP.rotationDegrees(flyPitch));

        // Wobbly "jelly" squash along the travel axis.
        float wobble = 1.0f + 0.1f * Mth.sin(age * 1.3f);
        pose.scale(1.0f / (float) Math.sqrt(wobble), 1.0f / (float) Math.sqrt(wobble), wobble);

        drawNerve(pose, buffers, light, age);
        ToonRenderHelper.solidSphere(pose, buffers, light, RADIUS * 1.09f, 12, 18, (x, y, z) -> 0xFF0D0C12, true);
        float glance = Mth.sin(age * 0.45f) * 0.12f; // the pupil darts around a bit
        ToonRenderHelper.solidSphere(pose, buffers, light, RADIUS, 22, 32,
                (x, y, z) -> eyeColor(x + glance, y, z), false);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** Colour by angle from the +Z (forward) axis: pupil, iris ring, iris, sclera with veins, glint. */
    private static int eyeColor(float x, float y, float z) {
        // Glint: small spot up-left of the pupil.
        float gx = x + 0.22f;
        float gy = y - 0.28f;
        if (z > 0.8f && gx * gx + gy * gy < 0.012f) return GLINT;
        if (z > 0.93f) return PUPIL;
        if (z > 0.86f) return IRIS;
        if (z > 0.82f) return IRIS_DARK;
        // Veins: thin wavy lines creeping from the back toward the iris.
        if (z < 0.6f) {
            double angle = Math.atan2(y, x);
            double wave = Math.sin(angle * 7.0 + z * 9.0);
            if (Math.abs(wave) < 0.12 && z > -0.4f) return VEIN;
        }
        return SCLERA;
    }

    /** Two wiggling nerve strands trailing behind (local -Z). Flat strips, so no culling issues. */
    private static void drawNerve(PoseStack pose, MultiBufferSource buffers, int light, float age) {
        for (int strand = 0; strand < 2; strand++) {
            float offset = strand == 0 ? -0.035f : 0.035f;
            int segments = 8;
            for (int i = 0; i < segments; i++) {
                float t0 = (float) i / segments;
                float t1 = (float) (i + 1) / segments;
                float z0 = -RADIUS * 0.8f - t0 * 0.55f;
                float z1 = -RADIUS * 0.8f - t1 * 0.55f;
                float x0 = offset + Mth.sin(age * 0.9f + t0 * 6.0f + strand) * 0.06f * t0;
                float x1 = offset + Mth.sin(age * 0.9f + t1 * 6.0f + strand) * 0.06f * t1;
                float y0 = Mth.cos(age * 0.7f + t0 * 5.0f) * 0.04f * t0;
                float y1 = Mth.cos(age * 0.7f + t1 * 5.0f) * 0.04f * t1;
                float w0 = 0.03f * (1.0f - t0 * 0.7f);
                float w1 = 0.03f * (1.0f - t1 * 0.7f);
                int color = i % 2 == 0 ? NERVE : NERVE_DARK;
                // vertical ribbon + horizontal ribbon = reads as a tube from any angle
                ToonRenderHelper.quad3(pose, buffers, color, x0, y0 - w0, z0, x0, y0 + w0, z0, x1, y1 + w1, z1, x1, y1 - w1, z1, 1, 0, 0);
                ToonRenderHelper.quad3(pose, buffers, color, x0 - w0, y0, z0, x0 + w0, y0, z0, x1 + w1, y1, z1, x1 - w1, y1, z1, 0, 1, 0);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonEyeProjectileEntity entity) {
        return TEXTURE;
    }
}

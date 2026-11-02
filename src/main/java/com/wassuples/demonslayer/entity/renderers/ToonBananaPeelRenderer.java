package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.ToonBananaPeelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A proper cartoon banana peel, built in 3D: a stubby yellow core with a brown stem on top and four peel
 * flaps that droop from the core down onto the floor and curl up at their brown tips. Pale inner peel on
 * the underside, dark ink edge for the toon outline. Tumbles while thrown, lies flat once it lands.
 */
public class ToonBananaPeelRenderer extends EntityRenderer<ToonBananaPeelEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    private static final int PEEL = 0xFFFFDD2E;
    private static final int PEEL_SHADE = 0xFFE8B917;
    private static final int INNER = 0xFFFBF1C4;
    private static final int TIP = 0xFF5E3B12;
    private static final int SPOT = 0xFF7A4E1B;
    private static final int INK = 0xFF2B1C08;

    private static final int FLAPS = 4;
    private static final int SEGMENTS = 7;
    private static final float FLAP_LENGTH = 0.36f;
    private static final float CORE_HEIGHT = 0.16f;

    public ToonBananaPeelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.18f;
    }

    @Override
    public void render(ToonBananaPeelEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTick;
        boolean landed = entity.isNoGravity(); // set (and synced) when the peel sticks to a block

        pose.pushPose();
        // Each peel gets its own resting rotation so a pile of them doesn't look copy-pasted.
        pose.mulPose(Vector3f.YP.rotationDegrees((entity.getId() * 47) % 360));
        if (!landed) {
            pose.translate(0.0, 0.1, 0.0);
            pose.mulPose(Vector3f.XP.rotationDegrees(age * 31.0f));
            pose.mulPose(Vector3f.ZP.rotationDegrees(age * 17.0f));
            pose.translate(0.0, -0.1, 0.0);
        } else {
            // Idle "come at me" jiggle of the flaps.
            float squash = 1.0f + 0.04f * Mth.sin(age * 0.3f);
            pose.scale(1.0f, squash, 1.0f);
        }
        drawPeel(pose, buffers, age, landed);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void drawPeel(PoseStack pose, MultiBufferSource buffers, float age, boolean landed) {
        // Core: a tapered "stalk" the flaps hang from, with a brown stem nub on top.
        ToonRenderHelper.cylinder(pose, buffers, INK, 0.075f, 0.058f, 0.0f, CORE_HEIGHT + 0.004f, 10);
        ToonRenderHelper.cylinder(pose, buffers, PEEL_SHADE, 0.07f, 0.052f, 0.0f, CORE_HEIGHT, 10);
        ToonRenderHelper.discXZ(pose, buffers, PEEL, 0.052f, CORE_HEIGHT, 10);
        ToonRenderHelper.cylinder(pose, buffers, TIP, 0.026f, 0.02f, CORE_HEIGHT, CORE_HEIGHT + 0.06f, 8);
        ToonRenderHelper.discXZ(pose, buffers, INK, 0.02f, CORE_HEIGHT + 0.06f, 8);

        for (int f = 0; f < FLAPS; f++) {
            float angle = f * (Mth.TWO_PI / FLAPS) + 0.25f * Mth.sin(f * 2.3f);
            float lengthScale = 0.85f + 0.3f * ((f * 37) % 10) / 10.0f;
            float wiggle = landed ? 0.0f : 0.06f * Mth.sin(age * 0.8f + f);
            drawFlap(pose, buffers, angle, FLAP_LENGTH * lengthScale, wiggle);
        }
    }

    /** One peel flap: a tapered strip leaving the top of the core, drooping to the floor, curling up at the tip. */
    private static void drawFlap(PoseStack pose, MultiBufferSource buffers, float angle, float length, float wiggle) {
        float dirX = Mth.cos(angle);
        float dirZ = Mth.sin(angle);
        float sideX = -dirZ;
        float sideZ = dirX;

        float[] px = new float[SEGMENTS + 1];
        float[] py = new float[SEGMENTS + 1];
        float[] pz = new float[SEGMENTS + 1];
        float[] half = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            float reach = 0.045f + length * t;
            // Droop from the core top to the floor, then curl up over the last stretch.
            float droop = CORE_HEIGHT * (1.0f - t) * (1.0f - t);
            float curl = t > 0.72f ? (t - 0.72f) * (t - 0.72f) * 0.9f : 0.0f;
            px[i] = dirX * reach;
            pz[i] = dirZ * reach;
            py[i] = 0.012f + droop + curl + wiggle * t;
            // Widest a third of the way out, rounded to a point at the tip.
            half[i] = 0.05f * Mth.sin((float) Math.PI * Math.min(1.0f, t * 0.85f + 0.12f)) + 0.012f;
        }

        for (int i = 0; i < SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            int top = t >= 0.78f ? TIP : (i % 3 == 1 ? PEEL : PEEL_SHADE);
            float ax = px[i], ay = py[i], az = pz[i], bx = px[i + 1], by = py[i + 1], bz = pz[i + 1];
            float ha = half[i], hb = half[i + 1];

            // ink rim (slightly wider, just below) gives the cartoon outline from above
            float oa = ha + 0.012f;
            float ob = hb + 0.012f;
            ToonRenderHelper.quad3(pose, buffers, INK,
                    ax - sideX * oa, ay - 0.004f, az - sideZ * oa, ax + sideX * oa, ay - 0.004f, az + sideZ * oa,
                    bx + sideX * ob, by - 0.004f, bz + sideZ * ob, bx - sideX * ob, by - 0.004f, bz - sideZ * ob,
                    0, 1, 0);
            // outer (yellow) skin on top
            ToonRenderHelper.quad3(pose, buffers, top,
                    ax - sideX * ha, ay + 0.002f, az - sideZ * ha, ax + sideX * ha, ay + 0.002f, az + sideZ * ha,
                    bx + sideX * hb, by + 0.002f, bz + sideZ * hb, bx - sideX * hb, by + 0.002f, bz - sideZ * hb,
                    0, 1, 0);
            // pale inner peel underneath (visible where the flap lifts off the floor / curls)
            ToonRenderHelper.quad3(pose, buffers, INNER,
                    ax - sideX * ha, ay - 0.002f, az - sideZ * ha, bx - sideX * hb, by - 0.002f, bz - sideZ * hb,
                    bx + sideX * hb, by - 0.002f, bz + sideZ * hb, ax + sideX * ha, ay - 0.002f, az + sideZ * ha,
                    0, -1, 0);
            // a couple of ripeness spots
            if (i == 3) {
                float sx = (ax + bx) * 0.5f + sideX * ha * 0.35f;
                float sz = (az + bz) * 0.5f + sideZ * ha * 0.35f;
                float sy = (ay + by) * 0.5f + 0.004f;
                float r = 0.012f;
                ToonRenderHelper.quad3(pose, buffers, SPOT,
                        sx - r, sy, sz - r, sx + r, sy, sz - r, sx + r, sy, sz + r, sx - r, sy, sz + r, 0, 1, 0);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ToonBananaPeelEntity entity) {
        return TEXTURE;
    }
}

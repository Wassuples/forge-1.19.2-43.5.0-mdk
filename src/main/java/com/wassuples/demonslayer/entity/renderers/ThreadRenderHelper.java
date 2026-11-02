package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Rui's threads: razor-thin, taut, glowing lines. Every thread is drawn twice as camera-facing ribbons with
 * additive blending: a wide, faint coloured glow and a thin, bright core. Additive light means crossings
 * get brighter, which is what sells "steel wire catching the moonlight".
 *
 * <p>All positions are relative to the entity's render origin; {@code cam} is the camera in the same space.
 */
final class ThreadRenderHelper {
    /** Rui's normal threads: white core, faint cold glow. */
    static final int WHITE_CORE = 0xF0FFFFFF;
    static final int WHITE_GLOW = 0x40B8D4FF;
    /** Blood-hardened threads (Eye Basket, contracting cages): pale pink core, crimson glow. */
    static final int BLOOD_CORE = 0xF0FFE0E6;
    static final int BLOOD_GLOW = 0x55FF1E3C;

    static final float CORE_WIDTH = 0.022f;
    static final float GLOW_WIDTH = 0.11f;

    private ThreadRenderHelper() {
    }

    static VertexConsumer buffer(MultiBufferSource buffers) {
        // Additive, unlit, full bright: exactly what a glowing wire needs.
        return buffers.getBuffer(RenderType.lightning());
    }

    /** One straight thread segment with glow + core. */
    static void thread(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 a, Vec3 b, int core, int glow, float brightness) {
        ribbon(pose, vc, cam, a, b, GLOW_WIDTH, scaleAlpha(glow, brightness));
        ribbon(pose, vc, cam, a, b, CORE_WIDTH, scaleAlpha(core, brightness));
    }

    /** A thread through several points (curves, sags, spirals). */
    static void polyline(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3[] points, int count, int core, int glow, float brightness) {
        for (int i = 0; i < count - 1; i++) {
            thread(pose, vc, cam, points[i], points[i + 1], core, glow, brightness);
        }
    }

    /** Camera-facing quad from a to b, {@code width} wide. Emitted in both windings so culling can't eat it. */
    static void ribbon(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 a, Vec3 b, float width, int argb) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-8) return;
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = dir.cross(cam.subtract(mid));
        if (side.lengthSqr() < 1.0E-8) return;
        side = side.normalize().scale(width * 0.5);

        Matrix4f m = pose.last().pose();
        int alpha = argb >>> 24 & 255;
        int r = argb >>> 16 & 255;
        int g = argb >>> 8 & 255;
        int bl = argb & 255;
        float ax0 = (float) (a.x - side.x), ay0 = (float) (a.y - side.y), az0 = (float) (a.z - side.z);
        float ax1 = (float) (a.x + side.x), ay1 = (float) (a.y + side.y), az1 = (float) (a.z + side.z);
        float bx0 = (float) (b.x - side.x), by0 = (float) (b.y - side.y), bz0 = (float) (b.z - side.z);
        float bx1 = (float) (b.x + side.x), by1 = (float) (b.y + side.y), bz1 = (float) (b.z + side.z);

        vc.vertex(m, ax0, ay0, az0).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, ax1, ay1, az1).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, bx1, by1, bz1).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, bx0, by0, bz0).color(r, g, bl, alpha).endVertex();

        vc.vertex(m, bx0, by0, bz0).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, bx1, by1, bz1).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, ax1, ay1, az1).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, ax0, ay0, az0).color(r, g, bl, alpha).endVertex();
    }

    static int scaleAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, (int) ((argb >>> 24 & 255) * factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Blend two ARGB colours (t = 0 → a, 1 → b). */
    static int mix(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int aa = a >>> 24 & 255, ar = a >>> 16 & 255, ag = a >>> 8 & 255, ab = a & 255;
        int ba = b >>> 24 & 255, br = b >>> 16 & 255, bg = b >>> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Point on a dome (half spheroid) of horizontal radius {@code r} and height {@code h}; t 0 = apex, 1 = ground. */
    static Vec3 dome(double r, double h, double theta, double t) {
        double phi = t * Math.PI / 2.0;
        double ring = Math.sin(phi) * r;
        return new Vec3(Math.cos(theta) * ring, Math.cos(phi) * h, Math.sin(theta) * ring);
    }
}

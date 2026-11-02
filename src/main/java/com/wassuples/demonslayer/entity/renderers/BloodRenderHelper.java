package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Gyutaro's blood blades. A blade is a crescent (like the blade of his kama) drawn in the local XY plane with
 * its cutting edge on the left (-X) and its tips at ±Y:
 * <ul>
 *   <li>a dark, slightly translucent clotted-blood body,</li>
 *   <li>a bright arterial-red band along the cutting edge,</li>
 *   <li>an additive glow halo and a hot pink-white highlight line on the edge, so it reads at night and in motion.</li>
 * </ul>
 * Everything is double-sided, so the caller only has to orient the plane.
 */
final class BloodRenderHelper {
    static final int BODY = 0xE65A0A14;
    static final int EDGE = 0xFFB3121F;
    static final int GLOW = 0x7AFF1626;
    static final int HIGHLIGHT = 0xD8FFB4BE;

    private static final int SEGMENTS = 14;
    /** Inner arc circle: centre (C*R, 0), radius K*R, chosen so both arcs meet at the tips (0, ±R). */
    private static final float C = 0.45f;
    private static final float K = (float) Math.sqrt(C * C + 1.0f);

    private BloodRenderHelper() {
    }

    /** Draws one crescent blade of outer radius {@code r}. {@code brightness} scales glow/alpha (0..1+). */
    static void crescent(PoseStack pose, MultiBufferSource buffers, float r, float brightness) {
        float[] ox = new float[SEGMENTS + 1];
        float[] oy = new float[SEGMENTS + 1];
        float[] ix = new float[SEGMENTS + 1];
        float[] iy = new float[SEGMENTS + 1];
        float[] ex = new float[SEGMENTS + 1]; // inner edge of the bright cutting band
        for (int i = 0; i <= SEGMENTS; i++) {
            double phi = Math.PI / 2.0 + Math.PI * i / SEGMENTS; // top → left → bottom
            float y = (float) Math.sin(phi) * r;
            ox[i] = (float) Math.cos(phi) * r;
            oy[i] = y;
            ix[i] = C * r - (float) Math.sqrt(Math.max(0.0, K * K * r * r - y * y));
            iy[i] = y;
            ex[i] = ox[i] + (ix[i] - ox[i]) * 0.35f;
        }

        int body = ToonRenderHelper.withAlpha(BODY, Math.min(1.0f, brightness));
        int edge = ToonRenderHelper.withAlpha(EDGE, Math.min(1.0f, brightness));
        for (int i = 0; i < SEGMENTS; i++) {
            // body (between the edge band and the inner arc)
            ToonRenderHelper.quad3(pose, buffers, body,
                    ex[i], oy[i], 0, ex[i + 1], oy[i + 1], 0, ix[i + 1], iy[i + 1], 0, ix[i], iy[i], 0, 0, 0, 1);
            // cutting band
            ToonRenderHelper.quad3(pose, buffers, edge,
                    ox[i], oy[i], -0.002f, ox[i + 1], oy[i + 1], -0.002f, ex[i + 1], oy[i + 1], -0.002f, ex[i], oy[i], -0.002f, 0, 0, 1);
        }

        // Additive glow halo outside the edge (fades outward) and a hot highlight line on it.
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        int glowA = (int) Math.min(255, (GLOW >>> 24) * brightness);
        int hiA = (int) Math.min(255, (HIGHLIGHT >>> 24) * brightness);
        for (int i = 0; i < SEGMENTS; i++) {
            float sx0 = ox[i] * 1.18f, sy0 = oy[i] * 1.05f;
            float sx1 = ox[i + 1] * 1.18f, sy1 = oy[i + 1] * 1.05f;
            // taper the glow toward the tips
            float t0 = (float) Math.sin(Math.PI * i / SEGMENTS);
            float t1 = (float) Math.sin(Math.PI * (i + 1) / SEGMENTS);
            additiveQuad(glow, m, ox[i], oy[i], ox[i + 1], oy[i + 1], sx1, sy1, sx0, sy0,
                    GLOW, (int) (glowA * t0), (int) (glowA * t1), 0, 0);
            float hx0 = ox[i] + (ex[i] - ox[i]) * 0.25f;
            float hx1 = ox[i + 1] + (ex[i + 1] - ox[i + 1]) * 0.25f;
            additiveQuad(glow, m, ox[i], oy[i], ox[i + 1], oy[i + 1], hx1, oy[i + 1], hx0, oy[i],
                    HIGHLIGHT, (int) (hiA * t0), (int) (hiA * t1), (int) (hiA * t1 * 0.3f), (int) (hiA * t0 * 0.3f));
        }
    }

    /**
     * Additive streak behind a moving blade, in the local XZ plane from z=0 back to z=-length, {@code width} wide
     * at the blade and tapering to nothing.
     */
    static void streak(PoseStack pose, MultiBufferSource buffers, float width, float length, float brightness) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        int a = (int) Math.min(255, 0x90 * brightness);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps;
            float t1 = (float) (i + 1) / steps;
            float w0 = width * (1.0f - t0);
            float w1 = width * (1.0f - t1);
            int a0 = (int) (a * (1.0f - t0));
            int a1 = (int) (a * (1.0f - t1));
            float z0 = -length * t0;
            float z1 = -length * t1;
            int c = 0xFF1A2A;
            emit(vc, m, -w0, 0, z0, c, a0);
            emit(vc, m, w0, 0, z0, c, a0);
            emit(vc, m, w1, 0, z1, c, a1);
            emit(vc, m, -w1, 0, z1, c, a1);
            emit(vc, m, -w1, 0, z1, c, a1);
            emit(vc, m, w1, 0, z1, c, a1);
            emit(vc, m, w0, 0, z0, c, a0);
            emit(vc, m, -w0, 0, z0, c, a0);
        }
    }

    /** Additive quad (both windings) in the XY plane with per-corner alpha. */
    static void additiveQuad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2,
                             float x3, float y3, int rgb, int a0, int a1, int a2, int a3) {
        emit(vc, m, x0, y0, 0, rgb, a0);
        emit(vc, m, x1, y1, 0, rgb, a1);
        emit(vc, m, x2, y2, 0, rgb, a2);
        emit(vc, m, x3, y3, 0, rgb, a3);
        emit(vc, m, x3, y3, 0, rgb, a3);
        emit(vc, m, x2, y2, 0, rgb, a2);
        emit(vc, m, x1, y1, 0, rgb, a1);
        emit(vc, m, x0, y0, 0, rgb, a0);
    }

    private static void emit(VertexConsumer vc, Matrix4f m, float x, float y, float z, int rgb, int alpha) {
        vc.vertex(m, x, y, z).color(rgb >>> 16 & 255, rgb >>> 8 & 255, rgb & 255, Math.max(0, Math.min(255, alpha))).endVertex();
    }
}

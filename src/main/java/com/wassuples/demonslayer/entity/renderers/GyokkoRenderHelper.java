package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Building blocks for Gyokko's art, which is all about his treasured vases and the grotesque sea life that comes
 * out of them: a porcelain vase with a wavy blue-and-gold glaze, a wobbling water bubble, octopus tentacles with
 * suckers, and his lipped goldfish.
 */
final class GyokkoRenderHelper {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");

    static final int PORCELAIN = 0xFFF3F1EA;
    static final int GLAZE = 0xFF2A5FA8;
    static final int GLAZE_DARK = 0xFF173A6E;
    static final int GOLD = 0xFFD9AE4A;

    // Vase profile (height, radius), bottom to lip. ~1.2 blocks tall.
    private static final float[] VASE_H = {0.0f, 0.06f, 0.25f, 0.45f, 0.65f, 0.82f, 0.95f, 1.05f, 1.15f, 1.2f};
    private static final float[] VASE_R = {0.22f, 0.26f, 0.38f, 0.43f, 0.39f, 0.27f, 0.17f, 0.18f, 0.27f, 0.25f};

    interface LatheColor {
        int at(int ring, double angle);
    }

    private GyokkoRenderHelper() {
    }

    /** Solid surface of revolution around Y (outward-facing, culled, lit). */
    static void solidLathe(PoseStack pose, MultiBufferSource buffers, int light, float[] heights, float[] radii,
                           int segments, LatheColor colors) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int j = 0; j < heights.length - 1; j++) {
            float y0 = heights[j], y1 = heights[j + 1];
            float r0 = radii[j], r1 = radii[j + 1];
            float slope = (r0 - r1) / Math.max(1.0E-4f, y1 - y0);
            for (int s = 0; s < segments; s++) {
                double a0 = Math.PI * 2.0 * s / segments;
                double a1 = Math.PI * 2.0 * (s + 1) / segments;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                int color = colors.at(j, (a0 + a1) * 0.5);
                float nx = (c0 + c1) * 0.5f, nz = (s0 + s1) * 0.5f;
                float len = Mth.sqrt(nx * nx + slope * slope + nz * nz);
                // CCW seen from outside: (a0,low) (a0,high) (a1,high) (a1,low)
                v(vc, m, n, c0 * r0, y0, s0 * r0, color, light, nx / len, slope / len, nz / len);
                v(vc, m, n, c0 * r1, y1, s0 * r1, color, light, nx / len, slope / len, nz / len);
                v(vc, m, n, c1 * r1, y1, s1 * r1, color, light, nx / len, slope / len, nz / len);
                v(vc, m, n, c1 * r0, y0, s1 * r0, color, light, nx / len, slope / len, nz / len);
            }
        }
    }

    /** Gyokko's vase, standing on its base at the current origin. {@code wobble} squashes it when something bursts out. */
    static void vase(PoseStack pose, MultiBufferSource buffers, int light, float wobble) {
        pose.pushPose();
        pose.scale(1.0f + wobble * 0.08f, 1.0f - wobble * 0.06f, 1.0f + wobble * 0.08f);
        solidLathe(pose, buffers, light, VASE_H, VASE_R, 24, (ring, angle) -> {
            if (ring == 0 || ring >= VASE_H.length - 3) return GOLD;                    // foot and lip
            if (ring == 3 || ring == 4) {                                               // wave band on the belly
                double wave = Math.sin(angle * 6.0) * 0.5 + 0.5;
                return wave > 0.55 ? GLAZE : (wave > 0.35 ? GLAZE_DARK : PORCELAIN);
            }
            if (ring == 5) return ((int) (angle / (Math.PI / 12.0)) % 2 == 0) ? GLAZE_DARK : PORCELAIN; // shoulder checks
            return PORCELAIN;
        });
        // Dark mouth so you can't see "inside" the shell.
        ToonRenderHelper.discXZ(pose, buffers, 0xFF0B0A12, VASE_R[VASE_R.length - 1] * 0.92f, VASE_H[VASE_H.length - 1] - 0.02f, 16);
        pose.popPose();
    }

    static float vaseMouthHeight() {
        return VASE_H[VASE_H.length - 1];
    }

    /**
     * Translucent wobbling sphere of water (double-sided, unlit), with a brighter rim and a specular glint.
     * Colour alpha controls opacity.
     */
    static void waterBubble(PoseStack pose, MultiBufferSource buffers, float radius, float age, int color) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int rings = 14;
        int segments = 22;
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segments; j++) {
                Vec3 a = bubblePoint(radius, age, i, j, rings, segments);
                Vec3 b = bubblePoint(radius, age, i, j + 1, rings, segments);
                Vec3 c = bubblePoint(radius, age, i + 1, j + 1, rings, segments);
                Vec3 d = bubblePoint(radius, age, i + 1, j, rings, segments);
                // Fresnel-ish: brighter/more opaque toward the poles' rim band.
                float lat = (float) Math.abs(Math.cos(Math.PI * (i + 0.5) / rings));
                int col = ToonRenderHelper.withAlpha(color, 0.7f + 0.5f * lat);
                quadRaw(vc, m, n, a, b, c, d, col);
            }
        }
    }

    private static Vec3 bubblePoint(float r, float age, int i, int j, int rings, int segments) {
        double theta = Math.PI * i / rings;
        double phi = Math.PI * 2.0 * j / segments;
        double wob = 1.0 + 0.05 * Math.sin(phi * 3.0 + age * 0.25) * Math.sin(theta * 4.0 + age * 0.18);
        double rr = r * wob;
        return new Vec3(Math.sin(theta) * Math.cos(phi) * rr, Math.cos(theta) * rr, Math.sin(theta) * Math.sin(phi) * rr);
    }

    /**
     * Tapered tube along {@code points} (radius from {@code r0} at the root to {@code r1} at the tip), coloured
     * top/bottom so suckers show on the underside. Unlit, double-sided.
     */
    static void tentacle(PoseStack pose, MultiBufferSource buffers, Vec3[] points, float r0, float r1, int top, int under, int sucker) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int sides = 8;
        Vec3[] prevRing = null;
        for (int i = 0; i < points.length; i++) {
            float t = (float) i / (points.length - 1);
            float r = Mth.lerp(t, r0, r1);
            Vec3 tangent = (i < points.length - 1 ? points[i + 1] : points[i]).subtract(i > 0 ? points[i - 1] : points[i]);
            if (tangent.lengthSqr() < 1.0E-8) tangent = new Vec3(0, 1, 0);
            tangent = tangent.normalize();
            Vec3 a = tangent.cross(new Vec3(0, 1, 0));
            if (a.lengthSqr() < 1.0E-4) a = tangent.cross(new Vec3(1, 0, 0));
            a = a.normalize();
            Vec3 b = tangent.cross(a).normalize(); // roughly "down" for a horizontal tentacle
            Vec3[] ring = new Vec3[sides];
            for (int s = 0; s < sides; s++) {
                double ang = Math.PI * 2.0 * s / sides;
                ring[s] = points[i].add(a.scale(Math.cos(ang) * r)).add(b.scale(Math.sin(ang) * r));
            }
            if (prevRing != null) {
                for (int s = 0; s < sides; s++) {
                    int s2 = (s + 1) % sides;
                    boolean underside = Math.sin(Math.PI * 2.0 * (s + 0.5) / sides) > 0.3;
                    int col = underside ? under : top;
                    quadRaw(vc, m, n, prevRing[s], prevRing[s2], ring[s2], ring[s], col);
                }
                // Suckers: little pale discs on the underside every other segment.
                if (i % 2 == 0 && i < points.length - 2) {
                    Vec3 centre = points[i].add(b.scale(r * 1.02));
                    float sr = r * 0.45f;
                    Vec3 p1 = centre.add(a.scale(sr)), p2 = centre.add(tangent.scale(sr)), p3 = centre.subtract(a.scale(sr)), p4 = centre.subtract(tangent.scale(sr));
                    quadRaw(vc, m, n, p1, p2, p3, p4, sucker);
                }
            }
            prevRing = ring;
        }
    }

    /**
     * One of Gyokko's goldfish, facing +Z with its belly down, about {@code size} blocks long: plump orange body
     * with a white belly, bulging eyes, pouting human lips, and a flowing translucent fan tail.
     */
    static void goldfish(PoseStack pose, MultiBufferSource buffers, int light, float size, float age, float mouthOpen) {
        pose.pushPose();
        pose.scale(size, size, size);
        // Body (slightly swaying)
        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(Mth.sin(age * 0.6f) * 6.0f));
        pose.scale(0.55f, 0.62f, 1.0f);
        ToonRenderHelper.solidSphere(pose, buffers, light, 0.5f, 12, 18, (x, y, z) -> {
            if (y < -0.35f) return 0xFFFBEFE3;                                       // belly
            double scale = Math.sin(z * 18.0) * Math.sin(Math.atan2(y, x) * 9.0);
            return scale > 0.6 ? 0xFFF7A23A : (y > 0.45f ? 0xFFD9431E : 0xFFF07A23); // scales, darker back
        }, false);
        pose.popPose();
        // Eyes
        for (int side = -1; side <= 1; side += 2) {
            final int s = side;
            pose.pushPose();
            pose.translate(s * 0.2f, 0.12f, 0.3f);
            // Pupils look forward and slightly outward.
            ToonRenderHelper.solidSphere(pose, buffers, light, 0.1f, 8, 10,
                    (x, y, z) -> z * 0.8f + x * s * 0.6f > 0.75f ? 0xFF120A0A : 0xFFFFF7E6, false);
            pose.popPose();
        }
        // Lips: two puffy rings
        pose.pushPose();
        pose.translate(0, -0.04f, 0.49f);
        float open = 0.04f + 0.06f * mouthOpen;
        ToonRenderHelper.solidBox(pose, buffers, 0xFFE0566E, light, -0.1f, open, -0.03f, 0.1f, open + 0.06f, 0.05f, 0.0f);
        ToonRenderHelper.solidBox(pose, buffers, 0xFFE0566E, light, -0.1f, -open - 0.06f, -0.03f, 0.1f, -open, 0.05f, 0.0f);
        ToonRenderHelper.solidBox(pose, buffers, 0xFF2A0810, light, -0.07f, -open, -0.02f, 0.07f, open, 0.03f, 0.0f);
        pose.popPose();
        // Fan tail and dorsal fin (translucent, flapping)
        float flap = Mth.sin(age * 0.9f) * 0.35f;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        int fin = 0xB8FF8C3A;
        int finTip = 0x80FFD8A8;
        for (int lobe = -1; lobe <= 1; lobe += 2) {
            Vec3 root = new Vec3(0, 0, -0.42);
            Vec3 tipA = new Vec3(flap + lobe * 0.05, lobe * 0.42, -0.95);
            Vec3 tipB = new Vec3(flap * 1.3, lobe * 0.12, -1.05);
            quadRaw(vc, m, nm, root, tipA, tipB, tipB, fin);
            quadRaw(vc, m, nm, tipA, tipA.add(0, lobe * 0.08, -0.08), tipB, tipB, finTip);
        }
        quadRaw(vc, m, nm, new Vec3(0, 0.28, 0.05), new Vec3(0, 0.55, -0.15), new Vec3(0, 0.3, -0.35), new Vec3(0, 0.25, -0.2), fin);
        pose.popPose();
    }

    static void quadRaw(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        v(vc, m, n, (float) a.x, (float) a.y, (float) a.z, color, 0xF000F0, 0, 1, 0);
        v(vc, m, n, (float) b.x, (float) b.y, (float) b.z, color, 0xF000F0, 0, 1, 0);
        v(vc, m, n, (float) c.x, (float) c.y, (float) c.z, color, 0xF000F0, 0, 1, 0);
        v(vc, m, n, (float) d.x, (float) d.y, (float) d.z, color, 0xF000F0, 0, 1, 0);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, int color, int light,
                          float nx, float ny, float nz) {
        vc.vertex(m, x, y, z)
                .color(color >>> 16 & 255, color >>> 8 & 255, color & 255, color >>> 24 & 255)
                .uv(0.5f, 0.5f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }
}

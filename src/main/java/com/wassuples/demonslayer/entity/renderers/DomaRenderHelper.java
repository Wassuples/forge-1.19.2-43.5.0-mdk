package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Building blocks for Doma's Cryokinesis: clear faceted ice (translucent, lit, with bright additive edges),
 * crystal lotus flowers, hexagonal ice spikes, billowing freezing mist (soft camera-facing puffs) and his pair of
 * golden iron fans.
 */
final class DomaRenderHelper {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final ResourceLocation PUFF = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/doma/frost_puff.png");

    static final int ICE = 0x8CCFEFFF;
    static final int ICE_DEEP = 0x9C8CC8F0;
    static final int ICE_PALE = 0xA8EAF8FF;
    static final int EDGE = 0xB0E8F8FF;
    static final int GOLD = 0xFFE3B54A;
    static final int GOLD_DARK = 0xFF9C6B1E;

    private DomaRenderHelper() {
    }

    // ------------------------------------------------------------------------------------------- ice solids

    /** Translucent, lit, culled quad with whichever winding faces away from {@code inside}. */
    static void iceQuad(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb, int light, Vec3 inside) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        if (normal.lengthSqr() < 1.0E-12) normal = c.subtract(a).cross(d.subtract(a));
        Vec3 centre = a.add(b).add(c).add(d).scale(0.25);
        boolean flip = normal.dot(centre.subtract(inside)) < 0;
        Vec3 nr = normal.lengthSqr() < 1.0E-12 ? new Vec3(0, 1, 0) : normal.normalize();
        if (flip) nr = nr.scale(-1);
        Vec3[] order = flip ? new Vec3[]{d, c, b, a} : new Vec3[]{a, b, c, d};
        for (Vec3 p : order) {
            vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                    .color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24)
                    .uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(n, (float) nr.x, (float) nr.y, (float) nr.z).endVertex();
        }
    }

    static VertexConsumer iceBuffer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityTranslucentCull(WHITE));
    }

    /**
     * Ice block from 8 corners (bit 0 = +x, bit 1 = +y, bit 2 = +z, as in ToonRenderHelper.solidHexa), with
     * additive bright edges along its 12 edges.
     */
    static void iceHexa(PoseStack pose, MultiBufferSource buffers, Vec3[] c, int argb, int light, Vec3 cam, float edge) {
        VertexConsumer vc = iceBuffer(buffers);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        Vec3 centre = Vec3.ZERO;
        for (Vec3 p : c) centre = centre.add(p);
        centre = centre.scale(1.0 / 8.0);
        int[][] faces = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] f : faces) {
            int shade = f == faces[3] ? argb : ThreadRenderHelper.mix(argb, ICE_DEEP, 0.3f);
            iceQuad(vc, m, n, c[f[0]], c[f[1]], c[f[2]], c[f[3]], shade, light, centre);
        }
        if (edge > 0.0f && cam != null) {
            VertexConsumer lines = ThreadRenderHelper.buffer(buffers);
            int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] e : edges) {
                ThreadRenderHelper.ribbon(pose, lines, cam, c[e[0]], c[e[1]], 0.05f, ThreadRenderHelper.scaleAlpha(EDGE, edge));
            }
        }
    }

    /** Box corners in a frame: centre + right*x + up*y + fwd*z with half extents, tapering to {@code topScale} at +y. */
    static Vec3[] frameBox(Vec3 centre, Vec3 right, Vec3 up, Vec3 fwd, double hx, double hy, double hz, double topScale) {
        Vec3[] c = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            boolean top = (i & 2) != 0;
            double s = top ? topScale : 1.0;
            double x = ((i & 1) != 0 ? hx : -hx) * s;
            double y = top ? hy : -hy;
            double z = ((i & 4) != 0 ? hz : -hz) * s;
            c[i] = centre.add(right.scale(x)).add(up.scale(y)).add(fwd.scale(z));
        }
        return c;
    }

    /** A segment of limb (box) from {@code a} to {@code b}, {@code r0}/{@code r1} half-thickness at each end. */
    static void iceLimb(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 b, double r0, double r1, int argb, int light, Vec3 cam) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-8) return;
        Vec3 up = axis.normalize();
        Vec3 right = up.cross(Math.abs(up.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 fwd = right.cross(up).normalize();
        Vec3[] c = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            boolean top = (i & 2) != 0;
            double r = top ? r1 : r0;
            Vec3 base = top ? b : a;
            c[i] = base.add(right.scale((i & 1) != 0 ? r : -r)).add(fwd.scale((i & 4) != 0 ? r : -r));
        }
        iceHexa(pose, buffers, c, argb, light, cam, 0.5f);
    }

    /** Hexagonal ice crystal spike from {@code base} along {@code dir}, {@code length} long, {@code radius} thick. */
    static void iceSpike(PoseStack pose, MultiBufferSource buffers, Vec3 base, Vec3 dir, double length, double radius, int argb, int light, Vec3 cam) {
        if (length <= 0.01) return;
        VertexConsumer vc = iceBuffer(buffers);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        Vec3 d = dir.normalize();
        Vec3 u = d.cross(Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = d.cross(u).normalize();
        Vec3 shoulder = base.add(d.scale(length * 0.72));
        Vec3 tip = base.add(d.scale(length));
        Vec3[] ring0 = new Vec3[6], ring1 = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0 * i / 6;
            Vec3 o = u.scale(Math.cos(a) * radius).add(v.scale(Math.sin(a) * radius));
            ring0[i] = base.add(o);
            ring1[i] = shoulder.add(o.scale(0.85));
        }
        Vec3 inside = base.add(d.scale(length * 0.4));
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            int col = i % 2 == 0 ? argb : ThreadRenderHelper.mix(argb, ICE_DEEP, 0.35f);
            iceQuad(vc, m, n, ring0[i], ring0[j], ring1[j], ring1[i], col, light, inside);
            iceQuad(vc, m, n, ring1[i], ring1[j], tip, tip, ThreadRenderHelper.mix(argb, ICE_PALE, 0.4f), light, inside);
        }
        if (cam != null) {
            VertexConsumer lines = ThreadRenderHelper.buffer(buffers);
            for (int i = 0; i < 6; i += 2) {
                ThreadRenderHelper.ribbon(pose, lines, cam, ring1[i], tip, 0.035f, ThreadRenderHelper.scaleAlpha(EDGE, 0.8f));
                ThreadRenderHelper.ribbon(pose, lines, cam, ring0[i], ring1[i], 0.035f, ThreadRenderHelper.scaleAlpha(EDGE, 0.5f));
            }
        }
    }

    // ------------------------------------------------------------------------------------------- lotus

    /**
     * Crystal lotus flower at {@code centre} facing up: three tiers of pointed ice petals (8, 8 and 5) that open
     * from a bud as {@code open} goes 0 -> 1, a glowing heart, {@code size} blocks across when open.
     */
    static void lotus(PoseStack pose, MultiBufferSource buffers, Vec3 centre, float size, float open, float spin, int light, Vec3 cam) {
        if (size <= 0.01f) return;
        VertexConsumer vc = iceBuffer(buffers);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int[] counts = {8, 8, 5};
        float[] lengths = {0.5f, 0.38f, 0.26f};
        float[] lifts = {0.35f, 0.75f, 1.15f};
        for (int tier = 0; tier < 3; tier++) {
            int count = counts[tier];
            float len = lengths[tier] * size;
            // closed: petals point straight up; open: they lean outward by tier
            float lean = Mth.lerp(open, 0.1f, lifts[tier]);
            for (int p = 0; p < count; p++) {
                double a = spin + Math.PI * 2.0 * (p + tier * 0.5) / count;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 side = new Vec3(-out.z, 0, out.x);
                Vec3 base = centre.add(out.scale(0.05 * size)).add(0, tier * 0.04 * size, 0);
                Vec3 dir = out.scale(Math.sin(lean)).add(0, Math.cos(lean), 0);
                Vec3 tip = base.add(dir.scale(len));
                Vec3 mid = base.add(dir.scale(len * 0.45)).add(0, 0.04 * size, 0);
                double w = len * 0.32;
                Vec3 l = mid.add(side.scale(w)), r = mid.subtract(side.scale(w));
                int col = tier == 0 ? ICE : (tier == 1 ? ICE_PALE : 0xB8F4FBFF);
                Vec3 inside = centre.add(0, -0.2 * size, 0);
                iceQuad(vc, m, n, base, l, tip, r, col, light, inside);
                iceQuad(vc, m, n, base, r, tip, l, ThreadRenderHelper.mix(col, ICE_DEEP, 0.4f), light, centre.add(dir.scale(len)).add(0, 1, 0));
            }
        }
        if (cam != null) {
            VertexConsumer lines = ThreadRenderHelper.buffer(buffers);
            ThreadRenderHelper.ribbon(pose, lines, cam, centre, centre.add(0, 0.25 * size, 0), 0.3f * size, ThreadRenderHelper.scaleAlpha(0x80CFF4FF, open));
            for (int p = 0; p < 8; p++) {
                double a = spin + Math.PI * 2.0 * p / 8;
                float lean = Mth.lerp(open, 0.1f, lifts[0]);
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 dir = out.scale(Math.sin(lean)).add(0, Math.cos(lean), 0);
                ThreadRenderHelper.ribbon(pose, lines, cam, centre, centre.add(dir.scale(lengths[0] * size)), 0.03f, ThreadRenderHelper.scaleAlpha(EDGE, 0.7f));
            }
        }
    }

    // ------------------------------------------------------------------------------------------- mist

    /**
     * A soft puff of freezing mist facing the camera at {@code at}, {@code size} blocks across, rotated by
     * {@code roll} radians, with the given alpha (0..1).
     */
    static void puff(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, float size, float roll, float alpha) {
        if (alpha <= 0.01f || size <= 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(PUFF));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 up = new Vec3(camera.getUpVector());
        float c = Mth.cos(roll), s = Mth.sin(roll);
        Vec3 a1 = left.scale(c).add(up.scale(s)).scale(size * 0.5);
        Vec3 a2 = up.scale(c).subtract(left.scale(s)).scale(size * 0.5);
        Vec3[] q = {at.add(a1).add(a2), at.subtract(a1).add(a2), at.subtract(a1).subtract(a2), at.add(a1).subtract(a2)};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        int al = (int) (255 * Mth.clamp(alpha, 0.0f, 1.0f));
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, (float) q[i].x, (float) q[i].y, (float) q[i].z).color(255, 255, 255, al).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0, 1, 0).endVertex();
        }
    }

    // ------------------------------------------------------------------------------------------- fans

    /**
     * One of Doma's golden iron fans, opened {@code open} (0 closed .. 1 fully spread over ~150°) in the local XY
     * plane, pivoting at the origin and spreading toward +Y; ribs, a gold leaf and a darker rim.
     */
    static void fan(PoseStack pose, MultiBufferSource buffers, float radius, float open, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float spread = Mth.lerp(open, 0.12f, 2.6f);
        int ribs = 10;
        float inner = radius * 0.32f;
        for (int i = 0; i < ribs; i++) {
            float a0 = -spread / 2 + spread * i / ribs;
            float a1 = -spread / 2 + spread * (i + 1) / ribs;
            float x0 = Mth.sin(a0), y0 = Mth.cos(a0), x1 = Mth.sin(a1), y1 = Mth.cos(a1);
            int leaf = i % 2 == 0 ? GOLD : ToonRenderHelper.shade(GOLD, 0.88f);
            fanVertex(vc, m, n, x0 * inner, y0 * inner, leaf, light);
            fanVertex(vc, m, n, x0 * radius, y0 * radius, leaf, light);
            fanVertex(vc, m, n, x1 * radius, y1 * radius, leaf, light);
            fanVertex(vc, m, n, x1 * inner, y1 * inner, leaf, light);
            // dark rim along the top edge
            float rimIn = radius * 0.93f;
            fanVertex(vc, m, n, x0 * rimIn, y0 * rimIn, GOLD_DARK, light);
            fanVertex(vc, m, n, x0 * radius * 1.01f, y0 * radius * 1.01f, GOLD_DARK, light);
            fanVertex(vc, m, n, x1 * radius * 1.01f, y1 * radius * 1.01f, GOLD_DARK, light);
            fanVertex(vc, m, n, x1 * rimIn, y1 * rimIn, GOLD_DARK, light);
        }
        // ribs (guards) from the pivot
        for (int i = 0; i <= ribs; i++) {
            float a = -spread / 2 + spread * i / ribs;
            float x = Mth.sin(a), y = Mth.cos(a);
            float w = (i == 0 || i == ribs) ? 0.035f : 0.012f;
            float px = Mth.cos(a) * w, py = -Mth.sin(a) * w;
            fanVertex(vc, m, n, px, py, GOLD_DARK, light);
            fanVertex(vc, m, n, x * radius + px, y * radius + py, GOLD_DARK, light);
            fanVertex(vc, m, n, x * radius - px, y * radius - py, GOLD_DARK, light);
            fanVertex(vc, m, n, -px, -py, GOLD_DARK, light);
        }
    }

    private static void fanVertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, int color, int light) {
        vc.vertex(m, x, y, 0.0f).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).uv(0.5f, 0.5f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
    }
}

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
 * Building blocks for Hantengu's art. Zohakuten (Hatred) fights with wooden dragons that grow out of the drums on
 * his back: thick bark-covered necks with ring-grooves and knots, horned wooden heads with glowing eyes and gaping
 * jaws, and a ring of five red-lacquered taiko drums, each painted with 憎 (hatred). Aizetsu (Sorrow) wields a
 * cross-bladed jumonji spear. Everything here is procedural geometry on the plain white texture.
 */
final class HantenguRenderHelper {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    static final int FULL_BRIGHT = 0xF000F0;

    static final int BARK = 0xFF6E4B2C;
    static final int BARK_DARK = 0xFF3F2817;
    static final int BARK_LIGHT = 0xFF94704A;
    static final int HEARTWOOD = 0xFFC9A06A;
    static final int EYE = 0xFFFFE27A;
    static final int MOUTH = 0xFF1E0B07;
    static final int TOOTH = 0xFFEDE3CC;
    static final int LACQUER = 0xFFA3231B;
    static final int LACQUER_DARK = 0xFF661410;
    static final int DRUM_SKIN = 0xFFEADFC4;
    static final int BRASS = 0xFFD7AE4E;

    private HantenguRenderHelper() {
    }

    // ------------------------------------------------------------------------------------------- wooden tube

    /**
     * Solid, lit, bark-covered tube through {@code pts} with the given radius at each point. Uses parallel-transport
     * frames so the bark doesn't twist, alternates groove rings every few segments and darkens knots so it reads as
     * a carved wooden dragon body rather than a pipe.
     */
    static void woodTube(PoseStack pose, MultiBufferSource buffers, int light, Vec3[] pts, float[] radii, int sides, int seed) {
        int n = pts.length;
        if (n < 2) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        Vec3[][] rings = new Vec3[n][sides];
        Vec3 normal = null;
        for (int i = 0; i < n; i++) {
            Vec3 tangent = pts[Math.min(n - 1, i + 1)].subtract(pts[Math.max(0, i - 1)]);
            if (tangent.lengthSqr() < 1.0E-10) tangent = new Vec3(0, 1, 0);
            tangent = tangent.normalize();
            if (normal == null) {
                normal = tangent.cross(Math.abs(tangent.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0));
            }
            normal = normal.subtract(tangent.scale(normal.dot(tangent)));
            if (normal.lengthSqr() < 1.0E-8) normal = tangent.cross(new Vec3(1, 0, 0));
            normal = normal.normalize();
            Vec3 binormal = tangent.cross(normal);
            for (int s = 0; s < sides; s++) {
                double a = Math.PI * 2.0 * s / sides;
                // Slightly irregular cross-section: wood isn't perfectly round.
                double wobble = 1.0 + 0.07 * Math.sin(a * 3.0 + i * 0.7 + seed);
                rings[i][s] = pts[i].add(normal.scale(Math.cos(a) * radii[i] * wobble)).add(binormal.scale(Math.sin(a) * radii[i] * wobble));
            }
        }
        for (int i = 0; i < n - 1; i++) {
            boolean groove = (i + seed) % 4 == 0;
            for (int s = 0; s < sides; s++) {
                int s2 = (s + 1) % sides;
                boolean knot = ((i * 7 + s * 13 + seed) % 23) == 0;
                boolean stripe = (s + (i / 2)) % 3 == 0;
                int color = knot ? BARK_DARK : groove ? BARK_DARK : stripe ? BARK_LIGHT : BARK;
                Vec3 axis = pts[i].add(pts[i + 1]).scale(0.5);
                solidQuad(vc, m, nm, rings[i][s], rings[i][s2], rings[i + 1][s2], rings[i + 1][s], color, light, axis);
            }
        }
        // Cap the root end so you never see inside.
        Vec3 into = pts[1].subtract(pts[0]);
        capRing(vc, m, nm, rings[0], pts[0], into.lengthSqr() < 1.0E-10 ? new Vec3(0, 1, 0) : into.normalize(), BARK_DARK, light);
    }

    /** Flat cap over a tube end, facing away from {@code into} (the direction the tube continues in). */
    private static void capRing(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3[] ring, Vec3 centre, Vec3 into, int color, int light) {
        Vec3 inside = centre.add(into.scale(0.05));
        for (int s = 0; s < ring.length; s++) {
            solidQuad(vc, m, nm, centre, ring[s], ring[(s + 1) % ring.length], ring[(s + 1) % ring.length], color, light, inside);
        }
    }

    /**
     * Culled lit quad, emitted with whichever winding faces away from {@code inside} (the point the surface is
     * wrapped around), so callers never have to think about vertex order.
     */
    static void solidQuad(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int light, Vec3 inside) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        if (normal.lengthSqr() < 1.0E-12) normal = c.subtract(a).cross(d.subtract(a));
        Vec3 centre = a.add(b).add(c).add(d).scale(0.25);
        boolean flip = normal.dot(centre.subtract(inside)) < 0;
        Vec3 nrm = normal.lengthSqr() < 1.0E-12 ? new Vec3(0, 1, 0) : normal.normalize();
        if (flip) {
            nrm = nrm.scale(-1);
            vtx(vc, m, nm, d, color, light, nrm);
            vtx(vc, m, nm, c, color, light, nrm);
            vtx(vc, m, nm, b, color, light, nrm);
            vtx(vc, m, nm, a, color, light, nrm);
        } else {
            vtx(vc, m, nm, a, color, light, nrm);
            vtx(vc, m, nm, b, color, light, nrm);
            vtx(vc, m, nm, c, color, light, nrm);
            vtx(vc, m, nm, d, color, light, nrm);
        }
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 p, int color, int light, Vec3 n) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(color >>> 16 & 255, color >>> 8 & 255, color & 255, 255)
                .uv(0.5f, 0.5f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(nm, (float) n.x, (float) n.y, (float) n.z)
                .endVertex();
    }

    /** Smooth taper radii for a dragon neck: thick at the root, swelling slightly behind the head. */
    static float[] neckRadii(int count, float root, float neck) {
        float[] r = new float[count];
        for (int i = 0; i < count; i++) {
            float t = (float) i / (count - 1);
            r[i] = Mth.lerp(t, root, neck) * (1.0f + 0.12f * Mth.sin(t * (float) Math.PI * 3.0f));
        }
        return r;
    }

    // ------------------------------------------------------------------------------------------- dragon head

    /**
     * Wooden dragon head facing +Z, about 1.8 blocks long at scale 1, origin at the back of the skull (where the neck
     * attaches). {@code mouthOpen} 0..1 drops the lower jaw; {@code glow} 0..1 brightens the eyes and throat.
     */
    static void dragonHead(PoseStack pose, MultiBufferSource buffers, int light, float mouthOpen, float glow) {
        int eyeLight = glow > 0.2f ? FULL_BRIGHT : light;
        // Skull
        ToonRenderHelper.solidHexa(pose, buffers, BARK, light, taper(-0.42f, 0.42f, -0.22f, 0.5f, 0.0f, 0.55f,
                -0.36f, 0.36f, -0.12f, 0.42f), false);
        // Brow ridges, heavy and angry
        for (int side = -1; side <= 1; side += 2) {
            ToonRenderHelper.solidBox(pose, buffers, BARK_DARK, light,
                    side < 0 ? -0.42f : 0.12f, 0.36f, 0.32f, side < 0 ? -0.12f : 0.42f, 0.5f, 0.62f, 0.0f);
        }
        // Eyes (glowing when charging)
        int eyeColor = ToonRenderHelper.shade(EYE, 0.7f + 0.5f * glow);
        ToonRenderHelper.solidBox(pose, buffers, eyeColor, eyeLight, -0.40f, 0.24f, 0.44f, -0.22f, 0.35f, 0.58f, 0.0f);
        ToonRenderHelper.solidBox(pose, buffers, eyeColor, eyeLight, 0.22f, 0.24f, 0.44f, 0.40f, 0.35f, 0.58f, 0.0f);

        // Upper jaw / snout, tilts up a little as the mouth opens
        pose.pushPose();
        pose.translate(0, 0.05f, 0.5f);
        pose.mulPose(Vector3f.XP.rotation(-mouthOpen * 0.25f));
        ToonRenderHelper.solidHexa(pose, buffers, BARK, light, taper(-0.36f, 0.36f, -0.06f, 0.36f, 0.0f, 1.15f,
                -0.22f, 0.22f, 0.0f, 0.2f), false);
        // Nostril ridge and whisker knobs
        ToonRenderHelper.solidBox(pose, buffers, BARK_DARK, light, -0.2f, 0.2f, 0.95f, 0.2f, 0.27f, 1.12f, 0.0f);
        // Upper teeth
        for (int t = 0; t < 5; t++) {
            float z = 0.25f + t * 0.18f;
            for (int side = -1; side <= 1; side += 2) {
                float x = side * (0.27f - t * 0.025f);
                ToonRenderHelper.solidHexa(pose, buffers, TOOTH, light, fang(x, -0.06f, z, 0.05f, -0.16f), false);
            }
        }
        // Throat glow inside the upper jaw
        if (glow > 0.0f) {
            ToonRenderHelper.solidBox(pose, buffers, ToonRenderHelper.shade(0xFFFFC870, 0.6f + 0.6f * glow), FULL_BRIGHT,
                    -0.24f, -0.055f, 0.0f, 0.24f, -0.04f, 0.9f, 0.0f);
        } else {
            ToonRenderHelper.solidBox(pose, buffers, MOUTH, light, -0.24f, -0.055f, 0.0f, 0.24f, -0.04f, 0.9f, 0.0f);
        }
        pose.popPose();

        // Lower jaw, hinged at the back of the mouth
        pose.pushPose();
        pose.translate(0, -0.02f, 0.45f);
        pose.mulPose(Vector3f.XP.rotation(mouthOpen * 0.75f));
        ToonRenderHelper.solidHexa(pose, buffers, BARK_LIGHT, light, taper(-0.32f, 0.32f, -0.26f, -0.04f, 0.0f, 1.05f,
                -0.2f, 0.2f, -0.18f, -0.06f), false);
        ToonRenderHelper.solidBox(pose, buffers, MOUTH, light, -0.22f, -0.045f, 0.05f, 0.22f, -0.03f, 0.9f, 0.0f);
        for (int t = 0; t < 4; t++) {
            float z = 0.3f + t * 0.2f;
            for (int side = -1; side <= 1; side += 2) {
                float x = side * (0.24f - t * 0.025f);
                ToonRenderHelper.solidHexa(pose, buffers, TOOTH, light, fang(x, -0.04f, z, 0.045f, 0.14f), false);
            }
        }
        pose.popPose();

        // Antlers: two branching wooden horns sweeping back
        for (int side = -1; side <= 1; side += 2) {
            Vec3 base = new Vec3(side * 0.28, 0.45, 0.05);
            Vec3 mid = base.add(side * 0.25, 0.45, -0.35);
            Vec3 tip = mid.add(side * 0.1, 0.35, -0.55);
            woodTube(pose, buffers, light, new Vec3[]{base, mid, tip}, new float[]{0.09f, 0.065f, 0.02f}, 5, side + 3);
            Vec3 branchTip = mid.add(side * 0.3, 0.25, 0.1);
            woodTube(pose, buffers, light, new Vec3[]{mid, mid.lerp(branchTip, 0.5).add(0, 0.05, 0), branchTip},
                    new float[]{0.05f, 0.035f, 0.015f}, 5, side + 7);
        }
        // Spiked ridge down the back of the skull
        for (int k = 0; k < 3; k++) {
            float z = 0.25f - k * 0.22f;
            ToonRenderHelper.solidHexa(pose, buffers, BARK_DARK, light, fang(0.0f, 0.5f - k * 0.04f, z, 0.07f, 0.22f), false);
        }
    }

    /** Corners for a box tapering along Z: back face [x0,x1]x[y0,y1] at z0, front face [fx0,fx1]x[fy0,fy1] at z1. */
    private static float[][] taper(float x0, float x1, float y0, float y1, float z0, float z1,
                                   float fx0, float fx1, float fy0, float fy1) {
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            boolean front = (i & 4) != 0;
            float xa = front ? fx0 : x0, xb = front ? fx1 : x1;
            float ya = front ? fy0 : y0, yb = front ? fy1 : y1;
            c[i] = new float[]{(i & 1) != 0 ? xb : xa, (i & 2) != 0 ? yb : ya, front ? z1 : z0};
        }
        return c;
    }

    /** A tooth/spike: square base of half-width w centred at (x, y, z), tapering to a point {@code len} away in Y. */
    private static float[][] fang(float x, float y, float z, float w, float len) {
        float tipY = y + len;
        float lo = Math.min(y, tipY), hi = Math.max(y, tipY);
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            boolean top = (i & 2) != 0;
            boolean isTip = len > 0 ? top : !top;
            float ww = isTip ? 0.004f : w;
            c[i] = new float[]{x + ((i & 1) != 0 ? ww : -ww), top ? hi : lo, z + ((i & 4) != 0 ? ww : -ww)};
        }
        return c;
    }

    // ------------------------------------------------------------------------------------------- taiko drum

    /**
     * The kanji 憎 ("hatred", niku) painted on every one of Zohakuten's drums, as a 24x24 bitmap rasterised from a
     * brush (serif) font; drawn as merged horizontal runs of ink.
     */
    private static final String[] NIKU = {
            "........................",
            "....###..........#......",
            "....###....###...###....",
            "....###....###..###.....",
            "....###..#..##..##..#...",
            "....####.#############..",
            "...#########..###..###..",
            "...#########..###..###..",
            "..####################..",
            "..#####..###..###..###..",
            ".######..###..###..###..",
            "..#.###..###..###..###..",
            "....###..#############..",
            "....###..##.......##....",
            "....###...###########...",
            "....###...###.....###...",
            "....###...###.....##....",
            "....###...##########....",
            "....###...###.....##....",
            "....###...###.....##....",
            "....###...###.....##....",
            "....###...##########....",
            "....##....##......##....",
            "........................"
    };

    /**
     * Red-lacquered taiko drum, axis along +Z (the skin you see faces +Z), centred on the origin: barrel with brass
     * tack rings and a pale skin painted with 憎 in black ink. {@code shudder} 0..1 bulges the skin after a hit and
     * {@code glow} 0..1 is the vibrating red light the drums give off when struck.
     */
    static void taikoDrum(PoseStack pose, MultiBufferSource buffers, int light, float radius, float depth, float shudder, float glow) {
        pose.pushPose();
        pose.mulPose(Vector3f.XP.rotationDegrees(90.0f)); // lathe axis Y -> Z
        pose.translate(0, -depth * 0.5f, 0);
        float[] h = {0.0f, 0.04f, depth * 0.5f, depth - 0.04f, depth};
        float[] r = {radius * 0.92f, radius, radius * 1.12f, radius, radius * 0.92f};
        GyokkoRenderHelper.solidLathe(pose, buffers, light, h, r, 20, (ring, angle) -> {
            if (ring == 0 || ring == 3) {
                return ((int) (angle / (Math.PI / 10.0)) % 2 == 0) ? BRASS : LACQUER_DARK; // tack rings
            }
            return ring == 1 ? LACQUER : ToonRenderHelper.shade(LACQUER, 0.9f);
        });
        // Skins on both ends (solid discs). The struck drum's skin flushes red and lights up.
        float bulge = 0.02f * shudder;
        int skin = ThreadRenderHelper.mix(DRUM_SKIN, 0xFFFF3A2A, Mth.clamp(glow, 0.0f, 1.0f) * 0.75f);
        int skinLight = glow > 0.05f ? FULL_BRIGHT : light;
        for (int end = 0; end <= 1; end++) {
            float y = end == 0 ? -0.001f : depth + 0.001f;
            float[] hh = end == 0 ? new float[]{y, y - bulge} : new float[]{y, y + bulge};
            float[] rr = {radius * 0.93f, 0.0f};
            int endLight = end == 1 ? skinLight : light;
            int endColor = end == 1 ? skin : DRUM_SKIN;
            GyokkoRenderHelper.solidLathe(pose, buffers, endLight, end == 0 ? new float[]{hh[1], hh[0]} : hh,
                    end == 0 ? new float[]{0.0f, radius * 0.93f} : rr, 20, (ring, angle) -> endColor);
        }
        // 憎 on the front (+Z after rotation = +Y here) skin, upright when seen from the front.
        pose.translate(0, depth + 0.004f + bulge, 0);
        niku(pose, buffers, radius * 1.15f, glow > 0.05f ? 0xFF3A0606 : 0xFF141010);
        pose.popPose();
    }

    /**
     * Draws 憎 in the XZ plane facing +Y, {@code size} blocks across: glyph columns run along +X and rows along +Z
     * (which the drum's rotation turns into "down").
     */
    static void niku(PoseStack pose, MultiBufferSource buffers, float size, int color) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        int n = NIKU.length;
        float px = size / n;
        float half = size * 0.5f;
        for (int row = 0; row < n; row++) {
            String line = NIKU[row];
            int col = 0;
            while (col < line.length()) {
                if (line.charAt(col) != '#') {
                    col++;
                    continue;
                }
                int start = col;
                while (col < line.length() && line.charAt(col) == '#') col++;
                float x0 = start * px - half, x1 = col * px - half;
                float z0 = row * px - half, z1 = (row + 1) * px - half;
                GyokkoRenderHelper.quadRaw(vc, m, nm, new Vec3(x0, 0, z0), new Vec3(x0, 0, z1), new Vec3(x1, 0, z1), new Vec3(x1, 0, z0), color);
            }
        }
    }

    /** Three interlocking tomoe commas in the XZ plane (facing +Y), each a round head with a curling tail. */
    static void mitsudomoe(PoseStack pose, MultiBufferSource buffers, float size, int color) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        for (int k = 0; k < 3; k++) {
            double base = Math.PI * 2.0 * k / 3.0;
            // Head
            double hx = Math.cos(base) * size * 0.42, hz = Math.sin(base) * size * 0.42;
            float hr = size * 0.26f;
            for (int s = 0; s < 12; s++) {
                double a0 = Math.PI * 2.0 * s / 12, a1 = Math.PI * 2.0 * (s + 1) / 12;
                Vec3 c = new Vec3(hx, 0, hz);
                Vec3 p0 = c.add(Math.cos(a0) * hr, 0, Math.sin(a0) * hr);
                Vec3 p1 = c.add(Math.cos(a1) * hr, 0, Math.sin(a1) * hr);
                GyokkoRenderHelper.quadRaw(vc, m, nm, c, p1, p0, p0, color);
            }
            // Tail: spirals around the centre, thinning to a point
            int steps = 10;
            Vec3 prevIn = null, prevOut = null;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double ang = base + t * 2.2;
                double rad = size * (0.42 + 0.5 * t);
                double half = hr * (1.0 - t);
                Vec3 centre = new Vec3(Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
                Vec3 radial = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                Vec3 in = centre.subtract(radial.scale(half));
                Vec3 out = centre.add(radial.scale(half));
                if (prevIn != null) GyokkoRenderHelper.quadRaw(vc, m, nm, prevIn, in, out, prevOut, color);
                prevIn = in;
                prevOut = out;
            }
        }
    }

    // ------------------------------------------------------------------------------------------- additive fx

    /** Jagged lightning arc from a to b (additive): a bright core and a blue glow, re-rolled a few times a second. */
    static void lightningArc(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 a, Vec3 b, long seed, float brightness, float width) {
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        java.util.Random rnd = new java.util.Random(seed);
        int segments = Math.max(4, (int) (a.distanceTo(b) * 1.5));
        Vec3 dir = b.subtract(a);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        Vec3 prev = a;
        double jag = Math.min(1.2, dir.length() * 0.08);
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 p = a.add(dir.scale(t));
            if (i < segments) {
                p = p.add(side.scale((rnd.nextDouble() - 0.5) * 2 * jag)).add(up.scale((rnd.nextDouble() - 0.5) * 2 * jag));
            }
            ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, width * 4.0f, ThreadRenderHelper.scaleAlpha(0x506A9CFF, brightness));
            ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, width, ThreadRenderHelper.scaleAlpha(0xF0F4F8FF, brightness));
            // Occasional fork
            if (rnd.nextInt(5) == 0 && i < segments) {
                Vec3 fork = p.add(side.scale((rnd.nextDouble() - 0.5) * 3 * jag)).add(dir.scale(0.06)).add(0, -jag, 0);
                ThreadRenderHelper.ribbon(pose, vc, cam, p, fork, width * 0.6f, ThreadRenderHelper.scaleAlpha(0xC0D8E8FF, brightness));
            }
            prev = p;
        }
    }

    /**
     * Ring of compressed sound (additive): a thick translucent band of radius {@code r} around the axis
     * {@code normal} through {@code centre}, with a brighter leading edge.
     */
    static void soundRing(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 centre, Vec3 normal, float r, float thickness, int argb, float brightness) {
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        Vec3 nrm = normal.normalize();
        Vec3 u = nrm.cross(Math.abs(nrm.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = nrm.cross(u).normalize();
        int segments = Math.max(16, Math.min(64, (int) (r * 6)));
        Vec3 prev = null;
        for (int s = 0; s <= segments; s++) {
            double a = Math.PI * 2.0 * s / segments;
            Vec3 p = centre.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
            if (prev != null) {
                ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, thickness, ThreadRenderHelper.scaleAlpha(argb, brightness));
                ThreadRenderHelper.ribbon(pose, vc, cam, prev, p, thickness * 0.25f, ThreadRenderHelper.scaleAlpha(0xE0FFFFFF, brightness * 0.8f));
            }
            prev = p;
        }
    }

    /** Point on a quadratic Bezier. */
    static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u).add(p1.scale(2 * u * t)).add(p2.scale(t * t));
    }

    /** Rotate the pose so local +Z points along {@code dir} (local +Y stays as close to world up as possible). */
    static void faceAlong(PoseStack pose, Vec3 dir) {
        if (dir.lengthSqr() < 1.0E-8) return;
        Vec3 d = dir.normalize();
        float yaw = (float) Mth.atan2(d.x, d.z);
        float pitch = (float) Math.asin(Mth.clamp(-d.y, -1.0, 1.0));
        pose.mulPose(Vector3f.YP.rotation(yaw));
        pose.mulPose(Vector3f.XP.rotation(pitch));
    }
}

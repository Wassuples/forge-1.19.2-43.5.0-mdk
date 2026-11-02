package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Akaza's art as crisp procedural geometry (no textures to blur when scaled):
 * <ul>
 *   <li>the Technique Development / Compass Needle snowflake: six long arms and six short ones, fern-like branches,
 *   twelve hexagonal nodes holding the kanji numerals 一 to 十二, glowing red at the centre fading to light blue at
 *   the tips, drawn flat on the ground with additive lines;</li>
 *   <li>the red energy clones of Akaza that the anime places on the snowflake's points for Chaotic Afterglow;</li>
 *   <li>thin needle-like shockwaves.</li>
 * </ul>
 */
final class AkazaRenderHelper {
    static final int RED = 0xFF3A40;
    static final int LIGHT_BLUE = 0xA6E2FF;

    /** Kanji numerals 一..十二 as bitmaps (24 rows; 24 wide, or 40 wide for the two-character ones). */
    private static final String[][] NUMERALS = {
            {"........................", "........................", "........................", "........................", "........................", "........................", "........................", "..####################..", "..####################..", "..####################..", "..####################..", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................"},
            {"........................", "........................", "........................", "........................", "....################....", "....################....", "....################....", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "........................", "..####################..", "..####################..", "..####################..", "..####################..", "........................", "........................", "........................", "........................"},
            {"........................", "........................", "........................", "...#################....", "...#################....", "........................", "........................", "........................", "........................", "........................", ".....##############.....", ".....##############.....", ".....##############.....", "........................", "........................", "........................", "........................", "..####################..", "..####################..", "..####################..", "........................", "........................", "........................", "........................"},
            {"........................", "........................", "...###################..", "...###################..", "...###################..", "...###..###.####..####..", "...###..###.####..####..", "...###..###.####..####..", "...###..###.####..####..", "...###.####.####.#####..", "...###.###..##########..", "...#######..##########..", "...#######...#########..", "...######.....###.####..", "...###.#..........####..", "...###............####..", "...###............####..", "...###################..", "...###################..", "...###################..", "...###............####..", "........................", "........................", "........................"},
            {"........................", "........................", "........................", "...##################...", "...##################...", ".........####...........", ".........###............", ".........###............", "........####............", "....###############.....", "....###############.....", "....###############.....", "........###....####.....", "........###....####.....", ".......####....####.....", ".......####....####.....", ".......####....####.....", ".......####....####.....", "..####################..", "..####################..", "..####################..", "........................", "........................", "........................"},
            {"........................", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..####################..", "..####################..", "..####################..", "........................", "........................", "................#.......", "......####....####......", "......####....####......", ".....####......####.....", ".....####......#####....", "....#####.......####....", "...#####........#####...", "...#####.........####...", "..#####..........#####..", "..####............####..", "...###............###...", "........................", "........................"},
            {"........................", "........................", "........####............", "........####............", "........####............", "........####............", "........####............", "........####....######..", "........##############..", "...###################..", "..##################....", "..############..........", "..#####.####............", "........####............", "........####.......#....", "........####.......###..", "........####......####..", "........####......####..", "........####......####..", "........#############...", ".........############...", ".........###########....", "........................", "........................"},
            {"........................", "........................", "......###########.......", "......###########.......", "......###########.......", "..............###.......", "..............###.......", "......####....###.......", "......####....####......", "......####....####......", "......####....####......", "......####....####......", ".....####......####.....", ".....####......####.....", ".....####......####.....", "....####........####....", "....####........####....", "...####.........#####...", "..#####..........#####..", "..####............####..", "...##.............###...", "....#..............#....", "........................", "........................"},
            {"........................", "........###.............", "........###.............", "........###.............", "........###.............", "........###.............", ".......####.............", "..################......", "..################......", "..################......", ".......####...####......", ".......####...####......", ".......####...####......", ".......###....####......", "......####....####......", "......####....####......", ".....####.....####..##..", "....#####.....####..###.", "...#####......####..###.", "..#####.......#########.", "..####.........#######..", "...##..........######...", "........................", "........................"},
            {"........................", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..####################..", "..####################..", "..####################..", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "..........####..........", "........................"},
            {"........................................", "........................................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", ".##################..##################.", ".##################..##################.", ".##################..##################.", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........####............................", "........................................", "........................................"},
            {"........................................", "........................................", "........####............................", "........####............................", "........####............................", "........####...........##############...", "........####...........##############...", "........####...........##############...", "........####............................", "........####............................", ".##################.....................", ".##################.....................", ".##################.....................", "........####............................", "........####............................", "........####............................", "........####............................", "........####.........##################.", "........####.........##################.", "........####.........##################.", "........####............................", "........####............................", "........................................", "........................................"}
    };

    private AkazaRenderHelper() {
    }

    // ------------------------------------------------------------------------------------------- flat lines

    /**
     * Flat additive line lying in the XZ plane at its own heights, {@code width} wide, coloured {@code rgbA} at a
     * and {@code rgbB} at b with the given alphas (0..255). Emitted in both windings.
     */
    static void flatLine(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width, int rgbA, int rgbB, int alphaA, int alphaB) {
        double dx = b.x - a.x, dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6) return;
        float sx = (float) (-dz / len * width * 0.5), sz = (float) (dx / len * width * 0.5);
        float ax = (float) a.x, ay = (float) a.y, az = (float) a.z;
        float bx = (float) b.x, by = (float) b.y, bz = (float) b.z;
        int ar = rgbA >> 16 & 255, ag = rgbA >> 8 & 255, ab = rgbA & 255;
        int br = rgbB >> 16 & 255, bg = rgbB >> 8 & 255, bb = rgbB & 255;
        vc.vertex(m, ax - sx, ay, az - sz).color(ar, ag, ab, alphaA).endVertex();
        vc.vertex(m, ax + sx, ay, az + sz).color(ar, ag, ab, alphaA).endVertex();
        vc.vertex(m, bx + sx, by, bz + sz).color(br, bg, bb, alphaB).endVertex();
        vc.vertex(m, bx - sx, by, bz - sz).color(br, bg, bb, alphaB).endVertex();
        vc.vertex(m, bx - sx, by, bz - sz).color(br, bg, bb, alphaB).endVertex();
        vc.vertex(m, bx + sx, by, bz + sz).color(br, bg, bb, alphaB).endVertex();
        vc.vertex(m, ax + sx, ay, az + sz).color(ar, ag, ab, alphaA).endVertex();
        vc.vertex(m, ax - sx, ay, az - sz).color(ar, ag, ab, alphaA).endVertex();
    }

    /** Red at the centre to light blue at the tips. */
    static int gradient(double radiusFraction) {
        return ThreadRenderHelper.mix(0xFF000000 | RED, 0xFF000000 | LIGHT_BLUE, (float) Mth.clamp(radiusFraction * 1.25 - 0.1, 0.0, 1.0)) & 0xFFFFFF;
    }

    // ------------------------------------------------------------------------------------------- compass

    /**
     * The Compass Needle snowflake, centred on {@code centre} (flat, facing up), {@code radius} blocks from the
     * centre to the tip of a long arm. {@code unfold} 0..1 grows it outward from the centre; {@code brightness}
     * 0..1 fades it; {@code spin} rotates it (radians); {@code pulse} 0..1 flares the nodes.
     */
    static void compass(PoseStack pose, MultiBufferSource buffers, Vec3 centre, float radius, float unfold, float brightness,
                        float spin, float pulse) {
        if (brightness <= 0.0f || unfold <= 0.0f) return;
        Line line = new Line(ThreadRenderHelper.buffer(buffers), pose.last().pose(), centre.add(0, 0.02, 0), radius, unfold, brightness, spin);
        // Centre ring
        line.circle(0.0, 0.0, 0.075, 0.014, 24);
        for (int k = 0; k < 6; k++) {
            double a = Math.PI / 2.0 + k * Math.PI / 3.0;
            // Long arm with fern branches and a hexagon node at its tip.
            line.ray(a, 0.075, 0.80, 0.017);
            double[] at = {0.22, 0.36, 0.50, 0.63};
            double[] len = {0.17, 0.15, 0.12, 0.08};
            for (int b = 0; b < at.length; b++) {
                for (int side = -1; side <= 1; side += 2) {
                    double ba = a + side * Math.toRadians(52);
                    line.branch(a, at[b], ba, len[b], 0.011);
                    // twig off each branch
                    double tx = Math.cos(a) * at[b] + Math.cos(ba) * len[b] * 0.55;
                    double tz = Math.sin(a) * at[b] + Math.sin(ba) * len[b] * 0.55;
                    double ta = ba + side * Math.toRadians(40);
                    line.segment(tx, tz, tx + Math.cos(ta) * len[b] * 0.4, tz + Math.sin(ta) * len[b] * 0.4, 0.008);
                }
            }
            line.node(a, 0.875, 0.068, k * 2, pulse);
            line.dot(a, 0.975, 0.016);
            // Short arm between the long ones, with its own node.
            double s = a + Math.PI / 6.0;
            line.ray(s, 0.075, 0.48, 0.013);
            for (int side = -1; side <= 1; side += 2) {
                line.branch(s, 0.26, s + side * Math.toRadians(52), 0.1, 0.009);
            }
            line.node(s, 0.555, 0.062, k * 2 + 1, pulse);
            line.dot(s, 0.65, 0.013);
        }
    }

    /** Draws the snowflake's lines in local units (fractions of the radius), clipped to the unfold front. */
    private static final class Line {
        final VertexConsumer vc;
        final Matrix4f m;
        final Vec3 centre;
        final float radius, unfold, brightness;
        final double cos, sin;

        Line(VertexConsumer vc, Matrix4f m, Vec3 centre, float radius, float unfold, float brightness, float spin) {
            this.vc = vc;
            this.m = m;
            this.centre = centre;
            this.radius = radius;
            this.unfold = unfold;
            this.brightness = brightness;
            this.cos = Math.cos(spin);
            this.sin = Math.sin(spin);
        }

        Vec3 world(double x, double z) {
            double rx = x * cos - z * sin, rz = x * sin + z * cos;
            return centre.add(rx * radius, 0, rz * radius);
        }

        void segment(double x0, double z0, double x1, double z1, double width) {
            double r0 = Math.sqrt(x0 * x0 + z0 * z0), r1 = Math.sqrt(x1 * x1 + z1 * z1);
            double front = unfold * 1.05;
            if (Math.min(r0, r1) > front) return;
            if (Math.max(r0, r1) > front) {
                // clip the outer end to the unfold front
                double t = (front - Math.min(r0, r1)) / Math.max(1.0E-6, Math.abs(r1 - r0));
                if (r0 > r1) { x0 = x1 + (x0 - x1) * t; z0 = z1 + (z0 - z1) * t; r0 = front; }
                else { x1 = x0 + (x1 - x0) * t; z1 = z0 + (z1 - z0) * t; r1 = front; }
            }
            Vec3 a = world(x0, z0), b = world(x1, z1);
            int ca = gradient(r0), cb = gradient(r1);
            int glow = (int) (70 * brightness), core = (int) (235 * brightness);
            flatLine(vc, m, a, b, (float) (width * radius * 3.2), ca, cb, glow, glow);
            flatLine(vc, m, a.add(0, 0.004, 0), b.add(0, 0.004, 0), (float) (width * radius), ThreadRenderHelper.mix(0xFF000000 | ca, 0xFFFFFFFF, 0.45f) & 0xFFFFFF,
                    ThreadRenderHelper.mix(0xFF000000 | cb, 0xFFFFFFFF, 0.45f) & 0xFFFFFF, core, core);
        }

        void ray(double angle, double from, double to, double width) {
            segment(Math.cos(angle) * from, Math.sin(angle) * from, Math.cos(angle) * to, Math.sin(angle) * to, width);
        }

        void branch(double armAngle, double at, double angle, double length, double width) {
            double x = Math.cos(armAngle) * at, z = Math.sin(armAngle) * at;
            segment(x, z, x + Math.cos(angle) * length, z + Math.sin(angle) * length, width);
        }

        void circle(double cx, double cz, double r, double width, int segments) {
            for (int s = 0; s < segments; s++) {
                double a0 = Math.PI * 2.0 * s / segments, a1 = Math.PI * 2.0 * (s + 1) / segments;
                segment(cx + Math.cos(a0) * r, cz + Math.sin(a0) * r, cx + Math.cos(a1) * r, cz + Math.sin(a1) * r, width);
            }
        }

        void dot(double angle, double at, double r) {
            circle(Math.cos(angle) * at, Math.sin(angle) * at, r, r * 0.9, 8);
        }

        /** Hexagon node holding numeral {@code index + 1}, its top pointing outward. */
        void node(double angle, double at, double r, int index, float pulse) {
            double cx = Math.cos(angle) * at, cz = Math.sin(angle) * at;
            if (at > unfold * 1.05) return;
            double grow = 1.0 + 0.25 * pulse;
            for (int s = 0; s < 6; s++) {
                double a0 = angle + Math.PI * 2.0 * s / 6, a1 = angle + Math.PI * 2.0 * (s + 1) / 6;
                segment(cx + Math.cos(a0) * r * grow, cz + Math.sin(a0) * r * grow, cx + Math.cos(a1) * r * grow, cz + Math.sin(a1) * r * grow, 0.011);
            }
            numeral(cx, cz, angle, r * 1.05, index);
        }

        /** Kanji numeral, upright when read from the centre looking outward along {@code angle}. */
        void numeral(double cx, double cz, double angle, double size, int index) {
            String[] rows = NUMERALS[index];
            int h = rows.length;
            int w = rows[0].length();
            double px = size / 24.0 * 1.5;
            // glyph "up" = outward; glyph "right" = clockwise of outward
            double ux = Math.cos(angle), uz = Math.sin(angle);
            double rx = -uz, rz = ux;
            int col = gradient(at(cx, cz));
            int light = ThreadRenderHelper.mix(0xFF000000 | col, 0xFFFFFFFF, 0.6f) & 0xFFFFFF;
            int alpha = (int) (230 * brightness);
            for (int row = 0; row < h; row++) {
                String line = rows[row];
                int c = 0;
                while (c < w) {
                    if (line.charAt(c) != '#') { c++; continue; }
                    int start = c;
                    while (c < w && line.charAt(c) == '#') c++;
                    double y = (h / 2.0 - row - 0.5) * px;           // + is outward
                    double x0 = (start - w / 2.0) * px, x1 = (c - w / 2.0) * px;
                    Vec3 a = world(cx + rx * x0 + ux * y, cz + rz * x0 + uz * y).add(0, 0.008, 0);
                    Vec3 b = world(cx + rx * x1 + ux * y, cz + rz * x1 + uz * y).add(0, 0.008, 0);
                    flatLine(vc, m, a, b, (float) (px * radius), light, light, alpha, alpha);
                }
            }
        }

        private static double at(double x, double z) {
            return Math.sqrt(x * x + z * z);
        }
    }

    // ------------------------------------------------------------------------------------------- clones

    /**
     * One of Akaza's red energy clones (additive, glowing), standing at {@code feet} facing {@code forward}, about
     * {@code height} blocks tall. {@code phase} (in ticks) drives an endless chain of punches and kicks.
     */
    static void energyClone(PoseStack pose, MultiBufferSource buffers, Vec3 feet, Vec3 forward, float height, float phase, float brightness) {
        if (brightness <= 0.0f) return;
        VertexConsumer vc = ThreadRenderHelper.buffer(buffers);
        Matrix4f m = pose.last().pose();
        Vec3 f = new Vec3(forward.x, 0, forward.z);
        f = f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
        Vec3 r = new Vec3(-f.z, 0, f.x);
        Vec3 up = new Vec3(0, 1, 0);
        float s = height / 1.8f;
        int a = (int) (110 * brightness);
        int color = 0xFF2A36;
        // Cycle: right punch, left punch, right kick, left punch (3 ticks each).
        int beat = Mth.floor(phase / 3.0f) & 3;
        float k = (phase / 3.0f) - Mth.floor(phase / 3.0f);
        float strike = Mth.sin((float) Math.PI * Mth.clamp(k * 1.6f, 0.0f, 1.0f));
        float rArm = beat == 0 ? -1.6f * strike - 0.9f * (1 - strike) : -0.9f;
        float lArm = (beat == 1 || beat == 3) ? -1.6f * strike - 0.9f * (1 - strike) : -0.9f;
        float rLeg = beat == 2 ? -1.6f * strike : 0.15f;
        float lLeg = beat == 2 ? 0.2f : -0.15f;
        float twist = beat == 0 || beat == 2 ? -0.25f * strike : (0.25f * strike);
        Vec3 ft = f.yRot(twist), rt = r.yRot(twist);
        Frame body = new Frame(feet, ft, rt, up, s);
        body.box(vc, m, 0, 1.125, 0, 0.25, 0.375, 0.13, 0, 0, 0, color, a);           // torso
        body.box(vc, m, 0, 1.75, 0, 0.25, 0.25, 0.25, 0, 0, 0, color, a);             // head
        body.limb(vc, m, 0.375, 1.375, 0, 0.12, 0.375, 0.12, rArm, color, a);         // right arm
        body.limb(vc, m, -0.375, 1.375, 0, 0.12, 0.375, 0.12, lArm, color, a);        // left arm
        Frame legs = new Frame(feet, f, r, up, s);
        legs.limb(vc, m, 0.125, 0.75, 0, 0.12, 0.375, 0.12, rLeg, color, a);          // right leg
        legs.limb(vc, m, -0.125, 0.75, 0, 0.12, 0.375, 0.12, lLeg, color, a);         // left leg
    }

    /** Body-local frame: x along right, y up, z forward, scaled. */
    private record Frame(Vec3 origin, Vec3 f, Vec3 r, Vec3 up, float s) {
        Vec3 p(double x, double y, double z) {
            return origin.add(r.scale(x * s)).add(up.scale(y * s)).add(f.scale(z * s));
        }

        /** Axis-aligned box (half extents) centred on (cx, cy, cz), optionally swung about a pivot by pitch. */
        void box(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double hx, double hy, double hz,
                 double pivotY, double pivotZ, double pitch, int color, int alpha) {
            Vec3[] c = new Vec3[8];
            for (int i = 0; i < 8; i++) {
                double x = cx + ((i & 1) != 0 ? hx : -hx);
                double y = cy + ((i & 2) != 0 ? hy : -hy);
                double z = cz + ((i & 4) != 0 ? hz : -hz);
                if (pitch != 0.0) {
                    double dy = y - pivotY, dz = z - pivotZ;
                    double cs = Math.cos(pitch), sn = Math.sin(pitch);
                    // negative pitch swings a hanging limb forward/up (like the player model)
                    y = pivotY + dy * cs + dz * sn;
                    z = pivotZ - dy * sn + dz * cs;
                }
                c[i] = p(x, y, z);
            }
            int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
            int cr = color >> 16 & 255, cg = color >> 8 & 255, cb = color & 255;
            for (int[] face : faces) {
                for (int j = 0; j < 4; j++) {
                    Vec3 v = c[face[j]];
                    vc.vertex(m, (float) v.x, (float) v.y, (float) v.z).color(cr, cg, cb, alpha).endVertex();
                }
                for (int j = 3; j >= 0; j--) {
                    Vec3 v = c[face[j]];
                    vc.vertex(m, (float) v.x, (float) v.y, (float) v.z).color(cr, cg, cb, alpha).endVertex();
                }
            }
        }

        /** Limb hanging from a joint at (jx, jy, jz), {@code hy} half-length, swung by {@code pitch}. */
        void limb(VertexConsumer vc, Matrix4f m, double jx, double jy, double jz, double hx, double hy, double hz,
                  double pitch, int color, int alpha) {
            box(vc, m, jx, jy - hy, jz, hx, hy, hz, jy, jz, pitch, color, alpha);
        }
    }

    // ------------------------------------------------------------------------------------------- needles

    /** A thin needle-like shockwave from {@code tail} to {@code head}: a fine white core in a cyan sheath. */
    static void needle(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 tail, Vec3 head, float brightness) {
        if (brightness <= 0.0f) return;
        ThreadRenderHelper.ribbon(pose, vc, cam, tail, head, 0.16f, ThreadRenderHelper.scaleAlpha(0x5050B8FF, brightness));
        ThreadRenderHelper.ribbon(pose, vc, cam, tail.lerp(head, 0.35), head, 0.045f, ThreadRenderHelper.scaleAlpha(0xF0EEF6FF, brightness));
    }
}

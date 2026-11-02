package com.wassuples.demonslayer.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class ToonRenderHelper {
    private static final ResourceLocation WHITE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/entity/white.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private ToonRenderHelper() {
    }

    static void ellipse(PoseStack stack, MultiBufferSource buffer, int color, float radiusX, float radiusY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        int segments = 28;

        for (int i = 0; i < segments; i++) {
            float x0 = -radiusX + (2.0f * radiusX * i / segments);
            float x1 = -radiusX + (2.0f * radiusX * (i + 1) / segments);
            float y0 = radiusY * (float) Math.sqrt(Math.max(0.0f, 1.0f - (x0 * x0) / (radiusX * radiusX)));
            float y1 = radiusY * (float) Math.sqrt(Math.max(0.0f, 1.0f - (x1 * x1) / (radiusX * radiusX)));

            quad(vc, pose, normal, x0, -y0, z, x1, -y1, z, x1, y1, z, x0, y0, z, color, FULL_BRIGHT, 0, 0, 1);
        }
    }

    static void quad(PoseStack stack, MultiBufferSource buffer, int color, float minX, float minY, float maxX, float maxY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        quad(vc, pose, normal, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z, color, FULL_BRIGHT, 0, 0, 1);
    }

    static void texturedQuad(PoseStack stack, MultiBufferSource buffer, ResourceLocation texture,
                             float minX, float minY, float maxX, float maxY, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        quad(vc, pose, normal, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z,
                0xFFFFFFFF, FULL_BRIGHT, 0, 0, 1);
    }

    static void box(PoseStack stack, MultiBufferSource buffer, int color,
                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        quad(vc, pose, normal, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, FULL_BRIGHT, 0, 0, 1);
        quad(vc, pose, normal, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, color, FULL_BRIGHT, 0, 0, -1);
        quad(vc, pose, normal, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, color, FULL_BRIGHT, 0, 1, 0);
        quad(vc, pose, normal, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color, FULL_BRIGHT, 0, -1, 0);
        quad(vc, pose, normal, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, color, FULL_BRIGHT, 1, 0, 0);
        quad(vc, pose, normal, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color, FULL_BRIGHT, -1, 0, 0);
    }

    /** Arbitrary 3D quad (counter-clockwise when seen from the side the normal points to). */
    static void quad3(PoseStack stack, MultiBufferSource buffer, int color,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      float normalX, float normalY, float normalZ) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        quad(vc, stack.last().pose(), stack.last().normal(), x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                color, FULL_BRIGHT, normalX, normalY, normalZ);
    }

    /** Flat triangle in the XY plane (rendered as a degenerate quad so it shares the batch). */
    static void tri(PoseStack stack, MultiBufferSource buffer, int color,
                    float x1, float y1, float x2, float y2, float x3, float y3, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        quad(vc, stack.last().pose(), stack.last().normal(), x1, y1, z, x2, y2, z, x3, y3, z, x3, y3, z,
                color, FULL_BRIGHT, 0, 0, 1);
    }

    /** Filled polygon in the XY plane, fanned from its centroid. Works for star-like (radially convex) shapes. */
    static void polygon(PoseStack stack, MultiBufferSource buffer, int color, float[] xs, float[] ys, float z) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float cx = 0.0f;
        float cy = 0.0f;
        for (int i = 0; i < xs.length; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= xs.length;
        cy /= ys.length;
        for (int i = 0; i < xs.length; i++) {
            int j = (i + 1) % xs.length;
            quad(vc, pose, normal, cx, cy, z, xs[i], ys[i], z, xs[j], ys[j], z, xs[j], ys[j], z,
                    color, FULL_BRIGHT, 0, 0, 1);
        }
    }

    /** Filled circle in the XY plane. */
    static void disc(PoseStack stack, MultiBufferSource buffer, int color, float cx, float cy, float radius, int segments, float z) {
        float[] xs = new float[segments];
        float[] ys = new float[segments];
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            xs[i] = cx + (float) Math.cos(a) * radius;
            ys[i] = cy + (float) Math.sin(a) * radius;
        }
        polygon(stack, buffer, color, xs, ys, z);
    }

    /** Filled circle lying in the XZ plane at height y (faces up). */
    static void discXZ(PoseStack stack, MultiBufferSource buffer, int color, float radius, float y, int segments) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            quad(vc, pose, normal, 0, y, 0, x1, y, z1, x0, y, z0, x0, y, z0, color, FULL_BRIGHT, 0, 1, 0);
        }
    }

    /** Open-ended cylinder wall around the Y axis, from y0 to y1, with optional flare (r1 at the top). */
    static void cylinder(PoseStack stack, MultiBufferSource buffer, int color, float r0, float r1, float y0, float y1, int segments) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            quad(vc, pose, normal,
                    c0 * r0, y0, s0 * r0, c1 * r0, y0, s1 * r0, c1 * r1, y1, s1 * r1, c0 * r1, y1, s0 * r1,
                    color, FULL_BRIGHT, (c0 + c1) * 0.5f, 0, (s0 + s1) * 0.5f);
        }
    }

    // ------------------------------------------------------------------ cel-shaded solids with ink outlines

    /** Box with flat cel shading: bright top, two side tones, dark bottom. */
    static void shadedBox(PoseStack stack, MultiBufferSource buffer, int color,
                          float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        taperedBox(stack, buffer, color, minX, minY, minZ, maxX, maxY, maxZ, minY, maxY, minZ, maxZ, false);
    }

    /**
     * Inverted-hull outline: the same box grown by {@code grow}, drawn with only its far faces visible
     * (culled render type + reversed winding), so it shows as an ink rim around the solid in front of it.
     */
    static void hullBox(PoseStack stack, MultiBufferSource buffer, int ink, float grow,
                        float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        taperedBox(stack, buffer, ink, minX - grow, minY - grow, minZ - grow, maxX + grow, maxY + grow, maxZ + grow,
                minY - grow, maxY + grow, minZ - grow, maxZ + grow, true);
    }

    /**
     * A box whose +X end has its own Y/Z extents (minY1..maxY1, minZ1..maxZ1), i.e. a frustum along X.
     * With equal extents it is a plain box. {@code hull} = reversed winding on a culled type (outline pass).
     */
    static void taperedBox(PoseStack stack, MultiBufferSource buffer, int color,
                           float minX, float minY0, float minZ0, float maxX, float maxY0, float maxZ0,
                           float minY1, float maxY1, float minZ1, float maxZ1, boolean hull) {
        VertexConsumer vc = buffer.getBuffer(hull ? RenderType.entityCutout(WHITE) : RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        int top = hull ? color : shade(color, 1.18f);
        int sideA = hull ? color : color;
        int sideB = hull ? color : shade(color, 0.82f);
        int end = hull ? color : shade(color, 0.92f);
        int bottom = hull ? color : shade(color, 0.6f);
        // corners: x0 end uses *0 extents, x1 end uses *1 extents
        float[][] q = {
                // +Z side
                {minX, minY0, maxZ0, maxX, minY1, maxZ1, maxX, maxY1, maxZ1, minX, maxY0, maxZ0},
                // -Z side
                {maxX, minY1, minZ1, minX, minY0, minZ0, minX, maxY0, minZ0, maxX, maxY1, minZ1},
                // top
                {minX, maxY0, maxZ0, maxX, maxY1, maxZ1, maxX, maxY1, minZ1, minX, maxY0, minZ0},
                // bottom
                {minX, minY0, minZ0, maxX, minY1, minZ1, maxX, minY1, maxZ1, minX, minY0, maxZ0},
                // +X end
                {maxX, minY1, maxZ1, maxX, minY1, minZ1, maxX, maxY1, minZ1, maxX, maxY1, maxZ1},
                // -X end
                {minX, minY0, minZ0, minX, minY0, maxZ0, minX, maxY0, maxZ0, minX, maxY0, minZ0}};
        int[] colors = {sideA, sideB, top, bottom, end, end};
        float[][] normals = {{0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}};
        for (int i = 0; i < q.length; i++) {
            float[] v = q[i];
            if (hull) {
                quad(vc, pose, normal, v[9], v[10], v[11], v[6], v[7], v[8], v[3], v[4], v[5], v[0], v[1], v[2],
                        colors[i], FULL_BRIGHT, -normals[i][0], -normals[i][1], -normals[i][2]);
            } else {
                quad(vc, pose, normal, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
                        colors[i], FULL_BRIGHT, normals[i][0], normals[i][1], normals[i][2]);
            }
        }
    }

    /** Raised pixel-art lettering on a plane facing +Z: each '#' becomes a small box {@code px} wide. */
    static void pixelText(PoseStack stack, MultiBufferSource buffer, int color, String[][] glyphs,
                          float px, float depth, float spacing) {
        float width = 0.0f;
        for (String[] glyph : glyphs) width += glyph[0].length() * px + spacing;
        width -= spacing;
        float x = -width / 2.0f;
        for (String[] glyph : glyphs) {
            int rows = glyph.length;
            for (int r = 0; r < rows; r++) {
                String row = glyph[r];
                for (int c = 0; c < row.length(); c++) {
                    if (row.charAt(c) != '#') continue;
                    float x0 = x + c * px;
                    float y1 = (rows / 2.0f - r) * px;
                    shadedBox(stack, buffer, color, x0, y1 - px, 0.0f, x0 + px, y1, depth);
                }
            }
            x += glyph[0].length() * px + spacing;
        }
    }

    /** Filled UV sphere with cel bands (lighter toward the top). */
    static void sphere(PoseStack stack, MultiBufferSource buffer, int color, float radius, int rings, int segments) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int r = 0; r < rings; r++) {
            double t0 = Math.PI * r / rings;
            double t1 = Math.PI * (r + 1) / rings;
            float y0 = (float) Math.cos(t0) * radius;
            float y1 = (float) Math.cos(t1) * radius;
            float s0 = (float) Math.sin(t0) * radius;
            float s1 = (float) Math.sin(t1) * radius;
            float band = 0.78f + 0.3f * (float) Math.cos((t0 + t1) * 0.5);
            int c = shade(color, Math.min(1.08f, band));
            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * 2.0 * i / segments;
                double a1 = Math.PI * 2.0 * (i + 1) / segments;
                float c0 = (float) Math.cos(a0);
                float n0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float n1 = (float) Math.sin(a1);
                quad(vc, pose, normal,
                        c0 * s0, y0, n0 * s0, c1 * s0, y0, n1 * s0, c1 * s1, y1, n1 * s1, c0 * s1, y1, n0 * s1,
                        c, FULL_BRIGHT, c0, (y0 + y1) / (2 * radius), n0);
            }
        }
    }

    // ------------------------------------------------------------------ solid, back-face-culled 3D props
    // These use entitySolid (culled, lit by normals) so real 3D models read correctly from every side, and so the
    // classic "inverted hull" outline works: draw a slightly bigger copy with reversed winding in black, and only
    // its far inside faces survive culling, leaving an ink rim around the silhouette.

    /** Face corner indices for a hexahedron; corner i = (x ? 1 : 0) | (y ? 2 : 0) | (z ? 4 : 0). CCW from outside. */
    private static final int[][] HEXA_FACES = {
            {4, 5, 7, 6}, // +Z
            {1, 0, 2, 3}, // -Z
            {6, 7, 3, 2}, // +Y
            {0, 1, 5, 4}, // -Y
            {5, 1, 3, 7}, // +X
            {0, 4, 6, 2}  // -X
    };

    /** Solid six-sided shape from 8 corners (indexed as in {@link #HEXA_FACES}); lets you build tapers and wedges. */
    static void solidHexa(PoseStack stack, MultiBufferSource buffer, int color, int light, float[][] corners, boolean inverted) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int[] face : HEXA_FACES) {
            float[] a = corners[face[0]];
            float[] b = corners[face[1]];
            float[] c = corners[face[2]];
            float[] d = corners[face[3]];
            // Face normal from the diagonals (works for non-planar-ish tapered faces too).
            float ux = c[0] - a[0], uy = c[1] - a[1], uz = c[2] - a[2];
            float vx = d[0] - b[0], vy = d[1] - b[1], vz = d[2] - b[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6f) {
                // Degenerate (e.g. a horn tip collapsing to an edge): still emit so the shape stays closed.
                nx = 0; ny = 1; nz = 0;
            } else {
                nx /= len; ny /= len; nz /= len;
            }
            if (inverted) {
                quad(vc, pose, normal, d[0], d[1], d[2], c[0], c[1], c[2], b[0], b[1], b[2], a[0], a[1], a[2],
                        color, light, -nx, -ny, -nz);
            } else {
                quad(vc, pose, normal, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2],
                        color, light, nx, ny, nz);
            }
        }
    }

    static float[][] boxCorners(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            c[i] = new float[]{(i & 1) != 0 ? maxX : minX, (i & 2) != 0 ? maxY : minY, (i & 4) != 0 ? maxZ : minZ};
        }
        return c;
    }

    /** Solid box with an ink outline {@code outline} blocks thick (0 = none). */
    static void solidBox(PoseStack stack, MultiBufferSource buffer, int color, int light,
                         float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float outline) {
        solidHexa(stack, buffer, color, light, boxCorners(minX, minY, minZ, maxX, maxY, maxZ), false);
        if (outline > 0.0f) {
            solidHexa(stack, buffer, 0xFF0D0C12, light, boxCorners(minX - outline, minY - outline, minZ - outline,
                    maxX + outline, maxY + outline, maxZ + outline), true);
        }
    }

    /** Push every corner away from the shape's centre by {@code amount} (outline shell for tapered shapes). */
    static float[][] inflate(float[][] corners, float amount) {
        float cx = 0, cy = 0, cz = 0;
        for (float[] c : corners) { cx += c[0]; cy += c[1]; cz += c[2]; }
        cx /= 8; cy /= 8; cz /= 8;
        float[][] out = new float[8][];
        for (int i = 0; i < 8; i++) {
            float[] c = corners[i];
            out[i] = new float[]{
                    c[0] + Math.signum(c[0] - cx) * amount,
                    c[1] + Math.signum(c[1] - cy) * amount,
                    c[2] + Math.signum(c[2] - cz) * amount};
        }
        return out;
    }

    /** Colour for a point on a unit sphere, given its outward normal. */
    interface SphereColor {
        int at(float nx, float ny, float nz);
    }

    /** Solid UV sphere, culled, coloured per vertex (used for the Eye Pop eyeball). */
    static void solidSphere(PoseStack stack, MultiBufferSource buffer, int light, float radius, int rings, int segments,
                            SphereColor colors, boolean inverted) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(WHITE));
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int i = 0; i < rings; i++) {
            double t0 = Math.PI * i / rings;
            double t1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < segments; j++) {
                double p0 = Math.PI * 2.0 * j / segments;
                double p1 = Math.PI * 2.0 * (j + 1) / segments;
                float[] a = spherePoint(t0, p0);
                float[] b = spherePoint(t0, p1);
                float[] c = spherePoint(t1, p1);
                float[] d = spherePoint(t1, p0);
                float[][] verts = inverted ? new float[][]{d, c, b, a} : new float[][]{a, b, c, d};
                for (float[] v : verts) {
                    int color = colors.at(v[0], v[1], v[2]);
                    float s = inverted ? -1.0f : 1.0f;
                    vc.vertex(pose, v[0] * radius, v[1] * radius, v[2] * radius)
                            .color(color >>> 16 & 255, color >>> 8 & 255, color & 255, 255)
                            .uv(0.5f, 0.5f)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(light)
                            .normal(normal, v[0] * s, v[1] * s, v[2] * s)
                            .endVertex();
                }
            }
        }
    }

    private static float[] spherePoint(double theta, double phi) {
        return new float[]{
                (float) (Math.sin(theta) * Math.cos(phi)),
                (float) Math.cos(theta),
                (float) (Math.sin(theta) * Math.sin(phi))};
    }

    static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) ((color >>> 24 & 255) * alpha)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    static int shade(int color, float factor) {
        int r = Math.min(255, (int) ((color >>> 16 & 255) * factor));
        int g = Math.min(255, (int) ((color >>> 8 & 255) * factor));
        int b = Math.min(255, (int) ((color & 255) * factor));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static void text(Font font, PoseStack stack, MultiBufferSource buffer, Component text, int color) {
        Matrix4f pose = stack.last().pose();
        float x = -font.width(text) / 2.0f;
        font.drawInBatch(text, x, 0.0f, color, false, pose, buffer, true, 0, FULL_BRIGHT);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int color, int light, float normalX, float normalY, float normalZ) {
        int a = color >>> 24 & 255;
        int r = color >>> 16 & 255;
        int g = color >>> 8 & 255;
        int b = color & 255;
        vertex(vc, pose, normal, x1, y1, z1, 0, 1, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x2, y2, z2, 1, 1, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x3, y3, z3, 1, 0, r, g, b, a, light, normalX, normalY, normalZ);
        vertex(vc, pose, normal, x4, y4, z4, 0, 0, r, g, b, a, light, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light,
                               float normalX, float normalY, float normalZ) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}

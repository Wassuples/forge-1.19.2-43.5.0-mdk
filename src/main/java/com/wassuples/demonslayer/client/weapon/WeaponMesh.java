package com.wassuples.demonslayer.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

/**
 * Geometry helpers for the Hashira weapons. Units are model pixels (the renderer scales by 1/16), texture
 * coordinates are atlas pixels on a 128x256 atlas (see {@code gen/hashira_tex.py} for the layout).
 *
 * <p>Everything is emitted as lit quads into an {@code entityCutoutNoCull} buffer, so faces are visible from both
 * sides and decals can use alpha for their silhouettes.
 */
final class WeaponMesh {
    static final float TW = 128f, TH = 256f;

    // swatch indices (match hashira_tex.py)
    static final int SW_WHITE = 0, SW_SPINE = 1, SW_HABAKI = 2, SW_KASHIRA = 3, SW_TSUBA_SIDE = 4, SW_CHAIN = 5,
            SW_BALL = 6, SW_SPIKE = 7, SW_RIBBON = 8, SW_HAFT_END = 9, SW_SEPPA = 10, SW_ACCENT = 11, SW_EDGE = 12, SW_ROD = 13;

    private final PoseStack pose;
    private final VertexConsumer vc;
    private final int light, overlay;

    WeaponMesh(PoseStack pose, VertexConsumer vc, int light, int overlay) {
        this.pose = pose;
        this.vc = vc;
        this.light = light;
        this.overlay = overlay;
    }

    PoseStack pose() {
        return pose;
    }

    // ------------------------------------------------------------------ primitives

    /** One quad; p = {x, y, z}; uv in atlas pixels. Normal from (p1 - p0) x (p3 - p0). */
    void quad(float[] p0, float[] p1, float[] p2, float[] p3,
              float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        float ax = p1[0] - p0[0], ay = p1[1] - p0[1], az = p1[2] - p0[2];
        float bx = p3[0] - p0[0], by = p3[1] - p0[1], bz = p3[2] - p0[2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6f) {
            // degenerate (e.g. a tip); try the other diagonal
            bx = p2[0] - p0[0]; by = p2[1] - p0[1]; bz = p2[2] - p0[2];
            nx = ay * bz - az * by; ny = az * bx - ax * bz; nz = ax * by - ay * bx;
            len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6f) return;
        }
        nx /= len; ny /= len; nz /= len;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        vert(m, n, p0, u0, v0, nx, ny, nz);
        vert(m, n, p1, u1, v1, nx, ny, nz);
        vert(m, n, p2, u2, v2, nx, ny, nz);
        vert(m, n, p3, u3, v3, nx, ny, nz);
    }

    private void vert(Matrix4f m, Matrix3f n, float[] p, float u, float v, float nx, float ny, float nz) {
        vc.vertex(m, p[0], p[1], p[2]).color(255, 255, 255, 255).uv(u / TW, v / TH)
                .overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    static float swU(int sw) {
        return 64 + (sw % 8) * 8 + 4;
    }

    static float swV(int sw) {
        return 64 + (sw / 8) * 8 + 4;
    }

    /** A quad filled with the centre of a swatch cell. */
    void swatchQuad(float[] p0, float[] p1, float[] p2, float[] p3, int sw) {
        float u = swU(sw), v = swV(sw);
        quad(p0, p1, p2, p3, u - 3, v - 3, u - 3, v + 3, u + 3, v + 3, u + 3, v - 3);
    }

    /** Axis-aligned box filled with a swatch. */
    void box(float x0, float y0, float z0, float x1, float y1, float z1, int sw) {
        float[] a = {x0, y0, z0}, b = {x1, y0, z0}, c = {x1, y0, z1}, d = {x0, y0, z1};
        float[] e = {x0, y1, z0}, f = {x1, y1, z0}, g = {x1, y1, z1}, h = {x0, y1, z1};
        swatchQuad(a, b, c, d, sw);   // bottom
        swatchQuad(e, h, g, f, sw);   // top
        swatchQuad(a, e, f, b, sw);   // north
        swatchQuad(c, g, h, d, sw);   // south
        swatchQuad(d, h, e, a, sw);   // west
        swatchQuad(b, f, g, c, sw);   // east
    }

    /**
     * Box whose four sides wrap a vertical texture strip (a sword grip): the strip {@code uw} pixels wide is split
     * into four faces; v runs from {@code v0} at the top (y1) to {@code v0 + vh} at the bottom (y0). Ends use a swatch.
     */
    void wrappedBox(float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float uw, float vh, int capSw) {
        float q = uw / 4f, vt = v0, vb = v0 + vh;
        float[] a = {x0, y0, z0}, b = {x1, y0, z0}, c = {x1, y0, z1}, d = {x0, y0, z1};
        float[] e = {x0, y1, z0}, f = {x1, y1, z0}, g = {x1, y1, z1}, h = {x0, y1, z1};
        quad(a, e, f, b, u0, vb, u0, vt, u0 + q, vt, u0 + q, vb);                 // north
        quad(b, f, g, c, u0 + q, vb, u0 + q, vt, u0 + 2 * q, vt, u0 + 2 * q, vb); // east
        quad(c, g, h, d, u0 + 2 * q, vb, u0 + 2 * q, vt, u0 + 3 * q, vt, u0 + 3 * q, vb); // south
        quad(d, h, e, a, u0 + 3 * q, vb, u0 + 3 * q, vt, u0 + 4 * q, vt, u0 + 4 * q, vb); // west
        swatchQuad(a, b, c, d, capSw);
        swatchQuad(e, h, g, f, capSw);
    }

    /**
     * A flat decal in the XZ plane (a tsuba seen from above), stacked {@code layers} times through {@code thick} so
     * it reads as a plate from the side. Texture rectangle (u0,v0)-(u1,v1) maps x0..x1 / z0..z1.
     */
    void decalH(float x0, float z0, float x1, float z1, float y, float thick, int layers, float u0, float v0, float u1, float v1) {
        for (int i = 0; i < layers; i++) {
            float yy = y + (layers == 1 ? 0 : thick * i / (layers - 1));
            quad(new float[]{x0, yy, z0}, new float[]{x0, yy, z1}, new float[]{x1, yy, z1}, new float[]{x1, yy, z0},
                    u0, v0, u0, v1, u1, v1, u1, v0);
        }
    }

    /** A flat decal in the XY plane (side-on art like an axe head), stacked through z. v0 maps to the top (y1). */
    void decalV(float x0, float y0, float x1, float y1, float z, float thick, int layers, float u0, float v0, float u1, float v1) {
        for (int i = 0; i < layers; i++) {
            float zz = z - thick / 2 + (layers == 1 ? 0 : thick * i / (layers - 1));
            quad(new float[]{x0, y0, zz}, new float[]{x1, y0, zz}, new float[]{x1, y1, zz}, new float[]{x0, y1, zz},
                    u0, v1, u1, v1, u1, v0, u0, v0);
        }
    }

    /** Four-sided pyramid (a spike) standing on a square base centred at c, pointing along unit dir. */
    void spike(float[] c, float[] dir, float base, float height, int sw) {
        float[] up = Math.abs(dir[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        float[] s = norm(cross(dir, up));
        float[] t = cross(s, dir);
        float h = base / 2;
        float[][] q = new float[4][];
        float[][] sgn = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        for (int i = 0; i < 4; i++) {
            q[i] = new float[]{c[0] + (s[0] * sgn[i][0] + t[0] * sgn[i][1]) * h, c[1] + (s[1] * sgn[i][0] + t[1] * sgn[i][1]) * h,
                    c[2] + (s[2] * sgn[i][0] + t[2] * sgn[i][1]) * h};
        }
        float[] tip = {c[0] + dir[0] * height, c[1] + dir[1] * height, c[2] + dir[2] * height};
        for (int i = 0; i < 4; i++) {
            swatchQuad(q[i], q[(i + 1) % 4], tip, tip, sw);
        }
    }

    /**
     * A blade swept along a centreline. For each section: spine point, edge direction and thickness direction (unit),
     * width (spine to edge) and spine thickness; {@code vTex} is the blade-strip v coordinate (0 = tip end).
     * Faces: two flats textured with the blade strip (u 0 = spine, 32 = edge) and the spine with a swatch.
     */
    void blade(float[][] spine, float[][] edgeDir, float[][] thickDir, float[] width, float[] thick, float[] vTex, int spineSw) {
        int n = spine.length;
        float[][] sl = new float[n][], sr = new float[n][], ed = new float[n][];
        for (int i = 0; i < n; i++) {
            float ht = thick[i] / 2;
            sl[i] = add(spine[i], thickDir[i], ht);
            sr[i] = add(spine[i], thickDir[i], -ht);
            ed[i] = add(spine[i], edgeDir[i], width[i]);
        }
        float us = 0.5f, ue = 31.5f;
        for (int i = 0; i < n - 1; i++) {
            float va = vTex[i], vb = vTex[i + 1];
            quad(sl[i], sl[i + 1], ed[i + 1], ed[i], us, va, us, vb, ue, vb, ue, va);
            quad(sr[i], ed[i], ed[i + 1], sr[i + 1], us, va, ue, va, ue, vb, us, vb);
            swatchQuad(sl[i], sr[i], sr[i + 1], sl[i + 1], spineSw);
        }
        // end caps
        swatchQuad(sl[0], ed[0], ed[0], sr[0], spineSw);
        swatchQuad(sl[n - 1], sr[n - 1], ed[n - 1], ed[n - 1], spineSw);
    }

    // ------------------------------------------------------------------ chain

    /** Chain links between consecutive points; links alternate between flat-on and edge-on. */
    void chain(float[][] pts, float linkW, float linkT, int sw) {
        for (int i = 0; i < pts.length - 1; i++) {
            float[] a = pts[i], b = pts[i + 1];
            float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-4f) continue;
            float[] dir = {dx / len, dy / len, dz / len};
            float[] up = Math.abs(dir[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{0, 0, 1};
            float[] s = norm(cross(dir, up));
            float[] t = cross(s, dir);
            if (i % 2 == 1) {
                float[] tmp = s;
                s = t;
                t = tmp;
            }
            float ext = len * 0.62f;  // links overlap a little
            float[] c = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
            orientedBox(c, dir, s, t, ext, linkW / 2, linkT / 2, sw);
        }
    }

    /** Box centred at c with half extents along three unit axes. */
    void orientedBox(float[] c, float[] ax, float[] ay, float[] az, float hx, float hy, float hz, int sw) {
        float[][] p = new float[8][];
        int k = 0;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                for (int l = -1; l <= 1; l += 2) {
                    p[k++] = new float[]{
                            c[0] + ax[0] * hx * i + ay[0] * hy * j + az[0] * hz * l,
                            c[1] + ax[1] * hx * i + ay[1] * hy * j + az[1] * hz * l,
                            c[2] + ax[2] * hx * i + ay[2] * hy * j + az[2] * hz * l};
                }
            }
        }
        // index = (i>0)*4 + (j>0)*2 + (l>0)
        swatchQuad(p[0], p[1], p[3], p[2], sw);
        swatchQuad(p[4], p[6], p[7], p[5], sw);
        swatchQuad(p[0], p[2], p[6], p[4], sw);
        swatchQuad(p[1], p[5], p[7], p[3], sw);
        swatchQuad(p[0], p[4], p[5], p[1], sw);
        swatchQuad(p[2], p[3], p[7], p[6], sw);
    }

    // ------------------------------------------------------------------ vector maths

    static float[] add(float[] a, float[] d, float s) {
        return new float[]{a[0] + d[0] * s, a[1] + d[1] * s, a[2] + d[2] * s};
    }

    static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static float[] norm(float[] a) {
        float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-6f ? new float[]{0, 1, 0} : new float[]{a[0] / l, a[1] / l, a[2] / l};
    }
}

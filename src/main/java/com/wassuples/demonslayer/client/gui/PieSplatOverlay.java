package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Random;

/**
 * First-person cream pie to the face, drawn procedurally (no texture needed): a few lumpy cream blobs with a
 * darker rim and highlights, crust crumbs, and drips that run down the screen. The whole splat slowly slides
 * down and fades. Each splat is randomised.
 */
public class PieSplatOverlay {
    private static final int CREAM = 0xFFFFFAEC;
    private static final int CREAM_RIM = 0xFFE9D9B4;
    private static final int CREAM_HIGHLIGHT = 0xFFFFFFFF;
    private static final int CRUST = 0xFFD39A55;
    private static final int CRUST_DARK = 0xFF9A6428;

    private static int activeTicks = 0;
    private static int totalTicks = 1;
    private static long seed = 0L;

    public static void activate(int durationTicks) {
        if (activeTicks <= 0) {
            seed = System.nanoTime();
        }
        activeTicks = Math.max(activeTicks, durationTicks);
        totalTicks = Math.max(1, activeTicks);
    }

    public static void tick() {
        if (activeTicks > 0) {
            activeTicks--;
        }
    }

    public static final IGuiOverlay HUD_PIE_SPLAT = (gui, poseStack, partialTick, width, height) -> {
        if (activeTicks <= 0) {
            return;
        }

        float elapsed = (totalTicks - activeTicks) + partialTick;
        float alpha = Mth.clamp((activeTicks - partialTick) / 20.0f, 0.0f, 1.0f);
        float slideDown = elapsed * elapsed * 0.012f;
        // Impact punch: the splat lands oversized and settles.
        float impact = elapsed < 5.0f ? 1.0f + 0.25f * (1.0f - elapsed / 5.0f) : 1.0f;

        Random rnd = new Random(seed);
        float cx = width * (0.42f + rnd.nextFloat() * 0.16f);
        float cy = height * (0.38f + rnd.nextFloat() * 0.12f) + slideDown;
        float base = Math.min(width, height) * 0.36f * impact;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull(); // procedural triangles have mixed winding
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        // Main blob + satellite blobs
        int blobs = 4 + rnd.nextInt(3);
        float[] bx = new float[blobs];
        float[] by = new float[blobs];
        float[] br = new float[blobs];
        float[] phase = new float[blobs];
        for (int i = 0; i < blobs; i++) {
            if (i == 0) {
                bx[i] = cx;
                by[i] = cy;
                br[i] = base;
            } else {
                double a = rnd.nextDouble() * Math.PI * 2.0;
                float dist = base * (0.75f + rnd.nextFloat() * 0.55f);
                bx[i] = cx + (float) Math.cos(a) * dist;
                by[i] = cy + (float) Math.sin(a) * dist * 0.8f;
                br[i] = base * (0.22f + rnd.nextFloat() * 0.3f);
            }
            phase[i] = rnd.nextFloat() * 10.0f;
        }
        for (int i = 0; i < blobs; i++) {
            blob(builder, matrix, bx[i], by[i], br[i] * 1.07f + 3.0f, phase[i], alpha, CREAM_RIM);
        }
        for (int i = 0; i < blobs; i++) {
            blob(builder, matrix, bx[i], by[i], br[i], phase[i], alpha, CREAM);
        }
        // glossy highlights
        for (int i = 0; i < blobs; i++) {
            blob(builder, matrix, bx[i] - br[i] * 0.28f, by[i] - br[i] * 0.3f, br[i] * 0.22f, phase[i] + 3.0f, alpha * 0.9f, CREAM_HIGHLIGHT);
        }

        // Drips running down from the blobs
        int drips = 7 + rnd.nextInt(5);
        for (int i = 0; i < drips; i++) {
            int from = rnd.nextInt(blobs);
            float x = bx[from] + (rnd.nextFloat() - 0.5f) * br[from] * 1.4f;
            float top = by[from] + br[from] * 0.4f;
            float speed = 1.2f + rnd.nextFloat() * 2.6f;
            float length = Math.min(height * 0.6f, 10.0f + elapsed * speed);
            float w = 3.0f + rnd.nextFloat() * 5.0f;
            rect(builder, matrix, x - w - 1.5f, top, x + w + 1.5f, top + length, alpha, CREAM_RIM);
            rect(builder, matrix, x - w, top, x + w, top + length, alpha, CREAM);
            circle(builder, matrix, x, top + length, w * 1.6f + 1.5f, 14, alpha, CREAM_RIM);
            circle(builder, matrix, x, top + length, w * 1.6f, 14, alpha, CREAM);
        }

        // Crust crumbs flung around the edge
        int crumbs = 10 + rnd.nextInt(8);
        for (int i = 0; i < crumbs; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            float dist = base * (0.85f + rnd.nextFloat() * 0.7f);
            float x = cx + (float) Math.cos(a) * dist;
            float y = cy + (float) Math.sin(a) * dist * 0.8f;
            float s = 3.0f + rnd.nextFloat() * 7.0f;
            float spin = rnd.nextFloat() * 6.0f;
            quadRot(builder, matrix, x, y, s + 1.5f, spin, alpha, CRUST_DARK);
            quadRot(builder, matrix, x, y, s, spin, alpha, CRUST);
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    };

    /** Lumpy, wobbling circle (cream blob). */
    private static void blob(BufferBuilder b, Matrix4f m, float cx, float cy, float r, float phase, float alpha, int color) {
        int segments = 40;
        float prevX = 0;
        float prevY = 0;
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            float lump = 1.0f + 0.14f * Mth.sin((float) a * 5.0f + phase) + 0.07f * Mth.sin((float) a * 11.0f + phase * 1.7f);
            float x = cx + (float) Math.cos(a) * r * lump;
            float y = cy + (float) Math.sin(a) * r * lump * 0.85f;
            if (i > 0) {
                tri(b, m, cx, cy, prevX, prevY, x, y, alpha, color);
            }
            prevX = x;
            prevY = y;
        }
    }

    private static void circle(BufferBuilder b, Matrix4f m, float cx, float cy, float r, int segments, float alpha, int color) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            tri(b, m, cx, cy, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r,
                    cx + (float) Math.cos(a1) * r, cy + (float) Math.sin(a1) * r, alpha, color);
        }
    }

    private static void rect(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float alpha, int color) {
        tri(b, m, x0, y0, x0, y1, x1, y1, alpha, color);
        tri(b, m, x0, y0, x1, y1, x1, y0, alpha, color);
    }

    private static void quadRot(BufferBuilder b, Matrix4f m, float cx, float cy, float s, float angle, float alpha, int color) {
        float c = Mth.cos(angle) * s;
        float sn = Mth.sin(angle) * s;
        float x0 = cx - c + sn, y0 = cy - sn - c;
        float x1 = cx + c + sn, y1 = cy + sn - c;
        float x2 = cx + c - sn, y2 = cy + sn + c;
        float x3 = cx - c - sn, y3 = cy - sn + c;
        tri(b, m, x0, y0, x1, y1, x2, y2, alpha, color);
        tri(b, m, x0, y0, x2, y2, x3, y3, alpha, color);
    }

    private static void tri(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2,
                            float alpha, int color) {
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float bl = (color & 255) / 255.0f;
        float a = (color >>> 24) / 255.0f * alpha;
        b.vertex(m, x0, y0, 0.0f).color(r, g, bl, a).endVertex();
        b.vertex(m, x1, y1, 0.0f).color(r, g, bl, a).endVertex();
        b.vertex(m, x2, y2, 0.0f).color(r, g, bl, a).endVertex();
    }
}

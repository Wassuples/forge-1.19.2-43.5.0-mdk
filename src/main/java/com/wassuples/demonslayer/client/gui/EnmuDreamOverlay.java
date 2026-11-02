package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.effects.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * What Forced Sleep looks like from inside: the screen edges sink into a breathing pink-violet dream haze, a slow
 * spiral turns in the middle of your vision, soft "z" drift upward, and the wake-up QTE is shown as proper key
 * caps (done / current / to do) instead of a line of chat-coloured text.
 *
 * <p>Note: the old {@code SleepQTEOverlay} was never registered, so players hit by Forced Sleep never actually
 * saw which keys to press. This overlay replaces it.
 */
public final class EnmuDreamOverlay {
    private EnmuDreamOverlay() {
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean asleep = mc.player.hasEffect(ModEffects.FORCED_SLEEP.get());
        boolean qte = SleepQTEHandler.isActive();
        if (!asleep && !qte) return;

        float t = mc.player.tickCount + partialTick;
        drawHaze(pose, width, height, t);
        drawDriftingZ(pose, mc.font, width, height, t);
        if (qte) {
            drawQte(pose, mc.font, width, height, t);
        }
    };

    // ------------------------------------------------------------------ haze + spiral

    private static void drawHaze(PoseStack pose, int width, int height, float t) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();

        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float breathe = 0.5f + 0.5f * Mth.sin(t * 0.07f);
        float outer = (float) Math.hypot(cx, cy) * 1.05f;
        float inner = outer * (0.52f - 0.08f * breathe);
        int segments = 48;
        // Radial gradient: transparent inside, dream colour at the edges (colour slowly shifts pink ↔ violet).
        float shift = 0.5f + 0.5f * Mth.sin(t * 0.03f);
        float r = Mth.lerp(shift, 0.93f, 0.55f);
        float g = Mth.lerp(shift, 0.55f, 0.32f);
        float bl = Mth.lerp(shift, 0.85f, 0.95f);
        float edgeAlpha = 0.62f + 0.12f * breathe;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float wob0 = 1.0f + 0.04f * Mth.sin((float) a0 * 6.0f + t * 0.1f);
            float wob1 = 1.0f + 0.04f * Mth.sin((float) a1 * 6.0f + t * 0.1f);
            float ix0 = cx + (float) Math.cos(a0) * inner * wob0, iy0 = cy + (float) Math.sin(a0) * inner * wob0;
            float ix1 = cx + (float) Math.cos(a1) * inner * wob1, iy1 = cy + (float) Math.sin(a1) * inner * wob1;
            float ox0 = cx + (float) Math.cos(a0) * outer, oy0 = cy + (float) Math.sin(a0) * outer;
            float ox1 = cx + (float) Math.cos(a1) * outer, oy1 = cy + (float) Math.sin(a1) * outer;
            vertex(b, m, ix0, iy0, r, g, bl, 0.0f);
            vertex(b, m, ox0, oy0, r, g, bl, edgeAlpha);
            vertex(b, m, ox1, oy1, r, g, bl, edgeAlpha);
            vertex(b, m, ix0, iy0, r, g, bl, 0.0f);
            vertex(b, m, ox1, oy1, r, g, bl, edgeAlpha);
            vertex(b, m, ix1, iy1, r, g, bl, 0.0f);
        }

        // Hypnotic spiral: three soft arms turning slowly through the centre.
        float maxR = Math.min(width, height) * 0.42f;
        for (int arm = 0; arm < 3; arm++) {
            double base = arm * Math.PI * 2.0 / 3.0 + t * 0.025;
            for (int s = 0; s < 60; s++) {
                double f0 = s / 60.0;
                double f1 = (s + 1) / 60.0;
                double a0 = base + f0 * Math.PI * 3.0;
                double a1 = base + f1 * Math.PI * 3.0;
                float r0 = (float) (f0 * maxR);
                float r1 = (float) (f1 * maxR);
                float w = 1.5f + 3.0f * (float) f0;
                float alpha = 0.10f * (float) Math.sin(Math.PI * f0);
                float x0 = cx + (float) Math.cos(a0) * r0, y0 = cy + (float) Math.sin(a0) * r0;
                float x1 = cx + (float) Math.cos(a1) * r1, y1 = cy + (float) Math.sin(a1) * r1;
                float nx = -(y1 - y0), ny = x1 - x0;
                float nl = Mth.sqrt(nx * nx + ny * ny);
                if (nl < 1.0E-4f) continue;
                nx = nx / nl * w;
                ny = ny / nl * w;
                vertex(b, m, x0 - nx, y0 - ny, 1.0f, 0.85f, 1.0f, alpha);
                vertex(b, m, x0 + nx, y0 + ny, 1.0f, 0.85f, 1.0f, alpha);
                vertex(b, m, x1 + nx, y1 + ny, 1.0f, 0.85f, 1.0f, alpha);
                vertex(b, m, x0 - nx, y0 - ny, 1.0f, 0.85f, 1.0f, alpha);
                vertex(b, m, x1 + nx, y1 + ny, 1.0f, 0.85f, 1.0f, alpha);
                vertex(b, m, x1 - nx, y1 - ny, 1.0f, 0.85f, 1.0f, alpha);
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float r, float g, float bl, float a) {
        b.vertex(m, x, y, 0.0f).color(r, g, bl, a).endVertex();
    }

    private static void drawDriftingZ(PoseStack pose, Font font, int width, int height, float t) {
        for (int i = 0; i < 6; i++) {
            float cycle = ((t * 0.6f + i * 37.0f) % 140.0f) / 140.0f;
            float alpha = Mth.sin((float) Math.PI * cycle);
            if (alpha < 0.05f) continue;
            float x = width * (0.12f + 0.15f * i) + Mth.sin(t * 0.04f + i) * 12.0f;
            float y = height * (0.9f - cycle * 0.75f);
            float scale = 1.5f + cycle * 2.0f;
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1.0f);
            int a = (int) (alpha * 150);
            font.draw(pose, i % 2 == 0 ? "Z" : "z", 0, 0, (a << 24) | 0xF6D8FF);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ QTE key caps

    private static void drawQte(PoseStack pose, Font font, int width, int height, float t) {
        List<Integer> sequence = SleepQTEHandler.getKeySequence();
        int current = SleepQTEHandler.getCurrentIndex();
        if (sequence.isEmpty()) return;

        int cap = 26;
        int gap = 8;
        int total = sequence.size() * cap + (sequence.size() - 1) * gap;
        int x0 = width / 2 - total / 2;
        int y0 = height / 2 + 34;

        // Title, pulsing.
        float pulse = 0.5f + 0.5f * Mth.sin(t * 0.3f);
        pose.pushPose();
        pose.translate(width / 2.0f, y0 - 22, 0);
        float titleScale = 1.6f + 0.1f * pulse;
        pose.scale(titleScale, titleScale, 1.0f);
        GuiComponent.drawCenteredString(pose, font, "WAKE UP!", 0, 0, 0xFFFF6AA8);
        pose.popPose();

        for (int i = 0; i < sequence.size(); i++) {
            int x = x0 + i * (cap + gap);
            boolean done = i < current;
            boolean now = i == current;
            int lift = now ? (int) (-2 - 2 * pulse) : 0;
            int border = done ? 0xFF3FD27A : now ? 0xFFFFE066 : 0xFF5A4A6A;
            int face = done ? 0xFF1E6B40 : now ? 0xFF6B4A12 : 0xFF2A2236;
            // drop shadow, border, face, top highlight
            GuiComponent.fill(pose, x + 2, y0 + 3, x + cap + 2, y0 + cap + 3, 0x80000000);
            GuiComponent.fill(pose, x, y0 + lift, x + cap, y0 + cap + lift, border);
            GuiComponent.fill(pose, x + 2, y0 + 2 + lift, x + cap - 2, y0 + cap - 2 + lift, face);
            GuiComponent.fill(pose, x + 2, y0 + 2 + lift, x + cap - 2, y0 + 5 + lift, 0x30FFFFFF);
            if (now) {
                int glowA = (int) (60 + 80 * pulse) << 24;
                GuiComponent.fill(pose, x - 3, y0 - 3 + lift, x + cap + 3, y0 + lift - 1, glowA | 0xFFE066);
                GuiComponent.fill(pose, x - 3, y0 + cap + lift + 1, x + cap + 3, y0 + cap + 3 + lift, glowA | 0xFFE066);
            }
            String label = done ? "✔" : SleepQTEHandler.getKeyName(sequence.get(i));
            if (font.width(label) > cap - 6) label = label.substring(0, 1);
            GuiComponent.drawCenteredString(pose, font, label, x + cap / 2, y0 + cap / 2 - 4 + lift,
                    done ? 0xFFB6FFD2 : now ? 0xFFFFF4C2 : 0xFFB0A0C0);
        }
        GuiComponent.drawCenteredString(pose, font, "Press the keys in order", width / 2, y0 + cap + 8, 0xC0E8D8F0);
    }
}

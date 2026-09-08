package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class PieSplatOverlay {
    private static int activeTicks = 0;
    private static int maxTicks = 1;
    private static final float[][] BLOBS = {
            {0.50f, 0.45f, 0.25f},
            {0.33f, 0.35f, 0.13f},
            {0.66f, 0.37f, 0.13f},
            {0.42f, 0.60f, 0.11f},
            {0.58f, 0.58f, 0.11f},
            {0.23f, 0.47f, 0.08f},
            {0.77f, 0.48f, 0.08f}
    };

    public static void activate(int durationTicks) {
        activeTicks = Math.max(activeTicks, durationTicks);
        maxTicks = Math.max(maxTicks, durationTicks);
    }

    public static void tick() {
        if (activeTicks > 0) {
            activeTicks--;
        } else {
            maxTicks = 1;
        }
    }

    public static final IGuiOverlay HUD_PIE_SPLAT = (gui, poseStack, partialTick, width, height) -> {
        if (activeTicks <= 0) {
            return;
        }

        float phase = 1.0f - (activeTicks / (float) Math.max(1, maxTicks));
        int fadeAlpha = (int) (Mth.clamp(activeTicks / (float) Math.max(20, maxTicks / 2), 0.25f, 1.0f) * 220.0f);
        int tint = (fadeAlpha << 24) | 0xE8BC6B;
        GuiComponent.fill(poseStack, 0, 0, width, height, tint);

        for (float[] blob : BLOBS) {
            int cx = (int) (blob[0] * width);
            int cy = (int) (blob[1] * height);
            int radius = (int) (blob[2] * Math.min(width, height));
            drawBlob(poseStack, cx, cy, radius, (Math.min(255, fadeAlpha + 25) << 24) | 0xF0DA98);
        }

        for (int i = 0; i < 8; i++) {
            float px = (i + 1) / 9.0f;
            int x = (int) (width * (0.15f + px * 0.7f));
            int top = (int) (height * (0.18f + (i % 3) * 0.02f));
            int drip = (int) (height * (0.1f + phase * (0.12f + i * 0.017f)));
            int w = 4 + (i % 3);
            int bottom = Math.min(height, top + drip);
            GuiComponent.fill(poseStack, x, top, x + w, bottom, (Math.min(255, fadeAlpha + 15) << 24) | 0xFFDD9A);
            drawBlob(poseStack, x + w / 2, Math.min(height - 2, bottom), 4 + (i % 2), (Math.min(255, fadeAlpha + 10) << 24) | 0xFFD184);
        }
    };

    private static void drawBlob(PoseStack poseStack, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int span = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
            GuiComponent.fill(poseStack, centerX - span, centerY + y, centerX + span + 1, centerY + y + 1, color);
        }
    }
}

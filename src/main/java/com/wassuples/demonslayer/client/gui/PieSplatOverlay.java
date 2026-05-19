package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class PieSplatOverlay {
    private static final ResourceLocation SPLAT_TEXTURE = new ResourceLocation("textures/misc/pumpkinblur.png");
    private static int activeTicks = 0;

    public static void activate(int durationTicks) {
        activeTicks = Math.max(activeTicks, durationTicks);
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

        float alpha = Math.min(1.0f, activeTicks / 20.0f);
        RenderSystem.setShaderColor(1.0f, 0.8f, 0.6f, alpha);
        RenderSystem.setShaderTexture(0, SPLAT_TEXTURE);
        GuiComponent.blit(poseStack, 0, 0, -90, 0.0f, 0.0f, width, height, width, height);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // Dripping particles logic
            java.util.Random random = new java.util.Random(42); // Consistent seed for "layers" of drips
            for (int i = 0; i < 8; i++) {
                int x = random.nextInt(width);
                int startY = random.nextInt(height / 3);
                int dripSpeed = 2 + random.nextInt(4);
                int currentY = startY + ((60 - activeTicks) * dripSpeed);

                if (currentY < height) {
                    int dripHeight = 10 + random.nextInt(20);
                    GuiComponent.fill(poseStack, x, currentY, x + 3, currentY + dripHeight, 0xAAFFE1A3);
                }
            }
        }
    };
}


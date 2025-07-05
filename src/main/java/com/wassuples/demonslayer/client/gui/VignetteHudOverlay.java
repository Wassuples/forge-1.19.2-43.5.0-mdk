package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.events.ClientEvents;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class VignetteHudOverlay {
    private static final ResourceLocation VIGNETTE_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/vignette.png");

    // This contains the exact same drawing logic as before, now as an IGuiOverlay.
    public static final IGuiOverlay HUD_VIGNETTE = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        // We still check our flag from ClientEvents to see if we should draw.
        if (!ClientEvents.ClientForgeEvents.isVignetteActive) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 0.1f, 0.1f, 0.4f); // Red tint, 40% opaque
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // Draw our vignette texture stretched across the entire screen.
        gui.blit(poseStack, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    };
}
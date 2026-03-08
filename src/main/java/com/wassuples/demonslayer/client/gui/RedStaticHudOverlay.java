package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.client.DemonThreatEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * A subtle red static/noise overlay used by demon intimidation.
 */
public class RedStaticHudOverlay {
    private static final ResourceLocation STATIC_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/red_static.png");

    public static final IGuiOverlay HUD_RED_STATIC = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        if (!DemonThreatEffects.isActive()) return;

        float alpha = DemonThreatEffects.getStaticAlpha();
        if (alpha <= 0.001f) return;

        // Add a tiny flicker to keep it from looking like a flat tint.
        float flicker = 0.85f + 0.15f * (Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() % 4 / 3.0f
                : 1.0f);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1.0f, 0.05f, 0.05f, alpha * flicker);
        RenderSystem.setShaderTexture(0, STATIC_TEXTURE);
        GuiComponent.blit(poseStack, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    };
}


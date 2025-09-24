package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BreathHudOverlay {
    private static final ResourceLocation BREATH_BAR_TEXTURE =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/breath_bar.png");

    public static final IGuiOverlay HUD_BREATH = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        int barWidth = 12;
        int barHeight = 50;
        int xPos = 10;
        int yPos = (screenHeight / 2) - (barHeight / 2);

        if (!Minecraft.getInstance().options.hideGui && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isCreative() && !Minecraft.getInstance().player.isSpectator()) {
            Minecraft.getInstance().player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(playerBreath -> {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, BREATH_BAR_TEXTURE);

                // --- Draw the Background ---
                GuiComponent.blit(poseStack, xPos, yPos, 0, 0, barWidth, barHeight, 12, 100);

                // --- Draw the Foreground ---
                int currentBreath = playerBreath.getBreath();
                int maxBreath = playerBreath.getMaxBreath();
                int fillAmount = (int) (((float) currentBreath / maxBreath) * barHeight);

                // Using the same explicit blit method for the foreground
                GuiComponent.blit(poseStack,
                        xPos,
                        yPos + barHeight - fillAmount,
                        0,       // u-offset of the filled texture part
                        50,      // v-offset of the filled texture part
                        barWidth,
                        fillAmount,
                        12,      // total texture width
                        100);    // total texture height

                // The debug text proves the data is correct, so you can comment this out if you wish.
                String debugText = "Breath: " + currentBreath + "/" + maxBreath;
                GuiComponent.drawString(poseStack, Minecraft.getInstance().font, debugText, 5, 5, 0xFFFFFF);
            });
        }
    };
}
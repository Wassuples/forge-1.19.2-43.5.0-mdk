package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client GUI component: Ranking Board Screen.
 */
public class RankingBoardScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/ranking_board.png");
    private final List<DemonDisplayData> kizuki;
    private final List<DemonDisplayData> fodder;

    public RankingBoardScreen(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder) {
        super(Component.literal("Demon Ranking Board"));
        this.kizuki = kizuki;
        this.fodder = fodder;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        this.blit(pPoseStack, x, y, 0, 0, 256, 256);

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, y + 15, 0xFFFFFF);

        int startX = x + 20;
        int startY = y + 40;

        drawString(pPoseStack, this.font, "The Twelve Kizuki", startX, startY, 0xFFD700);
        startY += 12;
        for (DemonDisplayData data : this.kizuki) {
            String line = String.format("%-14s | %s %s", data.rank(), data.name(), data.title());
            drawString(pPoseStack, this.font, line, startX, startY, 0xFFFFFF);
            startY += 10;
        }

        startY += 10;
        drawString(pPoseStack, this.font, "Top Fodder", startX, startY, 0xAAAAAA);
        startY += 12;
        for (DemonDisplayData data : this.fodder) {
            String line = String.format("%s %s (BP: %d)", data.name(), data.title(), data.bloodPoints());
            drawString(pPoseStack, this.font, line, startX, startY, 0xFFFFFF);
            startY += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
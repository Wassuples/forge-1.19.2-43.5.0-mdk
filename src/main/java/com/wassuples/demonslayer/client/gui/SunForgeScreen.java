package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.inventory.SunForgeMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Sun Forge screen in the mod's UI style: two ingredient slots feeding a sunbeam arrow into a gold-framed output,
 * a sun/moon badge showing whether the forge can work right now, and hover tooltips for both.
 */
public class SunForgeScreen extends AbstractContainerScreen<SunForgeMenu> {
    private static final ResourceLocation WIDGETS = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/station_widgets.png");
    private static final int ARROW_X = 79, ARROW_Y = 35, ARROW_W = 24, ARROW_H = 17;
    private static final int BADGE_X = 154, BADGE_Y = 4;

    public SunForgeScreen(SunForgeMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        KnyUi.panel(pose, x, y, imageWidth, imageHeight);
        GuiComponent.fill(pose, x + 4, y + 4, x + imageWidth - 4, y + 16, 0x60080405);
        GuiComponent.fill(pose, x + 4, y + 16, x + imageWidth - 4, y + 17, KnyUi.accent(0xA0));
        GuiComponent.fill(pose, x + 7, y + inventoryLabelY - 3, x + imageWidth - 7, y + inventoryLabelY - 2, 0x50C9A04A);

        // faint sun emblem behind the work area
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WIDGETS);
        RenderSystem.setShaderColor(1f, 1f, 1f, menu.isSunlit() ? 1f : 0.35f);
        blit(pose, x + 67, y + 20, 64, 0, 48, 48);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // gold bracket joining the two inputs to the arrow
        int bx = x + 61;
        GuiComponent.fill(pose, bx, y + 32, bx + 6, y + 33, KnyUi.GOLD_DARK);
        GuiComponent.fill(pose, bx, y + 56, bx + 6, y + 57, KnyUi.GOLD_DARK);
        GuiComponent.fill(pose, bx + 6, y + 32, bx + 7, y + 57, KnyUi.GOLD_DARK);
        GuiComponent.fill(pose, bx + 7, y + 43, x + ARROW_X, y + 44, KnyUi.GOLD_DARK);

        for (Slot s : menu.slots) {
            if (s.index == 2 && s.container != this.minecraft.player.getInventory()) KnyUi.resultSlot(pose, x + s.x, y + s.y);
            else KnyUi.slot(pose, x + s.x, y + s.y);
        }

        RenderSystem.setShaderTexture(0, WIDGETS);
        blit(pose, x + ARROW_X, y + ARROW_Y, 0, 0, ARROW_W, ARROW_H);
        int p = menu.getScaledProgress(ARROW_W);
        if (p > 0) blit(pose, x + ARROW_X, y + ARROW_Y, 0, 17, p, ARROW_H);
        blit(pose, x + BADGE_X, y + BADGE_Y - 2, menu.isSunlit() ? 32 : 48, 0, 16, 16);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.drawShadow(pose, this.title, this.titleLabelX, this.titleLabelY, KnyUi.GOLD_LIGHT);
        this.font.draw(pose, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, KnyUi.DIM);
        String status = menu.isSunlit() ? (menu.isCrafting() ? "Forging" : "Sunlit") : "No sun";
        int col = menu.isSunlit() ? KnyUi.GOLD : KnyUi.BAD;
        this.font.draw(pose, status, BADGE_X - 3 - this.font.width(status), this.titleLabelY, col);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float delta) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, delta);
        renderTooltip(pose, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= ARROW_X && rx < ARROW_X + ARROW_W && ry >= ARROW_Y && ry < ARROW_Y + ARROW_H) {
            renderComponentTooltip(pose, List.of(Component.literal(menu.isCrafting() ? "Forging: " + menu.getProgressPercent() + "%" : "Idle"),
                    Component.literal("Raw Scarlet + Iron -> Crimson Iron Sand").withStyle(s -> s.withColor(0x9A8E8E)),
                    Component.literal("Crimson Iron Sand + Iron -> Nichirin Steel").withStyle(s -> s.withColor(0x9A8E8E))), mouseX, mouseY);
        } else if (rx >= BADGE_X && rx < BADGE_X + 16 && ry >= BADGE_Y - 2 && ry < BADGE_Y + 14) {
            renderComponentTooltip(pose, menu.isSunlit()
                    ? List.of(Component.literal("Sunlit"), Component.literal("The forge draws heat from the sun.").withStyle(s -> s.withColor(0x9A8E8E)))
                    : List.of(Component.literal("No sunlight"), Component.literal("Needs open sky above it during a clear day.").withStyle(s -> s.withColor(0x9A8E8E)),
                    Component.literal("Progress cools away while it waits.").withStyle(s -> s.withColor(0x9A8E8E))), mouseX, mouseY);
        }
    }
}

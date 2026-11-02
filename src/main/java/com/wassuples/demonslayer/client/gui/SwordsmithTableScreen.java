package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.inventory.SwordsmithTableMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Swordsmith's Table screen in the mod's UI style: an inset 3x3 forging grid, an arrow, and a gold-framed result.
 */
public class SwordsmithTableScreen extends AbstractContainerScreen<SwordsmithTableMenu> {
    private static final ResourceLocation WIDGETS = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/station_widgets.png");
    private static final int ARROW_X = 92, ARROW_Y = 35;

    public SwordsmithTableScreen(SwordsmithTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
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

        // grid well
        KnyUi.inset(pose, x + 25, y + 13, 64, 62);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, WIDGETS);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        blit(pose, x + 120, y + 18, 112, 0, 48, 48);   // faint katana emblem behind the result

        for (Slot s : menu.slots) {
            if (s.index == SwordsmithTableMenu.RESULT_SLOT && s.container != this.minecraft.player.getInventory()) KnyUi.resultSlot(pose, x + s.x, y + s.y);
            else KnyUi.slot(pose, x + s.x, y + s.y);
        }

        RenderSystem.setShaderTexture(0, WIDGETS);
        boolean ready = menu.getSlot(SwordsmithTableMenu.RESULT_SLOT).hasItem();
        blit(pose, x + ARROW_X, y + ARROW_Y, 0, ready ? 17 : 34, 24, 17);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.drawShadow(pose, this.title, this.titleLabelX, this.titleLabelY, KnyUi.GOLD_LIGHT);
        this.font.draw(pose, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, KnyUi.DIM);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float delta) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, delta);
        renderTooltip(pose, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= ARROW_X && rx < ARROW_X + 24 && ry >= ARROW_Y && ry < ARROW_Y + 17) {
            renderComponentTooltip(pose, List.of(Component.literal("Swordsmithing"),
                    Component.literal("Only blades and smithing patterns forge here.").withStyle(s -> s.withColor(0x9A8E8E)),
                    Component.literal("Nichirin Blade: 2 Nichirin Steel over a Stick").withStyle(s -> s.withColor(0x9A8E8E))), mouseX, mouseY);
        }
    }
}

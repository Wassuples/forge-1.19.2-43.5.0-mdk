package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.TamayoOfferResponseC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A simple, multi-page dialogue screen with explicit consent at the end.
 *
 * IMPORTANT: This is client-side UI only. The server validates everything before applying the transformation.
 */
public class TamayoConsentScreen extends Screen {
    private final int tamayoEntityId;
    private final List<Component> pages;
    private int pageIndex = 0;

    private Button nextButton;
    private Button backButton;
    private Button acceptButton;
    private Button declineButton;

    public TamayoConsentScreen(int tamayoEntityId, List<Component> pages) {
        super(Component.literal("Tamayo"));
        this.tamayoEntityId = tamayoEntityId;
        this.pages = pages;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int buttonY = this.height - 28;

        this.backButton = this.addRenderableWidget(new Button(centerX - 110, buttonY, 60, 20, Component.literal("Back"), b -> {
            if (pageIndex > 0) pageIndex--;
            updateButtons();
        }));

        this.nextButton = this.addRenderableWidget(new Button(centerX - 45, buttonY, 60, 20, Component.literal("Next"), b -> {
            if (pageIndex < pages.size() - 1) pageIndex++;
            updateButtons();
        }));

        this.acceptButton = this.addRenderableWidget(new Button(centerX + 20, buttonY, 80, 20, Component.literal("I consent"), b -> {
            ModMessages.sendToServer(new TamayoOfferResponseC2SPacket(this.tamayoEntityId, true));
            this.onClose();
        }));

        this.declineButton = this.addRenderableWidget(new Button(centerX + 105, buttonY, 80, 20, Component.literal("No"), b -> {
            ModMessages.sendToServer(new TamayoOfferResponseC2SPacket(this.tamayoEntityId, false));
            this.onClose();
        }));

        updateButtons();
    }

    private void updateButtons() {
        boolean last = this.pageIndex >= this.pages.size() - 1;
        this.backButton.active = this.pageIndex > 0;
        this.nextButton.active = !last;
        this.acceptButton.visible = last;
        this.declineButton.visible = last;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        int margin = 20;
        int maxWidth = this.width - margin * 2;

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, margin, 0xFFFFFF);

        if (!pages.isEmpty()) {
            Component page = pages.get(Math.min(pageIndex, pages.size() - 1));
            int textTop = margin + 20;
            this.font.drawWordWrap(page, margin, textTop, maxWidth, 0xEEEEEE);
        }

        // Page indicator
        String indicator = (pageIndex + 1) + " / " + Math.max(1, pages.size());
        drawCenteredString(poseStack, this.font, indicator, this.width / 2, this.height - 54, 0xAAAAAA);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().setScreen(null);
    }
}

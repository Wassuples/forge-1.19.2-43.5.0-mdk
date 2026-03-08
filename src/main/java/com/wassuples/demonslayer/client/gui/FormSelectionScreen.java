package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.SelectFormC2SPacket;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.KeyBinding;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Client GUI component: Form Selection Screen.
 */
public class FormSelectionScreen extends Screen {
    private static final ResourceLocation WHEEL_BACKGROUND =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/wheel_background.png");
    private static final ResourceLocation WHEEL_CENTER =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/wheel_center.png");

    private final List<IForm> availableForms;
    private final boolean isBda;
    private int selectedFormIndex = -1;

    private static final int OUTER_RADIUS = 110;
    private static final int INNER_RADIUS = 45;
    private static final int ICON_RADIUS = 78;
    private static final int ICON_SIZE = 24;

    public FormSelectionScreen(List<IForm> availableForms, boolean isBda) {
        super(Component.literal("Form Selection"));
        this.availableForms = availableForms;
        this.isBda = isBda;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int numForms = availableForms.size();

        if (numForms == 0) return;

        // --- 1. Calculate Hover ---
        double mouseAngle = Mth.atan2(mouseY - centerY, mouseX - centerX);
        double mouseDist = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        double angleStep = 2 * Math.PI / numForms;

        this.selectedFormIndex = -1;
        if (mouseDist > INNER_RADIUS && mouseDist < OUTER_RADIUS) {
            double normalizedAngle = mouseAngle < 0 ? mouseAngle + 2 * Math.PI : mouseAngle;
            this.selectedFormIndex = (int) Math.round(normalizedAngle / angleStep) % numForms;
        }

        // --- 2. Draw Background and Segments ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw the main background texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, WHEEL_BACKGROUND);
        blit(poseStack, centerX - 128, centerY - 128, 0, 0, 256, 256);

        // Define colors for the segments
        int segmentColor = 0x90222222; // Semi-transparent dark gray
        int hoverColor = 0x90888888; // Lighter gray for hover

        // Draw all segments procedurally
        for (int i = 0; i < numForms; i++) {
            float startAngle = (float) (i * angleStep - (angleStep / 2));
            float endAngle = (float) (startAngle + angleStep);
            int color = (i == selectedFormIndex) ? hoverColor : segmentColor;
            drawSegment(poseStack, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, startAngle, endAngle, color);
        }

        // --- 3. Draw Icons and Centerpiece ---
        // Draw the form icons
        for (int i = 0; i < numForms; i++) {
            IForm form = availableForms.get(i); // MODIFIED: Now an IForm
            double iconAngle = angleStep * i;
            int x = centerX + (int) (ICON_RADIUS * Math.cos(iconAngle));
            int y = centerY + (int) (ICON_RADIUS * Math.sin(iconAngle));

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, form.getIcon()); // This now works for both!
            blit(poseStack, x - ICON_SIZE / 2, y - ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }

        RenderSystem.setShaderTexture(0, WHEEL_CENTER);
        blit(poseStack, centerX - 32, centerY - 32, 0, 0, 64, 64, 64, 64);

        if (selectedFormIndex != -1) {
            IForm selectedForm = availableForms.get(selectedFormIndex);
            drawCenteredString(poseStack, this.font, selectedForm.getDisplayName(), centerX, centerY, 0xFFFFFF);
        }
    }

    private void drawSegment(PoseStack poseStack, float x, float y, float rIn, float rOut, float startAngle, float endAngle, int color) {
        float margin = 0.025f;
        startAngle += margin;
        endAngle -= margin;

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        RenderSystem.disableTexture();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        // Create the segment by drawing a strip of triangles
        for (float angle = startAngle; angle <= endAngle; angle += 0.05f) { // Small step for a smooth curve
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            bufferBuilder.vertex(matrix, x + rIn * cos, y + rIn * sin, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, x + rOut * cos, y + rOut * sin, 0).color(r, g, b, a).endVertex();
        }

        Tesselator.getInstance().end();
        RenderSystem.enableTexture();
    }

    private void selectForm() {
        if (this.selectedFormIndex != -1) {
            IForm selectedForm = availableForms.get(this.selectedFormIndex);
            int ordinal = ((Enum<?>) selectedForm).ordinal();
            ModMessages.sendToServer(new SelectFormC2SPacket(ordinal, this.isBda));
            this.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.selectedFormIndex != -1) {
            selectForm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyBinding.CHANGE_KEY.getKey().getValue()) {
            selectForm();
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
        }
    }
}
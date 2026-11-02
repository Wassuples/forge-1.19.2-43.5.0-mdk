package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.client.ClientCooldowns;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.SelectFormC2SPacket;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.KeyBinding;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The form wheel (hold the change-form key). Reworked to be readable at a glance:
 * <ul>
 *   <li>a title naming what you're choosing from (your Blood Demon Art, or your breathing style);</li>
 *   <li>crimson segments for Blood Demon Arts, the currently selected form outlined in gold;</li>
 *   <li>forms on cooldown are darkened with the seconds left on them; BDA forms you can't afford (hunger) are
 *   tinted red;</li>
 *   <li>the hovered form's name wraps in the centre instead of overflowing, with its cost and cooldown, and its
 *   description under the wheel;</li>
 *   <li>releasing the change-form key selects, whether it's bound to a key or to a mouse button.</li>
 * </ul>
 */
public class FormSelectionScreen extends Screen {
    private static final ResourceLocation WHEEL_BACKGROUND =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/wheel_background.png");
    private static final ResourceLocation WHEEL_CENTER =
            new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/wheel_hub.png");

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
        if (numForms == 0 || this.minecraft == null || this.minecraft.player == null) return;

        // --- Hover ---
        double mouseAngle = Mth.atan2(mouseY - centerY, mouseX - centerX);
        double mouseDist = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        double angleStep = 2 * Math.PI / numForms;
        this.selectedFormIndex = -1;
        if (mouseDist > INNER_RADIUS && mouseDist < OUTER_RADIUS + 20) {
            double normalizedAngle = mouseAngle < 0 ? mouseAngle + 2 * Math.PI : mouseAngle;
            this.selectedFormIndex = (int) Math.round(normalizedAngle / angleStep) % numForms;
        }

        IForm current = currentForm();
        int food = this.minecraft.player.getFoodData().getFoodLevel();
        boolean creative = this.minecraft.player.getAbilities().instabuild;

        // --- Title ---
        Component title = titleText();
        int titleW = this.font.width(title) + 16;
        KnyUi.panel(poseStack, centerX - titleW / 2, centerY - OUTER_RADIUS - 26, titleW, 16);
        drawCenteredString(poseStack, this.font, title, centerX, centerY - OUTER_RADIUS - 22, isBda ? 0xFFE6E6 : 0xE6F0FF);

        // --- Background and segments ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, WHEEL_BACKGROUND);
        blit(poseStack, centerX - 128, centerY - 128, 0, 0, 256, 256);

        for (int i = 0; i < numForms; i++) {
            IForm form = availableForms.get(i);
            float startAngle = (float) (i * angleStep - (angleStep / 2));
            float endAngle = (float) (startAngle + angleStep);
            boolean hover = i == selectedFormIndex;
            boolean poor = isBda && !creative && form.getBreathCost() > food;
            int color;
            if (isBda) {
                color = hover ? 0xC0B8283A : 0x90401018;
                if (poor) color = hover ? 0xC0D03030 : 0x90701010;
            } else {
                color = hover ? KnyUi.blend(KnyUi.WISTERIA, 0xC0000000, 0.35f) : 0x90241A30;
            }
            drawSegment(poseStack, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, startAngle, endAngle, color);
            if (form == current) {
                drawSegment(poseStack, centerX, centerY, OUTER_RADIUS, OUTER_RADIUS + 3, startAngle, endAngle, 0xFFE8C15A);
            }
        }

        // --- Icons, cooldowns ---
        for (int i = 0; i < numForms; i++) {
            IForm form = availableForms.get(i);
            double iconAngle = angleStep * i;
            int x = centerX + (int) (ICON_RADIUS * Math.cos(iconAngle));
            int y = centerY + (int) (ICON_RADIUS * Math.sin(iconAngle));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, form.getIcon());
            blit(poseStack, x - ICON_SIZE / 2, y - ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            int cd = ClientCooldowns.remaining(form);
            if (cd > 0) {
                float frac = ClientCooldowns.fraction(form);
                int h = Mth.ceil(ICON_SIZE * frac);
                fill(poseStack, x - ICON_SIZE / 2, y - ICON_SIZE / 2, x + ICON_SIZE / 2, y - ICON_SIZE / 2 + h, 0xB0000000);
                String secs = String.format("%.1f", cd / 20.0f);
                drawCenteredString(poseStack, this.font, secs, x, y - 4, 0xFF9A9A);
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, WHEEL_CENTER);
        blit(poseStack, centerX - 32, centerY - 32, 0, 0, 64, 64, 64, 64);

        // --- Centre: hovered (or current) form name, wrapped, plus cost / cooldown ---
        IForm shown = selectedFormIndex != -1 ? availableForms.get(selectedFormIndex) : current;
        if (shown != null) {
            List<FormattedCharSequence> lines = this.font.split(FormattedText.of(shown.getDisplayName()), 78);
            int total = lines.size() * 10;
            int ty = centerY - total / 2 - 4;
            for (FormattedCharSequence line : lines) {
                this.font.drawShadow(poseStack, line, centerX - this.font.width(line) / 2.0f, ty, 0xFFFFFF);
                ty += 10;
            }
            int cd = ClientCooldowns.remaining(shown);
            String info;
            int infoColor;
            if (cd > 0) {
                info = String.format("%.1fs", cd / 20.0f);
                infoColor = 0xFF9A9A;
            } else if (isBda && shown.getBreathCost() > 0) {
                info = shown.getBreathCost() + " hunger";
                infoColor = (creative || shown.getBreathCost() <= food) ? 0xE8D0A0 : 0xFF4040;
            } else if (!isBda && shown.getBreathCost() > 0) {
                info = shown.getBreathCost() + " breath";
                infoColor = 0xA0D8FF;
            } else {
                info = shown.getInputType() == IForm.InputType.HOLD ? "hold" : "";
                infoColor = 0xB0B0B0;
            }
            if (!info.isEmpty()) {
                drawCenteredString(poseStack, this.font, info, centerX, ty + 1, infoColor);
            }

            // Description under the wheel.
            String desc = shown.getDescription();
            if (desc != null && !desc.isBlank() && !desc.endsWith(" desc.")) {
                List<FormattedCharSequence> d = this.font.split(FormattedText.of(desc), 300);
                int dy = centerY + OUTER_RADIUS + 14;
                int maxLines = Math.min(d.size(), 4);
                KnyUi.panel(poseStack, centerX - 156, dy - 4, 312, maxLines * 10 + 6);
                for (int i = 0; i < maxLines; i++) {
                    this.font.draw(poseStack, d.get(i), centerX - this.font.width(d.get(i)) / 2.0f, dy + i * 10, 0xD8D0D0);
                }
            }
        }
    }

    private Component titleText() {
        if (this.minecraft == null || this.minecraft.player == null) return Component.empty();
        if (isBda) {
            BloodDemonArts art = this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(d -> d.getEquippedBda()).orElse(BloodDemonArts.NONE);
            return Component.literal("Blood Demon Art: " + art.getDisplayName());
        }
        String style = this.minecraft.player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                .map(s -> s.getCurrentStyles().getDisplayName()).orElse("Breathing");
        return Component.literal(style);
    }

    /** The form currently selected for use (outlined in gold). */
    private IForm currentForm() {
        if (this.minecraft == null || this.minecraft.player == null) return null;
        if (isBda) {
            return this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(d -> (IForm) d.getCurrentDemonForm()).orElse(null);
        }
        return this.minecraft.player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                .map(s -> (IForm) s.getCurrentForms()).orElse(null);
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
        for (float angle = startAngle; angle <= endAngle; angle += 0.05f) {
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            bufferBuilder.vertex(matrix, x + rIn * cos, y + rIn * sin, 0).color(r, g, b, a).endVertex();
            bufferBuilder.vertex(matrix, x + rOut * cos, y + rOut * sin, 0).color(r, g, b, a).endVertex();
        }
        float cos = Mth.cos(endAngle);
        float sin = Mth.sin(endAngle);
        bufferBuilder.vertex(matrix, x + rIn * cos, y + rIn * sin, 0).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + rOut * cos, y + rOut * sin, 0).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.enableTexture();
    }

    private void selectForm() {
        if (this.selectedFormIndex != -1) {
            IForm selectedForm = availableForms.get(this.selectedFormIndex);
            int ordinal = ((Enum<?>) selectedForm).ordinal();
            ModMessages.sendToServer(new SelectFormC2SPacket(ordinal, this.isBda));
        }
        this.onClose();
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // The change-form key may be bound to a mouse button: releasing it selects too.
        InputConstants.Key key = KeyBinding.CHANGE_KEY.getKey();
        if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == button) {
            selectForm();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyBinding.CHANGE_KEY.matches(keyCode, scanCode)) {
            selectForm();
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

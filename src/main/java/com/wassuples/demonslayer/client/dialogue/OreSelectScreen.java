package com.wassuples.demonslayer.client.dialogue;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.DialogueChoiceC2SPacket;
import com.wassuples.demonslayer.networking.packet.OpenDialogueS2CPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The ore choice at the end of the Final Selection: the five ores laid out on a lacquered tray, each turning slowly
 * in 3D. Hover to look closer (it lifts and turns faster, with its description); click, or press 1-5, to choose the
 * steel for your blade. The answer goes back into the twins' conversation.
 */
public class OreSelectScreen extends Screen {
    private final OpenDialogueS2CPacket page;
    private final ItemStack[] ores = new ItemStack[5];
    private int hovered = -1;
    private boolean answered;
    private boolean handedOver;
    private int ticks;

    public OreSelectScreen(OpenDialogueS2CPacket page) {
        super(page.speaker);
        this.page = page;
        for (int i = 0; i < 5; i++) ores[i] = new ItemStack(ModItems.SELECTION_ORES.get(i).get());
    }

    public int token() {
        return page.token;
    }

    void handOver() {
        handedOver = true;
    }

    void closeFromServer() {
        handedOver = true;
        onClose();
    }

    @Override
    protected void init() {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIALOGUE_OPEN.get(), 0.8F, 0.6F));
    }

    @Override
    public void tick() {
        ticks++;
    }

    private int slotX(int i) {
        int spacing = Math.min(84, (this.width - 40) / 5);
        return this.width / 2 + (i - 2) * spacing;
    }

    private int slotY() {
        return this.height / 2 - 6;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.fillGradient(pose, 0, 0, this.width, this.height, 0xC0100A0E, 0xE0050306);
        int spacing = Math.min(84, (this.width - 40) / 5);
        int trayW = spacing * 5 + 20, trayX = (this.width - trayW) / 2, trayY = slotY() - 44;
        KnyUi.panel(pose, trayX, trayY, trayW, 118);
        Component prompt = page.text;
        List<FormattedCharSequence> lines = this.font.split(prompt, trayW - 20);
        int ty = trayY - 14 - lines.size() * 10;
        this.font.drawShadow(pose, page.speaker, (this.width - this.font.width(page.speaker)) / 2.0F, ty - 12, 0xFF000000 | page.color);
        for (FormattedCharSequence l : lines) {
            this.font.draw(pose, l, (this.width - this.font.width(l)) / 2.0F, ty, KnyUi.TEXT);
            ty += 10;
        }

        hovered = -1;
        for (int i = 0; i < 5; i++) {
            int cx = slotX(i), cy = slotY();
            boolean hover = Math.abs(mouseX - cx) < spacing / 2 - 2 && Math.abs(mouseY - cy) < 42;
            if (hover) hovered = i;
            // a little cloth cushion under each ore
            GuiComponent.fill(pose, cx - 26, cy + 24, cx + 26, cy + 30, hover ? 0xFF6A3A7A : 0xFF3A2440);
            GuiComponent.fill(pose, cx - 26, cy + 24, cx + 26, cy + 25, 0x60FFFFFF);
            this.font.draw(pose, String.valueOf(i + 1), cx - 2.5F, cy + 34, hover ? KnyUi.GOLD_LIGHT : KnyUi.DIM);
            renderOre(pose, ores[i], cx, cy + (hover ? -6 : 0), hover ? 50 : 42, (ticks + partialTick) * (hover ? 4.0F : 1.2F) + i * 72);
        }
        if (hovered >= 0 && hovered < page.options.size()) {
            Component name = ores[hovered].getHoverName();
            Component desc = page.options.get(hovered);
            int by = trayY + 126;
            this.font.drawShadow(pose, name, (this.width - this.font.width(name)) / 2.0F, by, KnyUi.GOLD_LIGHT);
            for (FormattedCharSequence l : this.font.split(desc, trayW)) {
                by += 11;
                this.font.draw(pose, l, (this.width - this.font.width(l)) / 2.0F, by, KnyUi.TEXT);
            }
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    /** Draws an item's 3D model at screen (x, y), {@code size} pixels across, turned {@code spin} degrees. */
    private void renderOre(PoseStack pose, ItemStack stack, int x, int y, float size, float spin) {
        if (this.minecraft == null) return;
        BakedModel model = this.itemRenderer.getModel(stack, null, null, 0);
        pose.pushPose();
        pose.translate(x, y, 150);
        pose.scale(size, -size, size);
        pose.mulPose(Vector3f.XP.rotationDegrees(20));
        pose.mulPose(Vector3f.YP.rotationDegrees(spin));
        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();
        Lighting.setupFor3DItems();
        this.itemRenderer.render(stack, ItemTransforms.TransformType.FIXED, false, pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        buffers.endBatch();
        Lighting.setupForFlatItems();
        pose.popPose();
    }

    private void choose(int i) {
        if (answered || i < 0 || i >= page.options.size()) return;
        answered = true;
        handedOver = true;
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MISSION_BELL.get(), 1.4F, 0.5F));
        ModMessages.sendToServer(new DialogueChoiceC2SPacket(page.token, i));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered >= 0) {
            choose(hovered);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_5) {
            choose(key - InputConstants.KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void removed() {
        if (!handedOver && this.minecraft != null && this.minecraft.getConnection() != null) {
            ModMessages.sendToServer(new DialogueChoiceC2SPacket(page.token, -1));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

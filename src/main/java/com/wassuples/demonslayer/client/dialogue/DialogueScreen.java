package com.wassuples.demonslayer.client.dialogue;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.DialogueChoiceC2SPacket;
import com.wassuples.demonslayer.networking.packet.OpenDialogueS2CPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation window: a scroll-like panel along the bottom of the screen with the speaker's portrait (the actual
 * NPC, rendered live and looking at your cursor), their name and epithet in their colour, the line typed out letter by
 * letter, and the answers stacked above the panel once the line has finished. Click or press space to finish a line;
 * keys 1-9 pick an answer. The world keeps running behind it.
 */
public class DialogueScreen extends Screen {
    private static final int PANEL_H = 92, PORTRAIT_W = 70;
    private static final float CHARS_PER_TICK = 1.6F;

    private OpenDialogueS2CPacket page;
    private List<FormattedCharSequence> lines = List.of();
    private int totalChars;
    private float revealed;
    private int ticks;
    private boolean answered;
    private boolean closingFromServer;
    private final List<Button> optionButtons = new ArrayList<>();

    public DialogueScreen(OpenDialogueS2CPacket page) {
        super(page.speaker);
        this.page = page;
    }

    public int token() {
        return page.token;
    }

    private int panelW() {
        return Math.min(440, this.width - 24);
    }

    private int panelX() {
        return (this.width - panelW()) / 2;
    }

    private int panelY() {
        return this.height - PANEL_H - 10;
    }

    private int textX() {
        return panelX() + PORTRAIT_W + 14;
    }

    private int textW() {
        return panelW() - PORTRAIT_W - 26;
    }

    @Override
    protected void init() {
        layoutPage(false);
    }

    /** A new page from the server (same conversation). */
    void showPage(OpenDialogueS2CPacket next) {
        this.page = next;
        this.answered = false;
        layoutPage(true);
    }

    private void layoutPage(boolean fresh) {
        lines = this.font.split(page.text, textW());
        totalChars = page.text.getString().length();
        if (fresh || ticks == 0) {
            revealed = 0;
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIALOGUE_OPEN.get(), 1.0F, 0.6F));
            }
        }
        for (Button b : optionButtons) this.removeWidget(b);
        optionButtons.clear();
        int n = page.options.size();
        int bw = Math.min(260, panelW() - 40);
        int bx = panelX() + panelW() - bw - 6;
        int by = panelY() - 8 - n * 22;
        for (int i = 0; i < n; i++) {
            final int index = i;
            Component label = Component.literal((i + 1) + ".  ").append(page.options.get(i));
            Button b = new OptionButton(bx, by + i * 22, bw, 20, label, page.primary.get(i), btn -> answer(index));
            b.visible = false;
            optionButtons.add(this.addRenderableWidget(b));
        }
    }

    private boolean fullyShown() {
        return revealed >= totalChars;
    }

    @Override
    public void tick() {
        ticks++;
        if (!fullyShown()) {
            int before = (int) revealed;
            revealed = Math.min(totalChars, revealed + CHARS_PER_TICK);
            if ((int) revealed / 4 != before / 4 && this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIALOGUE_BLIP.get(), 0.9F + (ticks % 5) * 0.04F, 0.35F));
            }
        }
        boolean show = fullyShown();
        for (Button b : optionButtons) b.visible = show;
        Entity e = portraitEntity();
        if (e == null || !e.isAlive()) onClose();
    }

    private LivingEntity portraitEntity() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        return this.minecraft.level.getEntity(page.entityId) instanceof LivingEntity l ? l : null;
    }

    private void answer(int index) {
        if (answered || !fullyShown()) return;
        answered = true;
        ModMessages.sendToServer(new DialogueChoiceC2SPacket(page.token, index));
    }

    void closeFromServer() {
        closingFromServer = true;
        onClose();
    }

    /** Another screen continues this same conversation: leaving must not end it. */
    void handOver() {
        closingFromServer = true;
    }

    @Override
    public void removed() {
        if (!closingFromServer && this.minecraft != null && this.minecraft.getConnection() != null) {
            ModMessages.sendToServer(new DialogueChoiceC2SPacket(page.token, -1));
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!fullyShown()) {
            revealed = totalChars;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_RETURN) {
            if (!fullyShown()) {
                revealed = totalChars;
                return true;
            }
            if (page.options.size() == 1) {
                answer(0);
                return true;
            }
        }
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
            int index = key - InputConstants.KEY_1;
            if (index < page.options.size()) {
                if (!fullyShown()) revealed = totalChars;
                else answer(index);
                return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        // a soft shade over the lower part of the screen keeps the panel readable without hiding the world
        this.fillGradient(pose, 0, this.height / 2, this.width, this.height, 0x00000000, 0xA0000000);

        int px = panelX(), py = panelY(), pw = panelW();
        int accent = 0xFF000000 | page.color;
        KnyUi.panel(pose, px, py, pw, PANEL_H);
        GuiComponent.fill(pose, px + 2, py + 2, px + pw - 2, py + 3, (0x90 << 24) | (page.color & 0xFFFFFF));

        // portrait frame rising out of the panel
        int fx = px + 8, fy = py - 22, fw = PORTRAIT_W - 4, fh = PANEL_H + 14;
        GuiComponent.fill(pose, fx - 2, fy - 2, fx + fw + 2, fy + fh + 2, KnyUi.GOLD_DARK);
        GuiComponent.fill(pose, fx - 1, fy - 1, fx + fw + 1, fy + fh + 1, accent);
        this.fillGradient(pose, fx, fy, fx + fw, fy + fh, 0xFF2A1B30, 0xFF0E080C);
        LivingEntity entity = portraitEntity();
        if (entity != null) {
            float h = Math.max(0.6F, entity.getBbHeight());
            int scale = (int) (52 / h);
            int ex = fx + fw / 2, ey = fy + fh - 10;
            InventoryScreen.renderEntityInInventory(ex, ey, scale, ex - mouseX, ey - (int) (h * scale * 0.85F) - mouseY, entity);
        }

        // name plate
        int tx = textX();
        this.font.drawShadow(pose, page.speaker, tx, py + 8, accent);
        int nameW = this.font.width(page.speaker);
        if (!page.epithet.getString().isEmpty()) {
            this.font.draw(pose, page.epithet, tx + nameW + 8, py + 8, KnyUi.DIM);
        }
        GuiComponent.fill(pose, tx, py + 19, tx + textW(), py + 20, (0x70 << 24) | (page.color & 0xFFFFFF));

        // the line, typed out
        int shown = (int) revealed;
        int y = py + 26;
        int consumed = 0;
        for (FormattedCharSequence line : lines) {
            if (consumed >= shown) break;
            int len = lineLength(line);
            if (consumed + len <= shown) {
                this.font.draw(pose, line, tx, y, KnyUi.TEXT);
            } else {
                this.font.draw(pose, truncate(line, shown - consumed), tx, y, KnyUi.TEXT);
            }
            consumed += len;
            y += 11;
        }
        if (fullyShown() && page.options.isEmpty()) {
            this.font.draw(pose, Component.translatable("dialogue.demonslayer.click"), tx, py + PANEL_H - 14, KnyUi.DIM);
        } else if (!fullyShown() && (ticks / 6) % 2 == 0) {
            GuiComponent.fill(pose, px + pw - 14, py + PANEL_H - 12, px + pw - 9, py + PANEL_H - 9, accent);
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private static int lineLength(FormattedCharSequence seq) {
        int[] n = {0};
        seq.accept((index, style, codePoint) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private static FormattedCharSequence truncate(FormattedCharSequence seq, int count) {
        return sink -> {
            int[] n = {0};
            return seq.accept((index, style, codePoint) -> {
                if (n[0]++ >= count) return false;
                return sink.accept(index, style, codePoint);
            });
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Answer button: framed in the mod's style; the story's main path is drawn in the speaker's colour. */
    private final class OptionButton extends Button {
        private final boolean primary;

        OptionButton(int x, int y, int w, int h, Component message, boolean primary, OnPress onPress) {
            super(x, y, w, h, message, onPress);
            this.primary = primary;
        }

        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            boolean hover = this.isHoveredOrFocused();
            KnyUi.buttonFrame(pose, this.x, this.y, this.width, this.height, true, hover, false);
            if (primary) GuiComponent.fill(pose, this.x + 1, this.y + 1, this.x + 3, this.y + this.height - 1, 0xFF000000 | page.color);
            int color = hover ? 0xFFFFF4D0 : primary ? KnyUi.blend(0xFF000000 | page.color, 0xFFFFFFFF, 0.45F) : KnyUi.TEXT;
            DialogueScreen.this.font.drawShadow(pose, this.getMessage(), this.x + 8, this.y + (this.height - 8) / 2, color);
        }
    }
}

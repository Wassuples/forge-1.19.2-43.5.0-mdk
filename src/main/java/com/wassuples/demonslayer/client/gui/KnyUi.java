package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The mod's shared UI look: dark lacquered panels framed in gold, with one accent colour (crimson for demons,
 * wisteria purple for slayers) for selection and highlights. Every screen draws its frames, cards, separators and
 * buttons through here so they all match.
 */
public final class KnyUi {
    public static final int INK_TOP = 0xF41A1012;
    public static final int INK_BOTTOM = 0xF40C0708;
    public static final int GOLD = 0xFFC9A04A;
    public static final int GOLD_LIGHT = 0xFFF0D080;
    public static final int GOLD_DARK = 0xFF6E4E1C;
    public static final int TEXT = 0xFFEDE3E3;
    public static final int DIM = 0xFF9A8E8E;
    public static final int CRIMSON = 0xFFD8263A;
    public static final int WISTERIA = 0xFF9A7AE0;
    public static final int CARD = 0xC0261618;
    public static final int GOOD = 0xFF8CFF9A;
    public static final int BAD = 0xFFFF7070;

    private KnyUi() {
    }

    /** Crimson for demons, wisteria for slayers. */
    public static int accent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return WISTERIA;
        return mc.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(d -> d.isDemon()).orElse(false) ? CRIMSON : WISTERIA;
    }

    /** Accent at {@code alpha} (0..255). */
    public static int accent(int alpha) {
        return (alpha << 24) | (accent() & 0xFFFFFF);
    }

    /** Main window: black edge, bevelled gold frame, gradient ink inside, gold corner diamonds. */
    public static void panel(PoseStack pose, int x, int y, int w, int h) {
        GuiComponent.fill(pose, x - 3, y - 3, x + w + 3, y + h + 3, 0xFF050304);
        GuiComponent.fill(pose, x - 2, y - 2, x + w + 2, y + h + 2, GOLD_DARK);
        GuiComponent.fill(pose, x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
        GuiComponent.fill(pose, x - 1, y - 1, x + w + 1, y, GOLD_LIGHT);
        fillGradient(pose, x, y, x + w, y + h, INK_TOP, INK_BOTTOM);
        GuiComponent.fill(pose, x + 2, y + 2, x + w - 2, y + 3, accent(0x60));
        diamond(pose, x, y);
        diamond(pose, x + w, y);
        diamond(pose, x, y + h);
        diamond(pose, x + w, y + h);
    }

    /** Recessed area inside a panel (lists, sub-windows). */
    public static void inset(PoseStack pose, int x, int y, int w, int h) {
        GuiComponent.fill(pose, x, y, x + w, y + h, 0x80000000);
        GuiComponent.fill(pose, x, y, x + w, y + 1, 0x60000000);
        GuiComponent.fill(pose, x, y + h - 1, x + w, y + h, 0x40C9A04A);
    }

    /** Header strip with a title (and optional right-aligned text), underlined in the accent colour. */
    public static void header(PoseStack pose, Font font, int x, int y, int w, String title, String right) {
        GuiComponent.fill(pose, x, y, x + w, y + 16, 0xC0080405);
        GuiComponent.fill(pose, x, y + 16, x + w, y + 17, accent());
        font.drawShadow(pose, title, x + 6, y + 4, TEXT);
        if (right != null) font.drawShadow(pose, right, x + w - 6 - font.width(right), y + 4, GOLD_LIGHT);
    }

    /** A list card; selected cards get the accent tint and a bar on the left. */
    public static void card(PoseStack pose, int x, int y, int w, int h, boolean selected, boolean hover) {
        int fill = selected ? (0xC0000000 | (blend(accent(), 0xFF261618, 0.45f) & 0xFFFFFF)) : (hover ? 0xC0382024 : CARD);
        GuiComponent.fill(pose, x, y, x + w, y + h, fill);
        if (selected) GuiComponent.fill(pose, x, y, x + 2, y + h, accent());
        GuiComponent.fill(pose, x, y + h - 1, x + w, y + h, 0x30C9A04A);
    }

    /** Thin gold rule with a small centred label (e.g. section separators in a list). */
    public static void separator(PoseStack pose, Font font, int x, int y, int w, String label) {
        int tw = font.width(label);
        int mid = x + w / 2;
        GuiComponent.fill(pose, x + 2, y + 4, mid - tw / 2 - 4, y + 5, GOLD_DARK);
        GuiComponent.fill(pose, mid + tw / 2 + 4, y + 4, x + w - 2, y + 5, GOLD_DARK);
        font.draw(pose, label, mid - tw / 2.0f, y, GOLD);
    }

    /** Small gold diamond ornament centred on (x, y). */
    public static void diamond(PoseStack pose, int x, int y) {
        for (int i = 0; i <= 3; i++) {
            GuiComponent.fill(pose, x - (3 - i), y - i, x + (3 - i) + 1, y - i + 1, GOLD_LIGHT);
            GuiComponent.fill(pose, x - (3 - i), y + i, x + (3 - i) + 1, y + i + 1, GOLD);
        }
    }

    /** Frame + fill for a button or tab. */
    public static void buttonFrame(PoseStack pose, int x, int y, int w, int h, boolean active, boolean hover, boolean pressed) {
        GuiComponent.fill(pose, x, y, x + w, y + h, active ? (hover || pressed ? GOLD : GOLD_DARK) : 0xFF3A3434);
        int inner;
        if (!active) inner = 0xFF1A1616;
        else if (pressed) inner = blend(accent(), 0xFF1A1012, 0.55f);
        else if (hover) inner = blend(accent(), 0xFF1A1012, 0.35f);
        else inner = 0xFF1E1214;
        fillGradient(pose, x + 1, y + 1, x + w - 1, y + h - 1, inner, blend(inner, 0xFF000000, 0.35f));
        if (active) GuiComponent.fill(pose, x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
    }

    /** Recessed 18x18 item slot whose top-left item pixel is (x, y) (i.e. a container slot's own x/y). */
    public static void slot(PoseStack pose, int x, int y) {
        GuiComponent.fill(pose, x - 1, y - 1, x + 17, y + 17, 0xFF3A2A1E);
        GuiComponent.fill(pose, x - 1, y - 1, x + 17, y, 0xFF0A0607);
        GuiComponent.fill(pose, x - 1, y - 1, x, y + 17, 0xFF0A0607);
        GuiComponent.fill(pose, x, y, x + 16, y + 16, 0xFF160E10);
    }

    /** Larger gold-framed result slot around a 16x16 slot at (x, y). */
    public static void resultSlot(PoseStack pose, int x, int y) {
        GuiComponent.fill(pose, x - 5, y - 5, x + 21, y + 21, GOLD_DARK);
        GuiComponent.fill(pose, x - 4, y - 4, x + 20, y + 20, GOLD);
        GuiComponent.fill(pose, x - 4, y - 4, x + 20, y - 3, GOLD_LIGHT);
        GuiComponent.fill(pose, x - 3, y - 3, x + 19, y + 19, 0xFF0A0607);
        GuiComponent.fill(pose, x - 2, y - 2, x + 18, y + 18, accent(0x50));
        GuiComponent.fill(pose, x, y, x + 16, y + 16, 0xFF160E10);
    }

    public static int blend(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static void fillGradient(PoseStack pose, int x0, int y0, int x1, int y1, int top, int bottom) {
        GRADIENT.fill(pose, x0, y0, x1, y1, top, bottom);
    }

    /** GuiComponent.fillGradient is an instance method; this tiny subclass exposes it statically. */
    private static final class Gradient extends GuiComponent {
        void fill(PoseStack pose, int x0, int y0, int x1, int y1, int top, int bottom) {
            this.fillGradient(pose, x0, y0, x1, y1, top, bottom);
        }
    }

    private static final Gradient GRADIENT = new Gradient();

    /** A button in the mod's style (drop-in replacement for the vanilla Button). */
    public static class KnyButton extends Button {
        public KnyButton(int x, int y, int w, int h, Component message, OnPress onPress) {
            super(x, y, w, h, message, onPress);
        }

        @Override
        public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            buttonFrame(pose, this.x, this.y, this.width, this.height, this.active, this.isHoveredOrFocused(), false);
            int color = this.active ? (this.isHoveredOrFocused() ? 0xFFFFF4D0 : TEXT) : 0xFF6E6464;
            drawCenteredString(pose, mc.font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
        }
    }
}

package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.networking.packet.MissionNoticeS2CPacket;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Story messages on the mod's own HUD (instead of chat and the vanilla title):
 * <ul>
 *   <li><b>banner</b>: an ink brush-stroke band across the upper screen with a large title and a subtitle;</li>
 *   <li><b>notices</b>: short lines on paper slips stacked under the mission tracker, sliding in and fading out.</li>
 * </ul>
 */
public final class MissionNoticeOverlay {
    private static final int MAX_NOTICES = 5, NOTICE_W = 168;
    private static final List<Notice> NOTICES = new ArrayList<>();
    @Nullable
    private static Banner banner;

    private MissionNoticeOverlay() {
    }

    public static void push(MissionNoticeS2CPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (packet.kind == MissionNoticeS2CPacket.REST) {
            RestFadeOverlay.start(packet.title, now, packet.ticks);
            return;
        }
        if (packet.kind == MissionNoticeS2CPacket.BANNER) {
            banner = new Banner(packet.title, packet.subtitle, packet.color, now, Math.max(40, packet.ticks));
            return;
        }
        NOTICES.add(0, new Notice(packet.title, packet.kind == MissionNoticeS2CPacket.WARNING, packet.color, now, Math.max(40, packet.ticks)));
        while (NOTICES.size() > MAX_NOTICES) NOTICES.remove(NOTICES.size() - 1);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(packet.kind == MissionNoticeS2CPacket.WARNING
                ? ModSounds.MISSION_FAILED.get() : ModSounds.NOTICE.get(), packet.kind == MissionNoticeS2CPacket.WARNING ? 1.6F : 1.0F, 0.35F));
    }

    public static void clear() {
        NOTICES.clear();
        banner = null;
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        long now = mc.level.getGameTime();
        float t = now + partialTick;
        Font font = mc.font;

        // ---- notices
        int x = width - NOTICE_W - 8;
        int y = Math.max(8, MissionTrackerOverlay.lastBottom + 4);
        for (Iterator<Notice> it = NOTICES.iterator(); it.hasNext(); ) {
            Notice n = it.next();
            float age = t - n.start;
            if (age > n.ticks || age < -5) {
                it.remove();
                continue;
            }
            float in = Math.min(1.0F, age / 6.0F), out = Math.min(1.0F, (n.ticks - age) / 20.0F);
            float a = Math.max(0.0F, Math.min(in, out));
            int slide = (int) ((1.0F - in) * 30);
            List<FormattedCharSequence> lines = font.split(n.text, NOTICE_W - 14);
            int h = lines.size() * 10 + 6;
            int bx = x + slide;
            int ink = ((int) (a * 0xD0) << 24) | 0x120A0E;
            GuiComponent.fill(pose, bx, y, bx + NOTICE_W, y + h, ink);
            int edge = ((int) (a * 255) << 24) | (n.warning ? 0xD8263A : (KnyUi.accent() & 0xFFFFFF));
            GuiComponent.fill(pose, bx, y, bx + 2, y + h, edge);
            GuiComponent.fill(pose, bx + 2, y + h - 1, bx + NOTICE_W, y + h, ((int) (a * 0x50) << 24) | 0xC9A04A);
            int textAlpha = Math.max(5, (int) (a * 255)) << 24;
            int ty = y + 4;
            for (FormattedCharSequence line : lines) {
                font.draw(pose, line, bx + 8, ty, textAlpha | (n.color & 0xFFFFFF));
                ty += 10;
            }
            y += h + 3;
        }

        // ---- banner
        Banner b = banner;
        if (b != null) {
            float age = t - b.start;
            if (age > b.ticks || age < -5) {
                banner = null;
                return;
            }
            float in = Math.min(1.0F, age / 10.0F), out = Math.min(1.0F, (b.ticks - age) / 25.0F);
            float a = Math.max(0.0F, Math.min(in, out));
            int cy = (int) (height * 0.26F);
            int bandH = b.subtitle.getString().isEmpty() ? 34 : 46;
            int reach = (int) (width * 0.36F * (0.6F + 0.4F * in));
            int cx = width / 2;
            // brush stroke: dense in the middle, feathered to the sides
            for (int i = 0; i < 24; i++) {
                float f = i / 24.0F;
                int alpha = (int) (a * 0xB8 * (1.0F - f * f));
                int x0 = cx - (int) (reach * (f + 1.0F / 24.0F)), x1 = cx - (int) (reach * f);
                int x2 = cx + (int) (reach * f), x3 = cx + (int) (reach * (f + 1.0F / 24.0F));
                int c = (alpha << 24) | 0x0C0608;
                int wob = (int) (Math.sin(i * 1.7F) * 2);
                GuiComponent.fill(pose, x0, cy - bandH / 2 + wob, x1, cy + bandH / 2 - wob, c);
                GuiComponent.fill(pose, x2, cy - bandH / 2 - wob, x3, cy + bandH / 2 + wob, c);
            }
            int lineA = (int) (a * 0xC0) << 24;
            GuiComponent.fill(pose, cx - reach / 2, cy - bandH / 2 + 3, cx + reach / 2, cy - bandH / 2 + 4, lineA | (b.color & 0xFFFFFF));
            GuiComponent.fill(pose, cx - reach / 2, cy + bandH / 2 - 4, cx + reach / 2, cy + bandH / 2 - 3, lineA | (b.color & 0xFFFFFF));
            int textAlpha = Math.max(5, (int) (a * 255)) << 24;
            pose.pushPose();
            pose.translate(cx, cy - (b.subtitle.getString().isEmpty() ? 8 : 14), 0);
            pose.scale(2.0F, 2.0F, 1.0F);
            font.drawShadow(pose, b.title, -font.width(b.title) / 2.0F, 0, textAlpha | (b.color & 0xFFFFFF));
            pose.popPose();
            if (!b.subtitle.getString().isEmpty()) {
                List<FormattedCharSequence> sub = font.split(b.subtitle, Math.max(120, reach * 2 - 20));
                int sy = cy + 7;
                for (FormattedCharSequence s : sub) {
                    font.draw(pose, s, cx - font.width(s) / 2.0F, sy, textAlpha | 0xD8CEC8);
                    sy += 10;
                }
            }
        }
    };

    private record Notice(Component text, boolean warning, int color, long start, int ticks) {
    }

    private record Banner(Component title, Component subtitle, int color, long start, int ticks) {
    }
}

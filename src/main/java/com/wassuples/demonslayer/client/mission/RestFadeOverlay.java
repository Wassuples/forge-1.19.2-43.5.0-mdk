package com.wassuples.demonslayer.client.mission;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Resting on a bedroll: the screen dims to night-dark and comes back up, with a line of text. */
public final class RestFadeOverlay {
    private static Component text = Component.empty();
    private static long start = -1;
    private static int ticks;

    private RestFadeOverlay() {
    }

    /** Leaving the world: nothing carries over. */
    public static void clear() {
        start = -1;
    }

    /** {@code duration} 0 gets the player up: the dark lifts over the next 15 ticks. */
    static void start(Component line, long now, int duration) {
        if (duration <= 0) {
            if (start >= 0) ticks = (int) Math.min(ticks, now - start + 15);
            return;
        }
        text = line;
        if (start >= 0 && now - start < ticks - 15) {   // already lying down: just a new line of text
            ticks = (int) Math.max(ticks, now - start + duration);
            return;
        }
        start = now;
        ticks = duration;
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (start < 0 || mc.level == null) return;
        float age = mc.level.getGameTime() - start + partialTick;
        if (age > ticks || age < -5) {
            start = -1;
            return;
        }
        float in = Math.min(1.0F, age / 20.0F), out = Math.min(1.0F, (ticks - age) / 15.0F);
        float a = Math.max(0, Math.min(in, out));
        GuiComponent.fill(pose, 0, 0, width, height, ((int) (a * 235) << 24) | 0x05030A);
        int textAlpha = Math.max(5, (int) (a * 255)) << 24;
        mc.font.drawShadow(pose, text, (width - mc.font.width(text)) / 2.0F, height / 2.0F - 4, textAlpha | 0xD8CEC8);
    };
}

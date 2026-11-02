package com.wassuples.demonslayer.client.dialogue;

import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import com.wassuples.demonslayer.networking.packet.OpenDialogueS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client side of conversations and in-world speech. Only ever loaded on the physical client. */
public final class ClientDialogueHandler {
    /** Active speech bubbles by entity id. */
    private static final Map<Integer, Speech> SPEECH = new HashMap<>();

    private ClientDialogueHandler() {
    }

    public static void handle(OpenDialogueS2CPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (packet.isClose()) {
            if (mc.screen instanceof DialogueScreen screen && screen.token() == packet.token) screen.closeFromServer();
            if (mc.screen instanceof OreSelectScreen ore && ore.token() == packet.token) ore.closeFromServer();
            return;
        }
        if ("ore".equals(packet.picker)) {
            if (mc.screen instanceof DialogueScreen screen && screen.token() == packet.token) screen.handOver();
            if (mc.player != null && mc.screen instanceof AbstractContainerScreen<?>) mc.player.closeContainer();
            mc.setScreen(new OreSelectScreen(packet));
            return;
        }
        if (mc.screen instanceof DialogueScreen screen && screen.token() == packet.token) {
            screen.showPage(packet);
            return;
        }
        if (mc.screen instanceof OreSelectScreen ore && ore.token() == packet.token) ore.handOver();
        if (mc.player != null && mc.screen instanceof AbstractContainerScreen<?>) mc.player.closeContainer();
        mc.setScreen(new DialogueScreen(packet));
    }

    public static void speech(NpcSpeechS2CPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SPEECH.put(packet.entityId, new Speech(packet.text, mc.level.getGameTime(), Math.max(20, packet.ticks)));
        if (SPEECH.size() > 64) prune(mc.level.getGameTime());
    }

    /** The bubble currently over {@code entityId}, or null. */
    @Nullable
    public static Speech speechFor(int entityId, long gameTime) {
        Speech s = SPEECH.get(entityId);
        if (s == null) return null;
        if (gameTime - s.start > s.ticks || gameTime < s.start) {
            SPEECH.remove(entityId);
            return null;
        }
        return s;
    }

    /** Entity ids with a bubble (copy; may include expired ones). */
    public static java.util.List<Integer> speakers() {
        return new java.util.ArrayList<>(SPEECH.keySet());
    }

    private static void prune(long now) {
        for (Iterator<Speech> it = SPEECH.values().iterator(); it.hasNext(); ) {
            Speech s = it.next();
            if (now - s.start > s.ticks || now < s.start) it.remove();
        }
    }

    public static void clear() {
        SPEECH.clear();
    }

    public record Speech(Component text, long start, int ticks) {
        /** 0..1 opacity: pops in, holds, fades out over the last second. */
        public float alpha(long now, float partial) {
            float age = (now - start) + partial;
            float in = Math.min(1.0F, age / 4.0F);
            float out = Math.min(1.0F, (ticks - age) / 20.0F);
            return Math.max(0.0F, Math.min(in, out));
        }
    }
}

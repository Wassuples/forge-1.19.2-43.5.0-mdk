package com.wassuples.demonslayer.client;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.BrokeSleepC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Client-side component: Sleep QTEHandler.
 */
public class SleepQTEHandler {
    private static boolean isActive = false;
    private static List<Integer> keySequence = Collections.emptyList();
    private static int currentIndex = 0;

    public static void startQTE(List<Integer> sequence) {
        Minecraft mc = Minecraft.getInstance();
        isActive = true;
        currentIndex = 0;

        keySequence = new ArrayList<>();
        for (int id : sequence) {
            switch (id) {
                case 0 -> keySequence.add(mc.options.keyUp.getKey().getValue());
                case 1 -> keySequence.add(mc.options.keyLeft.getKey().getValue());
                case 2 -> keySequence.add(mc.options.keyDown.getKey().getValue());
                case 3 -> keySequence.add(mc.options.keyRight.getKey().getValue());
            }
        }
    }

    public static void stopQTE() {
        isActive = false;
        keySequence = Collections.emptyList();
        currentIndex = 0;
    }

    public static void handleKeyInput(int keyCode) {
        if (!isActive || keySequence.isEmpty()) {
            return;
        }

        if (keyCode == keySequence.get(currentIndex)) {
            currentIndex++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.5f, 1.0f));

            if (currentIndex >= keySequence.size()) {
                completeQTE();
            }
        } else {
            currentIndex = 0;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f));
        }
    }

    private static void completeQTE() {
        ModMessages.sendToServer(new BrokeSleepC2SPacket());
        stopQTE();
    }

    public static boolean isActive() {
        return isActive;
    }

    public static List<Integer> getKeySequence() {
        return keySequence;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static String getKeyName(int keyCode) {
        Minecraft mc = Minecraft.getInstance();
        if (keyCode == mc.options.keyUp.getKey().getValue()) {
            return mc.options.keyUp.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        } else if (keyCode == mc.options.keyLeft.getKey().getValue()) {
            return mc.options.keyLeft.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        } else if (keyCode == mc.options.keyDown.getKey().getValue()) {
            return mc.options.keyDown.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        } else if (keyCode == mc.options.keyRight.getKey().getValue()) {
            return mc.options.keyRight.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        }
        return "?";
    }
}

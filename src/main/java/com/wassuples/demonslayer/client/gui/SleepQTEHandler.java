package com.wassuples.demonslayer.client.gui;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.BrokeSleepC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;


public class SleepQTEHandler {
    private static boolean isActive = false;
    private static List<Integer> keySequence = Collections.emptyList();
    private static int currentIndex = 0;

    public static void startQTE(List<Integer> sequence) {
        isActive = true;
        keySequence = sequence;
        currentIndex = 0;
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
        return switch (keyCode) {
            case GLFW.GLFW_KEY_W -> "W";
            case GLFW.GLFW_KEY_A -> "A";
            case GLFW.GLFW_KEY_S -> "S";
            case GLFW.GLFW_KEY_D -> "D";
            default -> "?";
        };
    }
}

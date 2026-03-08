package com.wassuples.demonslayer.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * Client GUI component: Sleep QTEOverlay.
 */
public class SleepQTEOverlay {
    public static final IGuiOverlay HUD_SLEEP_QTE = (gui, poseStack, partialTick, width, height) -> {
        if (!SleepQTEHandler.isActive()) {
            return; // Don't draw if the QTE isn't active
        }

        Minecraft mc = Minecraft.getInstance();
        int x = width / 2;
        int y = height / 2 + 30; // Position it below the center of the screen

        List<Integer> sequence = SleepQTEHandler.getKeySequence();
        int currentIndex = SleepQTEHandler.getCurrentIndex();

        // Build the string to display (e.g., "[W] [A] [S] [D]")
        StringBuilder displayText = new StringBuilder();
        for (int i = 0; i < sequence.size(); i++) {
            String keyName = SleepQTEHandler.getKeyName(sequence.get(i));
            ChatFormatting color;

            if (i < currentIndex) {
                // Correctly pressed keys are green
                color = ChatFormatting.GREEN;
            } else if (i == currentIndex) {
                // The current key to press is yellow and bold
                color = ChatFormatting.YELLOW;
                keyName = ChatFormatting.BOLD + keyName;
            } else {
                // Keys not yet pressed are gray
                color = ChatFormatting.GRAY;
            }

            displayText.append(color).append("[").append(keyName).append(color).append("] ");
        }

        int textWidth = mc.font.width(displayText.toString());
        GuiComponent.drawCenteredString(poseStack, mc.font, "MASH TO WAKE UP", x, y - 12, ChatFormatting.RED.getColor());
        GuiComponent.drawCenteredString(poseStack, mc.font, displayText.toString(), x, y, -1);
    };
}
package com.wassuples.demonslayer.client.mission;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client-only: opens the letter screen. */
public final class ClientLetterHandler {
    private ClientLetterHandler() {
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new LetterScreen(stack.copy()));
    }
}

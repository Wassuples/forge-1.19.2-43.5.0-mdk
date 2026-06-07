package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class LoveMinigameScreen extends StyleMinigameScreen {

    private static final int[] KEYS    = { GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D };
    private static final String[] LABELS = { "W", "A", "S", "D" };
    /** Ticks before idle stall counts as a broken chain. */
    private static final int IDLE_TIMEOUT = 28;

    private final int requiredLength;

    private int[] chain;
    private int chainPos = 0;  // current position player must input
    private int chainLen = 1;  // how many steps exist so far

    private int idleTick = 0;

    public LoveMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Love Breathing – Momentum & Escalation"));
        this.requiredLength = 4 + (tier - 1) * 2;   // 4, 6, 8, 10, 12
        this.chain = new int[requiredLength + 1];
        appendStep();
    }

    private void appendStep() {
        int prev = chainLen > 1 ? chain[chainLen - 2] : -1;
        int next;
        do { next = (int)(Math.random() * KEYS.length); } while (next == prev);
        chain[chainLen - 1] = next;
    }

    @Override
    protected void tickGame() {
        idleTick++;
        if (idleTick >= IDLE_TIMEOUT) {
            // Stalled — treat as broken chain (instant fail, retry immediately)
            finishMinigame(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Find which game key was pressed
        int pressed = -1;
        for (int i = 0; i < KEYS.length; i++) {
            if (keyCode == KEYS[i]) { pressed = i; break; }
        }
        // Pass ESC and other non-game keys to super (handles closing)
        if (pressed == -1) return super.keyPressed(keyCode, scanCode, modifiers);

        idleTick = 0;

        if (pressed == chain[chainPos]) {
            chainPos++;
            if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.3f,
                    1.0f + chainPos * 0.04f));
            if (chainPos >= chainLen) {
                // Completed this step — extend chain
                chainLen++;
                chainPos = 0;
                if (chainLen - 1 >= requiredLength) {
                    finishMinigame(true);
                    return true;
                }
                appendStep();
            }
        } else {
            // Wrong key — chain broken, attempt over
            finishMinigame(false);
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        drawTitle(pose, "Momentum & Escalation", 0xFF88BB);
        drawInstructions(pose, "Keep the chain going — one wrong key ends it!", 0xCCCCCC);
        drawCenteredString(pose, this.font,
            "Chain: " + (chainLen - 1) + " / " + requiredLength,
            cx, 35, 0xFF88BB);

        // Current chain display (shows up to 12 steps, centred)
        int stepW = 24;
        int display = Math.min(chainLen, 12);
        int startSlot = Math.max(0, chainPos - 6);
        int startX = cx - (display * stepW) / 2;

        for (int i = 0; i < display; i++) {
            int idx = startSlot + i;
            if (idx >= chainLen) break;
            int key = chain[idx];
            int x   = startX + i * stepW + stepW / 2;

            boolean isCurrent = idx == chainPos;
            boolean isDone    = idx < chainPos;
            int bg = isDone ? 0x55FF88BB : isCurrent ? 0xAAFF88BB : 0x22FF88BB;
            int fg = isDone ? 0xFF55FF55 : isCurrent ? 0xFFFFFFFF  : 0xFFFF88BB;

            fill(pose, x - stepW / 2 + 2, cy - 11, x + stepW / 2 - 2, cy + 11, bg);
            drawCenteredString(pose, this.font, LABELS[key], x, cy - 4, fg);
        }

        // Idle timeout bar
        float idlePct = 1f - (float) idleTick / IDLE_TIMEOUT;
        int barW = 180, barX = cx - barW / 2;
        fill(pose, barX, cy + 22, barX + barW, cy + 28, 0x44000000);
        fill(pose, barX, cy + 22, barX + (int)(idlePct * barW), cy + 28,
            idlePct > 0.35f ? 0xFFFF88BB : 0xFFFF4444);
        drawCenteredString(pose, this.font, "Don't stop!", cx, cy + 32, 0x888888);
        drawCenteredString(pose, this.font, "Press ESC to cancel", cx, this.height - 18, 0x555555);
    }
}

package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Love Breathing – Momentum &amp; Escalation minigame.
 *
 * <p><b>Mechanic</b>: A combo chain — each successful input extends the chain and appends a
 * new random input to the end. The chain must keep growing; stopping or breaking it resets
 * the attempt. The player must accumulate a chain of {@code requiredLength} to pass.
 *
 * <p><b>Pass condition</b>: Build the chain up to {@code requiredLength} consecutive inputs.
 * <br><b>Fail</b>: Chain broken → attempt resets immediately (no penalty).
 */
public class LoveMinigameScreen extends StyleMinigameScreen {

    private static final int[] KEYS    = { GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A,
                                            GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D };
    private static final String[] LABELS = { "W", "A", "S", "D" };
    private static final int[] COLOURS   = { 0xFF8888, 0xFFAAAA, 0xFF6699, 0xFFBBBB };

    /** Ticks the player has before a stall is registered as a break. */
    private static final int INPUT_TIMEOUT_TICKS = 40;

    private final int requiredLength;

    // The current chain: first N entries are the full chain so far.
    private int[] chain;
    private int   chainPos  = 0;   // which position the player is at
    private int   chainLen  = 1;   // how long the chain currently is
    private int   bestChain = 0;

    private int   idleTick  = 0;
    private boolean broken  = false;
    private int   breakFlashTick = 0;

    public LoveMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Love Breathing – Momentum & Escalation"));
        this.requiredLength = 8 + (tier - 1) * 4;   // 8, 12, 16, …
        this.chain = new int[requiredLength + 1];
        generateNextStep();
    }

    private void generateNextStep() {
        // Append a new random key that differs from the last one (avoids boring doubles)
        int prev = chainLen > 1 ? chain[chainLen - 1] : -1;
        int next;
        do { next = (int)(Math.random() * KEYS.length); } while (next == prev);
        chain[chainLen - 1] = next;
    }

    private void breakChain() {
        broken = true;
        breakFlashTick = 0;
        chainPos = 0;
        chainLen = 1;
        idleTick = 0;
        generateNextStep();
        if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.9f));
    }

    @Override
    protected void tickGame() {
        if (broken) {
            breakFlashTick++;
            if (breakFlashTick >= 18) broken = false;
            return;
        }
        idleTick++;
        if (idleTick >= INPUT_TIMEOUT_TICKS) {
            breakChain();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (broken) return false;
        int pressed = -1;
        for (int i = 0; i < KEYS.length; i++) {
            if (keyCode == KEYS[i]) { pressed = i; break; }
        }
        if (pressed == -1) return false;
        idleTick = 0;

        if (pressed == chain[chainPos]) {
            chainPos++;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.3f,
                            1.0f + chainPos * 0.03f));

            if (chainPos >= chainLen) {
                // Completed this step — extend chain
                chainLen++;
                chainPos = 0;
                bestChain = Math.max(bestChain, chainLen - 1);
                if (chainLen - 1 >= requiredLength) {
                    finishMinigame(true);
                    return true;
                }
                generateNextStep();
            }
        } else {
            breakChain();
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Momentum & Escalation", 0xFF88BB);
        drawInstructions(pose, "Keep the chain going — don't stop or break it!", 0xCCCCCC);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Progress
        drawCenteredString(pose, this.font,
                "Chain: " + (chainLen - 1) + " / " + requiredLength + "   Best: " + bestChain,
                cx, 35, 0xFF88BB);

        // Chain display — show the full chain with current position highlighted
        int stepW = 24;
        int displayLen = Math.min(chainLen, 14); // show up to 14 steps
        int startIdx = Math.max(0, chainPos - 7);
        int totalDisplayW = displayLen * stepW;
        int startX = cx - totalDisplayW / 2;

        for (int i = 0; i < displayLen; i++) {
            int idx = startIdx + i;
            if (idx >= chainLen) break;
            int key = chain[idx];
            int x = startX + i * stepW + stepW / 2;
            boolean isCurrent = idx == chainPos;
            boolean isDone    = idx < chainPos;

            int bg = isDone ? 0x44FF88BB : isCurrent ? 0xAAFF88BB : 0x22FF88BB;
            int fg = isDone ? 0xFF55FF55  : isCurrent ? 0xFFFFFFFF  : 0xFFFF88BB;

            fill(pose, x - stepW / 2 + 2, cy - 10, x + stepW / 2 - 2, cy + 10, bg);
            drawCenteredString(pose, this.font, LABELS[key], x, cy - 4, fg);
        }

        // Idle timeout bar
        float idlePct = 1f - (float) idleTick / INPUT_TIMEOUT_TICKS;
        int barW = 180;
        int barX = cx - barW / 2;
        fill(pose, barX, cy + 25, barX + barW, cy + 31, 0x44000000);
        fill(pose, barX, cy + 25, barX + (int)(idlePct * barW), cy + 31,
                idlePct > 0.4f ? 0xFFFF88BB : 0xFFFF4444);
        drawCenteredString(pose, this.font, "Don't stop!", cx, cy + 34, 0x888888);

        if (broken) {
            drawCenteredString(pose, this.font, "Chain broken! Starting over…", cx, cy - 30, 0xFF4444);
        }
    }
}

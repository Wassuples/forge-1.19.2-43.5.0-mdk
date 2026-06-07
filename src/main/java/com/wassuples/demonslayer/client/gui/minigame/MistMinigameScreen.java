package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MistMinigameScreen extends StyleMinigameScreen {

    private static final int[] KEYS          = { GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D };
    private static final int[] ARROW_KEYS    = { GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT };
    private static final String[] SYMBOLS    = { "↑", "←", "↓", "→" };
    private static final int[] COLOURS       = { 0x88FFFF, 0xFFFF88, 0xFF8888, 0x88FF88 };
    /** Constant across all rounds and tiers — 14 ticks (0.7 s) per key. */
    private static final int SHOW_TICKS_PER_STEP = 14;

    private final int requiredRounds;
    private int roundsDone   = 0;
    /** Base sequence length is 4; grows by 1 each round regardless of tier. */
    private int currentSeqLen = 4;

    private enum Phase { SHOWING, INPUT, ROUND_PASS, FAILED }
    private Phase phase = Phase.SHOWING;

    private final List<Integer> sequence    = new ArrayList<>();
    private final List<Integer> playerInput = new ArrayList<>();
    private int showIndex  = 0;
    private int showTick   = 0;
    private int resultTick = 0;
    /** Index of the single key being shown right now (highlighted); -1 = none visible. */
    private int highlightedKey = 0;
    private final Random rng = new Random();

    public MistMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Mist Breathing – Pattern Recognition"));
        this.requiredRounds = 2 + (tier - 1);
        buildSequence();
    }

    private void buildSequence() {
        sequence.clear();
        playerInput.clear();
        showIndex      = 0;
        showTick       = 0;
        highlightedKey = 0;
        phase          = Phase.SHOWING;
        for (int i = 0; i < currentSeqLen; i++) sequence.add(rng.nextInt(4));
    }

    @Override
    protected void tickGame() {
        switch (phase) {
            case SHOWING -> {
                showTick++;
                // Each key is visible only for its own SHOW_TICKS_PER_STEP window
                if (showTick >= SHOW_TICKS_PER_STEP) {
                    showTick = 0;
                    showIndex++;
                    if (showIndex >= sequence.size()) {
                        // Sequence done — go dark, switch to INPUT
                        highlightedKey = -1;
                        phase = Phase.INPUT;
                    } else {
                        highlightedKey = showIndex;
                    }
                } else {
                    highlightedKey = showIndex;
                }
            }
            case ROUND_PASS -> {
                resultTick++;
                if (resultTick >= 20) {
                    resultTick = 0;
                    roundsDone++;
                    currentSeqLen++; // increase by 1 each round
                    if (roundsDone >= requiredRounds) {
                        finishMinigame(true);
                    } else {
                        buildSequence();
                    }
                }
            }
            case FAILED -> {
                resultTick++;
                if (resultTick >= 20) finishMinigame(false);
            }
            default -> {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (phase != Phase.INPUT) return super.keyPressed(keyCode, scanCode, modifiers);

        int dir = -1;
        for (int i = 0; i < KEYS.length; i++) {
            if (keyCode == KEYS[i] || keyCode == ARROW_KEYS[i]) { dir = i; break; }
        }
        if (dir == -1) return super.keyPressed(keyCode, scanCode, modifiers);

        int expected = sequence.get(playerInput.size());
        playerInput.add(dir);

        if (dir != expected) {
            phase = Phase.FAILED;
            resultTick = 0;
            if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
            return true;
        }
        if (minecraft != null) minecraft.getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_FLUTE, 1.2f));
        if (playerInput.size() >= sequence.size()) {
            phase = Phase.ROUND_PASS;
            resultTick = 0;
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        drawTitle(pose, "Pattern Recognition", 0xAADDFF);
        drawInstructions(pose,
            phase == Phase.SHOWING ? "Memorise the sequence — it won't show again!"
                : phase == Phase.INPUT ? "Reproduce it with WASD or arrow keys"
                  : "",
            0xCCCCCC);
        drawCenteredString(pose, this.font,
            "Round " + (roundsDone + 1) + " / " + requiredRounds + "   Sequence length: " + currentSeqLen,
            cx, 35, 0xAADDFF);

        // ── SHOWING phase: one key visible at a time, centred ─────────────────
        if (phase == Phase.SHOWING && highlightedKey >= 0 && highlightedKey < sequence.size()) {
            int dir = sequence.get(highlightedKey);
            // Big centred arrow
            float fade = 1f - (float) showTick / SHOW_TICKS_PER_STEP;
            int alpha = Math.max(0x44, (int)(0xFF * fade));
            int col = (alpha << 24) | (COLOURS[dir] & 0x00FFFFFF);
            drawCenteredString(pose, this.font, SYMBOLS[dir], cx, cy - 8, col | 0xFF000000);
            // Position number
            drawCenteredString(pose, this.font,
                (highlightedKey + 1) + " / " + currentSeqLen, cx, cy + 10, 0x888888);
        }

        // ── INPUT phase: show player's typed input so far ─────────────────────
        if (phase == Phase.INPUT || phase == Phase.ROUND_PASS || phase == Phase.FAILED) {
            int stepW = 22;
            int totalW = playerInput.size() * stepW;
            int startX = cx - totalW / 2;
            drawCenteredString(pose, this.font, "Your input:", cx, cy - 20, 0x888888);
            for (int i = 0; i < playerInput.size(); i++) {
                int d = playerInput.get(i);
                boolean ok = playerInput.get(i).equals(sequence.get(i));
                int col = ok ? 0xFF55FF55 : 0xFFFF4444;
                drawCenteredString(pose, this.font, SYMBOLS[d],
                    startX + i * stepW + stepW / 2, cy - 4, col);
            }
            // Remaining slots shown as underscores
            for (int i = playerInput.size(); i < currentSeqLen; i++) {
                drawCenteredString(pose, this.font, "_",
                    cx - (currentSeqLen - playerInput.size()) * stepW / 2
                        + (i - playerInput.size()) * stepW + stepW / 2,
                    cy - 4, 0x444444);
            }
        }

        if (phase == Phase.ROUND_PASS)
            drawCenteredString(pose, this.font, "Correct!", cx, cy + 24, 0x55FF55);
        if (phase == Phase.FAILED)
            drawCenteredString(pose, this.font, "Wrong — cooling down…", cx, cy + 24, 0xFF4444);
    }
}

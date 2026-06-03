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

/**
 * Mist Breathing – Pattern Recognition minigame.
 *
 * <p><b>Mechanic</b>: A movement sequence plays as a series of directional arrows (WASD/arrow keys).
 * The player must watch the entire sequence, then reproduce it with inputs. Sequences grow longer
 * each tier. Retry after 200 ticks (10 s) on failure.
 *
 * <p><b>Pass condition</b>: Reproduce {@code requiredRounds} sequences correctly.
 * <br><b>Fail</b>: Wrong input → 200-tick cooldown (handled server-side).
 */
public class MistMinigameScreen extends StyleMinigameScreen {

    private static final int[] DIRS = { GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A,
                                         GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D };
    private static final String[] ARROW_SYMBOLS = { "↑", "←", "↓", "→" };
    private static final int[] ARROW_COLOURS = { 0x88FFFF, 0xFFFF88, 0xFF8888, 0x88FF88 };

    private final int seqLength;
    private final int requiredRounds;
    private final int showTicksPerStep = 22; // ticks each arrow is highlighted during show phase

    private enum Phase { SHOWING, INPUT, ROUND_PASS, FAILED }
    private Phase phase = Phase.SHOWING;

    private final List<Integer> sequence  = new ArrayList<>();
    private final List<Integer> playerInput = new ArrayList<>();
    private int showIndex  = 0;   // which step is currently being shown
    private int showTick   = 0;
    private int roundsDone = 0;
    private boolean wrongInput = false;
    private int resultTick  = 0;
    private final Random rng = new Random();

    public MistMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Mist Breathing – Pattern Recognition"));
        this.seqLength     = 3 + (tier - 1) * 2;   // 3, 5, 7, …
        this.requiredRounds = 2 + (tier - 1);
        buildSequence();
    }

    private void buildSequence() {
        sequence.clear();
        playerInput.clear();
        showIndex = 0;
        showTick  = 0;
        phase     = Phase.SHOWING;
        for (int i = 0; i < seqLength; i++) {
            sequence.add(rng.nextInt(4));
        }
    }

    @Override
    protected void tickGame() {
        switch (phase) {
            case SHOWING -> {
                showTick++;
                if (showTick >= showTicksPerStep) {
                    showTick = 0;
                    showIndex++;
                    if (showIndex >= sequence.size()) {
                        // Finished showing; switch to input phase
                        phase = Phase.INPUT;
                    }
                }
            }
            case ROUND_PASS -> {
                resultTick++;
                if (resultTick >= 20) {
                    resultTick = 0;
                    roundsDone++;
                    if (roundsDone >= requiredRounds) {
                        finishMinigame(true);
                    } else {
                        buildSequence();
                    }
                }
            }
            case FAILED -> {
                resultTick++;
                if (resultTick >= 20) {
                    finishMinigame(false);
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (phase != Phase.INPUT) return false;
        int dir = -1;
        for (int i = 0; i < DIRS.length; i++) {
            if (keyCode == DIRS[i]) { dir = i; break; }
        }
        // Also accept arrow keys
        if (dir == -1) {
            dir = switch (keyCode) {
                case GLFW.GLFW_KEY_UP    -> 0;
                case GLFW.GLFW_KEY_LEFT  -> 1;
                case GLFW.GLFW_KEY_DOWN  -> 2;
                case GLFW.GLFW_KEY_RIGHT -> 3;
                default -> -1;
            };
        }
        if (dir == -1) return false;

        int expected = sequence.get(playerInput.size());
        playerInput.add(dir);

        if (dir != expected) {
            wrongInput = true;
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
        drawTitle(pose, "Pattern Recognition", 0xAADDFF);
        drawInstructions(pose,
                phase == Phase.SHOWING ? "Watch the sequence carefully…"
                                       : "Reproduce the sequence with WASD / arrow keys",
                0xCCCCCC);
        drawCenteredString(pose, this.font, "Round " + (roundsDone + 1) + " / " + requiredRounds,
                this.width / 2, 35, 0xAADDFF);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int stepW = 28;
        int totalW = seqLength * stepW;
        int startX = cx - totalW / 2;

        for (int i = 0; i < seqLength; i++) {
            int x = startX + i * stepW + stepW / 2;
            int dirIdx = sequence.get(i);
            boolean isShown = (phase == Phase.SHOWING && i < showIndex)
                           || (phase == Phase.SHOWING && i == showIndex - 1 && showTick < showTicksPerStep / 2)
                           || phase == Phase.INPUT || phase == Phase.ROUND_PASS;
            boolean isCurrent = phase == Phase.SHOWING && i == showIndex;
            boolean playerHit  = phase == Phase.INPUT && i < playerInput.size();
            boolean playerWrong = playerHit && !playerInput.get(i).equals(sequence.get(i));

            int col;
            if (isCurrent) {
                col = ARROW_COLOURS[dirIdx];
            } else if (playerWrong) {
                col = 0xFF4444;
            } else if (playerHit) {
                col = 0x55FF55;
            } else if (isShown) {
                col = 0x555555;
            } else {
                col = 0x222222;
            }

            drawCenteredString(pose, this.font, ARROW_SYMBOLS[dirIdx], x, cy - 6, col);
            // box outline
            fill(pose, x - stepW / 2 + 2, cy - 10, x + stepW / 2 - 2, cy + 6,
                    isCurrent ? 0x44AADDFF : 0x22444444);
        }

        // Player input echo
        if (phase == Phase.INPUT) {
            int inputY = cy + 30;
            drawCenteredString(pose, this.font, "Your input:", cx, inputY - 14, 0x888888);
            for (int i = 0; i < playerInput.size(); i++) {
                int x = cx - (playerInput.size() * stepW / 2) + i * stepW + stepW / 2;
                boolean ok = playerInput.get(i).equals(sequence.get(i));
                drawCenteredString(pose, this.font, ARROW_SYMBOLS[playerInput.get(i)], x, inputY,
                        ok ? 0x55FF55 : 0xFF4444);
            }
        }

        if (phase == Phase.ROUND_PASS) {
            drawCenteredString(pose, this.font, "Correct!", cx, cy + 30, 0x55FF55);
        }
        if (phase == Phase.FAILED) {
            drawCenteredString(pose, this.font, "Wrong input! Cooling down…", cx, cy + 30, 0xFF4444);
        }
    }
}

package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

/**
 * Insect Breathing – Precision Timing minigame.
 *
 * <p><b>Mechanic</b>: Multiple small, fast targets appear simultaneously. A brief flash
 * sequence reveals the required strike order. The player must click all targets in that
 * order before they disappear. Retry immediately on failure.
 *
 * <p><b>Pass condition</b>: Successfully strike all targets in order across {@code requiredRounds} rounds.
 * <br><b>Fail</b>: Wrong target clicked or all targets disappear before completion → retry.
 */
public class InsectMinigameScreen extends StyleMinigameScreen {

    private static final int REVEAL_TICKS   = 40;  // how long order numbers are shown
    private static final int ACTIVE_TICKS   = 60;  // window to click all targets after reveal

    private final int targetCount;
    private final int requiredRounds;

    private enum Phase { REVEAL, ACTIVE, ROUND_PASS, FAILED }
    private Phase phase = Phase.REVEAL;

    private final List<InsectTarget> targets = new ArrayList<>();
    private int revealTick   = 0;
    private int activeTick   = 0;
    private int nextExpected = 0;  // index into strikeOrder the player must click next
    private int roundsDone   = 0;
    private int resultTick   = 0;
    private final Random rng = new Random();

    public InsectMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Insect Breathing – Precision Timing"));
        this.targetCount   = 3 + (tier - 1);       // 3, 4, 5, …
        this.requiredRounds = 2 + (tier - 1);
        spawnRound();
    }

    private void spawnRound() {
        targets.clear();
        nextExpected = 0;
        revealTick   = 0;
        activeTick   = 0;
        phase        = Phase.REVEAL;

        // Assign random positions
        Set<String> usedCells = new HashSet<>();
        for (int i = 0; i < targetCount; i++) {
            int x, y;
            String key;
            int attempts = 0;
            do {
                x = 50 + rng.nextInt(this.width - 100);
                y = 50 + rng.nextInt(this.height - 100);
                key = (x / 30) + "," + (y / 30);
                attempts++;
            } while (usedCells.contains(key) && attempts < 30);
            usedCells.add(key);
            targets.add(new InsectTarget(x, y, i));  // strikeOrder = i
        }
        // Shuffle strike order (targets are placed; their displayed hit number differs)
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) order.add(i);
        Collections.shuffle(order, rng);
        for (int i = 0; i < targets.size(); i++) {
            targets.get(i).strikeOrder = order.get(i);
        }
    }

    @Override
    protected void tickGame() {
        switch (phase) {
            case REVEAL -> {
                revealTick++;
                if (revealTick >= REVEAL_TICKS) {
                    phase = Phase.ACTIVE;
                }
            }
            case ACTIVE -> {
                activeTick++;
                if (activeTick >= ACTIVE_TICKS) {
                    // Time ran out
                    phase = Phase.FAILED;
                    resultTick = 0;
                }
            }
            case ROUND_PASS -> {
                resultTick++;
                if (resultTick >= 18) {
                    roundsDone++;
                    if (roundsDone >= requiredRounds) {
                        finishMinigame(true);
                    } else {
                        spawnRound();
                    }
                }
            }
            case FAILED -> {
                resultTick++;
                if (resultTick >= 20) {
                    finishMinigame(false);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != Phase.ACTIVE) return false;

        InsectTarget clickedTarget = null;
        for (InsectTarget t : targets) {
            if (!t.struck) {
                double dx = mx - t.x;
                double dy = my - t.y;
                if (dx * dx + dy * dy <= 14 * 14) {
                    clickedTarget = t;
                    break;
                }
            }
        }

        if (clickedTarget == null) return false;

        if (clickedTarget.strikeOrder == nextExpected) {
            clickedTarget.struck = true;
            nextExpected++;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BEE_STING, 1.0f, 1.8f));
            if (nextExpected >= targetCount) {
                phase = Phase.ROUND_PASS;
                resultTick = 0;
            }
        } else {
            // Wrong order
            phase = Phase.FAILED;
            resultTick = 0;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Precision Timing", 0xCC66FF);
        boolean revealing = (phase == Phase.REVEAL);
        drawInstructions(pose,
                revealing ? "Memorise the strike order!"
                          : "Click targets in the numbered order!",
                0xCCCCCC);
        drawCenteredString(pose, this.font, "Round " + (roundsDone + 1) + " / " + requiredRounds,
                this.width / 2, 35, 0xCC66FF);

        // Active-phase countdown bar
        if (phase == Phase.ACTIVE) {
            float pct = 1f - (float) activeTick / ACTIVE_TICKS;
            int bw = 160, bx = (this.width - bw) / 2;
            fill(pose, bx, this.height - 25, bx + bw, this.height - 17, 0x88000000);
            fill(pose, bx, this.height - 25, bx + (int)(pct * bw), this.height - 17, 0xFFCC66FF);
        }

        for (InsectTarget t : targets) {
            if (t.struck) continue;
            int col = 0xFF882288;
            if (phase == Phase.REVEAL || revealing) col = 0xFFCC66FF;
            else if (t.strikeOrder == nextExpected) col = 0xFFFFFFFF;
            fillCircle(pose, t.x, t.y, 14, col);
            drawCircleOutline(pose, t.x, t.y, 14, 2, 0xFFCC66FF);
            if (phase == Phase.REVEAL) {
                drawCenteredString(pose, this.font, String.valueOf(t.strikeOrder + 1), t.x, t.y - 4, 0xFF000000);
            }
        }

        if (phase == Phase.ROUND_PASS)
            drawCenteredString(pose, this.font, "Round clear!", this.width / 2, this.height / 2, 0x55FF55);
        if (phase == Phase.FAILED)
            drawCenteredString(pose, this.font, "Wrong order!", this.width / 2, this.height / 2, 0xFF4444);
    }

    private static class InsectTarget {
        int x, y;
        int strikeOrder;
        boolean struck = false;
        InsectTarget(int x, int y, int order) { this.x = x; this.y = y; this.strikeOrder = order; }
    }
}

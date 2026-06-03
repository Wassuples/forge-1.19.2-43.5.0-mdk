package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Serpent Breathing – Misdirection minigame.
 *
 * <p><b>Mechanic</b>: Several identical targets flash briefly; only one is real, marked by a
 * barely-visible tell (a faint outline tint). The player has three lives. Wrong target → lose
 * a life. Teaches the style's deceptive nature. Retry immediately.
 *
 * <p><b>Pass condition</b>: Identify the real target correctly across {@code requiredRounds} rounds.
 * <br><b>Fail</b>: All lives lost → retry immediately (server-side life deduction handled in result).
 */
public class SerpentMinigameScreen extends StyleMinigameScreen {

    private final int decoyCount;
    private final int requiredRounds;
    /** Ticks the targets are visible before they all go dark. */
    private final int visibleTicks;

    private enum Phase { FLASH, CHOOSE, RESULT }
    private Phase phase = Phase.FLASH;

    private int flashTick  = 0;
    private int resultTick = 0;
    private int livesLeft;
    private int roundsDone = 0;
    private boolean lastCorrect = false;
    private int correctIndex = 0;

    private final List<DecoyTarget> targets = new ArrayList<>();
    private final Random rng = new Random();

    public SerpentMinigameScreen(UUID sessionId, int tier, int lives) {
        super(sessionId, tier, Component.literal("Serpent Breathing – Misdirection"));
        this.decoyCount    = 2 + tier;               // 3, 4, 5, …  (total = decoyCount + 1 real)
        this.requiredRounds = 2 + (tier - 1);
        this.visibleTicks  = Math.max(20, 50 - (tier - 1) * 8);  // 50 → 20 ticks
        this.livesLeft     = lives;
        spawnRound();
    }

    private void spawnRound() {
        targets.clear();
        flashTick  = 0;
        phase      = Phase.FLASH;
        correctIndex = rng.nextInt(decoyCount + 1);

        int totalTargets = decoyCount + 1;
        int cols = (int)Math.ceil(Math.sqrt(totalTargets));
        int rows = (int)Math.ceil((float)totalTargets / cols);

        // Place in a loose grid around the centre
        int cx = this.width / 2;
        int cy = this.height / 2;
        int spacingX = 60, spacingY = 55;
        int gridW = cols * spacingX, gridH = rows * spacingY;

        for (int i = 0; i < totalTargets; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = cx - gridW / 2 + col * spacingX + spacingX / 2;
            int y = cy - gridH / 2 + row * spacingY + spacingY / 2;
            targets.add(new DecoyTarget(x, y, i == correctIndex));
        }
    }

    @Override
    protected void tickGame() {
        switch (phase) {
            case FLASH -> {
                flashTick++;
                if (flashTick >= visibleTicks) {
                    // Targets go dark — player must now choose based on memory of the tell
                    phase = Phase.CHOOSE;
                }
            }
            case RESULT -> {
                resultTick++;
                if (resultTick >= 22) {
                    if (!lastCorrect && livesLeft <= 0) {
                        finishMinigame(false);
                    } else if (lastCorrect) {
                        roundsDone++;
                        if (roundsDone >= requiredRounds) {
                            finishMinigame(true);
                        } else {
                            spawnRound();
                        }
                    } else {
                        spawnRound(); // lost a life but still has more
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != Phase.CHOOSE && phase != Phase.FLASH) return false;

        for (DecoyTarget t : targets) {
            double dx = mx - t.x, dy = my - t.y;
            if (dx * dx + dy * dy <= 18 * 18) {
                lastCorrect = t.isReal;
                if (!t.isReal) {
                    livesLeft--;
                    if (minecraft != null) minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
                } else {
                    if (minecraft != null) minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.4f));
                }
                phase = Phase.RESULT;
                resultTick = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Misdirection", 0xCCCCCC);
        drawInstructions(pose,
                phase == Phase.FLASH ? "Spot the real one — look for the subtle tell…"
                                     : "Choose the real target!",
                0xCCCCCC);

        drawCenteredString(pose, this.font,
                "Lives: " + livesLeft + "   Round: " + (roundsDone + 1) + "/" + requiredRounds,
                this.width / 2, 35, 0xCCCCCC);

        // Flash timer bar
        if (phase == Phase.FLASH) {
            float pct = 1f - (float) flashTick / visibleTicks;
            int bw = 140, bx = (this.width - bw) / 2;
            fill(pose, bx, this.height - 28, bx + bw, this.height - 20, 0x44000000);
            fill(pose, bx, this.height - 28, bx + (int)(pct * bw), this.height - 20, 0xFFCCCCCC);
        }

        boolean flashPhase = (phase == Phase.FLASH);
        boolean resultPhase = (phase == Phase.RESULT);

        for (DecoyTarget t : targets) {
            int baseCol, outlineCol;
            if (resultPhase) {
                // After click: reveal all
                if (t.isReal) {
                    baseCol   = 0xFF2255AA;
                    outlineCol = 0xFF4488FF;
                } else {
                    baseCol   = 0xFF333333;
                    outlineCol = 0xFF555555;
                }
            } else if (flashPhase) {
                // All look identical except the real one has a barely-visible warm outline
                baseCol = 0xFF444444;
                outlineCol = t.isReal ? 0xFF6C5A4A : 0xFF555555; // subtle warm tint on real
            } else {
                // CHOOSE phase: everything dark
                baseCol   = 0xFF333333;
                outlineCol = 0xFF444444;
            }

            fillCircle(pose, t.x, t.y, 18, baseCol);
            drawCircleOutline(pose, t.x, t.y, 18, 2, outlineCol);
        }

        if (resultPhase) {
            String msg = lastCorrect ? "Correct!" : "Wrong!";
            int col    = lastCorrect  ? 0x55FF55   : 0xFF4444;
            drawCenteredString(pose, this.font, msg, this.width / 2, this.height / 2 - 50, col);
            if (!lastCorrect) {
                drawCenteredString(pose, this.font, "Lives remaining: " + livesLeft,
                        this.width / 2, this.height / 2 - 36, 0xFF4444);
            }
        }
    }

    private static class DecoyTarget {
        int x, y;
        boolean isReal;
        DecoyTarget(int x, int y, boolean real) { this.x = x; this.y = y; this.isReal = real; }
    }
}

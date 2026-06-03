package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Flame Breathing – Burst Timing minigame.
 *
 * <p><b>Mechanic</b>: A charge bar fills rapidly. The player must press SPACE (or click)
 * exactly at the <i>peak</i> — when the bar is in the gold zone (85–100%). Multiple bursts
 * in sequence, increasing in speed each tier.
 *
 * <p><b>Pass condition</b>: Hit the peak window on {@code requiredBursts} consecutive bursts.
 * <br><b>Fail</b>: Miss the window and lose a life (handled server-side after result packet).
 * All lives lost → 1 in-game day cooldown.
 */
public class FlameMinigameScreen extends StyleMinigameScreen {

    // ── config ────────────────────────────────────────────────────────────────
    private final int  requiredBursts;
    private final int  chargeCycleTicks; // ticks to fill from 0 → 100 %
    private final float peakLow  = 0.82f;
    private final float peakHigh = 1.0f;

    // ── state ─────────────────────────────────────────────────────────────────
    private float chargeProgress  = 0f;   // 0.0 → 1.0
    private int   burstsDone      = 0;
    private int   livesLeft;
    private boolean waitingForInput = true; // true = bar is running, waiting for press
    private int   resultDisplayTick = 0;   // flash "PERFECT!" / "MISS!" briefly
    private boolean lastHitPeak    = false;
    private boolean gameOver        = false;

    public FlameMinigameScreen(UUID sessionId, int tier, int lives) {
        super(sessionId, tier, Component.literal("Flame Breathing – Burst Timing"));
        this.requiredBursts   = 3 + (tier - 1);          // 3, 4, 5, …
        this.chargeCycleTicks = Math.max(20, 60 - (tier - 1) * 8); // 60 → 20 ticks
        this.livesLeft        = lives;
    }

    // ── game logic ────────────────────────────────────────────────────────────

    @Override
    protected void tickGame() {
        if (gameOver) return;

        if (resultDisplayTick > 0) {
            resultDisplayTick--;
            if (resultDisplayTick == 0) {
                if (livesLeft <= 0) {
                    gameOver = true;
                    finishMinigame(false);
                    return;
                }
                resetCharge();
            }
            return;
        }

        if (waitingForInput) {
            chargeProgress += 1.0f / chargeCycleTicks;
            if (chargeProgress >= 1.0f) {
                chargeProgress = 1.0f;
                // Overshot – miss
                registerMiss();
            }
        }
    }

    private void attemptStrike() {
        if (gameOver || !waitingForInput) return;

        if (chargeProgress >= peakLow && chargeProgress <= peakHigh) {
            burstsDone++;
            lastHitPeak = true;
            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.FIRECHARGE_USE, 1.3f));
            }
            if (burstsDone >= requiredBursts) {
                gameOver = true;
                finishMinigame(true);
                return;
            }
            resultDisplayTick = 18;
            waitingForInput = false;
        } else {
            registerMiss();
        }
    }

    private void registerMiss() {
        livesLeft--;
        lastHitPeak = false;
        resultDisplayTick = 25;
        waitingForInput = false;
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
        }
    }

    private void resetCharge() {
        chargeProgress = 0f;
        waitingForInput = true;
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            attemptStrike();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        attemptStrike();
        return true;
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Burst Timing", 0xFF6600);
        drawInstructions(pose, "Press SPACE at the PEAK of the charge!", 0xCCCCCC);

        // Lives
        drawCenteredString(pose, this.font, "Lives: " + livesLeft, this.width / 2, 30, 0xFF4444);

        // Progress: bursts
        drawCenteredString(pose, this.font, "Bursts: " + burstsDone + " / " + requiredBursts,
                this.width / 2, 45, 0xFFAA44);

        // Charge bar
        int barW = 240;
        int barH = 28;
        int barX = (this.width - barW) / 2;
        int barY = this.height / 2 - barH / 2;

        // Background
        fill(pose, barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0xFF222222);
        fill(pose, barX, barY, barX + barW, barY + barH, 0xFF444444);

        // Peak zone highlight
        int peakStart = barX + (int)(peakLow * barW);
        int peakEnd   = barX + barW;
        fill(pose, peakStart, barY, peakEnd, barY + barH, 0x44FFD700);

        // Charge fill
        int fillWidth = (int)(chargeProgress * barW);
        boolean inPeak = chargeProgress >= peakLow;
        int fillColour = inPeak ? 0xFFFFD700 : 0xFFFF6600;
        fill(pose, barX, barY, barX + fillWidth, barY + barH, fillColour);

        // Labels
        drawString(pose, this.font, "RELEASE HERE", peakStart + 2, barY + barH / 2 - 4, 0xFFFFD700);

        // Result flash
        if (resultDisplayTick > 0) {
            String msg = lastHitPeak ? "PERFECT!" : "MISS!";
            int col = lastHitPeak ? 0xFFD700 : 0xFF4444;
            drawCenteredString(pose, this.font, msg, this.width / 2, this.height / 2 + 30, col);
        }
    }
}

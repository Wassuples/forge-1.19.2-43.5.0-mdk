package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class FlameMinigameScreen extends StyleMinigameScreen {

    private static final float PEAK_LOW  = 0.80f;
    private static final float PEAK_HIGH = 1.00f;
    /** Base ticks for the first burst to fill 0→100%. Decreases each burst. */
    private static final int BASE_CHARGE_TICKS = 40;
    private static final int CHARGE_REDUCTION   = 10; // ticks removed per burst
    private static final int MIN_CHARGE_TICKS   = 20;

    private final int requiredBursts;
    private int livesLeft;

    private enum Phase { IDLE, CHARGING, RESULT_PASS, RESULT_MISS }
    private Phase phase = Phase.IDLE;

    private float chargeProgress  = 0f;
    private int   burstsDone      = 0;
    private int   resultTick      = 0;
    private boolean holdingSpace  = false;
    private boolean gameOver      = false;

    /** Ticks it takes to fill the bar for the current burst. Decreases each burst. */
    private int currentChargeTicks;

    public FlameMinigameScreen(UUID sessionId, int tier, int lives) {
        super(sessionId, tier, Component.literal("Flame Breathing – Burst Timing"));
        this.requiredBursts    = 3 + (tier - 1);
        this.livesLeft         = lives;
        this.currentChargeTicks = Math.max(MIN_CHARGE_TICKS,
            BASE_CHARGE_TICKS - (tier - 1) * CHARGE_REDUCTION);
    }

    @Override
    protected void tickGame() {
        if (gameOver) return;

        switch (phase) {
            case IDLE -> {
                // Waiting for the player to press SPACE — do nothing
            }
            case CHARGING -> {
                if (holdingSpace) {
                    chargeProgress += 1.0f / currentChargeTicks;
                    if (chargeProgress >= 1.0f) {
                        chargeProgress = 1.0f;
                        // Held too long past peak — auto-miss
                        registerMiss();
                    }
                }
            }
            case RESULT_PASS, RESULT_MISS -> {
                resultTick++;
                if (resultTick >= 22) {
                    resultTick = 0;
                    if (phase == Phase.RESULT_MISS && livesLeft <= 0) {
                        gameOver = true;
                        finishMinigame(false);
                    } else {
                        chargeProgress = 0f;
                        holdingSpace   = false;
                        phase          = Phase.IDLE;
                    }
                }
            }
        }
    }

    private void onSpacePressed() {
        if (gameOver || phase != Phase.IDLE) return;
        holdingSpace   = true;
        chargeProgress = 0f;
        phase          = Phase.CHARGING;
    }

    private void onSpaceReleased() {
        if (gameOver || phase != Phase.CHARGING) return;
        holdingSpace = false;

        if (chargeProgress >= PEAK_LOW && chargeProgress <= PEAK_HIGH) {
            // Good release
            burstsDone++;
            if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.FIRECHARGE_USE, 1.3f));
            if (burstsDone >= requiredBursts) {
                gameOver = true;
                finishMinigame(true);
                return;
            }
            // Speed up the bar for the next burst
            currentChargeTicks = Math.max(MIN_CHARGE_TICKS,
                currentChargeTicks - CHARGE_REDUCTION);
            phase = Phase.RESULT_PASS;
            resultTick = 0;
        } else {
            registerMiss();
        }
    }

    private void registerMiss() {
        livesLeft--;
        holdingSpace = false;
        phase        = Phase.RESULT_MISS;
        resultTick   = 0;
        if (minecraft != null) minecraft.getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) { onSpacePressed(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) { onSpaceReleased(); return true; }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Burst Timing", 0xFF6600);
        drawInstructions(pose,
            phase == Phase.IDLE ? "Hold SPACE to charge — release AT the peak!"
                : phase == Phase.CHARGING ? "Release at the peak!"
                  : "",
            0xCCCCCC);

        drawCenteredString(pose, this.font,
            "Lives: " + livesLeft + "   Bursts: " + burstsDone + " / " + requiredBursts,
            this.width / 2, 35, 0xFFAA44);

        // Charge bar
        int barW = 240, barH = 28;
        int barX = (this.width - barW) / 2;
        int barY = this.height / 2 - barH / 2;

        fill(pose, barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0xFF222222);
        fill(pose, barX, barY, barX + barW, barY + barH, 0xFF3A3A3A);

        // Peak zone highlight
        int peakStart = barX + (int)(PEAK_LOW * barW);
        fill(pose, peakStart, barY, barX + barW, barY + barH, 0x44FFD700);

        // Charge fill
        if (phase == Phase.CHARGING || phase == Phase.RESULT_PASS || phase == Phase.RESULT_MISS) {
            int fw = (int)(chargeProgress * barW);
            int fc = (chargeProgress >= PEAK_LOW) ? 0xFFFFD700 : 0xFFFF6600;
            fill(pose, barX, barY, barX + fw, barY + barH, fc);
        }

        // Speed indicator (decreasing ticks)
        String speedStr = "Speed: " + currentChargeTicks + " ticks";
        drawCenteredString(pose, this.font, speedStr, this.width / 2, barY + barH + 10, 0x888888);

        drawString(pose, this.font, "RELEASE HERE →", peakStart + 4, barY + barH / 2 - 4, 0xFFD700);

        // Phase-specific feedback
        switch (phase) {
            case IDLE ->
                drawCenteredString(pose, this.font, "Hold SPACE to begin", this.width / 2,
                    this.height / 2 + 28, 0x888888);
            case RESULT_PASS ->
                drawCenteredString(pose, this.font, "PERFECT!", this.width / 2,
                    this.height / 2 + 28, 0xFFD700);
            case RESULT_MISS ->
                drawCenteredString(pose, this.font,
                    "MISS! " + livesLeft + " life/lives remaining",
                    this.width / 2, this.height / 2 + 28, 0xFF4444);
            default -> {}
        }
    }
}

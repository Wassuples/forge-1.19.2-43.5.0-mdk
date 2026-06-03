package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Stone Breathing – Endurance &amp; Resolve minigame.
 *
 * <p><b>Mechanic</b>: The player must maintain a minimum clicks-per-second (CPS) threshold
 * for a fixed duration. Both the required rate and duration escalate per tier.
 * Slowing down or stopping at any point fails the attempt.
 *
 * <p><b>Pass condition</b>: Maintain required CPS throughout the full duration.
 * <br><b>Fail</b>: CPS drops below threshold → 1 in-game day cooldown.
 */
public class StoneMinigameScreen extends StyleMinigameScreen {

    // ── config ────────────────────────────────────────────────────────────────
    private final int  durationTicks;
    private final float requiredCps;
    private final int  measureWindowTicks = 20; // 1 second rolling window for CPS

    // ── state ─────────────────────────────────────────────────────────────────
    private int   elapsed        = 0;
    private int[] clickHistory;  // circular buffer: click count per tick
    private int   bufferHead     = 0;
    private float currentCps     = 0f;
    private boolean started      = false;
    private boolean failed       = false;
    private int   failFlashTick  = 0;

    public StoneMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Stone Breathing – Endurance & Resolve"));
        this.durationTicks = 60 + (tier - 1) * 40;   // 60, 100, 140, … ticks
        this.requiredCps   = 2.5f + (tier - 1) * 0.5f; // 2.5, 3.0, 3.5, …
        this.clickHistory  = new int[measureWindowTicks];
    }

    @Override
    protected void tickGame() {
        if (failed) {
            failFlashTick++;
            if (failFlashTick >= 30) finishMinigame(false);
            return;
        }
        if (!started) return;

        elapsed++;

        // Advance circular buffer
        bufferHead = (bufferHead + 1) % measureWindowTicks;
        clickHistory[bufferHead] = 0; // clear the oldest slot

        // Compute rolling CPS
        int totalClicks = 0;
        for (int c : clickHistory) totalClicks += c;
        currentCps = totalClicks / (measureWindowTicks / 20.0f);

        if (elapsed > measureWindowTicks && currentCps < requiredCps) {
            failed = true;
            failFlashTick = 0;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6f));
            return;
        }

        if (elapsed >= durationTicks) {
            finishMinigame(true);
        }
    }

    private void registerClick() {
        if (failed) return;
        if (!started) started = true;
        clickHistory[bufferHead]++;
        if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0f, 1.2f));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        registerClick();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            registerClick();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Endurance & Resolve", 0xBB8844);
        drawInstructions(pose,
                started ? "Keep clicking / pressing SPACE!" : "Click or press SPACE to begin",
                0xCCCCCC);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Duration bar
        float progress = started ? Math.min(1f, (float) elapsed / durationTicks) : 0f;
        int barW = 220;
        int barX = cx - barW / 2;
        int barY = cy - 12;
        fill(pose, barX - 2, barY - 2, barX + barW + 2, barY + 18, 0xFF222222);
        fill(pose, barX, barY, barX + barW, barY + 14, 0xFF444444);
        fill(pose, barX, barY, barX + (int)(progress * barW), barY + 14,
                failed ? 0xFFAA3322 : 0xFFBB8844);
        drawCenteredString(pose, this.font,
                (int)(progress * 100) + "% complete",
                cx, barY + 18, 0xBB8844);

        // CPS gauge
        int gaugeY = cy + 30;
        drawCenteredString(pose, this.font,
                String.format("Current CPS: %.1f  /  Required: %.1f", currentCps, requiredCps),
                cx, gaugeY, currentCps >= requiredCps || !started ? 0x55FF55 : 0xFF4444);

        // Big warning
        if (failed) {
            drawCenteredString(pose, this.font, "SLOWED DOWN — FAILED!", cx, cy - 30, 0xFF4444);
        } else if (started && elapsed > measureWindowTicks && currentCps < requiredCps * 0.8f) {
            drawCenteredString(pose, this.font, "GOING TOO SLOW!", cx, cy - 30, 0xFFAA44);
        }

        // Instruction circle to click
        if (!started || (!failed)) {
            fillCircle(pose, cx, cy + 65, 22,
                    started ? 0x44BB8844 : 0x88BB8844);
            drawCircleOutline(pose, cx, cy + 65, 22, 2, 0xFFBB8844);
            drawCenteredString(pose, this.font, "CLICK", cx, cy + 61, 0xFFFFFF);
        }
    }
}

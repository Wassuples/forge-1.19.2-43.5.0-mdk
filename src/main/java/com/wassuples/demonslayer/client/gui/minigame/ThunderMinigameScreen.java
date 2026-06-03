package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;
import java.util.UUID;

/**
 * Thunder Breathing – Reaction Speed minigame.
 *
 * <p><b>Mechanic</b>: A single target flashes on screen for an extremely brief window
 * (shrinking per tier). Strike it within the window to pass. Pure single-reaction —
 * no chains. Persistence over perfection: retry immediately on miss.
 *
 * <p><b>Pass condition</b>: Click the target {@code requiredHits} times within their windows.
 * <br><b>Fail</b>: Window closes without a click → immediate retry.
 */
public class ThunderMinigameScreen extends StyleMinigameScreen {

    // ── config ────────────────────────────────────────────────────────────────
    private final int windowTicks;    // how long the target is visible
    private final int requiredHits;
    private final int cooldownTicks = 30; // delay between flashes (darkness period)

    // ── state ─────────────────────────────────────────────────────────────────
    private enum Phase { WAITING, VISIBLE, RESULT_FLASH }
    private Phase phase = Phase.WAITING;

    private int phaseTick  = 0;
    private int targetX, targetY;
    private int hitsAchieved = 0;
    private boolean lastHit  = false;
    private final Random rng = new Random();

    public ThunderMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Thunder Breathing – Reaction Speed"));
        this.windowTicks  = Math.max(5, 18 - (tier - 1) * 3);  // 18 → 5 ticks
        this.requiredHits = 3 + (tier - 1);
    }

    // ── game logic ────────────────────────────────────────────────────────────

    @Override
    protected void tickGame() {
        phaseTick++;
        switch (phase) {
            case WAITING -> {
                if (phaseTick >= cooldownTicks) {
                    flashTarget();
                }
            }
            case VISIBLE -> {
                if (phaseTick >= windowTicks) {
                    // Missed
                    lastHit = false;
                    phase = Phase.RESULT_FLASH;
                    phaseTick = 0;
                }
            }
            case RESULT_FLASH -> {
                if (phaseTick >= 15) {
                    phase = Phase.WAITING;
                    phaseTick = 0;
                }
            }
        }
    }

    private void flashTarget() {
        targetX = 40 + rng.nextInt(this.width  - 80);
        targetY = 50 + rng.nextInt(this.height - 100);
        phase   = Phase.VISIBLE;
        phaseTick = 0;
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 2.0f));
        }
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != Phase.VISIBLE) return false;

        double dx = mx - targetX;
        double dy = my - targetY;
        if (dx * dx + dy * dy <= 20 * 20) {
            hitsAchieved++;
            lastHit = true;
            phase = Phase.RESULT_FLASH;
            phaseTick = 0;
            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f));
            }
            if (hitsAchieved >= requiredHits) {
                finishMinigame(true);
            }
        }
        return true;
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Reaction Speed", 0xFFEE00);
        drawInstructions(pose, "Click the lightning bolt the instant it appears!", 0xCCCCCC);
        drawCenteredString(pose, this.font, "Hits: " + hitsAchieved + " / " + requiredHits,
                this.width / 2, 35, 0xFFEE00);

        if (phase == Phase.VISIBLE) {
            // Flash – bright yellow lightning target
            int alpha = (int)(255 * (1f - (float)phaseTick / windowTicks));
            int col = (alpha << 24) | 0x00FFEE00;
            fillCircle(pose, targetX, targetY, 20, col);
            drawCircleOutline(pose, targetX, targetY, 20, 3, 0xFFFFFFFF);
            drawCenteredString(pose, this.font, "⚡", targetX, targetY - 4, 0xFFFFFF);
        }

        if (phase == Phase.RESULT_FLASH) {
            String msg = lastHit ? "HIT!" : "TOO SLOW!";
            int col = lastHit ? 0xFFEE00 : 0xFF4444;
            drawCenteredString(pose, this.font, msg, this.width / 2, this.height / 2, col);
        }

        // Waiting counter (shows a dim "incoming" flash)
        if (phase == Phase.WAITING && phaseTick > cooldownTicks / 2) {
            fill(pose, this.width / 2 - 1, this.height / 2 - 1,
                      this.width / 2 + 1, this.height / 2 + 1, 0x33FFEE00);
        }
    }
}

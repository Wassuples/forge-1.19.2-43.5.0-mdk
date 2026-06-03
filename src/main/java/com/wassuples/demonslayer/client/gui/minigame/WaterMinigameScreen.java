package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

/**
 * Water Breathing – Flow &amp; Continuity minigame.
 *
 * <p><b>Mechanic</b>: Circular targets appear in waves along a flowing connected path.
 * Click each before it shrinks to zero without breaking the chain.
 * Breaking the chain resets the attempt immediately (no penalty).
 *
 * <p><b>Pass condition</b>: Complete {@code requiredChains} consecutive successful chains.
 * <br><b>Fail</b>: Chain broken → immediate retry (screen closes and re-opens is handled
 * server-side; here we simply notify the server with {@code passed = false}).
 */
public class WaterMinigameScreen extends StyleMinigameScreen {

    // ── config (scales with tier) ─────────────────────────────────────────────
    private final int targetsPerChain;   // how many targets in each chain wave
    private final int requiredChains;    // chains needed to pass
    private final int targetLifeTicks;   // starting lifetime per target (ticks)

    // ── state ─────────────────────────────────────────────────────────────────
    private final List<CircleTarget> targets = new ArrayList<>();
    private int currentChainIndex  = 0;   // next target index the player must click
    private int chainsCompleted    = 0;
    private boolean spawnNextWave  = true;
    private int waveDelayTick      = 0;

    public WaterMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Water Breathing – Flow & Continuity"));
        this.targetsPerChain = 4 + (tier - 1) * 2;          // 4, 6, 8, …
        this.requiredChains  = 2 + (tier - 1);               // 2, 3, 4, …
        this.targetLifeTicks = Math.max(30, 60 - (tier - 1) * 5); // 60 → 30 ticks
    }

    // ── game logic ────────────────────────────────────────────────────────────

    @Override
    protected void tickGame() {
        // Spawn first / next wave after a short pause
        if (spawnNextWave) {
            waveDelayTick++;
            if (waveDelayTick >= 20) { // 1 second delay between waves
                spawnWave();
                spawnNextWave = false;
                waveDelayTick = 0;
            }
            return;
        }

        boolean anyAlive = false;
        for (CircleTarget t : targets) {
            if (t.alive) {
                t.remainingTicks--;
                anyAlive = true;
                if (t.remainingTicks <= 0) {
                    t.alive = false;
                    // Target expired – chain broken
                    chainBroken();
                    return;
                }
            }
        }

        if (!anyAlive && currentChainIndex >= targets.size()) {
            // All targets in this chain clicked in time
            chainsCompleted++;
            if (chainsCompleted >= requiredChains) {
                finishMinigame(true);
            } else {
                spawnNextWave = true;
            }
        }
    }

    private void spawnWave() {
        targets.clear();
        currentChainIndex = 0;
        Random rng = new Random();

        // Generate a "flowing path" of positions
        int cx = this.width / 2;
        int cy = this.height / 2;
        double angle = rng.nextDouble() * Math.PI * 2;
        double radius = 60 + tier * 10;

        for (int i = 0; i < targetsPerChain; i++) {
            int x = cx + (int)(Math.cos(angle + i * 0.7) * radius * (0.6 + i * 0.1));
            int y = cy + (int)(Math.sin(angle + i * 0.7) * radius * (0.6 + i * 0.1));
            // Clamp to screen
            x = Math.max(20, Math.min(this.width - 20, x));
            y = Math.max(30, Math.min(this.height - 20, y));
            targets.add(new CircleTarget(x, y, targetLifeTicks, i));
        }
    }

    private void chainBroken() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 1.0f));
        }
        // Reset chain but keep chainsCompleted at 0
        targets.clear();
        currentChainIndex  = 0;
        chainsCompleted    = 0;
        spawnNextWave      = true;
        waveDelayTick      = 0;
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (targets.isEmpty()) return false;
        if (currentChainIndex >= targets.size()) return false;

        CircleTarget next = targets.get(currentChainIndex);
        if (!next.alive) return false;

        double dx = mx - next.x;
        double dy = my - next.y;
        int radius = next.currentRadius();

        if (dx * dx + dy * dy <= (double)(radius * radius)) {
            // Correct target clicked
            next.alive = false;
            currentChainIndex++;
            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.5f));
            }
        } else {
            // Clicked wrong target – chain broken
            chainBroken();
        }
        return true;
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Flow & Continuity", 0x55AAFF);
        drawInstructions(pose, "Click each target in order before it vanishes", 0xCCCCCC);

        // Progress bar (chains)
        int barW = 200;
        int barX = (this.width - barW) / 2;
        int barY = this.height - 30;
        fill(pose, barX, barY, barX + barW, barY + 8, 0x88000000);
        int filled = (int)((double)chainsCompleted / requiredChains * barW);
        fill(pose, barX, barY, barX + filled, barY + 8, 0xFF55AAFF);
        drawCenteredString(pose, this.font, chainsCompleted + " / " + requiredChains + " chains",
                this.width / 2, barY - 12, 0x55AAFF);

        // Targets
        for (int i = 0; i < targets.size(); i++) {
            CircleTarget t = targets.get(i);
            if (!t.alive) continue;

            int radius = t.currentRadius();
            float progress = (float) t.remainingTicks / targetLifeTicks;

            // Outer ring fades from blue to red as time runs out
            int r = (int)((1 - progress) * 255);
            int b = (int)(progress * 255);
            int colour = 0xFF000000 | (r << 16) | b;

            boolean isNext = (i == currentChainIndex);
            if (isNext) {
                fillCircle(pose, t.x, t.y, radius, 0x4455AAFF);
                drawCircleOutline(pose, t.x, t.y, radius, 2, colour);
                drawCenteredString(pose, this.font, String.valueOf(i + 1), t.x, t.y - 4, 0xFFFFFF);
            } else {
                drawCircleOutline(pose, t.x, t.y, radius, 2, 0x88888888);
                drawCenteredString(pose, this.font, String.valueOf(i + 1), t.x, t.y - 4, 0x888888);
            }
        }

        // "Chain broken!" flash
        if (spawnNextWave && waveDelayTick < 10) {
            drawCenteredString(pose, this.font, "Chain broken!", this.width / 2,
                    this.height / 2 - 20, 0xFF3333);
        }
    }

    // ── inner class ───────────────────────────────────────────────────────────

    private class CircleTarget {
        final int x, y, orderIndex;
        int remainingTicks;
        boolean alive = true;

        CircleTarget(int x, int y, int life, int order) {
            this.x = x; this.y = y; this.remainingTicks = life; this.orderIndex = order;
        }

        int currentRadius() {
            float progress = (float) remainingTicks / targetLifeTicks;
            return Math.max(6, (int)(24 * progress)); // shrinks from 24 down to 6 px
        }
    }
}

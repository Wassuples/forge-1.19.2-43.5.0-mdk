package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.UUID;

/**
 * Wind Breathing \u2013 Sustained Pressure minigame.
 *
 * <p><b>Mechanic</b>: A target follows an erratic wind-pattern path across the screen.
 * The player must click it repeatedly to score hits. Too many consecutive misses fails
 * the attempt immediately.
 *
 * <p><b>Pass condition</b>: Land {@code requiredHits} total clicks on the target within
 * the time limit.
 * <br><b>Fail</b>: Exceed {@code maxConsecutiveMisses} consecutive missed clicks, OR
 * the time limit expires before all required hits are landed.
 */
public class WindMinigameScreen extends StyleMinigameScreen {

    private final int requiredHits;
    private final int maxConsecutiveMisses;
    private final int targetRadius = 14;
    private final double baseSpeed;

    /** Total tick budget to land all required hits. */
    private final int timeLimitTicks;
    private int ticksElapsed = 0;

    // Target position & velocity
    private double tx, ty;
    private double vx, vy;

    private int hitsLanded        = 0;
    private int consecutiveMisses = 0;

    // Erratic wind direction change
    private int dirChangeTick     = 0;
    private int dirChangeInterval;

    private boolean failed = false;

    public WindMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Wind Breathing \u2013 Sustained Pressure"));
        this.requiredHits         = 6 + (tier - 1) * 3;        // 6, 9, 12, \u2026
        this.maxConsecutiveMisses = Math.max(3, 8 - tier);      // 7, 6, 5, 4, 3 \u2026
        this.baseSpeed            = 1.5 + (tier - 1) * 0.5;
        this.dirChangeInterval    = Math.max(15, 35 - tier * 4);
        // Time limit: 15 s base, shrinks by 1 s per tier (20 ticks = 1 s)
        this.timeLimitTicks       = Math.max(200, (15 - (tier - 1)) * 20); // 300\u2192200 ticks
        this.tx = 0; this.ty = 0;
        this.vx = baseSpeed; this.vy = baseSpeed * 0.6;
    }

    @Override
    protected void init() {
        super.init();
        tx = this.width  / 2.0;
        ty = this.height / 2.0;
        vx = baseSpeed;
        vy = baseSpeed * 0.6;
    }

    @Override
    protected void tickGame() {
        if (failed) return;

        // Time limit check
        ticksElapsed++;
        if (ticksElapsed >= timeLimitTicks) {
            failed = true;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
            finishMinigame(false);
            return;
        }

        // Move target
        tx += vx;
        ty += vy;

        // Bounce off edges
        if (tx < targetRadius || tx > this.width - targetRadius) {
            vx = -vx;
            tx = Math.max(targetRadius, Math.min(this.width - targetRadius, tx));
        }
        if (ty < 50 || ty > this.height - targetRadius) {
            vy = -vy;
            ty = Math.max(50, Math.min(this.height - targetRadius, ty));
        }

        // Erratic direction change
        dirChangeTick++;
        if (dirChangeTick >= dirChangeInterval) {
            dirChangeTick = 0;
            double angle = Math.random() * Math.PI * 2;
            double speed = baseSpeed + Math.random() * baseSpeed * 0.8;
            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;
            dirChangeInterval = 15 + (int)(Math.random() * 20);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (failed) return false;

        double dx = mx - tx;
        double dy = my - ty;
        if (dx * dx + dy * dy <= (double)(targetRadius * targetRadius)) {
            hitsLanded++;
            consecutiveMisses = 0;
            if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.4f));
            if (hitsLanded >= requiredHits) {
                finishMinigame(true);
            }
        } else {
            consecutiveMisses++;
            if (consecutiveMisses >= maxConsecutiveMisses) {
                failed = true;
                if (minecraft != null) minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
                finishMinigame(false);
            }
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Sustained Pressure", 0x88FF88);
        drawInstructions(pose, "Keep clicking the target \u2014 don\u2019t miss too many or run out of time!", 0xCCCCCC);

        drawCenteredString(pose, this.font,
                "Hits: " + hitsLanded + " / " + requiredHits
                        + "   Misses: " + consecutiveMisses + "/" + maxConsecutiveMisses,
                this.width / 2, 35, 0x88FF88);

        // Time limit bar
        float timePct = 1f - (float) ticksElapsed / timeLimitTicks;
        int bw = 160, bx = (this.width - bw) / 2;
        fill(pose, bx, this.height - 30, bx + bw, this.height - 22, 0x44000000);
        // Colour shifts red as time runs low
        int barColour = timePct > 0.4f ? 0xFF88FF88 : timePct > 0.2f ? 0xFFFFAA00 : 0xFFFF3333;
        fill(pose, bx, this.height - 30, bx + (int)(timePct * bw), this.height - 22, barColour);
        drawCenteredString(pose, this.font,
                "Time: " + Math.max(0, (timeLimitTicks - ticksElapsed) / 20) + "s",
                this.width / 2, this.height - 42, 0xAAAAAA);

        int ix = (int) tx, iy = (int) ty;

        // Wind gust lines radiating from target
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            int ex = ix + (int)(Math.cos(angle) * (targetRadius + 8));
            int ey = iy + (int)(Math.sin(angle) * (targetRadius + 8));
            fill(pose, Math.min(ix, ex), iy, Math.max(ix, ex) + 1, iy + 1, 0x4488FF88);
        }

        // Target
        fillCircle(pose, ix, iy, targetRadius, 0xFF1A5F1A);
        drawCircleOutline(pose, ix, iy, targetRadius, 2, 0xFF88FF88);
        drawCenteredString(pose, this.font, "\u2746", ix, iy - 4, 0xFFFFFF);

        // Consecutive miss warning
        if (consecutiveMisses >= maxConsecutiveMisses - 1) {
            drawCenteredString(pose, this.font, "WARNING: one more miss fails!",
                    this.width / 2, this.height - 55, 0xFF4444);
        }
    }
}

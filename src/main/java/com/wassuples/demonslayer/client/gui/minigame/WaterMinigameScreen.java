package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

/**
 * Water Breathing - Flow and Continuity minigame.
 *
 * Only the CURRENT circle is ever visible. All others are hidden until their turn.
 * Circles are placed in a connected winding path so each is near the next.
 */
public class WaterMinigameScreen extends StyleMinigameScreen {

    private static final int TARGET_COUNT = 12;

    private final CircleTarget[] targets = new CircleTarget[TARGET_COUNT];
    private final int targetLifeTicks;

    private int     activeIndex = 0;
    private boolean failed      = false;
    private int     failFlash   = 0;

    public WaterMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Water Breathing - Flow & Continuity"));
        this.targetLifeTicks = Math.max(30, 70 - (tier - 1) * 10);
    }

    @Override
    protected void init() {
        super.init();
        buildTargets();
    }

    private void buildTargets() {
        activeIndex = 0;
        failed      = false;
        failFlash   = 0;

        Random rng    = new Random();
        int margin    = 50;
        int usableW   = Math.max(1, this.width  - margin * 2);
        int usableH   = Math.max(1, this.height - margin * 2);

        int x = margin + rng.nextInt(usableW);
        int y = margin + rng.nextInt(usableH);
        double angle = rng.nextDouble() * 2 * Math.PI;

        for (int i = 0; i < TARGET_COUNT; i++) {
            // All targets start invisible/inactive; only the first is activated below
            targets[i] = new CircleTarget(x, y);

            // Advance position along a winding path for the NEXT circle
            double stepDist = 65 + rng.nextDouble() * 45;   // 65-110 px between circles
            angle += (rng.nextDouble() - 0.5) * Math.PI;    // up to +-90 deg variance per step

            int nx = x + (int)(Math.cos(angle) * stepDist);
            int ny = y + (int)(Math.sin(angle) * stepDist);

            // Bounce off horizontal walls
            if (nx < margin || nx > this.width - margin) {
                angle = Math.PI - angle;
                nx = Math.max(margin, Math.min(this.width - margin,
                        x + (int)(Math.cos(angle) * stepDist)));
            }
            // Bounce off vertical walls
            if (ny < margin || ny > this.height - margin) {
                angle = -angle;
                ny = Math.max(margin, Math.min(this.height - margin,
                        y + (int)(Math.sin(angle) * stepDist)));
            }

            x = nx;
            y = ny;
        }

        // Activate only the first target; all others remain invisible until their turn
        targets[0].activate(targetLifeTicks);
    }

    @Override
    protected void tickGame() {
        if (failed) {
            failFlash++;
            if (failFlash >= 25) finishMinigame(false);
            return;
        }
        if (activeIndex >= TARGET_COUNT) return;

        CircleTarget active = targets[activeIndex];
        if (!active.alive) return;

        active.remainingTicks--;
        if (active.remainingTicks <= 0) {
            failed    = true;
            failFlash = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (failed || activeIndex >= TARGET_COUNT) return false;

        CircleTarget active = targets[activeIndex];
        if (!active.alive || !active.activated) return false;

        int r = active.currentRadius();
        double dx = mx - active.x, dy = my - active.y;
        if (dx * dx + dy * dy <= (double)(r * r)) {
            active.alive = false;
            activeIndex++;

            if (minecraft != null) minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.5f));

            if (activeIndex >= TARGET_COUNT) {
                finishMinigame(true);
            } else {
                targets[activeIndex].activate(targetLifeTicks);
            }
        }
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        drawTitle(pose, "Flow & Continuity", 0x55AAFF);
        drawInstructions(pose, "Click each circle before it vanishes!", 0xCCCCCC);
        drawCenteredString(pose, this.font,
            activeIndex + " / " + TARGET_COUNT,
            this.width / 2, 35, 0x55AAFF);

        // Only the currently active circle is rendered; all others are invisible
        if (!failed && activeIndex < TARGET_COUNT) {
            CircleTarget t = targets[activeIndex];
            if (t.alive && t.activated) {
                int r = t.currentRadius();
                float progress    = (float) t.remainingTicks / t.totalLifeTicks;
                int   rr          = (int)((1f - progress) * 255);
                int   bb          = (int)(progress * 255);
                int   ringColour  = 0xFF000000 | (rr << 16) | bb;

                fillCircle(pose, t.x, t.y, r, 0x2255AAFF);
                drawCircleOutline(pose, t.x, t.y, r, 3, ringColour);
                drawCenteredString(pose, this.font,
                    String.valueOf(activeIndex + 1), t.x, t.y - 4, 0xFFFFFF);
            }
        }

        if (failed) {
            drawCenteredString(pose, this.font, "Too slow!",
                this.width / 2, this.height / 2, 0xFF4444);
        }
    }

    // -------------------------------------------------------------------------

    private static class CircleTarget {
        final int x, y;
        boolean activated     = false;  // false = not yet the player's turn; must not render
        boolean alive         = true;
        int     remainingTicks = 0;
        int     totalLifeTicks = 0;

        CircleTarget(int x, int y) { this.x = x; this.y = y; }

        void activate(int life) {
            this.totalLifeTicks  = life;
            this.remainingTicks  = life;
            this.activated       = true;
            this.alive           = true;
        }

        int currentRadius() {
            if (totalLifeTicks <= 0) return 8;
            float pct = (float) remainingTicks / totalLifeTicks;
            return Math.max(8, (int)(26 * pct));
        }
    }
}

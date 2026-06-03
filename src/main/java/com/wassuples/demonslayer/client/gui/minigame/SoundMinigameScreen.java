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
 * Sound Breathing – Rhythm minigame.
 *
 * <p><b>Mechanic</b>: A scrolling rhythm track shows beats as markers. The player presses
 * SPACE (or clicks) when a beat marker crosses the hit line. Full-miss streaks reduce the
 * attempt score below the pass threshold. Retry immediately.
 *
 * <p><b>Pass condition</b>: Hit at least {@code passPercent}% of beats before the track ends.
 * <br><b>Fail</b>: Score falls below threshold → retry immediately.
 */
public class SoundMinigameScreen extends StyleMinigameScreen {

    private static final int HIT_LINE_X_OFFSET = 80;  // px from left edge
    private static final int TRACK_Y_MID_RATIO  = 2;  // height / this
    private static final float PASS_PERCENT      = 0.65f;
    private static final int HIT_WINDOW_TICKS    = 5;  // ±5 ticks around the beat

    private final int beatInterval;   // ticks between beats
    private final int trackLength;    // total beats
    private final float scrollSpeed;  // px per tick

    private int   tick        = 0;
    private int   totalBeats  = 0;
    private int   beatsHit    = 0;
    private int   missStreak  = 0;
    private final int maxMissStreak;
    private boolean finished  = false;

    /** x-coordinate of each upcoming beat (in "scroll space", i.e. pixel offset from start). */
    private final List<BeatMarker> beats = new ArrayList<>();

    private final Random rng = new Random();

    public SoundMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Sound Breathing – Rhythm"));
        this.beatInterval  = Math.max(12, 24 - (tier - 1) * 3);  // 24, 21, 18, … ticks
        this.trackLength   = 12 + (tier - 1) * 4;                 // 12, 16, 20, …
        this.scrollSpeed   = 2.5f + (tier - 1) * 0.5f;
        this.maxMissStreak = Math.max(2, 5 - tier);
    }

    @Override
    protected void init() {
        super.init();
        // Pre-place beats: each beat is spread by beatInterval * scrollSpeed pixels apart
        int hitLineX = HIT_LINE_X_OFFSET;
        for (int i = 0; i < trackLength; i++) {
            int startX = hitLineX + (int)((i + 3) * beatInterval * scrollSpeed); // +3 so first beat enters from right
            beats.add(new BeatMarker(startX));
        }
        totalBeats = trackLength;
    }

    @Override
    protected void tickGame() {
        if (finished) return;
        tick++;

        // Scroll all beats left
        for (BeatMarker b : beats) {
            b.x -= scrollSpeed;
        }

        // Auto-miss any beat that scrolled past the hit line without being hit
        int hitLineX = HIT_LINE_X_OFFSET;
        for (BeatMarker b : beats) {
            if (!b.hit && !b.missed && b.x < hitLineX - HIT_WINDOW_TICKS * (int)scrollSpeed) {
                b.missed = true;
                missStreak++;
                if (missStreak >= maxMissStreak) {
                    finished = true;
                    finishMinigame(false);
                    return;
                }
            }
        }

        // Check if all beats are done
        boolean allDone = beats.stream().allMatch(b -> b.hit || b.missed);
        if (allDone) {
            finished = true;
            float score = (float) beatsHit / totalBeats;
            finishMinigame(score >= PASS_PERCENT);
        }
    }

    private void attemptHit() {
        if (finished) return;
        int hitLineX = HIT_LINE_X_OFFSET;
        for (BeatMarker b : beats) {
            if (!b.hit && !b.missed) {
                int dist = Math.abs(b.x - hitLineX);
                if (dist <= HIT_WINDOW_TICKS * (int)scrollSpeed) {
                    b.hit = true;
                    beatsHit++;
                    missStreak = 0;
                    if (minecraft != null) minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.2f, 1.0f));
                    return;
                }
            }
        }
        // No beat in window — phantom press, not counted as miss
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) { attemptHit(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        attemptHit();
        return true;
    }

    @Override
    protected void renderGame(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int trackY = this.height / TRACK_Y_MID_RATIO;
        int hitLineX = HIT_LINE_X_OFFSET;

        drawTitle(pose, "Rhythm", 0xFFAA44);
        drawInstructions(pose, "Press SPACE when the beat hits the line!", 0xCCCCCC);

        // Score line
        drawCenteredString(pose, this.font,
                "Hits: " + beatsHit + " / " + totalBeats + "   Miss streak: " + missStreak + "/" + maxMissStreak,
                cx, 35, 0xFFAA44);

        // Track lane
        fill(pose, 0, trackY - 12, this.width, trackY + 12, 0x44333333);

        // Hit line
        fill(pose, hitLineX - 2, trackY - 18, hitLineX + 2, trackY + 18, 0xFFFF8800);
        drawString(pose, this.font, "HIT", hitLineX - 8, trackY + 20, 0xFFAA44);

        // Beat markers
        for (BeatMarker b : beats) {
            if (b.x < 0 || b.x > this.width + 20) continue;
            int col;
            if (b.hit)   col = 0xFF55FF55;
            else if (b.missed) col = 0xFF444444;
            else          col = 0xFFFFAA44;
            fill(pose, b.x - 8, trackY - 10, b.x + 8, trackY + 10, col);
            fill(pose, b.x - 6, trackY - 8,  b.x + 6, trackY + 8,
                    b.hit ? 0xAA55FF55 : b.missed ? 0x22FFFFFF : 0xAAFFCC44);
        }

        // Pass indicator
        int need = (int)(totalBeats * PASS_PERCENT);
        drawString(pose, this.font,
                "Need " + need + "+ hits to pass",
                this.width - 130, this.height - 25, 0x888888);
    }

    private static class BeatMarker {
        int x;
        boolean hit    = false;
        boolean missed = false;
        BeatMarker(int x) { this.x = x; }
    }
}

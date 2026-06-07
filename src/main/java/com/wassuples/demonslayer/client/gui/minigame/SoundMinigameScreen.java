package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class SoundMinigameScreen extends StyleMinigameScreen {

    private static final int[] PIGSTEP   = {0,12,18,25,37,43,50,62,75,81,88,100,106,113,125,137,143,150,162,168,175,187,200};
    private static final int[] OTHERSIDE = {0,11,22,33,38,44,55,66,77,88,93,99,110,121,132,137,143,154,165,176,187,198};
    private static final int[] DISC_5    = {0,17,34,42,51,68,85,94,102,119,136,144,153,170,187,196,204};

    private static final String[] DISC_NAMES = {"Pigstep","Otherside","Disc Five"};
    private static final int[][] PATTERNS    = {PIGSTEP, OTHERSIDE, DISC_5};

    private static final int   HIT_LINE_X    = 90;
    private static final int   HIT_WINDOW_PX = 14;
    private static final float SCROLL_SPEED  = 2.2f;
    private static final float PASS_RATIO    = 0.65f;

    private final int    maxMissStreak;
    private final String discName;
    private final int    patIdx;

    private final List<BeatMarker> beats = new ArrayList<>();
    private int     beatsHit   = 0;
    private int     totalBeats = 0;
    private int     missStreak = 0;
    private boolean done       = false;

    /** Stored so we can stop playback when the screen is removed. */
    private SoundInstance musicSound = null;

    public SoundMinigameScreen(UUID sessionId, int tier) {
        super(sessionId, tier, Component.literal("Sound Breathing - Rhythm"));
        this.maxMissStreak = Math.max(2, 5 - tier);
        this.patIdx        = Math.min(tier - 1, PATTERNS.length - 1);
        this.discName      = DISC_NAMES[patIdx];
        this.totalBeats    = PATTERNS[patIdx].length;
        // Beat positions built in init() once this.width is valid
    }

    @Override
    protected void init() {
        super.init();
        beats.clear();
        beatsHit = 0; missStreak = 0; done = false;

        int[] pattern = PATTERNS[patIdx];
        for (int offset : pattern) {
            // Start off the right edge so they scroll in naturally
            int startX = HIT_LINE_X + (int)((offset + 40) / SCROLL_SPEED) + this.width;
            beats.add(new BeatMarker(startX));
        }

        // Play the matching music disc
        if (minecraft != null) {
            net.minecraft.sounds.SoundEvent discEvent = switch (patIdx) {
                case 1  -> SoundEvents.MUSIC_DISC_OTHERSIDE;
                case 2  -> SoundEvents.MUSIC_DISC_5;
                default -> SoundEvents.MUSIC_DISC_PIGSTEP;
            };
            musicSound = SimpleSoundInstance.forUI(discEvent, 1.0f);
            minecraft.getSoundManager().play(musicSound);
        }
    }

    /** Stop the music disc whenever this screen is removed (finish or ESC). */
    @Override
    public void removed() {
        if (minecraft != null && musicSound != null) {
            minecraft.getSoundManager().stop(musicSound);
            musicSound = null;
        }
        super.removed();
    }

    @Override
    protected void tickGame() {
        if (done) return;
        for (BeatMarker b : beats) b.x -= (int) SCROLL_SPEED;

        for (BeatMarker b : beats) {
            if (!b.hit && !b.missed && b.x < HIT_LINE_X - HIT_WINDOW_PX - 8) {
                b.missed = true;
                missStreak++;
                if (missStreak >= maxMissStreak) {
                    done = true;
                    finishMinigame(false);
                    return;
                }
            }
        }

        if (beats.stream().allMatch(b -> b.hit || b.missed)) {
            done = true;
            finishMinigame((float) beatsHit / totalBeats >= PASS_RATIO);
        }
    }

    private void attemptHit() {
        if (done) return;
        for (BeatMarker b : beats) {
            if (!b.hit && !b.missed && Math.abs(b.x - HIT_LINE_X) <= HIT_WINDOW_PX) {
                b.hit = true;
                beatsHit++;
                missStreak = 0;
                if (minecraft != null) minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0f));
                return;
            }
        }
        // Phantom press - soft feedback, no penalty
        if (minecraft != null) minecraft.getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.0f, 0.6f));
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
        int cy = this.height / 2;
        drawTitle(pose, "Rhythm - " + discName, 0xFFAA44);
        drawInstructions(pose, "Press SPACE (or click) when a beat reaches the orange line", 0xCCCCCC);
        drawCenteredString(pose, this.font,
            "Hits: " + beatsHit + " / " + totalBeats +
            "   Miss streak: " + missStreak + "/" + maxMissStreak,
            this.width / 2, 35, 0xFFAA44);

        // Track lane background
        fill(pose, 0, cy - 14, this.width, cy + 14, 0x33444444);

        // Hit line
        fill(pose, HIT_LINE_X - 2, cy - 20, HIT_LINE_X + 2, cy + 20, 0xFFFF8800);
        // Hit window brackets
        fill(pose, HIT_LINE_X - HIT_WINDOW_PX, cy - 18, HIT_LINE_X + HIT_WINDOW_PX, cy - 16, 0x88FF8800);
        fill(pose, HIT_LINE_X - HIT_WINDOW_PX, cy + 16, HIT_LINE_X + HIT_WINDOW_PX, cy + 18, 0x88FF8800);

        for (BeatMarker b : beats) {
            if (b.x < -20 || b.x > this.width + 20) continue;
            int col = b.hit ? 0xFF55FF55 : b.missed ? 0xFF333333 : 0xFFFFAA44;
            fill(pose, b.x - 9, cy - 12, b.x + 9, cy + 12, col);
        }

        drawString(pose, this.font,
            "Need " + (int)(totalBeats * PASS_RATIO) + "+ hits",
            this.width - 100, this.height - 22, 0x888888);
    }

    private static class BeatMarker {
        int x;
        boolean hit, missed;
        BeatMarker(int x) { this.x = x; }
    }
}

package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

public class SerpentMinigameScreen extends StyleMinigameScreen {

    private static final int FULL_TARGET_COUNT = 9;

    private final int requiredRounds;
    /** Ticks targets are shown before they all go dark (must choose from memory). */
    private final int visibleTicks;

    private enum Phase { FLASH, CHOOSE, RESULT }
    private Phase phase = Phase.FLASH;

    private int flashTick  = 0;
    private int resultTick = 0;
    private int livesLeft;
    private int roundsDone = 0;
    private boolean lastCorrect = false;

    private final List<Decoy> targets = new ArrayList<>();
    private final Random rng = new Random();

    public SerpentMinigameScreen(UUID sessionId, int tier, int lives) {
        super(sessionId, tier, Component.literal("Serpent Breathing \u2013 Misdirection"));
        this.requiredRounds = Math.max(3, 2 + (tier - 1));
        this.visibleTicks   = Math.max(10, 20 - (tier - 1) * 2);
        this.livesLeft      = lives;
        // spawnRound() is deferred to init() so this.width/height are valid
    }

    @Override
    protected void init() {
        super.init();
        spawnRound();
    }

    private void spawnRound() {
        targets.clear();
        flashTick  = 0;
        resultTick = 0;
        phase      = Phase.FLASH;
        spawnFullRound();
    }

    /** 9 circles in a 3x3 grid, centred on screen with slight random jitter. */
    private void spawnFullRound() {
        int realIdx = rng.nextInt(FULL_TARGET_COUNT);
        int cols = 3, rows = 3, spacX = 72, spacY = 60;
        int startX = this.width  / 2 - (cols * spacX) / 2 + spacX / 2;
        int startY = this.height / 2 - (rows * spacY) / 2 + spacY / 2;

        for (int i = 0; i < FULL_TARGET_COUNT; i++) {
            int col = i % cols, row = i / cols;
            int x = startX + col * spacX + rng.nextInt(16) - 8;
            int y = startY + row * spacY + rng.nextInt(12) - 6;
            targets.add(new Decoy(x, y, i == realIdx));
        }
    }

    @Override
    protected void tickGame() {
        switch (phase) {
            case FLASH -> {
                flashTick++;
                if (flashTick >= visibleTicks) phase = Phase.CHOOSE;
            }
            case RESULT -> {
                resultTick++;
                if (resultTick >= 25) {
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
                        spawnRound(); // lost a life, try again
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != Phase.CHOOSE && phase != Phase.FLASH) return false;
        for (Decoy t : targets) {
            double dx = mx - t.x, dy = my - t.y;
            if (dx * dx + dy * dy <= 20 * 20) {
                lastCorrect = t.isReal;
                if (!t.isReal) {
                    livesLeft--;
                    if (minecraft != null) minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.8f));
                } else {
                    if (minecraft != null) minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.5f));
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
            phase == Phase.FLASH  ? "One has a subtle tell \u2014 find it fast!"
          : phase == Phase.CHOOSE ? "Choose!"
          : "",
            0xCCCCCC);

        drawCenteredString(pose, this.font,
            "Lives: " + livesLeft + "   Round: " + (roundsDone + 1) + "/" + requiredRounds,
            this.width / 2, 35, 0xCCCCCC);

        // Flash timer bar
        if (phase == Phase.FLASH) {
            float pct = 1f - (float) flashTick / visibleTicks;
            int bw = 120, bx = (this.width - bw) / 2;
            fill(pose, bx, this.height - 28, bx + bw, this.height - 20, 0x44000000);
            fill(pose, bx, this.height - 28, bx + (int)(pct * bw), this.height - 20, 0xFFCCCCCC);
        }

        boolean flashing = (phase == Phase.FLASH);
        boolean reveal   = (phase == Phase.RESULT);

        for (Decoy t : targets) {
            int fill, ring;
            if (reveal) {
                fill = t.isReal ? 0xFF2255AA : 0xFF2A2A2A;
                ring = t.isReal ? 0xFF4488FF : 0xFF444444;
            } else if (flashing) {
                fill = 0xFF383838;
                ring = t.isReal
                    ? 0xFF6B5540   // warm ochre — the barely-visible tell
                    : 0xFF505050;
            } else {
                fill = 0xFF2A2A2A;
                ring = 0xFF3A3A3A;
            }
            fillCircle(pose, t.x, t.y, 20, fill);
            drawCircleOutline(pose, t.x, t.y, 20, 2, ring);
        }

        if (phase == Phase.RESULT) {
            String msg = lastCorrect ? "Correct!" : "Wrong! " + livesLeft + " left";
            drawCenteredString(pose, this.font, msg, this.width / 2,
                this.height / 2 - 60, lastCorrect ? 0x55FF55 : 0xFF4444);
        }
    }

    private static class Decoy {
        int x, y;
        boolean isReal;
        Decoy(int x, int y, boolean real) {
            this.x = x; this.y = y; this.isReal = real;
        }
    }
}

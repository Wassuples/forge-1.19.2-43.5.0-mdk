package com.wassuples.demonslayer.client.gui.minigame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.StyleMinigameResultC2SPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Abstract base for every style-minigame {@link Screen}.
 *
 * <p>Subclasses implement the specific mechanic in {@link #renderGame(PoseStack, int, int, float)},
 * {@link #tickGame()}, and input-handler overrides ({@link #mouseClicked}, {@link #keyPressed}, etc.).
 *
 * <p>Call {@link #finishMinigame(boolean)} from the subclass when the game is over.
 * The base class sends the result packet and closes the screen.
 */
public abstract class StyleMinigameScreen extends Screen {

    protected final UUID sessionId;
    /** Difficulty tier; 1 = initial unlock, 2+ = training for mastery. */
    protected final int tier;

    /** Whether the result has already been sent (guards against double-sending). */
    private boolean resultSent = false;

    protected StyleMinigameScreen(UUID sessionId, int tier, Component title) {
        super(title);
        this.sessionId = sessionId;
        this.tier      = tier;
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override
    public final void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent background
        this.renderBackground(poseStack);
        renderGame(poseStack, mouseX, mouseY, partialTick);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    /**
     * Called whenever the screen is dismissed — by ESC, by the game closing,
     * or by {@link #finishMinigame}. Sends a fail result if the game was not
     * completed normally, ensuring the server session is always cleared.
     */
    @Override
    public void onClose() {
        // finishMinigame guards against double-sending via resultSent
        finishMinigame(false);
        // Do NOT call super.onClose() — finishMinigame calls setScreen(null) itself.
    }

    @Override
    public final void tick() {
        tickGame();
    }

    /** Prevents pausing the game while the minigame is open. */
    @Override
    public boolean isPauseScreen() { return false; }

    // ── Abstract API for subclasses ───────────────────────────────────────────

    /** Draw all game-specific visuals. Called every frame inside {@link #render}. */
    protected abstract void renderGame(PoseStack poseStack, int mouseX, int mouseY, float partialTick);

    /** Advance game state by one server tick (20 Hz). Called inside {@link #tick}. */
    protected abstract void tickGame();

    // ── Result sending ────────────────────────────────────────────────────────

    /**
     * Call from the subclass when the minigame ends (pass or fail).
     * Sends the result packet exactly once, then closes the screen.
     */
    protected final void finishMinigame(boolean passed) {
        if (resultSent) return;
        resultSent = true;
        ModMessages.sendToServer(new StyleMinigameResultC2SPacket(sessionId, passed));
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    // ── Shared rendering helpers ──────────────────────────────────────────────

    /** Fills a circle approximation using filled rectangles. */
    protected void fillCircle(PoseStack pose, int cx, int cy, int radius, int colour) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt((double)(radius * radius - y * y));
            fill(pose, cx - halfWidth, cy + y, cx + halfWidth + 1, cy + y + 1, colour);
        }
    }

    /** Draws a hollow ring (outline only). */
    protected void drawCircleOutline(PoseStack pose, int cx, int cy, int radius, int thickness, int colour) {
        for (int r = radius - thickness; r <= radius; r++) {
            drawCircleRing(pose, cx, cy, r, colour);
        }
    }

    private void drawCircleRing(PoseStack pose, int cx, int cy, int radius, int colour) {
        int steps = Math.max(64, radius * 4);
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            int px = cx + (int)(Math.cos(angle) * radius);
            int py = cy + (int)(Math.sin(angle) * radius);
            fill(pose, px, py, px + 1, py + 1, colour);
        }
    }

    /** Draws centred text in the style's colour with a black shadow. */
    protected void drawTitle(PoseStack pose, String text, int colour) {
        int x = this.width / 2;
        int y = 16;
        drawCenteredString(pose, this.font, text, x, y, colour);
    }

    /** Draws a "Tier N" and instructions line. */
    protected void drawInstructions(PoseStack pose, String instructions, int colour) {
        drawString(pose, this.font, "Tier " + tier + "  —  " + instructions, 8, 8, 0xAAAAAA);
    }
}

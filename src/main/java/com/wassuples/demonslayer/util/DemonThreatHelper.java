package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.demon.PlayerDemon;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility for computing intimidation effects when a weaker demon fights a stronger demon.
 */
public final class DemonThreatHelper {
    private DemonThreatHelper() {}

    /**
     * Computes a simple "demon strength score" for comparison.
     *
     * Currently based on (rank ordinal) and blood points. This matches the user's progression
     * model and is stable server-side.
     */
    public static float getDemonStrengthScore(PlayerDemon demonData) {
        // Rank: give each rank step a big weight so ranks matter.
        float rankScore = demonData.getRank().ordinal() * 1000.0f;
        // Blood points: these are already an accumulation of power.
        float bloodScore = demonData.getBloodPoints();
        return rankScore + bloodScore;
    }

    /**
     * Computes threat intensity [0..1] based on relative strength.
     *
     * If opponent is not stronger, intensity is 0.
     */
    public static float computeThreatIntensity(float selfStrength, float opponentStrength) {
        if (opponentStrength <= selfStrength) return 0.0f;

        // ratio of how much stronger the opponent is than self.
        float ratio = (opponentStrength / Math.max(1.0f, selfStrength));

        // Map ratio -> [0..1] smoothly. Example:
        // 1.10x stronger => small (0.1-ish)
        // 2.00x stronger => fairly strong
        // 3.00x stronger => near max
        float t = (ratio - 1.0f) / 2.0f; // 3x => 1.0
        return clamp01(t);
    }

    public static boolean isDemonPlayer(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return false;
        return player.getCapability(com.wassuples.demonslayer.demon.PlayerDemonProvider.PLAYER_DEMON)
                .map(PlayerDemon::isDemon)
                .orElse(false);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}


package com.wassuples.demonslayer.util;

import java.util.Random;

/**
 * Shared, deterministic kick arcs for Explosive Flurry. The server picks a seed; the player animation (client) and
 * the shockwave visual (AkazaArtRenderer) both rebuild the same arcs from it, so the legs really trace the arcs
 * the shockwaves are drawn along. Angles are PlayerAnimator leg angles in radians: pitch (negative = forward/up)
 * and yaw (positive = toward the body's right).
 */
public final class AkazaKicks {
    public static final int KICKS = 5;
    public static final int INTERVAL = 3;

    /** One straight kick swept along a restricted arc: from (startPitch, startYaw) to (endPitch, endYaw). */
    public record Kick(boolean rightLeg, float startPitch, float startYaw, float endPitch, float endYaw) {
        public float pitchAt(float t) {
            return startPitch + (endPitch - startPitch) * t;
        }

        public float yawAt(float t) {
            return startYaw + (endYaw - startYaw) * t;
        }
    }

    private AkazaKicks() {
    }

    /** Kick {@code index} (1..KICKS) for {@code seed}. Legs alternate; heights and sweeps vary within limits. */
    public static Kick kick(int seed, int index) {
        Random rnd = new Random(seed * 1103515245L + index * 12345L);
        boolean right = index % 2 == 1;
        float height = -1.25f - rnd.nextFloat() * 0.75f;            // knee level to head height
        float sweep = 0.25f + rnd.nextFloat() * 0.35f;              // how far the foot travels sideways
        float centre = (rnd.nextFloat() - 0.5f) * 0.5f;             // where across the body it lands
        float dir = rnd.nextBoolean() ? 1.0f : -1.0f;
        float rise = (rnd.nextFloat() - 0.5f) * 0.5f;               // arc climbs or dips a little
        return new Kick(right, height - rise * 0.5f, centre - dir * sweep * 0.5f, height + rise * 0.5f, centre + dir * sweep * 0.5f);
    }

    /** Tick (from the form's start) at which kick {@code index} lands. */
    public static int strikeTick(int index) {
        return index * INTERVAL;
    }
}

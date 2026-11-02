package com.wassuples.demonslayer.client.toon;

import com.wassuples.demonslayer.util.FormAnimations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Single source of truth for the "fall flat on your back" motion used by Banana Peel (TOON_SLIP) and
 * Fake Tunnel (TOON_TUNNEL_SLAM). The PlayerAnimator keyframes, the first-person camera and the dizzy
 * birds' head position all sample these same keys, so the camera moves exactly with the body.
 *
 * <p>Values are PlayerAnimator "body" transforms: pitch in radians around a pivot 0.7 blocks above the
 * feet (positive = tipping backwards) and Y offset in blocks.
 */
@OnlyIn(Dist.CLIENT)
public final class ToonFallCurve {
    /** Pivot height PlayerAnimator rotates the body around. */
    public static final float PIVOT = 0.7f;
    /**
     * Sign of "backwards" for PlayerAnimator's body pitch. If players ever fall on their face instead of
     * their back in your PlayerAnimator build, flip this to -1 and everything (camera, birds) follows.
     */
    public static final float BACK = 1.0f;
    private static final float FLAT = (float) (Math.PI / 2.0);

    //                           tick, pitch,          bodyY
    public static final float[][] SLIP = {
            {0, 0.0f, 0.0f},
            {3, 0.45f, 0.25f},   // feet fly out, body airborne
            {9, FLAT, -0.58f},   // slam onto the back
            {12, 1.42f, -0.48f}, // little bounce
            {15, FLAT, -0.58f},
            {34, FLAT, -0.58f},  // lie there seeing stars
            {39, 0.75f, -0.30f}, // sit up
            {44, 0.0f, 0.0f}};

    public static final float[][] TUNNEL_SLAM = {
            {0, 0.0f, 0.0f},
            {2, -0.14f, 0.0f},   // face-first into the "tunnel"
            {4, 0.35f, 0.10f},   // rebound
            {10, FLAT, -0.58f},
            {13, 1.42f, -0.48f},
            {16, FLAT, -0.58f},
            {35, FLAT, -0.58f},
            {40, 0.75f, -0.30f},
            {44, 0.0f, 0.0f}};

    private ToonFallCurve() {
    }

    public static float[][] keysFor(String animationId) {
        if (FormAnimations.TOON_SLIP.equals(animationId)) return SLIP;
        if (FormAnimations.TOON_TUNNEL_SLAM.equals(animationId)) return TUNNEL_SLAM;
        return null;
    }

    public static int length(float[][] keys) {
        return (int) keys[keys.length - 1][0];
    }

    /** Body pitch at time t (ticks), already multiplied by {@link #BACK}. */
    public static float pitch(float[][] keys, float t) {
        return sample(keys, t, 1) * BACK;
    }

    public static float bodyY(float[][] keys, float t) {
        return sample(keys, t, 2);
    }

    /** How "down" the body is, 0 = standing, 1 = flat. */
    public static float flatness(float[][] keys, float t) {
        return Math.max(0.0f, Math.min(1.0f, sample(keys, t, 1) / FLAT));
    }

    /** Height above the feet of a point that sits {@code height} blocks up the standing body. */
    public static float pointHeight(float[][] keys, float t, float height) {
        float a = sample(keys, t, 1);
        return PIVOT + bodyY(keys, t) + (height - PIVOT) * (float) Math.cos(a);
    }

    /** How far behind the feet (along the body's back direction) that point has swung. */
    public static float pointBackOffset(float[][] keys, float t, float height) {
        float a = sample(keys, t, 1);
        return (height - PIVOT) * (float) Math.sin(a);
    }

    private static float sample(float[][] keys, float t, int column) {
        if (t <= keys[0][0]) return keys[0][column];
        for (int i = 0; i < keys.length - 1; i++) {
            float t0 = keys[i][0];
            float t1 = keys[i + 1][0];
            if (t <= t1) {
                float u = (t - t0) / Math.max(0.0001f, t1 - t0);
                float eased = 0.5f - 0.5f * (float) Math.cos(Math.PI * u); // == PlayerAnimator Ease.INOUTSINE
                return keys[i][column] + (keys[i + 1][column] - keys[i][column]) * eased;
            }
        }
        return keys[keys.length - 1][column];
    }
}

package com.wassuples.demonslayer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/**
 * Client-side controller for demon intimidation effects.
 *
 * Server sends a target intensity (0..1). We smooth it for rendering and audio.
 */
public final class DemonThreatEffects {
    private DemonThreatEffects() {}

    private static float targetIntensity = 0.0f;
    private static float intensity = 0.0f;

    private static int heartbeatCooldownTicks = 0;
    private static int rumbleCooldownTicks = 0;

    public static void setThreatIntensity(float newTarget) {
        targetIntensity = clamp01(newTarget);
    }

    public static float getIntensity(float partialTick) {
        // simple linear interpolation for rendering; intensity is updated on tick.
        return clamp01(intensity);
    }

    public static float getVignetteAlpha() {
        // subtle base, ramps up.
        return 0.05f + 0.35f * intensity;
    }

    public static float getStaticAlpha() {
        // slightly lighter than vignette; make it feel like noise.
        return 0.02f + 0.25f * intensity;
    }

    public static boolean isActive() {
        return intensity > 0.001f;
    }

    public static void clientTick() {
        // smooth towards target; quicker ramp-up, slower decay.
        float speed = targetIntensity > intensity ? 0.14f : 0.06f;
        intensity += (targetIntensity - intensity) * speed;
        intensity = clamp01(intensity);

        if (heartbeatCooldownTicks > 0) heartbeatCooldownTicks--;
        if (rumbleCooldownTicks > 0) rumbleCooldownTicks--;

        if (intensity <= 0.01f) {
            // keep target at 0 if server stopped sending; intensity will decay.
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Heartbeat: use a UI click-ish sound as placeholder. Interval scales down with intensity.
        // If you add custom sounds later, swap SoundEvents here.
        int heartbeatInterval = (int) lerp(26, 10, intensity); // ticks between beats
        if (heartbeatCooldownTicks <= 0) {
            float volume = 0.15f + 0.55f * intensity;
            float pitch = 0.6f + 0.5f * intensity;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, volume, pitch));
            heartbeatCooldownTicks = heartbeatInterval;
        }

        // Low-frequency rumble: very low-volume ambient cave as placeholder.
        int rumbleInterval = (int) lerp(40, 18, intensity);
        if (rumbleCooldownTicks <= 0) {
            float volume = 0.05f + 0.25f * intensity;
            float pitch = 0.5f + 0.2f * intensity;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMBIENT_CAVE, volume, pitch));
            rumbleCooldownTicks = rumbleInterval;
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}


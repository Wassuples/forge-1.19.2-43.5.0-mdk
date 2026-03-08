package com.wassuples.demonslayer.client.effects;

import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side storage for short-lived Heat Lightning crack overlays.
 */
public final class HeatLightningClientEffects {
    private HeatLightningClientEffects() {}

    // entityId -> expireGameTime
    private static final Map<Integer, Long> CRACKED_UNTIL = new ConcurrentHashMap<>();

    public static void markCracked(int entityId, int durationTicks) {
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        CRACKED_UNTIL.put(entityId, now + Math.max(0, durationTicks));
    }

    public static boolean shouldRenderCracks(int entityId) {
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        Long until = CRACKED_UNTIL.get(entityId);
        if (until == null) return false;
        if (until <= now) {
            CRACKED_UNTIL.remove(entityId);
            return false;
        }
        return true;
    }
}


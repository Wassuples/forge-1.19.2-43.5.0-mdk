package com.wassuples.demonslayer.client;

import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Client copy of the local player's form cooldowns (absolute client game time of expiry), fed by
 * CooldownSyncS2CPacket. Used by the Demon HUD and the form wheel.
 */
public final class ClientCooldowns {
    private static final Map<String, Long> EXPIRY = new HashMap<>();
    private static final Map<String, Integer> TOTAL = new HashMap<>();

    private ClientCooldowns() {
    }

    public static void accept(CompoundTag remaining) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Map<String, Long> next = new HashMap<>();
        for (String key : remaining.getAllKeys()) {
            int ticks = remaining.getInt(key);
            if (ticks <= 0) continue;
            next.put(key, now + ticks);
            // Remember the full length when a cooldown starts (or restarts longer), for progress bars.
            Long old = EXPIRY.get(key);
            if (old == null || old <= now || ticks > (old - now) + 2) TOTAL.put(key, ticks);
        }
        EXPIRY.clear();
        EXPIRY.putAll(next);
        TOTAL.keySet().retainAll(next.keySet());
    }

    /** Remaining cooldown ticks for {@code form} (0 = ready). */
    public static int remaining(IForm form) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || form == null) return 0;
        Long expiry = EXPIRY.get(PlayerCooldowns.debugFormId(form));
        if (expiry == null) return 0;
        long left = expiry - mc.level.getGameTime();
        return left <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, left);
    }

    /** 0..1 how much of {@code form}'s cooldown is still left (0 = ready). */
    public static float fraction(IForm form) {
        int left = remaining(form);
        if (left <= 0) return 0.0f;
        int total = TOTAL.getOrDefault(PlayerCooldowns.debugFormId(form), left);
        return Math.min(1.0f, left / (float) Math.max(1, total));
    }

    public static void clear() {
        EXPIRY.clear();
        TOTAL.clear();
    }
}

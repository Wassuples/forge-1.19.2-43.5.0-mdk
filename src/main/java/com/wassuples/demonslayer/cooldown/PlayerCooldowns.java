package com.wassuples.demonslayer.cooldown;

import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

public class PlayerCooldowns {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final static String NBT_KEY = "player_cooldowns";

    public void setOnCooldown(IForm form, int durationTicks) {
        if (durationTicks <= 0) return;
        long expirationTime = System.currentTimeMillis() + (durationTicks * 50L);
        cooldowns.put(form.getDisplayName(), expirationTime);
    }

    public boolean isOnCooldown(IForm form) {
        return getCooldownRemaining(form) > 0;
    }

    public long getCooldownRemaining(IForm form) {
        long expirationTime = cooldowns.getOrDefault(form.getDisplayName(), 0L);
        long currentTime = System.currentTimeMillis();
        return Math.max(0, expirationTime - currentTime);
    }

    public int getCooldownRemainingTicks(IForm form) {
        return (int) (getCooldownRemaining(form) / 50L);
    }

    public void saveNBTData(CompoundTag nbt) {
        CompoundTag cooldownsTag = new CompoundTag();
        cooldowns.forEach((name, expiry) -> {
            if (expiry > System.currentTimeMillis()) {
                cooldownsTag.putLong(name, expiry);
            }
        });
        nbt.put(NBT_KEY, cooldownsTag);
    }

    public void loadNBTData(CompoundTag nbt) {
        cooldowns.clear();
        if (nbt.contains(NBT_KEY)) {
            CompoundTag cooldownsTag = nbt.getCompound(NBT_KEY);
            for (String key : cooldownsTag.getAllKeys()) {
                cooldowns.put(key, cooldownsTag.getLong(key));
            }
        }
    }
}
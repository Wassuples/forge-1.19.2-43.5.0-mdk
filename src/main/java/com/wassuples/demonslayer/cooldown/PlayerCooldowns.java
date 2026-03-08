package com.wassuples.demonslayer.cooldown;

import com.wassuples.demonslayer.gamerule.ModGameRules;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores per-player cooldown state for forms.
 *
 * <p>Implementation details:
 *
 * <ul>
 *   <li>Cooldowns are stored as an absolute expiry value in {@link Level#getGameTime()} ticks.</li>
 *   <li>Keys are derived from the concrete enum type + constant name (see {@link #debugFormId(IForm)}).</li>
 *   <li>Cooldown persistence is saved as <b>remaining ticks</b> rather than absolute game time.</li>
 * </ul>
 *
 * <p>Why remaining ticks? It stays stable across save/load where game time may change.
 */
public class PlayerCooldowns {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private static final String NBT_KEY = "player_cooldowns";

    /**
     * Returns a stable identifier for a form.
     *
     * <p>This is used as the key for persisted cooldown entries.
     */
    private static String getFormId(IForm form) {
        return form.getClass().getName() + "#" + form;
    }

    /**
     * Debug helper so log statements can reference the internal cooldown key.
     *
     * @param form the form instance/enum constant
     * @return the internal cooldown key
     */
    public static String debugFormId(IForm form) {
        return getFormId(form);
    }

    /**
     * Scales a base cooldown by the {@code /gamerule formsCooldownMultiplier} value.
     *
     * Gamerule semantics:
     * - 0   = no cooldown
     * - 100 = default
     * - 200 = double
     */
    public static int applyFormsCooldownMultiplier(Level level, int baseCooldownTicks) {
        if (baseCooldownTicks <= 0) return 0;

        int percent = level.getGameRules().getInt(ModGameRules.FORMS_COOLDOWN_MULTIPLIER_PERCENT);
        if (percent <= 0) return 0;
        if (percent == 100) return baseCooldownTicks;

        // Use long math to avoid overflow then clamp.
        long scaled = Math.round(baseCooldownTicks * (percent / 100.0));
        if (scaled <= 0) return 0;
        return (scaled > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) scaled;
    }

    /**
     * Puts the given form on cooldown.
     *
     * <p>Contract:
     *
     * <ul>
     *   <li>{@code durationTicks <= 0} does nothing.</li>
     *   <li>Cooldown is measured in server ticks using {@link Level#getGameTime()}.</li>
     * </ul>
     */
    public void setOnCooldown(Level level, IForm form, int durationTicks) {
        if (durationTicks <= 0) return;
        long now = level.getGameTime();
        long expiryGameTime = now + durationTicks;
        cooldowns.put(getFormId(form), expiryGameTime);
    }

    /**
     * Ensures there's at least a 1-tick cooldown. This helps prevent spam when an action ends in the same tick
     * or when multiple packets arrive before the state updates.
     */
    public void setOnCooldownAtLeastOneTick(Level level, IForm form, int durationTicks) {
        setOnCooldown(level, form, Math.max(1, durationTicks));
    }

    public boolean isOnCooldown(Level level, IForm form) {
        return getCooldownRemainingTicks(level, form) > 0;
    }

    public int getCooldownRemainingTicks(Level level, IForm form) {
        long expiry = cooldowns.getOrDefault(getFormId(form), 0L);
        long remaining = expiry - level.getGameTime();
        if (remaining <= 0) return 0;
        return (remaining > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) remaining;
    }

    /**
     * Saves all active cooldowns to NBT as remaining ticks.
     *
     * <p>Expired cooldowns are not saved.
     */
    public void saveNBTData(Level level, CompoundTag nbt) {
        CompoundTag cooldownsTag = new CompoundTag();
        long now = level.getGameTime();
        cooldowns.forEach((id, expiry) -> {
            long remaining = expiry - now;
            if (remaining > 0) {
                cooldownsTag.putInt(id, (int) Math.min(Integer.MAX_VALUE, remaining));
            }
        });
        nbt.put(NBT_KEY, cooldownsTag);
    }

    /**
     * Loads cooldowns from NBT.
     *
     * <p>The NBT stores remaining ticks, and this method converts it back to an absolute expiry based
     * on the current {@link Level#getGameTime()}.
     */
    public void loadNBTData(Level level, CompoundTag nbt) {
        cooldowns.clear();
        if (!nbt.contains(NBT_KEY)) return;

        CompoundTag cooldownsTag = nbt.getCompound(NBT_KEY);
        long now = level.getGameTime();
        for (String key : cooldownsTag.getAllKeys()) {
            int remaining = cooldownsTag.getInt(key);
            if (remaining > 0) {
                cooldowns.put(key, now + remaining);
            }
        }
    }

    public void debugLogStatus(Level level, IForm form, String source) {
        // no-op (debug logging disabled)
    }
}

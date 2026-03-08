package com.wassuples.demonslayer.effects;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores server-side caster attribution for Heat Lightning DoT.
 *
 * This keeps the implementation small without introducing a new Capability.
 */
public final class HeatLightningEffectData {
    private HeatLightningEffectData() {}

    private static final Map<UUID, UUID> CASTER_BY_TARGET = new ConcurrentHashMap<>();

    public static void setCaster(LivingEntity target, LivingEntity caster) {
        if (target == null) return;
        if (caster == null) {
            CASTER_BY_TARGET.remove(target.getUUID());
            return;
        }
        CASTER_BY_TARGET.put(target.getUUID(), caster.getUUID());
    }

    public static UUID getCasterUUID(LivingEntity target) {
        if (target == null) return null;
        return CASTER_BY_TARGET.get(target.getUUID());
    }

    public static void clear(LivingEntity target) {
        if (target == null) return;
        CASTER_BY_TARGET.remove(target.getUUID());
    }
}


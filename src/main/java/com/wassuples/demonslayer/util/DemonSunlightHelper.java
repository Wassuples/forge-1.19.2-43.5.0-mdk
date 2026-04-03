package com.wassuples.demonslayer.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared sunlight weakness behavior for demon entities.
 */
public final class DemonSunlightHelper {
    private DemonSunlightHelper() {}

    /**
     * Applies daylight burning and rain-proof sunlight damage when exposed to open sky.
     */
    public static void applySunlightWeakness(LivingEntity entity) {
        if (!entity.isAlive() || entity.level.isClientSide()) {
            return;
        }

        if (!entity.level.isDay() || !entity.level.canSeeSky(entity.blockPosition())) {
            return;
        }

        entity.setSecondsOnFire(8);

        // Rain can extinguish flames, so keep sunlight lethal with periodic direct damage.
        if (entity.level.isRainingAt(entity.blockPosition()) && entity.tickCount % 20 == 0) {
            entity.hurt(DamageSource.ON_FIRE, 1.0F);
        }
    }
}


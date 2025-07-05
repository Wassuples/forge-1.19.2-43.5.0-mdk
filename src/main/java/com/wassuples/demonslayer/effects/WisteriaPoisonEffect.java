package com.wassuples.demonslayer.effects;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class WisteriaPoisonEffect extends MobEffect {

    public WisteriaPoisonEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    // This method is called every time the effect "ticks" on a target.
    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        // This mimics the vanilla poison effect: it can't kill, but leaves the target at 1 HP (half a heart).
        if (pLivingEntity.getHealth() > 1.0F) {
            pLivingEntity.hurt(DamageSource.MAGIC, 1.0F);
        }
    }

    // This determines how often the effect should tick.
    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        // This is the standard timing for vanilla poison.
        int i = 25 >> pAmplifier;
        if (i > 0) {
            return pDuration % i == 0;
        } else {
            return true;
        }
    }
}
package com.wassuples.demonslayer.effects;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class WisteriaPoisonEffect extends MobEffect {

    public WisteriaPoisonEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        boolean isSusceptible = false;

        if (pLivingEntity instanceof Player) {
            isSusceptible = pLivingEntity.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(demon -> demon.isDemon())
                    .orElse(false);
        }
        else if (pLivingEntity instanceof DemonEntity) {
            isSusceptible = true;
        }

        if (isSusceptible) {
            float damage = 1.0F + (pAmplifier * 0.5F);

            pLivingEntity.hurt(DamageSource.MAGIC, damage);

            if (pLivingEntity.level instanceof ServerLevel serverLevel) {
                final DustParticleOptions purplePoison = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
                serverLevel.sendParticles(purplePoison, pLivingEntity.getX(), pLivingEntity.getY() + 1.0, pLivingEntity.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
            }
            pLivingEntity.level.playSound(null, pLivingEntity.blockPosition(), SoundEvents.GENERIC_BURN, SoundSource.HOSTILE, 0.5f, 1.5f);

        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        int i = 25 >> pAmplifier;
        if (i > 0) {
            return pDuration % i == 0;
        } else {
            return true;
        }
    }
}
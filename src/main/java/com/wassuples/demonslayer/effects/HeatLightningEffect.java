package com.wassuples.demonslayer.effects;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.HeatLightningCracksS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Thunder Breathing V: Heat Lightning
 * Long DoT that represents the target's flesh cracking/burning from electrical heat.
 */
public class HeatLightningEffect extends MobEffect {
    public HeatLightningEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        // Damage scaling: amplifier 0 is already painful; higher amplifiers ramp slightly.
        float damage = 2.0f + (amplifier * 0.75f);

        DamageSource src = DamageSource.LIGHTNING_BOLT;
        UUID casterId = HeatLightningEffectData.getCasterUUID(target);
        if (casterId != null && target.level instanceof ServerLevel sl) {
            Entity caster = sl.getEntity(casterId);
            if (caster instanceof LivingEntity livingCaster) {
                // Attribute to attacker if available.
                src = com.wassuples.demonslayer.util.CombatHelper.getDamageSource(livingCaster);
            }
        }

        target.hurt(src, damage);

        // Visual is a model overlay (non-particle). Re-send occasionally so late-joiners/tracking clients see it.
        if (!target.level.isClientSide && target.level.getGameTime() % 10 == 0) {
            ModMessages.sendToClients(new HeatLightningCracksS2CPacket(target.getId(), 12)); // refresh small window
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 1s tick baseline, faster with higher amplifier.
        int interval = Math.max(5, 20 - (amplifier * 3));
        return duration % interval == 0;
    }
}


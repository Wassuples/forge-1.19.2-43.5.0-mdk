package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class CrazyCuttingForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double AOE_RADIUS = 4.5;
    private static final float DAMAGE_PER_TICK = 3.0f;
    private static final float KNOCKBACK = 0.4f;

    public CrazyCuttingForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    @Override
    public void onTick() {
        super.onTick();

        user.setDeltaMovement(user.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        user.hurtMarked = true;

        if (ticksActive % 3 == 0) {
            dealAOEDamage(user, user.position(), AOE_RADIUS, DAMAGE_PER_TICK, KNOCKBACK);
        }

        if (ticksActive % 2 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);

            float randomYaw = level.random.nextFloat() * 360;
            float randomPitch = -90 + level.random.nextFloat() * 180;

            spawnParticleArcInWorld(level, user.getEyePosition(), AOE_RADIUS * 0.8, 140, 15, randomYaw, randomPitch, ParticleTypes.CRIT);
        }
    }
}

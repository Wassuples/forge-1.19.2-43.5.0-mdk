package com.wassuples.demonslayer.forms.slayer.beast;

import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Beast Breathing form: Extending Bendy Slash.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ExtendingBendySlashForm extends ChargeableForm {
    private static final int WIND_UP_DURATION = 10;
    private static final double ATTACK_RANGE = 9.0;
    private static final float DAMAGE = 28.0f;

    public ExtendingBendySlashForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    protected int getReleaseDuration() {
        return 0;
    }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    protected void onChargingTick() {
        level.sendParticles(ParticleTypes.CRIT, user.getRandomX(0.8), user.getRandomY(), user.getRandomZ(0.8), 5, 0.1, 0.1, 0.1, 0.1);
    }

    @Override
    protected void onChargeRelease(boolean fullyCharged) {
        if (fullyCharged) {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
            dealDirectionalDamage(user, ATTACK_RANGE, DAMAGE, 0.85);
            spawnParticleArc(user, ParticleTypes.CRIT, ATTACK_RANGE, 90, 40);
        }
    }

    @Override
    protected void onReleaseTick() {
    }

    @Override
    protected void onChargeEnd() {
    }
}

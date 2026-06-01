package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealThrustDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

/**
 * Water Breathing form: Water Thrust.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WaterThrustForm extends ChargeableForm {
    public WaterThrustForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override protected int getReleaseDuration() {
        return 0;
    }
    @Override protected void onChargeRelease(float chargeRatio) {
        if (chargeRatio >= 1.0f) {
            performGenericAttack(user, SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
            dealThrustDamage(user, 8.0, 20.0f);
            spawnLineParticles(level, ParticleTypes.SPLASH, user.getEyePosition(), user.getEyePosition().add(user.getLookAngle().scale(8.0)), 30);
        }
    }

    @Override protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS, 0.5f, 1.5f);
    }
    @Override protected void onChargingTick() {
        Vec3 swordTipPos = user.getEyePosition().add(user.getLookAngle().scale(1.2));
        level.sendParticles(ParticleTypes.BUBBLE, swordTipPos.x, swordTipPos.y, swordTipPos.z, 1, 0, 0, 0, 0);
    }
    @Override protected void onReleaseTick() {}
    @Override protected void onChargeEnd() {
        if (!this.released) {
            level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
            level.sendParticles(ParticleTypes.POOF, user.getX(), user.getY() + 1, user.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
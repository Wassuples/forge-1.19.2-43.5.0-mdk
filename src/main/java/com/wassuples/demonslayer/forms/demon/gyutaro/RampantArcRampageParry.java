package com.wassuples.demonslayer.forms.demon.gyutaro;

import com.wassuples.demonslayer.entity.custom.BloodArcDomeEntity;
import com.wassuples.demonslayer.util.FormAnimations;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Gyutaro Blood Demon Art form: Rampant Arc Rampage Parry.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.ParryForm
 */
public class RampantArcRampageParry extends ParryForm {
    private static final int PARRY_WINDOW = 25;
    private static final float COUNTER_DAMAGE = 20.0f;
    private static final double COUNTER_RADIUS = 7.0;
    private static final float COUNTER_KNOCKBACK = 1.8f;
    private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 1.0f);

    private float rotation = 0;
    private BloodArcDomeEntity dome;

    public RampantArcRampageParry(LivingEntity user, IForm form) {
        super(user, form, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.7f);
        FormAnimations.play(user, FormAnimations.GYUTARO_GUARD);
        // Guard animation and the whirling sickles both last exactly the parry window.
        this.dome = new BloodArcDomeEntity(level, user, PARRY_WINDOW, (float) COUNTER_RADIUS);
        level.addFreshEntity(this.dome);
    }

    @Override
    public void onTick() {
        super.onTick();

        // Visuals are drawn by BloodArcDomeEntity (whirling blood sickles); only a whoosh here.
        if (ticksActive % 5 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.6f + level.random.nextFloat() * 0.3f);
        }
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.5f);

        dealAOEDamage(user, user.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK);

        if (this.dome != null && this.dome.isAlive()) {
            this.dome.burst(); // the whirling sickles fly outward
        }
        // Replace the guard (which would keep sweeping after the form ends) with a short outward fling.
        FormAnimations.play(user, FormAnimations.GYUTARO_BURST);
        level.sendParticles(BLOOD_PARTICLE, user.getX(), user.getEyeY(), user.getZ(), 30, 1.2, 0.8, 1.2, 0.1);
    }
}

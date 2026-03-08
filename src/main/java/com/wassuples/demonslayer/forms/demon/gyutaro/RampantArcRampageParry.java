package com.wassuples.demonslayer.forms.demon.gyutaro;

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

    public RampantArcRampageParry(LivingEntity user, IForm form) {
        super(user, form, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    public void onTick() {
        super.onTick();

        this.rotation += 40.0f;
        double radius = 2.0;

        spawnParticleArcInWorld(level, user.getEyePosition(), radius, 360, 10, this.rotation, 0, BLOOD_PARTICLE);
        spawnParticleArcInWorld(level, user.getEyePosition(), radius, 360, 10, -this.rotation, 90, ParticleTypes.SWEEP_ATTACK);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.5f);

        dealAOEDamage(user, user.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK);

        for (int i = 0; i < 200; i++) {
            Vec3 randomDir = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).normalize();
            Vec3 particlePos = user.getEyePosition().add(randomDir.scale(level.random.nextDouble() * COUNTER_RADIUS));
            level.sendParticles(i % 3 == 0 ? ParticleTypes.SWEEP_ATTACK : BLOOD_PARTICLE,
                    particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
        }
    }
}

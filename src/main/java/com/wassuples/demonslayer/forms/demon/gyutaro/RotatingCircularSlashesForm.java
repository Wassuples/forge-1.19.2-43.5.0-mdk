package com.wassuples.demonslayer.forms.demon.gyutaro;

import com.wassuples.demonslayer.entity.custom.BloodRingWaveEntity;
import com.wassuples.demonslayer.util.FormAnimations;


import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gyutaro Blood Demon Art form: Rotating Circular Slashes.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class RotatingCircularSlashesForm extends TimedForm {
    private static final int WAVE_DURATION = 30; // The wave expands over 1.5 seconds
    private static final double MAX_RANGE = 15.0; // The wave travels 15 blocks
    private static final double WAVE_HEIGHT = 4.0;  // The wave is 4 blocks tall
    private static final float DAMAGE = 15.0f;
    private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 1.0f);

    private float rotation = 0;
    private final List<UUID> hitEntities = new ArrayList<>();

    public RotatingCircularSlashesForm(LivingEntity user, IForm form) {
        super(user, form, WAVE_DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        FormAnimations.play(user, FormAnimations.GYUTARO_SPIN);
        level.addFreshEntity(new BloodRingWaveEntity(level, user, WAVE_DURATION, (float) MAX_RANGE, (float) WAVE_HEIGHT));
    }

    @Override
    public void onTick() {
        super.onTick();

        float progress = (float) ticksActive / WAVE_DURATION;
        float prevProgress = (float) (ticksActive - 1) / WAVE_DURATION;
        double currentRadius = MAX_RANGE * progress;
        double previousRadius = MAX_RANGE * prevProgress;

        AABB damageZone = user.getBoundingBox().inflate(currentRadius, WAVE_HEIGHT / 2.0, currentRadius);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                e -> e != user && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            double dist = target.distanceTo(user);
            if (dist <= currentRadius && dist > previousRadius) {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
                hitEntities.add(target.getUUID());

                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.push(knockbackDir.x * 0.8, 0.2, knockbackDir.z * 0.8);
            }
        }

        // Visuals: BloodRingWaveEntity draws the expanding ring of sickles on the same radius timeline.
        if (ticksActive % 4 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.2f + progress * 0.6f);
        }
    }
}

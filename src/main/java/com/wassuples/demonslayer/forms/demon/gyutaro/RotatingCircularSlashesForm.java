package com.wassuples.demonslayer.forms.demon.gyutaro;


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

        this.rotation += 30.0f; // Rotation speed
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10 + this.rotation);
            for (double y = 0; y <= WAVE_HEIGHT; y += 0.5) {
                Vec3 particlePos = user.position().add(
                        Math.cos(angle) * currentRadius,
                        y,
                        Math.sin(angle) * currentRadius
                );
                level.sendParticles(level.random.nextFloat() > 0.3f ? BLOOD_PARTICLE : ParticleTypes.SWEEP_ATTACK,
                        particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}

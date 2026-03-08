package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

/**
 * Akaza Blood Demon Art form: Disorder.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class DisorderForm extends TimedForm {
    private static final double CONE_RANGE = 15.0;
    private static final double CONE_ANGLE_DEGREES = 45.0;
    private static final int PULSE_RATE = 4;
    private static final float KNOCKBACK_STRENGTH = 1.2F;
    private static final double CONE_ANGLE_COS = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES));
    private final Set<UUID> knockedBackEntities = new HashSet<>();

    public DisorderForm(LivingEntity user, IForm form) {
        super(user, form, 100);
    } // 5s

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % PULSE_RATE == 0) {
            float damagePerPulse = 6.0f + (ticksActive / 40f);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);

            AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
            Vec3 userEyePos = user.getEyePosition();
            Vec3 userLookDir = user.getLookAngle();

            for (LivingEntity target : potentialTargets) {
                Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
                double dotProduct = vectorToTarget.dot(userLookDir);

                if (dotProduct > CONE_ANGLE_COS) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), damagePerPulse);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), damagePerPulse);
                    }

                    if (!knockedBackEntities.contains(target.getUUID())) {
                        Vec3 knockbackVec = userLookDir.normalize().scale(KNOCKBACK_STRENGTH);
                        target.setDeltaMovement(target.getDeltaMovement().add(knockbackVec.x, 0.4, knockbackVec.z));
                        target.hurtMarked = true;

                        knockedBackEntities.add(target.getUUID());
                    }
                }
            }
            int particlesPerPulse = 5 + (ticksActive / 30);
            for (int i = 0; i < particlesPerPulse; i++) {
                Vec3 randomPosInCone = getRandomPointInCone(user, CONE_RANGE, CONE_ANGLE_DEGREES);
                level.sendParticles(ParticleTypes.SONIC_BOOM, randomPosInCone.x, randomPosInCone.y, randomPosInCone.z, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_AGITATED, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private Vec3 getRandomPointInCone(LivingEntity entity, double maxRange, double angleDegrees) {
        Vec3 lookDir = entity.getLookAngle();
        Vec3 startPos = entity.getEyePosition();
        float randomYawOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float) angleDegrees;
        float randomPitchOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float) angleDegrees;
        Vec3 randomDir = lookDir.yRot((float) Math.toRadians(randomYawOffset)).xRot((float) Math.toRadians(randomPitchOffset));
        double randomDist = level.random.nextDouble() * maxRange;
        return startPos.add(randomDir.scale(randomDist));
    }
}

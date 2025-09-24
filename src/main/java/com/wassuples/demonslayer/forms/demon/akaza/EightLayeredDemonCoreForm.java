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

import java.util.List;

public class EightLayeredDemonCoreForm extends TimedForm {
    private static final int NUMBER_OF_PUNCHES = 8;
    private static final int TICKS_BETWEEN_PUNCHES = 5;

    private static final float BASE_DAMAGE = 8.0f;
    private static final float DAMAGE_RAMP_PER_PUNCH = 5.0f;

    private static final float FINAL_PUNCH_KNOCKBACK = 2.0f;

    private static final double CONE_RANGE = 12.0;
    private static final double CONE_ANGLE_DEGREES = 40.0;
    private static final double CONE_ANGLE_COS = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES));

    public EightLayeredDemonCoreForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_PUNCHES * TICKS_BETWEEN_PUNCHES);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_PUNCHES == 0) {
            int punchNumber = ticksActive / TICKS_BETWEEN_PUNCHES; // (1 through 8)
            float currentDamage = BASE_DAMAGE + ((punchNumber - 1) * DAMAGE_RAMP_PER_PUNCH);

            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.8f + (punchNumber * 0.05f), 1.2f + (punchNumber * 0.1f));
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    user.getX() + user.getLookAngle().x * 3,
                    user.getEyeY() + user.getLookAngle().y * 3,
                    user.getZ() + user.getLookAngle().z * 3,
                    1, 0, 0, 0, 0);

            AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
            Vec3 userEyePos = user.getEyePosition();
            Vec3 userLookDir = user.getLookAngle();

            for (LivingEntity target : potentialTargets) {
                Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
                double dotProduct = vectorToTarget.dot(userLookDir);

                if (dotProduct > CONE_ANGLE_COS) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), currentDamage);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), currentDamage);
                    }
                }
            }
        }
    }

    @Override
    public void onStop() {
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.0f);

        AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
        Vec3 userEyePos = user.getEyePosition();
        Vec3 userLookDir = user.getLookAngle();

        for (LivingEntity target : potentialTargets) {
            Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
            double dotProduct = vectorToTarget.dot(userLookDir);

            if (dotProduct > CONE_ANGLE_COS) {
                Vec3 horizontalPush = new Vec3(userLookDir.x, 0, userLookDir.z).normalize().scale(FINAL_PUNCH_KNOCKBACK);
                target.setDeltaMovement(target.getDeltaMovement().add(horizontalPush.add(0, 0.5, 0)));
                target.hurtMarked = true;
            }
        }
    }
}

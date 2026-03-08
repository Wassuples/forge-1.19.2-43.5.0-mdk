package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Doma Blood Demon Art form: Frozen Lotus.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FrozenLotusForm extends TimedForm {
    // --- Constants ---
    private static final int DURATION = 20; // Form lasts 1s to let visuals linger
    private static final double ATTACK_RANGE = 7.0;
    private static final double ATTACK_ANGLE_DEGREES = 60.0; // A wide 120-degree cone
    private static final double ATTACK_ANGLE_COS = Math.cos(Math.toRadians(ATTACK_ANGLE_DEGREES));

    private static final float DAMAGE = 18.0f;
    private static final int CHILL_DURATION = 80; // 4 seconds of slowness
    private static final int CHILL_AMPLIFIER = 2; // Slowness III

    public FrozenLotusForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);

        AABB broadPhaseBox = user.getBoundingBox().inflate(ATTACK_RANGE);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
        Vec3 userEyePos = user.getEyePosition();
        Vec3 userLookDir = user.getLookAngle();

        for (LivingEntity target : potentialTargets) {
            Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos);
            if (vectorToTarget.lengthSqr() > ATTACK_RANGE * ATTACK_RANGE) continue;

            double dotProduct = vectorToTarget.normalize().dot(userLookDir);
            if (dotProduct > ATTACK_ANGLE_COS) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_DURATION, CHILL_AMPLIFIER));
            }
        }

        spawnParticleArc(user, ParticleTypes.SNOWFLAKE, ATTACK_RANGE, 120, 50);
        spawnParticleArc(user, ParticleTypes.SWEEP_ATTACK, ATTACK_RANGE, 120, 5);

        for (int i = 0; i < 4; i++) {
            Vec3 randomPosInCone = getRandomPointInCone(user, ATTACK_RANGE * 0.8, ATTACK_ANGLE_DEGREES);
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(randomPosInCone)).above();
            spawnIceLotusVFX(level, Vec3.atCenterOf(groundPos));
        }
    }

    private void spawnIceLotusVFX(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 6; i++) {
            double angle = (2 * Math.PI / 6) * i;
            for (double r = 0.5; r <= 1.5; r += 0.2) {
                Vec3 petalPos = center.add(new Vec3(Math.cos(angle) * r, 0.2 + (r * 0.2), Math.sin(angle) * r));
                level.sendParticles(ParticleTypes.END_ROD, petalPos.x, petalPos.y, petalPos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private Vec3 getRandomPointInCone(LivingEntity entity, double maxRange, double angleDegrees) {
        Vec3 lookDir = entity.getLookAngle();
        Vec3 startPos = entity.getEyePosition();
        float randomYawOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float) angleDegrees;
        Vec3 randomDir = lookDir.yRot((float) Math.toRadians(randomYawOffset));
        double randomDist = 1.0 + (level.random.nextDouble() * (maxRange - 1.0));
        return startPos.add(randomDir.scale(randomDist));
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}

package com.wassuples.demonslayer.forms.demon.doma;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.*;

import java.util.List;

public class LotusVinesForm extends TimedForm {
    private static final int VINE_TRAVEL_DURATION = 15; // Vines take 0.75s to reach target
    private static final int GARDEN_LINGER_DURATION = 100; // The hazard lasts for 5 seconds
    private static final double MAX_RANGE = 20.0;
    private static final double GARDEN_RADIUS = 4.0;

    private static final float DAMAGE_PER_TICK = 3.0f; // Damage for standing in the garden
    private static final int SLOWNESS_AMPLIFIER = 3; // Slowness IV

    private final Vec3 targetPoint;
    private final Vec3 startPoint;
    private boolean hasBloomed = false;

    public LotusVinesForm(LivingEntity user, IForm form) {
        super(user, form, VINE_TRAVEL_DURATION + GARDEN_LINGER_DURATION);
        this.startPoint = user.position();

        Vec3 finalTarget = null;

        Vec3 eyePos = user.getEyePosition();
        Vec3 lookVec = user.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(MAX_RANGE));
        AABB searchBox = user.getBoundingBox().expandTowards(lookVec.scale(MAX_RANGE)).inflate(1.0D);

        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(level, user, eyePos, endPos, searchBox, e -> e instanceof LivingEntity && !e.is(user));

        if (entityHitResult != null) {
            finalTarget = entityHitResult.getEntity().position();
        } else {
            BlockHitResult blockHitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
            if (blockHitResult.getType() == HitResult.Type.MISS) {
                finalTarget = eyePos.add(lookVec.scale(MAX_RANGE));
            } else {
                finalTarget = Vec3.atCenterOf(blockHitResult.getBlockPos());
            }
        }
        this.targetPoint = finalTarget;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.FROGSPAWN_HATCH, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive <= VINE_TRAVEL_DURATION) {
            double progress = (double) ticksActive / VINE_TRAVEL_DURATION;

            for (int i = 0; i < 3; i++) {
                Vec3 vinePath = startPoint.lerp(targetPoint, progress);

                double weave_amplitude = 1.2;
                double weave_frequency = 2.0;
                double weave_offset = Math.sin(progress * Math.PI * weave_frequency + (i * 2)) * weave_amplitude;
                Vec3 sideDir = targetPoint.subtract(startPoint).cross(new Vec3(0, 1, 0)).normalize();
                Vec3 particlePos = vinePath.add(sideDir.scale(weave_offset));

                level.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y + 0.2, particlePos.z, 1, 0, 0, 0, 0);
            }

        } else {
            if (!hasBloomed) {
                hasBloomed = true;
                level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.5f, 1.8f);
                for (int i = 0; i < 5; i++) {
                    Vec3 lotusPos = targetPoint.add(new Vec3(
                            (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2),
                            0.2,
                            (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2)
                    ));
                    spawnIceLotusVFX(level, lotusPos);
                }
            }

            AABB gardenBox = new AABB(targetPoint, targetPoint).inflate(GARDEN_RADIUS, 2.0, GARDEN_RADIUS);
            List<LivingEntity> targetsInGarden = level.getEntitiesOfClass(LivingEntity.class, gardenBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targetsInGarden) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, SLOWNESS_AMPLIFIER));
                if (ticksActive % 10 == 0) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
                    }
                }
            }

            if (ticksActive % 2 == 0) {
                Vec3 mistPos = targetPoint.add(new Vec3(
                        (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2),
                        0.5 + level.random.nextDouble(),
                        (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2)
                ));
                level.sendParticles(ParticleTypes.WHITE_ASH, mistPos.x, mistPos.y, mistPos.z, 1, 0, 0, 0, 0);
            }
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
}

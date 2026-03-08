package com.wassuples.demonslayer.forms.demon.doma;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Doma Blood Demon Art form: Wintry Icicles.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class WintryIciclesForm extends TimedForm {
    private static final int TELEGRAPH_DURATION = 15;
    private static final double MAX_RANGE = 25.0;
    private static final double IMPACT_RADIUS = 2.5;
    private static final float DAMAGE = 35.0f;
    private final Vec3 targetPoint;

    public WintryIciclesForm(LivingEntity user, IForm form) {
        super(user, form, TELEGRAPH_DURATION + 10);

        BlockPos targetBlockPos;
        BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);

        if (hitResult.getType() == HitResult.Type.MISS) {
            Vec3 endPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
            targetBlockPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(endPoint));
        } else {
            targetBlockPos = hitResult.getBlockPos();
        }

        // Adjust Y to be on top of the block's collision shape so particles are visible
        VoxelShape shape = level.getBlockState(targetBlockPos).getCollisionShape(level, targetBlockPos, CollisionContext.of(user));
        double yOffset = shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y);

        this.targetPoint = Vec3.atBottomCenterOf(targetBlockPos).add(0, yOffset, 0);
    }

    @Override
    public void onStart() {
        if (!level.isClientSide) {
            level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0f, 2.0f);
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (!level.isClientSide) {
            if (ticksActive <= TELEGRAPH_DURATION) {
                double angle = (ticksActive * 0.4);
                double radius = IMPACT_RADIUS * 0.8;
                Vec3 particlePos = targetPoint.add(new Vec3(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius));
                level.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 2, 0, 0, 0, 0);
            }

            if (ticksActive == TELEGRAPH_DURATION) {
                level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);
                level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.2f, 1.0f);

                int icicleCount = 7;
                for (int i = 0; i < icicleCount; i++) {
                    Vec3 icicleBasePos = targetPoint.add(new Vec3(
                            (level.random.nextDouble() - 0.5) * (IMPACT_RADIUS),
                            0.1,
                            (level.random.nextDouble() - 0.5) * (IMPACT_RADIUS)
                    ));
                    spawnIcicleVFX(level, icicleBasePos);
                }

                AABB eruptionBox = new AABB(targetPoint, targetPoint).inflate(IMPACT_RADIUS, 2.0, IMPACT_RADIUS);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, eruptionBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    DamageSource source = (user instanceof Player p) ? DamageSource.playerAttack(p) : DamageSource.mobAttack(user);
                    target.hurt(source, DAMAGE);
                }
            }
        }
    }

    private void spawnIcicleVFX(ServerLevel level, Vec3 basePos) {
        final DustParticleOptions iceParticle = new DustParticleOptions(new Vector3f(0.7f, 0.9f, 1.0f), 1.0f);
        for (int i = 0; i < 20; i++) {
            double height = i * 0.25;
            double motionY = 0.8;
            level.sendParticles(iceParticle, basePos.x, basePos.y + height, basePos.z, 1, 0, motionY, 0, 0);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.CRIT, basePos.x, basePos.y + height, basePos.z, 1, 0, motionY, 0, 0);
            }
        }
    }
}

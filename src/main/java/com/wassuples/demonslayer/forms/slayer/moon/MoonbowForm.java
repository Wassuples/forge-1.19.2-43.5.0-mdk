package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;
import static com.wassuples.demonslayer.util.WorldHelper.destroyBlocksInRadius;

/**
 * Moon Breathing form: Moonbow.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class MoonbowForm extends TimedForm {
    private static final int TELEGRAPH_DURATION = 30;
    private static final int DESCENT_DURATION = 20;
    private static final double MAX_RANGE = 40.0;
    private static final double BOMBARDMENT_RADIUS = 10.0;
    private static final double IMPACT_RADIUS = 3.5;
    private static final float DAMAGE = 45.0f;
    private static final float KNOCKBACK = 2.5f;

    private static final DustParticleOptions PURPLE_SLASH = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 2.0f);

    private final List<Vec3> impactPoints = new ArrayList<>();
    private boolean impactsTriggered = false;

    public MoonbowForm(LivingEntity user, IForm form) {
        super(user, form, TELEGRAPH_DURATION + DESCENT_DURATION);
        BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
        Vec3 centerPoint;
        if (hitResult.getType() == HitResult.Type.MISS) {
            Vec3 endPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
            centerPoint = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(endPoint)));
        } else {
            centerPoint = Vec3.atCenterOf(hitResult.getBlockPos());
        }

        for (int i = 0; i < 6; i++) {
            double angle = (2 * Math.PI / 6) * i;
            double distance = BOMBARDMENT_RADIUS * (0.5 + level.random.nextDouble() * 0.5);
            Vec3 offset = new Vec3(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            impactPoints.add(Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(centerPoint.add(offset)))));
        }
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();
        int currentTick = this.ticksActive;

        if (currentTick <= TELEGRAPH_DURATION + DESCENT_DURATION) {
            for (Vec3 impactPoint : this.impactPoints) {
                if (currentTick <= TELEGRAPH_DURATION) {
                    spawnParticleArcInWorld(level, impactPoint.add(0, 0.2, 0), IMPACT_RADIUS, 360, 10, 0, 90, ParticleTypes.SOUL_FIRE_FLAME);
                }

                if (currentTick > TELEGRAPH_DURATION) {
                    double descentProgress = (double) (currentTick - TELEGRAPH_DURATION) / DESCENT_DURATION;
                    Vec3 skyPos = impactPoint.add(0, 25, 0);

                    Vec3 particlePos = skyPos.lerp(impactPoint, descentProgress);
                    level.sendParticles(PURPLE_SLASH, particlePos.x, particlePos.y, particlePos.z, 3, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }

        if (currentTick == TELEGRAPH_DURATION + DESCENT_DURATION && !this.impactsTriggered) {
            this.impactsTriggered = true;

            for (Vec3 impactPoint : this.impactPoints) {
                level.playSound(null, impactPoint.x, impactPoint.y, impactPoint.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.8f);

                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPoint.x, impactPoint.y, impactPoint.z, 1, 0, 0, 0, 0);
                dealAOEDamage(user, impactPoint, IMPACT_RADIUS, DAMAGE, KNOCKBACK, false, true);
                destroyBlocksInRadius(level, new BlockPos(impactPoint), IMPACT_RADIUS, 4, false, user);
            }
        }
    }
}

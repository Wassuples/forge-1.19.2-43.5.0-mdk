package com.wassuples.demonslayer.forms.demon.akaza;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Akaza Blood Demon Art form: Ten Thousand Leaves Willow.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class TenThousandLeavesWillowForm extends TimedForm {
    private static final int DURATION = 20;
    private static final double MAX_RADIUS = 12.0;
    private static final float DAMAGE = 25.0f;
    private static final float KNOCKBACK = 1.5f;

    private final Set<UUID> hitEntities = new HashSet<>();

    public TenThousandLeavesWillowForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        applyImmobility(user, DURATION + 5);
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        double progress = (double) ticksActive / DURATION;
        double previousProgress = (double) (ticksActive - 1) / DURATION;

        double currentRadius = MAX_RADIUS * progress;
        double previousRadius = MAX_RADIUS * previousProgress;

        AABB damageZone = user.getBoundingBox().inflate(currentRadius);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            double distanceToUser = target.distanceTo(user);

            if (distanceToUser <= currentRadius && distanceToUser > previousRadius) {
                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player).setExplosion(), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user).setExplosion(), DAMAGE);
                }

                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.knockback(KNOCKBACK, knockbackDir.x, knockbackDir.z);

                hitEntities.add(target.getUUID());
            }
        }

        BlockState groundState = level.getBlockState(user.blockPosition().below());
        if (groundState.isAir()) {
            groundState = Blocks.STONE.defaultBlockState();
        }
        spawnParticleArcInWorld(level, user.position(), currentRadius, 360, 40, 0, 0, new BlockParticleOption(ParticleTypes.BLOCK, groundState));
        if (ticksActive % 4 == 0) {
            spawnParticleArcInWorld(level, user.position(), currentRadius, 360, 5, 0, 0, ParticleTypes.SWEEP_ATTACK);
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}

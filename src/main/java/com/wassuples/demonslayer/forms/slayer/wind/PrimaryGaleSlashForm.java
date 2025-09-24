package com.wassuples.demonslayer.forms.slayer.wind;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.VFXHelper.spawnCycloneParticles;

public class PrimaryGaleSlashForm extends ActiveForm {
    private Vec3 pos, vel;
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 0.8f);

    public PrimaryGaleSlashForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.8f);
        user.setDeltaMovement(user.getLookAngle().scale(-1.8).add(0, 0.6, 0));
        user.hurtMarked = true;
        this.pos = user.getEyePosition();
        this.vel = user.getLookAngle().scale(3.0);
    }

    @Override
    public void onTick() {
        // --- Projectile Simulation ---
        if (ticksActive > 40) {
            ActiveFormManager.stop(user);
            return;
        }

        Vec3 nextPos = pos.add(vel);
        vel = vel.add(0, -0.05, 0);
        AABB checkZone = new AABB(pos, nextPos).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, checkZone, e -> e != user && e.isAlive());

        if (!targets.isEmpty() || !level.getBlockState(new BlockPos(nextPos)).isAir()) {
            LivingEntity hitTarget = targets.isEmpty() ? null : targets.get(0);
            if (hitTarget != null) {
                if (user instanceof ServerPlayer player) {
                    hitTarget.hurt(DamageSource.playerAttack(player), 25.0f);
                } else {
                    hitTarget.hurt(DamageSource.mobAttack(user), 25.0f);
                }
            }
            level.playSound(null, new BlockPos(nextPos), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.sendParticles(ParticleTypes.EXPLOSION, nextPos.x, nextPos.y, nextPos.z, 1, 0, 0, 0, 0);

            ActiveFormManager.stop(user);
        } else {
            pos = nextPos;
            spawnCycloneParticles(level, GREEN_DUST, ParticleTypes.CLOUD, pos, 1.2, 0, 25, ticksActive * 10);
        }
    }
}

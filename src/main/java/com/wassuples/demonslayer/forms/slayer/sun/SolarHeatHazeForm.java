package com.wassuples.demonslayer.forms.slayer.sun;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Sun Breathing form: Solar Heat Haze.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SolarHeatHazeForm extends TimedForm {
    private static final int DURATION = 15;
    private static final double DASH_SPEED = 1.8;
    private static final float DAMAGE = 38.0f;

    private static final ParticleOptions HEAT_HAZE = ParticleTypes.WHITE_ASH;

    private final Set<UUID> hitEntities = new HashSet<>();

    public SolarHeatHazeForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 2.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        Vec3 direction = user.getLookAngle();
        user.setDeltaMovement(direction.x * DASH_SPEED, 0.1, direction.z * DASH_SPEED);
        user.hurtMarked = true;

        spawnParticleArc(user, ParticleTypes.FLAME, 4.0, 90, 20);
        level.sendParticles(HEAT_HAZE, user.getX(), user.getEyeY(), user.getZ(), 2, 0.5, 0.5, 0.5, 0.01);

        AABB impactBox = user.getBoundingBox().inflate(1.5);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, impactBox,
                e -> e != user && e.isAlive() && !this.hitEntities.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user), DAMAGE);
            }
            this.hitEntities.add(target.getUUID());

            level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 1.5f);
            level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.3, 0.3, 0);
        }
    }
}

package com.wassuples.demonslayer.forms.slayer.sound;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

public class RoarForm extends TimedForm {
    private static final int SLAM_DELAY = 10;
    private static final float SLAM_DAMAGE = 15.0f;
    private static final double SLAM_RADIUS = 15.0;
    private static final float SLAM_KNOCKBACK = 1.8f;

    public RoarForm(LivingEntity user, IForm form) {
        super(user, form, SLAM_DELAY + 5);
    }

    @Override
    public void onStart() {
        applyImmobility(user, this.duration);
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive < SLAM_DELAY) {
            for (int i = 0; i < 3; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double x = user.getX() + Math.cos(angle) * 1.2;
                double z = user.getZ() + Math.sin(angle) * 1.2;
                level.sendParticles(ParticleTypes.NOTE, x, user.getY() + 1.0, z, 1, 0, 0, 0, 0);
            }
        }
        if (ticksActive == SLAM_DELAY) {
            // Damage and Sound logic (now uses the new 10-block radius)
            dealAOEDamage(user, user.position(), SLAM_RADIUS, SLAM_DAMAGE, SLAM_KNOCKBACK);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.2f);

            // Particle logic
            level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getY() + 0.5, user.getZ(), 1, 0, 0, 0, 0);

            BlockState groundState = level.getBlockState(user.blockPosition().below());
            if (!groundState.isAir()) {
                for (int i = 0; i < 50; i++) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double radius = 0.5 + level.random.nextDouble() * (SLAM_RADIUS - 1.0);
                    double x = user.getX() + Math.cos(angle) * radius;
                    double z = user.getZ() + Math.sin(angle) * radius;
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), x, user.getY() + 0.2, z, 1, 0.2, 0.1, 0.2, 0.1);
                }
            }
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}

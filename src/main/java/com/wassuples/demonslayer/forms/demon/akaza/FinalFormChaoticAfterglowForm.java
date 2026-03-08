package com.wassuples.demonslayer.forms.demon.akaza;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

/**
 * Akaza Blood Demon Art form: Final Chaotic Afterglow.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FinalFormChaoticAfterglowForm extends TimedForm {
    private static final int DURATION = 30;
    private static final double ATTACK_RADIUS = 16.0;
    private static final float DAMAGE = 240.0f;

    private static final DustParticleOptions BLUE_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.8f, 1.0f), 1.0f);
    private static final DustParticleOptions SILVER_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.95f), 1.0f);

    public FinalFormChaoticAfterglowForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        applyImmobility(user, DURATION);

        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.5f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive < DURATION - 5) {
            double progress = (double) ticksActive / (DURATION - 5);
            double radius = 5.0 * (1.0 - progress);
            for (int i = 0; i < 20; i++) {
                double angle = (level.random.nextDouble() * 2 * Math.PI);
                Vec3 offset = new Vec3(Math.cos(angle), (level.random.nextDouble() - 0.5) * 4.0, Math.sin(angle)).scale(radius);
                level.sendParticles(level.random.nextBoolean() ? BLUE_PARTICLE : SILVER_PARTICLE, user.getX() + offset.x, user.getY() + 1.0, user.getZ() + offset.z, 1, 0, 0, 0, 0);
            }
        }

        if (ticksActive == DURATION - 5) {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);

            AABB effectZone = user.getBoundingBox().inflate(ATTACK_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());
            DamageSource unblockableSource = DamageSource.mobAttack(user).setIsFall().setMagic();

            for (LivingEntity target : targets) {
                target.hurt(unblockableSource, DAMAGE);

                for (int i = 0; i < 50; i++) {
                    Vec3 randomStart = target.position().add(new Vec3(
                            (level.random.nextDouble() - 0.5) * 6.0,
                            (level.random.nextDouble() - 0.5) * 6.0,
                            (level.random.nextDouble() - 0.5) * 6.0
                    ));
                    spawnLineParticles(level, level.random.nextBoolean() ? BLUE_PARTICLE : ParticleTypes.CRIT, randomStart, target.getEyePosition(), 3);
                }
            }

            level.sendParticles(ParticleTypes.FLASH, user.getX(), user.getEyeY(), user.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getEyeY(), user.getZ(), 5, 0, 0, 0, 0);
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}

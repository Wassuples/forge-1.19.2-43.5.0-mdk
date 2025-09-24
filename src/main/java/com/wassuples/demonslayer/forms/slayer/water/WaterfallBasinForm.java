package com.wassuples.demonslayer.forms.slayer.water;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

public class WaterfallBasinForm extends ActiveForm {
    public WaterfallBasinForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onTick() {
        if (user.isOnGround() || user.isInWater()) {
            float fallDistance = user.fallDistance;
            float damage = 5.0f + Mth.clamp(fallDistance, 0, 50) * 0.8f;
            double radius = Mth.clamp(2.0 + fallDistance * 0.15, 2.0, 10.0);
            float knockback = 0.5f + fallDistance * 0.05f;

            dealAOEDamage(user, user.position(), radius, damage, knockback);

            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.8f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, user.getX(), user.getY(), user.getZ(), 1, 0, 0, 0, 0);

            final DustParticleOptions waterMist = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 1.0f), 1.2f);
            int particleCount = (int) (40 * radius); // More particles for a bigger splash

            for (int i = 0; i < particleCount; i++) {
                // Splash particles that fly outwards from the impact point
                double motionX = (level.random.nextDouble() - 0.5) * radius * 0.4;
                double motionY = level.random.nextDouble() * 0.6;
                double motionZ = (level.random.nextDouble() - 0.5) * radius * 0.4;
                level.sendParticles(ParticleTypes.SPLASH, user.getX(), user.getY() + 0.2, user.getZ(), 0, motionX, motionY, motionZ, 0.7);

                // Blue mist particles that rise up slowly
                if (i % 4 == 0) { // Spawn fewer mist particles
                    double mistX = user.getX() + (level.random.nextDouble() - 0.5) * radius;
                    double mistY = user.getY() + level.random.nextDouble() * 1.5;
                    double mistZ = user.getZ() + (level.random.nextDouble() - 0.5) * radius;
                    level.sendParticles(waterMist, mistX, mistY, mistZ, 0, 0, 0.1, 0, 1);
                }
            }

            ActiveFormManager.stop(user);
        }
    }
}
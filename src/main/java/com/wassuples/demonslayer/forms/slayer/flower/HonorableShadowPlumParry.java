package com.wassuples.demonslayer.forms.slayer.flower;

import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamageInYaw;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Flower Breathing form: Honorable Shadow Plum Parry.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.ParryForm
 */
public class HonorableShadowPlumParry extends ParryForm {
    private static final int PARRY_WINDOW = 20; // 1-second parry window
    private static final float COUNTER_DAMAGE = 15.0f;
    private static final double COUNTER_REACH = 5.0;

    public HonorableShadowPlumParry(LivingEntity user, IForm form) {
        super(user, form, PARRY_WINDOW);
    }

    @Override
    public void onStart() {
        super.onStart();
        level.playSound(null, user.blockPosition(), SoundEvents.SPORE_BLOSSOM_FALL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();
        if (level.random.nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    user.getX() + (level.random.nextDouble() - 0.5) * 3.0,
                    user.getY() + 2.5,
                    user.getZ() + (level.random.nextDouble() - 0.5) * 3.0,
                    1, 0, -0.08, 0, 1);
        }
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.8f);
        level.sendParticles(ParticleTypes.HEART, user.getX(), user.getEyeY(), user.getZ(), 10, 0.5, 0.5, 0.5, 0.1);

        for (int i = 0; i < 4; i++) {
            float yaw = user.getYRot() + (i * 90);
            dealDirectionalDamageInYaw(user, COUNTER_REACH, COUNTER_DAMAGE, 0.4f, yaw);
            spawnParticleArcInWorld(level, user.getEyePosition(), COUNTER_REACH, 140, 20, yaw, 0, ParticleTypes.FALLING_SPORE_BLOSSOM);
        }
    }
}

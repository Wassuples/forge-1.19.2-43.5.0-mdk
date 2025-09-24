package com.wassuples.demonslayer.forms.slayer.water;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;

public class ConstantFluxForm extends ActiveForm {
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.7f, 1.0f), 1.2f);

    public ConstantFluxForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        if (!user.isSprinting()) {
            ActiveFormManager.stop(user);
            return;
        }
        int speedAmp = Math.min(ticksActive / 40, 4);
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, speedAmp, false, false, false));

        int damageInterval = Math.max(20 - (ticksActive / 10), 5);
        if (ticksActive % damageInterval == 0) {
            float damage = Math.min(6.0f + (ticksActive / 25f), 15.0f);
            dealDirectionalDamage(user, 3.0, damage, 0.7);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
        // Particle Dragon
        int particleCount = Math.min(5 + (ticksActive / 10), 30);
        for (int i = 0; i < particleCount; i++) {
            double angle = (user.tickCount + i * 18) * 0.1;
            double radius = 1.0 + Mth.clamp(ticksActive / 100.0, 0, 2.0);
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            double yOffset = Math.sin((user.tickCount + i * 45) * 0.05) * 0.5 + 1.0;
            level.sendParticles(BLUE_DUST, user.getX() + xOffset, user.getY() + yOffset, user.getZ() + zOffset, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.8f, 0.8f);
    }
}

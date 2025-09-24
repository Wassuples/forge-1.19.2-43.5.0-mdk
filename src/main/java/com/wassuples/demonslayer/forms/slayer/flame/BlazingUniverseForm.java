package com.wassuples.demonslayer.forms.slayer.flame;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;

public class BlazingUniverseForm extends ActiveForm {
    public BlazingUniverseForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    @Override
    public void onTick() {
        user.setDeltaMovement(user.getDeltaMovement().add(0, -0.2, 0)); // a bit more gravity
        if (user.isOnGround()) {
            dealAOEDamage(user, user.position(), 4.0, 10.0f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.sendParticles(ParticleTypes.LAVA, user.getX(), user.getY() + 0.5, user.getZ(), 50, 2.0, 0.5, 2.0, 0);
            level.sendParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 0.5, user.getZ(), 100, 2.0, 0.5, 2.0, 0.1);
            ActiveFormManager.stop(user);
        }
    }
}
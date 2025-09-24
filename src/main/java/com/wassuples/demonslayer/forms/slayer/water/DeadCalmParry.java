package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

public  class DeadCalmParry extends ParryForm {
    public DeadCalmParry(LivingEntity user, IForm form) {
        super(user, form, 30);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        applyImmobility(user, 10);
        level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CRIT, user.getX(), user.getEyeY(), user.getZ(), 15, 0.3, 0.3, 0.3, 0.5);
        if (attacker instanceof LivingEntity living) {
            living.knockback(1.0, user.getX() - living.getX(), user.getZ() - living.getZ());
        }
    }

    @Override
    public void onStop() {
        removeImmobility(user);
    }
}

package com.wassuples.demonslayer.forms.slayer.wind;

import com.wassuples.demonslayer.forms.baseForms.ParryForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.WorldHelper.destroyBlocksInRadius;

/**
 * Wind Breathing form: Clear Storm Wind Tree Parry.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.ParryForm
 */
public class ClearStormWindTreeParry extends ParryForm {
    public ClearStormWindTreeParry(LivingEntity user, IForm form) {
        super(user, form, 30);
    }

    @Override
    protected void onParrySuccess(Entity attacker) {
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.5f);
        destroyBlocksInRadius(level, user.blockPosition(), 4.0, 1, false, user);
        if (attacker instanceof LivingEntity living) {
            living.knockback(2.5F, user.getX() - living.getX(), user.getZ() - living.getZ());
        }
        for (int i = 0; i < 100; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * 3.0;
            double x = user.getX() + Math.cos(angle) * radius;
            double z = user.getZ() + Math.sin(angle) * radius;
            double y = user.getY() + Math.random() * 2.5;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}

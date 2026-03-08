package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Thunder Breathing form: Rice Spirit.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class RiceSpiritForm extends TimedForm {
    private static final int NUMBER_OF_SLASHES = 5;
    private static final int TICKS_BETWEEN_SLASHES = 2; // 0.1s
    private static final float DAMAGE_PER_SLASH = 12.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    public RiceSpiritForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
    }

    @Override
    public void onTick() {
        super.onTick();

        // a slash every 2 ticks.
        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
            // Sound
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2f, 2.0f);

            // Animation
            user.swing(InteractionHand.MAIN_HAND);

            // Damage
            dealDirectionalDamage(user, 4.0, DAMAGE_PER_SLASH, 0.6);

            // Visuals
            spawnParticleArc(user, YELLOW_LIGHTNING, 2.5, 140, 25);
            spawnParticleArc(user, ParticleTypes.ELECTRIC_SPARK, 2.5, 140, 5);
        }
    }
}

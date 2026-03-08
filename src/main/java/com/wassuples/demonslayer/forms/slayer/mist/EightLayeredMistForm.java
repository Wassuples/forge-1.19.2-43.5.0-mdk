package com.wassuples.demonslayer.forms.slayer.mist;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Mist Breathing form: Eight Layered Mist.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class EightLayeredMistForm extends TimedForm {
    private static final int NUMBER_OF_SLASHES = 8;
    private static final int TICKS_BETWEEN_SLASHES = 3; // A very fast slash every 0.15 seconds
    private static final float DAMAGE_PER_SLASH = 4.0f; // Total damage = 32
    private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

    public EightLayeredMistForm(LivingEntity user, IForm form) {
        // Total duration is 8 slashes * 3 ticks each = 24 ticks (~1.2 seconds)
        super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
    }

    @Override
    public void onTick() {
        super.onTick();

        // Use the modulo operator to trigger a slash every 3 ticks.
        // We check ticksActive > 0 to prevent a slash on the very first tick.
        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
            // --- Perform a single slash ---

            // Play a quick swoosh sound, pitched up to sound faster.
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 2.0f);

            // Deal damage in a cone in front of the player.
            dealDirectionalDamage(user, 3.5, DAMAGE_PER_SLASH, 0.6);

            // Spawn an arc of mist particles. This will build up with each slash.
            spawnParticleArc(user, ParticleTypes.CLOUD, 2.0, 140, 15);
            spawnParticleArc(user, DENSE_MIST, 2.0, 140, 10);
        }
    }
}

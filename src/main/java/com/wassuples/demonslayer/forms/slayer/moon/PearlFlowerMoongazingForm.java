package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Moon Breathing form: Pearl Flower Moongazing.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class PearlFlowerMoongazingForm extends TimedForm {
    // --- CONSTANTS FOR TIMING ---
    private static final int X_SLASH_TICK = 3;
    private static final int H_SLASH_TICK = 7;
    private static final float SLASH_DAMAGE = 12.0f;

    private static final DustParticleOptions PEARL_WHITE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 1.0f), 1.0f);
    private static final ParticleOptions SILVER_SPARKLE = ParticleTypes.END_ROD;

    public PearlFlowerMoongazingForm(LivingEntity user, IForm form) {
        super(user, form, H_SLASH_TICK + 5);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 2.0f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (this.ticksActive == X_SLASH_TICK) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.9f);

            dealDirectionalDamage(user, 5.0, SLASH_DAMAGE, 0.6);
            spawnParticleArc(user, PEARL_WHITE, 3.5, 120, 30, 45.0f);
            dealDirectionalDamage(user, 5.0, SLASH_DAMAGE, 0.6);
            spawnParticleArc(user, PEARL_WHITE, 3.5, 120, 30, -45.0f);
        }

        if (this.ticksActive == H_SLASH_TICK) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9f, 1.8f);

            dealDirectionalDamage(user, 5.0, SLASH_DAMAGE, 0.6);
            spawnParticleArc(user, SILVER_SPARKLE, 3.8, 160, 40, 90.0f);
        }
    }
}

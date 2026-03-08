package com.wassuples.demonslayer.forms.slayer.love;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Love Breathing form: Shivers Of First Love.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ShiversOfFirstLoveForm extends TimedForm {
    // --- Constants ---
    private static final int NUMBER_OF_SLASHES = 6;
    private static final int TICKS_BETWEEN_SLASHES = 4; // every 0.2 seconds
    private static final float DAMAGE_PER_SLASH = 4.5f;
    private static final double FORWARD_SPEED = 0.6;
    private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
    private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

    public ShiversOfFirstLoveForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
    }

    @Override
    public void onTick() {
        super.onTick();

        Vec3 lookVec = user.getLookAngle();
        user.setDeltaMovement(lookVec.x * FORWARD_SPEED, user.getDeltaMovement().y, lookVec.z * FORWARD_SPEED);
        user.hurtMarked = true;

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
            user.swing(InteractionHand.MAIN_HAND);
            dealDirectionalDamage(user, 4.5, DAMAGE_PER_SLASH, 0.6);

            spawnParticleArc(user, PINK_SPARK, 3.0, 140, 15);
            spawnParticleArc(user, GREEN_SPARK, 3.0, 140, 15);
            level.sendParticles(ParticleTypes.HEART, user.getX(), user.getEyeY(), user.getZ(), 3, 1, 1, 1, 0.1);
        }
    }
}

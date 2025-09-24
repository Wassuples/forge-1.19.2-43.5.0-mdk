package com.wassuples.demonslayer.forms.slayer.flower;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealDirectionalDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

public class PeoniesOfFutilityForm extends TimedForm {
    private static final int NUMBER_OF_ATTACKS = 9;
    private static final int TICKS_BETWEEN_ATTACKS = 4;
    private static final float DAMAGE_PER_HIT = 8.0f;

    private static final DustParticleOptions PEONY_PINK = new DustParticleOptions(new Vector3f(0.9f, 0.5f, 0.6f), 1.0f);

    public PeoniesOfFutilityForm(LivingEntity user, IForm form) {
        super(user, form, NUMBER_OF_ATTACKS * TICKS_BETWEEN_ATTACKS);
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.AZALEA_LEAVES_FALL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_ATTACKS == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);

            dealDirectionalDamage(user, 5.0, DAMAGE_PER_HIT, 0.4);

            float randomYawOffset = (level.random.nextFloat() - 0.5f) * 60f; // -30 to +30 degrees
            float randomPitchOffset = (level.random.nextFloat() - 0.5f) * 20f; // -10 to +10 degrees

            spawnParticleArcInWorld(level, user.getEyePosition(), 3.0, 140, 40, user.getYRot() + randomYawOffset, user.getXRot() + randomPitchOffset, PEONY_PINK);
        }
    }

    @Override
    public void onStop() {
    }
}

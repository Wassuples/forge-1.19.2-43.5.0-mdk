package com.wassuples.demonslayer.forms.slayer.wind;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.MovementHelper.pushPlayer;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

/**
 * Wind Breathing form: Gale Sudden Gusts.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class GaleSuddenGustsForm extends ActiveForm {
    public GaleSuddenGustsForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);
        pushPlayer(user, 5.0, false);
    }

    @Override
    public void onTick() {
        if (user.isOnGround() || user.horizontalCollision) {
            ActiveFormManager.stop(user);
            return;
        }
        dealAOEDamage(user, user.position(), 3.5, 2.0f, 1.2f);
        spawnParticleArc(user, ParticleTypes.SWEEP_ATTACK, 3.5, 360, 5);
        if (ticksActive % 8 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    @Override
    public void onStop() {
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}

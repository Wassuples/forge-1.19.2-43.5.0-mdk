package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealThrustDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.MovementHelper.pushPlayer;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class FlowingDanceForm extends TimedForm {
    public FlowingDanceForm(LivingEntity user, IForm form) {
        super(user, form, 50);
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick(); // Handles timeout
        if (ticksActive % 5 == 0) {
            pushPlayer(user, 2.5, false);
            dealThrustDamage(user, 5.0, 8.0f);
            spawnParticleArc(user, ParticleTypes.SPLASH, 3.0, 120, 25);
        }
    }
}
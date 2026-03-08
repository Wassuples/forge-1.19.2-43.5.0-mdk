package com.wassuples.demonslayer.forms.slayer.wind;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.performGenericAttack;
import static com.wassuples.demonslayer.util.VFXHelper.spawnCycloneParticles;

/**
 * Wind Breathing form: Dust Whirlwind Cutter.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class DustWhirlwindCutterForm extends TimedForm {
    private static final DustParticleOptions WIND_DUST = new DustParticleOptions(new Vector3f(0.8f, 1.0f, 0.8f), 1.0f);
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.6f, 0.2f), 1.0f);
    private final Vec3 direction;

    public DustWhirlwindCutterForm(LivingEntity user, IForm form) {
        super(user, form, 10);
        this.direction = Vec3.directionFromRotation(0, user.getYRot()).normalize();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();
        user.setDeltaMovement(direction.scale(1.8));
        user.hurtMarked = true;
        dealAOEDamage(user, user.position(), 2.5, 5.0f, 0.4f);
        spawnCycloneParticles(level, WIND_DUST, GREEN_DUST, user.position().add(0, 1.1, 0), 2.0, 2.2, 8, ticksActive);
    }

    @Override
    public void onStop() {
        performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
    }
}

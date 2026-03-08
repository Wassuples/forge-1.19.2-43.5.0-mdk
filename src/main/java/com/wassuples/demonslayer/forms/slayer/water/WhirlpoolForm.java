package com.wassuples.demonslayer.forms.slayer.water;

import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class WhirlpoolForm extends TimedForm {
    private float startYaw;

    public WhirlpoolForm(LivingEntity user, IForm form) {
        super(user, form, 30);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        this.startYaw = user.getYRot();
        user.setDeltaMovement(user.getDeltaMovement().add(0, 1.2, 0));
        user.hurtMarked = true;
        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void onTick() {
        super.onTick();
        float progress = (float) ticksActive / duration;
        float newYaw = startYaw + (360 * 3 * progress);
        user.setYRot(newYaw);
        user.setYHeadRot(newYaw);

        spawnParticleArc(user, ParticleTypes.SPLASH, 2.5, 360, 20);
        dealAOEDamage(user, user.position(), 3.0, 3.0f, 1.2f);
    }
}

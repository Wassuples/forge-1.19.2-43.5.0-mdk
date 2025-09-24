package com.wassuples.demonslayer.forms.baseForms;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public abstract class ChargeableForm extends ActiveForm {
    public int chargeGoal;
    public int chargeTicks = 0;
    protected boolean released = false;
    private int stopTick = -1;

    public ChargeableForm(LivingEntity user, IForm form) {
        super(user, form);
        this.chargeGoal = form.getChargeTicks();
    }

    protected abstract int getReleaseDuration();

    @Override
    public void onStart() {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, chargeGoal + 40, 255, false, false));
        onChargeStart();
    }

    @Override
    public void onTick() {
        if (stopTick != -1 && user.tickCount >= stopTick) {
            ActiveFormManager.stop(user);
            return;
        }

        if (!released) {
            if (chargeTicks < chargeGoal) {
                chargeTicks++;
                onChargingTick();
            }
        } else {
            onReleaseTick();
        }
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        onChargeEnd();
    }

    @Override
    public void onHurt(LivingHurtEvent event) {
        // If the form is interruptible and we are not yet in the release phase, stop it
        if (isInterruptible() && !this.released) {
            level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.8f);
            ActiveFormManager.stop(user, true);
        }
    }

    public void release(boolean wasFullyCharged) {
        DemonSlayer.LOGGER.debug("[ChargeForm] release(wasFullyCharged={})", wasFullyCharged);
        if (this.released) return;
        this.released = true;
        this.ticksActive = 0;
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        onChargeRelease(wasFullyCharged);

        if (getReleaseDuration() <= 0) {
            ActiveFormManager.stop(user);
        } else {
            this.stopTick = user.tickCount + getReleaseDuration();
        }
    }

    protected abstract void onChargeStart();
    protected abstract void onChargingTick();
    protected abstract void onChargeRelease(boolean fullyCharged);
    protected abstract void onReleaseTick();
    protected abstract void onChargeEnd();
}

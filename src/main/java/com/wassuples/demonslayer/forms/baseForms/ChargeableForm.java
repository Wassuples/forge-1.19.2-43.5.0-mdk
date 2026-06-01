package com.wassuples.demonslayer.forms.baseForms;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * A form that requires the player to hold a key to charge, then releases an effect.
 *
 * <p>The lifecycle is:
 * <ol>
 *   <li>{@link #onChargeStart()} — called once when charging begins (user is slowed)</li>
 *   <li>{@link #onChargingTick()} — called each tick while charging</li>
 *   <li>{@link #release(boolean)} — triggered by key release; calls {@link #onChargeRelease(boolean)}</li>
 *   <li>{@link #onReleaseTick()} — called each tick after release (for projectile travel, etc.)</li>
 *   <li>{@link #onChargeEnd()} — cleanup when form stops</li>
 * </ol>
 *
 * <p>If the user is hit during the charge phase and the form is interruptible, the charge is cancelled.
 *
 * @see ActiveForm
 * @see ActiveFormManager#startCharge
 * @see ActiveFormManager#releaseCharge
 */

/**
 * Chargeable Form.
 */
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

    protected int getMaxChargeTicks() {
        return chargeGoal;
    }

    @Override
    public void onStart() {
        // Use a moderate slowdown (level 1 = -30% speed) so the player can still adjust position
        // while charging, but can clearly feel the form is active. Level 255 was effectively a
        // full freeze which made it feel broken.
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, getMaxChargeTicks() + 40, 1, false, false));
        onChargeStart();
    }

    @Override
    public void onTick() {
        if (stopTick != -1 && user.tickCount >= stopTick) {
            // Only stop if this instance is actually the active form.
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
            return;
        }

        if (!released) {
            if (chargeTicks < getMaxChargeTicks()) {
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

    public void release() {
        DemonSlayer.LOGGER.debug("[ChargeForm] release(chargeTicks={}, maxChargeTicks={})", chargeTicks, getMaxChargeTicks());
        if (this.released) return;
        this.released = true;
        this.ticksActive = 0;
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        // Charge ratio: 0.0 = released instantly, 1.0 = held to full max charge.
        // Subclasses use this to scale their output linearly without any binary threshold.
        float chargeRatio = getMaxChargeTicks() > 0
                ? Mth.clamp((float) chargeTicks / getMaxChargeTicks(), 0.0f, 1.0f)
                : 1.0f;
        onChargeRelease(chargeRatio);

        if (getReleaseDuration() <= 0) {
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
        } else {
            this.stopTick = user.tickCount + getReleaseDuration();
        }
    }

    protected abstract void onChargeStart();
    protected abstract void onChargingTick();
    protected abstract void onChargeRelease(float chargeRatio);
    protected abstract void onReleaseTick();
    protected abstract void onChargeEnd();
}

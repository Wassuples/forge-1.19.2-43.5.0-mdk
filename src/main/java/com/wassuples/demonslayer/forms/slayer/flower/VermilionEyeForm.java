package com.wassuples.demonslayer.forms.slayer.flower;

import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.VignetteS2CPacket;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Flower Breathing form: Vermilion Eye.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class VermilionEyeForm extends TimedForm {
    private static final int DURATION = 2000; // 100 seconds of super mode
    private static final int BURNOUT_DURATION = 300; // 15 seconds of debuffs after it ends
    private static final double AURA_RADIUS = 12.0;

    // Effect Amplifiers (0=I, 1=II, etc.)
    private static final int SPEED_AMP = 3;      // Speed IV
    private static final int STRENGTH_AMP = 2;   // Strength III
    private static final int HASTE_AMP = 3;      // Haste IV
    private static final int SLOWNESS_AMP = 3;   // Slowness IV
    private static final int FATIGUE_AMP = 3;    // Mining Fatigue IV

    public VermilionEyeForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION + 40, SPEED_AMP, false, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION + 40, STRENGTH_AMP, false, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, DURATION + 40, HASTE_AMP, false, false, true));
        level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (user instanceof ServerPlayer player) {
            ModMessages.sendToPlayer(new VignetteS2CPacket(true), player);
        }
    }

    @Override
    public void onTick() {
        // Do NOT call super.onTick() because TimedForm calls ActiveFormManager.stop(user, false),
        // which removes the *active* form, not this buff.
        if (ticksActive >= DURATION) {
            ActiveFormManager.stopBuffForm(user, form);
            return;
        }

        AABB auraBox = user.getBoundingBox().inflate(AURA_RADIUS);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != user && e.isAlive());

        for (LivingEntity entity : nearbyEntities) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, SLOWNESS_AMP, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, FATIGUE_AMP, false, false, true));
        }
    }

    @Override
    public void onStop() {
        level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);

        user.hurt(DamageSource.MAGIC, 2.0f);
        user.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BURNOUT_DURATION, 0, false, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BURNOUT_DURATION, 1, false, false, true));

        if (user instanceof ServerPlayer player) {
            ModMessages.sendToPlayer(new VignetteS2CPacket(false), player);
        }
    }
}

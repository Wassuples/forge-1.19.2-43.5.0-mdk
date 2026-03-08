package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Sun Breathing form: Fire Wheel.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class FireWheelForm extends TimedForm {
    private static final int DURATION = 30;
    private static final int SLASH_TICK = 12;
    private static final float LEAP_STRENGTH_UP = 1.3f;
    private static final float LEAP_STRENGTH_BACK = 1.0f;
    private static final float DAMAGE = 50.0f;
    private static final double IMPACT_RADIUS = 5.0;
    private static final float KNOCKBACK = 1.5f;
    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    private boolean hasSlashed = false;
    private final Vec3 impactCenter;

    public FireWheelForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.impactCenter = user.position();
    }

    @Override public boolean isInterruptible() { return false; }

    @Override
    public void onStart() {
        Vec3 backward = user.getLookAngle().scale(-1);
        Vec3 leap = new Vec3(backward.x * LEAP_STRENGTH_BACK, LEAP_STRENGTH_UP, backward.z * LEAP_STRENGTH_BACK);
        user.setDeltaMovement(user.getDeltaMovement().add(leap));
        user.hurtMarked = true;
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION, 5, false, false, true));
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 1.2f);
    }

    @Override
    public void onTick() {
        super.onTick();

        float rotation = this.ticksActive * 45;
        spawnParticleArcInWorld(level, user.position().add(0, 1.0, 0), 2.0, 360, 20, user.getYRot(), 90 + rotation, RED_HOT);

        if (ticksActive >= SLASH_TICK && !this.hasSlashed) {
            this.hasSlashed = true;

            level.playSound(null, this.impactCenter.x, this.impactCenter.y, this.impactCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8f, 0.9f);
            spawnParticleArcInWorld(level, this.impactCenter.add(0, 1.0, 0), IMPACT_RADIUS, 180, 60, user.getYRot(), 90, ParticleTypes.LAVA);

            dealAOEDamage(user, this.impactCenter, IMPACT_RADIUS, DAMAGE, KNOCKBACK, false, true);

            DamageSource damageSource = (user instanceof Player p) ? DamageSource.playerAttack(p).setExplosion() : DamageSource.mobAttack(user).setExplosion();
            // Only stop if this form is running as the active form.
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
        }
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}

package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Sun Breathing form: Setting Sun.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SettingSunForm extends TimedForm {
    private static final int DURATION = 25;
    private static final int SLASH_TICK = 10;
    private static final float LEAP_STRENGTH_UP = 1.2f;
    private static final float LEAP_STRENGTH_BACK = 1.5f;

    private static final float DAMAGE = 45.0f;
    private static final double IMPACT_RADIUS = 6.0;
    private static final float KNOCKBACK = 1.0f;

    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    private boolean hasSlashed = false;
    private final Vec3 impactCenter;

    public SettingSunForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.impactCenter = user.position();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void onStart() {
        Vec3 backward = user.getLookAngle().scale(-1);
        Vec3 leap = new Vec3(backward.x * LEAP_STRENGTH_BACK, LEAP_STRENGTH_UP, backward.z * LEAP_STRENGTH_BACK);
        user.setDeltaMovement(user.getDeltaMovement().add(leap));
        user.hurtMarked = true;

        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION, 5, false, false, true));
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        super.onTick();

        if (ticksActive >= SLASH_TICK && !hasSlashed) {
            hasSlashed = true;

            level.playSound(null, this.impactCenter.x, this.impactCenter.y, this.impactCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
            spawnParticleArcInWorld(level, this.impactCenter.add(0, 0.2, 0), IMPACT_RADIUS, 360, 80, 0, 0, RED_HOT);
            dealAOEDamage(user, this.impactCenter, IMPACT_RADIUS, DAMAGE, KNOCKBACK, false, true);
        }
    }

    @Override
    public void onStop() {
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}

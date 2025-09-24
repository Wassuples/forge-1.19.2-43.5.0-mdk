package com.wassuples.demonslayer.forms.slayer.flame;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.dealLineDamage;
import static com.wassuples.demonslayer.util.WorldHelper.carveFieryTrench;

public class RengokuForm extends ChargeableForm {
    private static final int DASH_DURATION = 20;
    private Vec3 direction, prevPos;

    public RengokuForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override protected int getReleaseDuration() {
        return DASH_DURATION;
    }
    @Override public boolean isInterruptible() {
        return false;
    }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 0.5f);
    }

    @Override
    protected void onChargingTick() {
        float progress = (float) chargeTicks / chargeGoal;
        int particleCount = (int) (10 + 90 * progress);
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.0f);
        DustParticleOptions yellow = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.2f);

        for (int i = 0; i < particleCount; i++) {
            double angle = (user.tickCount * 0.1) + (i * 0.2 * (1 - progress));
            double radius = 1.0 + (1.5 * progress);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = i * (0.05 * progress);
            level.sendParticles(i % 2 == 0 ? red : yellow, user.getX() + x, user.getY() + y, user.getZ() + z, 1, 0, 0, 0, 0);
        }
        if (chargeTicks % 20 == 0) {
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f + progress, 1.0f + progress);
        }
    }

    @Override
    protected void onChargeRelease(boolean fullyCharged) {
        if (fullyCharged) {
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 1.2f);
            this.direction = user.getLookAngle();
            this.prevPos = user.position();
        } else {
            ActiveFormManager.stop(user);
        }
    }

    @Override
    protected void onReleaseTick() {
        user.setDeltaMovement(direction.scale(3.5));
        user.hurtMarked = true;

        Vec3 currentPos = user.position();
        dealLineDamage(user, prevPos, currentPos, 35.0f, 10);
        carveFieryTrench(level, prevPos, currentPos, 3, 2);
        this.prevPos = currentPos;
    }

    @Override
    protected void onChargeEnd() {
        level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
    }
}

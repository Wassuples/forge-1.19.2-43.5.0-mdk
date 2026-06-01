package com.wassuples.demonslayer.forms.slayer.sun;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

/**
 * Sun Breathing form: Sunflower Thrust.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class SunflowerThrustForm extends ChargeableForm {
    private static final int DASH_DURATION = 8;
    private static final double DASH_SPEED = 3.0;
    private static final float THRUST_DAMAGE = 25.0f;
    private static final float BLOOM_BASE_DAMAGE = 15.0f;
    private static final float BLOOM_CHARGE_BONUS = 30.0f;
    private static final double BLOOM_RADIUS = 3.0;
    private static final DustParticleOptions RED_HOT = new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.1f), 1.5f);

    private Vec3 direction;
    private boolean hasHit = false;

    public SunflowerThrustForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override protected int getReleaseDuration() {
        return DASH_DURATION;
    }
    @Override public boolean isInterruptible() { return false; }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    protected void onChargingTick() {
        Vec3 swordTip = user.getEyePosition().add(user.getLookAngle().scale(1.5));
        level.sendParticles(ParticleTypes.LAVA, swordTip.x, swordTip.y, swordTip.z, 2, 0.1, 0.1, 0.1, 0);
    }

    @Override
    protected void onChargeRelease(float chargeRatio) {
        if (chargeRatio >= 1.0f) {
            this.direction = user.getLookAngle();
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5f, 1.2f);
        } else {
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
        }
    }

    @Override
    protected void onReleaseTick() {
        if (this.hasHit) return;

        user.setDeltaMovement(direction.scale(DASH_SPEED));
        user.hurtMarked = true;
        level.sendParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 1, user.getZ(), 5, 0.1, 0.1, 0.1, 0.01);

        AABB impactBox = user.getBoundingBox().inflate(1.2).move(direction);
        List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, impactBox, e -> e != user && e.isAlive());

        if (!hitTargets.isEmpty()) {
            this.hasHit = true;
            triggerImpact(hitTargets.get(0));
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
        } else if (ticksActive >= getReleaseDuration() - 1) {
            if (ActiveFormManager.getActiveForm(user) == this) {
                ActiveFormManager.stop(user);
            }
        }
    }

    private void triggerImpact(LivingEntity primaryTarget) {
        DamageSource damageSource = (user instanceof Player p) ? DamageSource.playerAttack(p) : DamageSource.mobAttack(user);
        primaryTarget.hurt(damageSource, THRUST_DAMAGE);

        float bloomDamage = BLOOM_BASE_DAMAGE + (this.chargeTicks >= this.chargeGoal ? BLOOM_CHARGE_BONUS : 0);
        Vec3 bloomCenter = primaryTarget.position();

        dealAOEDamage(user, bloomCenter, BLOOM_RADIUS, bloomDamage, 1.0f, false, true);

        level.playSound(null, bloomCenter.x, bloomCenter.y, bloomCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.0f);
        for (int i = 0; i < 360; i += 20) {
            Vec3 dir = Vec3.directionFromRotation(0, i);
            spawnLineParticles(level, RED_HOT, bloomCenter, bloomCenter.add(dir.scale(BLOOM_RADIUS)), 5);
        }
    }

    @Override protected void onChargeEnd() {}
}

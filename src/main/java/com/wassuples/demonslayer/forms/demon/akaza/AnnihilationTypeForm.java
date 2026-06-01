package com.wassuples.demonslayer.forms.demon.akaza;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.MovementHelper.removeImmobility;

/**
 * Akaza Blood Demon Art form: Annihilation Type.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class AnnihilationTypeForm extends ChargeableForm {
    private static final int DASH_DURATION = 15; // faster dash than Rengoku
    private static final double DASH_SPEED = 4.0;   // Faster than Rengoku's
    private static final float IMPACT_DAMAGE = 60.0f;
    private static final double IMPACT_RADIUS = 7.0;

    private static final DustParticleOptions BLUE_AURA = new DustParticleOptions(new Vector3f(0.2f, 0.5f, 1.0f), 1.0f);
    private static final DustParticleOptions PURPLE_AURA = new DustParticleOptions(new Vector3f(0.6f, 0.2f, 1.0f), 1.2f);

    private Vec3 direction;

    public AnnihilationTypeForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    protected int getReleaseDuration() {
        return DASH_DURATION;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    protected void onChargeStart() {
        applyImmobility(user, Integer.MAX_VALUE);
        level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    @Override
    protected void onChargingTick() {
        float progress = (float) chargeTicks / chargeGoal;

        int particleCount = (int) (20 * progress);
        for (int i = 0; i < particleCount; i++) {
            double angle = (user.tickCount * 0.2) + (i * 0.3);
            double radius = 3.0 * (1.0 - progress);
            Vec3 offset = new Vec3(Math.cos(angle) * radius, (level.random.nextDouble() - 0.5) * 3.0, Math.sin(angle) * radius);
            level.sendParticles(i % 2 == 0 ? BLUE_AURA : PURPLE_AURA, user.getX() + offset.x, user.getY() + 1.0, user.getZ() + offset.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onChargeRelease(float chargeRatio) {
        removeImmobility(user);
        if (chargeRatio >= 1.0f) {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
            this.direction = user.getLookAngle();
        } else {
            ActiveFormManager.stop(user);
        }
    }

    @Override
    protected void onReleaseTick() {
        user.setDeltaMovement(direction.scale(DASH_SPEED));
        user.hurtMarked = true;

        AABB impactBox = user.getBoundingBox().inflate(1.0).move(direction.scale(1.5));
        List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, impactBox, e -> e != user && e.isAlive());

        if (!hitTargets.isEmpty()) {
            triggerExplosion(hitTargets.get(0).position());
            ActiveFormManager.stop(user);
        } else if (ticksActive >= getReleaseDuration() -1) {
            triggerExplosion(user.position());
        }
    }

    @Override
    protected void onChargeEnd() {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
    }

    private void triggerExplosion(Vec3 position) {
        dealAOEDamage(user, position, IMPACT_RADIUS, IMPACT_DAMAGE, 3.0f, false, true);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.8f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z, 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, position.x, position.y, position.z, 2, 0, 0, 0, 0);
    }
}

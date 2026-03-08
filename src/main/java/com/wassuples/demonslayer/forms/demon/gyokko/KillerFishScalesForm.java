package com.wassuples.demonslayer.forms.demon.gyokko;

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
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;

/**
 * Gyokko Blood Demon Art form: Killer Fish Scales.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class KillerFishScalesForm extends TimedForm {
    // --- CONSTANTS ---
    private static final int DURATION = 100; // 5 seconds
    private static final int MAX_BOUNCES = 6;
    private static final double DASH_RANGE = 15.0;
    private static final float DAMAGE_PER_HIT = 12.0f;
    private static final double DAMAGE_RADIUS = 3.0;
    private static final double FORWARD_DASH_SPEED = 2.5;
    private static final double HOMING_DASH_SPEED = 2.8; // Speed for bouncing between targets

    // --- STATE FIELDS ---
    @Nullable
    private LivingEntity currentTarget;
    private int bouncesLeft;
    private final boolean isHoming;
    private final Set<LivingEntity> alreadyHit;

    public KillerFishScalesForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
        this.bouncesLeft = MAX_BOUNCES;
        this.currentTarget = getLookedAtEntity(user, DASH_RANGE);
        this.isHoming = this.currentTarget != null;
        this.alreadyHit = new HashSet<>();
    }

    @Override
    public void onStart() {
        // Shared effects for both modes
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION + 5, 2));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION + 5, 1));
        level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.5f, 1.2f);

        if (isHoming) {
            // Sound specific to the homing attack
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            // Launch the user forward for the dash attack
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.2f, 1.0f);
            Vec3 lookVec = user.getLookAngle();
            user.setDeltaMovement(lookVec.scale(FORWARD_DASH_SPEED).add(0, 0.2, 0));
            user.hurtMarked = true; // Essential for applying velocity changes on the server
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        // Shared spinning particle effect for both modes
        final DustParticleOptions scaleParticle = new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.7f), 1.0f);
        for (int i = 0; i < 5; i++) {
            double angle = (ticksActive * 0.5) + (i * Math.PI / 2.5);
            Vec3 offset = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(1.2);
            level.sendParticles(scaleParticle, user.getX() + offset.x, user.getY() + 1.0, user.getZ() + offset.z, 1, 0, 0.1, 0, 0);
        }

        if (isHoming) {
            // --- Homing Bounce Attack Logic ---
            if (this.currentTarget == null || !this.currentTarget.isAlive() || this.bouncesLeft <= 0) {
                ActiveFormManager.stop(user);
                return;
            }

            // Check for collision with the target
            if (user.getBoundingBox().inflate(0.75).intersects(this.currentTarget.getBoundingBox())) {
                // --- HIT AND BOUNCE ---
                dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_HIT, 0.8f);
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
                level.sendParticles(ParticleTypes.EXPLOSION, this.currentTarget.getX(), this.currentTarget.getY(0.5), this.currentTarget.getZ(), 1, 0.2, 0.2, 0.2, 0);
                this.bouncesLeft--;

                LivingEntity oldTarget = this.currentTarget;
                List<LivingEntity> potentialNextTargets = level.getEntitiesOfClass(LivingEntity.class,
                        user.getBoundingBox().inflate(DASH_RANGE),
                        e -> e != user && e != oldTarget && e.isAlive() && !(e instanceof Player && ((Player) e).isCreative()));

                this.currentTarget = potentialNextTargets.isEmpty() ? null : potentialNextTargets.get(level.random.nextInt(potentialNextTargets.size()));

                // Apply a vertical "bounce" to jump out of the attack
                user.setDeltaMovement(user.getDeltaMovement().x * 0.2, 0.7, user.getDeltaMovement().z * 0.2);
                user.hurtMarked = true;

            } else {
                // --- DASH TOWARDS TARGET ---
                Vec3 direction = this.currentTarget.position().subtract(user.position()).normalize();
                user.setDeltaMovement(direction.scale(HOMING_DASH_SPEED));
                user.hurtMarked = true;
            }
        } else {
            // --- Forward Dash Attack Logic ---
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getY(0.5), user.getZ(), 2, 0.1, 0.1, 0.1, 0.0);

            List<LivingEntity> hitEntities = level.getEntitiesOfClass(LivingEntity.class,
                    user.getBoundingBox().inflate(1.0),
                    e -> e != user && e.isAlive() && !alreadyHit.contains(e) && !(e instanceof Player && ((Player) e).isCreative()));

            for (LivingEntity target : hitEntities) {
                target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_HIT);
                alreadyHit.add(target);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }
    }

    @Override
    public void onStop() {
        // Clean up effects for both modes
        user.removeEffect(MobEffects.MOVEMENT_SPEED);
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}

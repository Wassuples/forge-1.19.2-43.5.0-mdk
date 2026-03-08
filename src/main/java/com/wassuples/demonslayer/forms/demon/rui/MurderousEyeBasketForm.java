package com.wassuples.demonslayer.forms.demon.rui;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.getLookedAtEntity;
import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Rui Blood Demon Art form: Murderous Eye Basket.
 *
 * @see com.wassuples.demonslayer.styles.DemonForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class MurderousEyeBasketForm extends ActiveForm {
    private enum Phase {FORMATION, TRAPPED, CONTRACTION}

    // --- Constants ---
    private static final int FORMATION_DURATION = 40; // 2 seconds to weave
    private static final int TRAPPED_DURATION = 60;   // 3 seconds of being trapped
    private static final int CONTRACTION_DURATION = 40; // 2 seconds to shrink
    private static final double MAX_RANGE = 20.0;
    private static final double CAGE_RADIUS = 4.0;
    private static final double CAGE_HEIGHT = 5.0;

    private static final float TRAPPED_DAMAGE = 1.0f; // Small damage for touching the cage
    private static final float CONTRACTION_BASE_DAMAGE = 2.0f; // Starting damage of the shrink
    private static final float CONTRACTION_RAMP_DAMAGE = 6.0f; // Added damage by the end of the shrink

    private static final DustParticleOptions THREAD_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 0.7f);
    private static final DustParticleOptions EYE_PARTICLE = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.0f);

    private Phase currentPhase = Phase.FORMATION;
    private int phaseTicks = 0;
    private final Vec3 cageCenter;
    private final Vec3 eyePos;
    private final AABB cageAABB;


    public MurderousEyeBasketForm(LivingEntity user, IForm form) {
        super(user, form);

        Vec3 targetPos = null;
        LivingEntity lookedAtEntity = getLookedAtEntity(user, MAX_RANGE);
        if (lookedAtEntity != null) {
            targetPos = lookedAtEntity.position();
        } else {
            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
            if (hitResult.getType() != HitResult.Type.MISS) {
                targetPos = hitResult.getLocation();
            }
        }

        if (targetPos != null) {
            this.cageCenter = targetPos;
            this.eyePos = this.cageCenter.add(0, CAGE_HEIGHT, 0);
            this.cageAABB = new AABB(this.cageCenter, this.cageCenter).inflate(CAGE_RADIUS, CAGE_HEIGHT, CAGE_RADIUS);
        } else {
            // No valid target, mark for immediate stop
            this.cageCenter = null;
            this.eyePos = null;
            this.cageAABB = null;
        }
    }

    @Override
    public void onStart() {
        if (this.cageCenter == null) {
            ActiveFormManager.stop(user);
            return;
        }
        level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.SPIDER_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    public void onTick() {
        if (this.cageCenter == null) {
            ActiveFormManager.stop(user);
            return;
        }

        this.phaseTicks++;

        switch (this.currentPhase) {
            case FORMATION:
                tickFormation();
                if (this.phaseTicks >= FORMATION_DURATION) {
                    this.currentPhase = Phase.TRAPPED;
                    this.phaseTicks = 0;
                    level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);
                }
                break;
            case TRAPPED:
                tickTrapped();
                if (this.phaseTicks >= TRAPPED_DURATION) {
                    this.currentPhase = Phase.CONTRACTION;
                    this.phaseTicks = 0;
                    level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
                break;
            case CONTRACTION:
                tickContraction();
                if (this.phaseTicks >= CONTRACTION_DURATION) {
                    // Final burst of damage
                    dealAOEDamage(user, this.cageCenter, 1.0, CONTRACTION_BASE_DAMAGE + CONTRACTION_RAMP_DAMAGE, 0.5f);
                    level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cageCenter.x, cageCenter.y, cageCenter.z, 1, 0, 0, 0, 0);
                    ActiveFormManager.stop(user);
                }
                break;
        }
    }

    private void tickFormation() {
        float progress = (float) this.phaseTicks / FORMATION_DURATION;
        int threadsToShow = (int) (12 * progress); // Weave 12 main threads

        // The Eye
        level.sendParticles(EYE_PARTICLE, eyePos.x, eyePos.y, eyePos.z, 2, 0.2, 0.2, 0.2, 0);

        // Weave the vertical threads
        for (int i = 0; i < threadsToShow; i++) {
            double angle = (2 * Math.PI / 12) * i;
            Vec3 pointOnEquator = cageCenter.add(new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(CAGE_RADIUS));
            spawnLineParticles(level, THREAD_PARTICLE, eyePos, pointOnEquator, 5);
        }
        // Weave the horizontal threads
        int horizontalLines = (int) (5 * progress);
        for (int i = 0; i <= horizontalLines; i++) {
            double yOffset = (CAGE_HEIGHT / 5.0) * i;
            double horizontalRadius = Math.sin(Math.PI * (yOffset / CAGE_HEIGHT)) * CAGE_RADIUS;
            spawnParticleArcInWorld(level, cageCenter.add(0, yOffset, 0), horizontalRadius, 360, 15, 0, 90, THREAD_PARTICLE);
        }
    }

    private void tickTrapped() {
        // Render the full cage
        renderCage(CAGE_RADIUS);

        // Apply effects and damage to entities inside
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, this.cageAABB, e -> e != user && e.isAlive());
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, false, false));
            if (this.phaseTicks % 20 == 0) { // Damage every second
                target.hurt(DamageSource.MAGIC, TRAPPED_DAMAGE);
            }
        }
    }

    private void tickContraction() {
        float progress = (float) this.phaseTicks / CONTRACTION_DURATION;
        double currentRadius = CAGE_RADIUS * (1.0 - progress);

        // Render the shrinking cage
        renderCage(currentRadius);

        // Damage entities inside every 4 ticks
        if (this.phaseTicks % 4 == 0) {
            float damage = CONTRACTION_BASE_DAMAGE + (CONTRACTION_RAMP_DAMAGE * progress);
            AABB shrinkingAABB = new AABB(this.cageCenter, this.cageCenter).inflate(currentRadius, CAGE_HEIGHT, currentRadius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, shrinkingAABB, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                target.hurt(DamageSource.MAGIC, damage);
            }
            level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.0f + progress);
        }
    }

    private void renderCage(double radius) {
        // The Eye
        level.sendParticles(EYE_PARTICLE, eyePos.x, eyePos.y, eyePos.z, 2, 0.2, 0.2, 0.2, 0);

        // Vertical Threads
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI / 12) * i;
            Vec3 pointOnEquator = cageCenter.add(new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(radius));
            spawnLineParticles(level, THREAD_PARTICLE, eyePos, pointOnEquator, 5);
        }
        // Horizontal Threads
        for (int i = 0; i <= 5; i++) {
            double yOffset = (CAGE_HEIGHT / 5.0) * i;
            double horizontalRadius = Math.sin(Math.PI * (yOffset / CAGE_HEIGHT)) * radius;
            spawnParticleArcInWorld(level, cageCenter.add(0, yOffset, 0), horizontalRadius, 360, 20, 0, 90, THREAD_PARTICLE);
        }
    }
}

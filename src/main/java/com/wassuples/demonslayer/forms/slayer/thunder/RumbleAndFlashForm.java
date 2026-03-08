package com.wassuples.demonslayer.forms.slayer.thunder;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.CombatHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wassuples.demonslayer.util.VFXHelper.spawnLineParticles;

public class RumbleAndFlashForm extends TimedForm {
    private static final int DURATION = 20; // Short duration for post-cast cooldown/animation
    private static final int BOLT_COUNT = 7;
    private static final double MAX_RANGE = 18.0;
    private static final double CONE_SPREAD_DEGREES = 90.0;
    private static final float DAMAGE = 14.0f;
    private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

    public RumbleAndFlashForm(LivingEntity user, IForm form) {
        super(user, form, DURATION);
    }

    @Override
    public void onStart() {
        // Play activation sound
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);

        fireLightningCone();
    }

    private void fireLightningCone() {
        Vec3 start = user.getEyePosition().subtract(0, 0.3, 0);
        Vec3 viewVec = user.getViewVector(1.0F);

        // We only perturb yaw, keeping pitch roughly flat or following the look

        // We want to fan out horizontally.
        // We can rotate the look vector around the Y axis.

        Set<Integer> hitEntities = new HashSet<>();
        hitEntities.add(user.getId()); // Don't hit self

        double halfSpread = CONE_SPREAD_DEGREES / 2.0;
        double step = CONE_SPREAD_DEGREES / (BOLT_COUNT - 1);

        for (int i = 0; i < BOLT_COUNT; i++) {
            double angleOffset = -halfSpread + (i * step);
            Vec3 dir = rotateY(viewVec, (float) Math.toRadians(angleOffset));

            fireSingleBolt(start, dir, hitEntities);
        }
    }

    private void fireSingleBolt(Vec3 start, Vec3 direction, Set<Integer> hitEntities) {
        Vec3 end = start.add(direction.scale(MAX_RANGE));

        // 1. Check for blocks (Walls)
        HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        Vec3 actualEnd = end;

        if (blockHit.getType() != HitResult.Type.MISS) {
            actualEnd = blockHit.getLocation();
        }

        // 2. Visuals
        spawnLineParticles(level, YELLOW_LIGHTNING, start, actualEnd, (int)(start.distanceTo(actualEnd) * 2));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, actualEnd.x, actualEnd.y, actualEnd.z, 3, 0.1, 0.1, 0.1, 0.05);

        // 3. Entity Damage
        // Define a box around the ray to find entities
        AABB searchBox = new AABB(start, actualEnd).inflate(1.5);
        List<Entity> entitiesToCheck = level.getEntities(user, searchBox);

        for (Entity entity : entitiesToCheck) {
            if (entity instanceof LivingEntity target && !hitEntities.contains(entity.getId())) {
                // Precise check: is the entity strictly close to the line segment?
                AABB targetBb = target.getBoundingBox().inflate(0.5);

                // Check if the segment intersects the inflated bounding box
                if (targetBb.clip(start, actualEnd).isPresent()) {
                    hitEntities.add(target.getId());
                    attackTarget(target);
                }
            }
        }
    }

    private void attackTarget(LivingEntity target) {
        float damage = CombatHelper.calculateFinalDamage(user, DAMAGE);
        target.hurt(DamageSource.LIGHTNING_BOLT, damage);

        // Stun effect for getting hit by lightning
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4));

        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 2.0f);
    }

    private Vec3 rotateY(Vec3 vec, float radians) {
        float f = Mth.cos(radians);
        float f1 = Mth.sin(radians);
        double d0 = vec.x * (double)f + vec.z * (double)f1;
        double d1 = vec.y;
        double d2 = vec.z * (double)f - vec.x * (double)f1;
        return new Vec3(d0, d1, d2);
    }
}

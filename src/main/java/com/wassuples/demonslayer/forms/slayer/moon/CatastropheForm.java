package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wassuples.demonslayer.util.MovementHelper.applyImmobility;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;

/**
 * Moon Breathing form: Catastrophe.
 */
public class CatastropheForm extends ChargeableForm {
    // Moon14: Longer charge = more bursts. Extra charge window expanded significantly.
    private static final int MAX_EXTRA_CHARGE_TICKS = 160;
    private static final int BURST_INTERVAL = 9;
    private static final int BURST_START_TICK = 5;
    private static final int MIN_BURSTS = 3;
    private static final int MAX_BURSTS = 14;  // was 8; more bursts reward longer charging
    private static final double START_RADIUS = 7.0;
    private static final double RADIUS_PER_BURST = 3.0;
    private static final float DAMAGE = 28.0f;
    private static final float KNOCKBACK = 1.8f;

    private static final DustParticleOptions PURPLE_SLASH =
            new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.7f);

    private int releaseTick = 0;
    private int burstCount = 0;
    private int burstsReleased = 0;
    private final Set<UUID> hitThisBurst = new HashSet<>();

    public CatastropheForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    @Override
    protected int getMaxChargeTicks() {
        return chargeGoal + MAX_EXTRA_CHARGE_TICKS;
    }

    @Override
    protected int getReleaseDuration() {
        return burstCount <= 0 ? 1 : BURST_START_TICK + burstCount * BURST_INTERVAL + 12;
    }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.9f, 1.25f);
    }

    @Override
    protected void onChargingTick() {
        float chargeRatio = Mth.clamp((float) chargeTicks / getMaxChargeTicks(), 0.0f, 1.0f);
        double radius = 2.0 + chargeRatio * 6.0;

        if (chargeTicks % 6 == 0) {
            spawnParticleArcInWorld(level, user.position().add(0, 0.15, 0), radius, 360, 28, user.getYRot(), 90, PURPLE_SLASH);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, user.getX(), user.getY() + 0.2, user.getZ(), 8, radius * 0.18, 0.08, radius * 0.18, 0.01);
        }

        if (chargeTicks == chargeGoal) {
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
    }

    @Override
    protected void onChargeRelease(float chargeRatio) {
        this.releaseTick = 0;
        this.burstsReleased = 0;
        this.hitThisBurst.clear();

        // Burst count scales continuously from MIN_BURSTS at 0 charge to MAX_BURSTS at full charge.
        // This means an instant-cast produces a small but real burst, and a full hold produces the
        // maximum barrage. No binary threshold: every release fires something.
        this.burstCount = Mth.clamp(
                MIN_BURSTS + Math.round(chargeRatio * (MAX_BURSTS - MIN_BURSTS)),
                MIN_BURSTS, MAX_BURSTS);

        if (chargeRatio >= 1.0f) {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.4f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.75f);
        } else {
            // Partial charge still sounds dramatic, just quieter.
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f + chargeRatio * 0.4f, 0.8f + chargeRatio * 0.25f);
        }
        applyImmobility(user, 12 + this.burstCount * BURST_INTERVAL);
    }

    @Override
    protected void onReleaseTick() {
        if (burstCount <= 0) {
            return;
        }

        this.releaseTick++;
        if (this.releaseTick < BURST_START_TICK) {
            return;
        }

        int burstSlot = (this.releaseTick - BURST_START_TICK) / BURST_INTERVAL;
        if (burstSlot >= this.burstCount || burstSlot < this.burstsReleased) {
            return;
        }

        this.burstsReleased++;
        doBurst(burstSlot);
    }

    @Override
    protected void onChargeEnd() {
        this.hitThisBurst.clear();
    }

    private void doBurst(int burstIndex) {
        this.hitThisBurst.clear();

        double radius = START_RADIUS + burstIndex * RADIUS_PER_BURST;
        AABB searchBox = user.getBoundingBox().inflate(radius, 4.0, radius);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive() && !this.hitThisBurst.contains(e.getUUID()));

        for (LivingEntity target : potentialTargets) {
            if (target.distanceTo(user) > radius) {
                continue;
            }

            if (user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player).setExplosion(), DAMAGE);
            } else {
                target.hurt(DamageSource.mobAttack(user).setExplosion(), DAMAGE);
            }

            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.knockback(KNOCKBACK, knockbackDir.x, knockbackDir.z);
            this.hitThisBurst.add(target.getUUID());
        }

        level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.85f + burstIndex * 0.03f);
        spawnParticleArcInWorld(level, user.position().add(0, 0.2, 0), radius, 360, (int) (radius * 9), user.getYRot(), 90, PURPLE_SLASH);
        spawnParticleArcInWorld(level, user.position().add(0, 1.2, 0), radius * 0.8, 360, (int) (radius * 7), user.getYRot() + 30.0f, 0, PURPLE_SLASH);

        for (int i = 0; i < 18; i++) {
            Vec3 randomDir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.35, level.random.nextGaussian()).normalize();
            Vec3 particlePos = user.position().add(randomDir.scale(radius * (0.35 + level.random.nextDouble() * 0.65))).add(0, 0.4, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, particlePos.x, particlePos.y, particlePos.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
    }
}

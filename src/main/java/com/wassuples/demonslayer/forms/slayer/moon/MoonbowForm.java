package com.wassuples.demonslayer.forms.slayer.moon;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.wassuples.demonslayer.util.CombatHelper.dealAOEDamage;
import static com.wassuples.demonslayer.util.CombatHelper.isMoonBdaActive;
import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArcInWorld;
import static com.wassuples.demonslayer.util.WorldHelper.destroyBlocksInRadius;

/**
 * Moon Breathing form: Moonbow, Half Moon.
 *
 * <p>Chargeable form: hold the ability key to charge. The minimum charge (chargeGoal ticks)
 * fires a single cluster of 6 impacts. Each additional charge tier beyond that adds 3 more
 * impact points, up to a maximum of 5 tiers (30 extra impacts total). Release early for a
 * small bombardment; hold to full for a devastating multi-cluster barrage.
 *
 * <p>If the Crescent BDA is active, each impact also leaves soul fire at the blast site.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms#MOON_16
 * @see ChargeableForm
 */
public class MoonbowForm extends ChargeableForm {

    // ----- Tuning constants -----
    /** Ticks for the "incoming" telegraph rings to appear before impact. */
    private static final int TELEGRAPH_DURATION = 30;
    /** Ticks for crescent bolts to visually descend from the sky. */
    private static final int DESCENT_DURATION   = 20;
    /** Total ticks of one burst wave (telegraph + descent). */
    private static final int WAVE_DURATION      = TELEGRAPH_DURATION + DESCENT_DURATION;

    /** Maximum reach when targeting the bombardment centre. */
    private static final double MAX_RANGE         = 40.0;
    /** Radius within which impact points are scattered. */
    private static final double BOMBARDMENT_RADIUS = 10.0;
    /** AOE radius of each individual impact. */
    private static final double IMPACT_RADIUS     = 3.5;

    private static final float DAMAGE     = 45.0f;
    private static final float KNOCKBACK  = 2.5f;

    /** Soul fire spread radius when Crescent BDA is active. */
    private static final int SOUL_FIRE_RADIUS = 3;

    /** Impacts fired at minimum (base) charge. */
    private static final int BASE_IMPACTS    = 3;
    /** Extra impacts added per full extra-charge tier. */
    private static final int IMPACTS_PER_TIER = 3;
    /** Maximum extra tiers available beyond the base chargeGoal. */
    private static final int MAX_EXTRA_TIERS  = 5;
    /** Ticks of extra charge needed to earn one additional tier. */
    private static final int TICKS_PER_TIER   = 10;

    private static final DustParticleOptions PURPLE_SLASH =
            new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 2.0f);

    // ----- Runtime state -----
    /** Populated on release: all impact positions for every burst wave. */
    private final List<List<Vec3>> burstWaves = new ArrayList<>();
    /** Whether the Crescent BDA soul-fire bonus applies. */
    private boolean crescentTandem;
    /** Centre point that was aimed at when the key was released. */
    private Vec3 aimCentre;

    /** Tick counter within the release phase, advancing each call to onReleaseTick(). */
    private int releaseTick = 0;
    /** Which burst wave is currently being executed (0-based). */
    private int currentWave = 0;
    /** Whether the current wave's impacts have been applied. */
    private boolean waveImpactsTriggered = false;

    public MoonbowForm(LivingEntity user, IForm form) {
        super(user, form);
    }

    // =========================================================================
    // ChargeableForm overrides
    // =========================================================================

    @Override
    protected int getMaxChargeTicks() {
        // Allow up to MAX_EXTRA_TIERS tiers of extra charge on top of the required chargeGoal.
        return chargeGoal + MAX_EXTRA_TIERS * TICKS_PER_TIER;
    }

    @Override
    protected int getReleaseDuration() {
        // Each burst wave takes WAVE_DURATION ticks; waves are staggered by WAVE_DURATION apart.
        return burstWaves.isEmpty() ? 1 : burstWaves.size() * WAVE_DURATION;
    }

    @Override
    protected void onChargeStart() {
        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    protected void onChargingTick() {
        // Visual feedback: pulsing arcs grow larger as the charge ratio increases.
        float chargeRatio = Mth.clamp((float) chargeTicks / getMaxChargeTicks(), 0.0f, 1.0f);
        double radius = 1.5 + chargeRatio * 5.0;

        if (chargeTicks % 5 == 0) {
            spawnParticleArcInWorld(level, user.position().add(0, 0.1, 0),
                    radius, 360, (int)(radius * 6), user.getYRot(), 90, PURPLE_SLASH);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    user.getX(), user.getY() + 0.1, user.getZ(),
                    (int)(chargeRatio * 8) + 2, radius * 0.15, 0.05, radius * 0.15, 0.01);
        }
    }

    /**
     * On release, compute how many burst waves to fire based on how long the key was held,
     * then build all the impact point lists up front so they are ready during the release phase.
     */
    @Override
    protected void onChargeRelease(float chargeRatio) {
        this.crescentTandem = isMoonBdaActive(user);

        // Impact count scales continuously with chargeRatio: 0.0 = BASE_IMPACTS, 1.0 = maximum.
        // Every release fires at least one wave, making the form instant-castable at reduced power.
        int extraTiers = Mth.clamp(Math.round(chargeRatio * MAX_EXTRA_TIERS), 0, MAX_EXTRA_TIERS);
        int totalImpacts = BASE_IMPACTS + extraTiers * IMPACTS_PER_TIER;

        // Find the aim centre.
        BlockHitResult hit = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 end = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
            this.aimCentre = Vec3.atBottomCenterOf(
                    level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(end)));
        } else {
            this.aimCentre = Vec3.atCenterOf(hit.getBlockPos());
        }

        // Split total impacts across burst waves of BASE_IMPACTS each so the sky lights up
        // in distinct salvos rather than all at once.
        // Each wave after the first shifts the scatter pattern slightly for visual variety.
        int remaining = totalImpacts;
        int waveIndex  = 0;
        while (remaining > 0) {
            int waveSize = Math.min(BASE_IMPACTS, remaining);
            List<Vec3> wave = new ArrayList<>(waveSize);
            double angleShift = waveIndex * (Math.PI / BASE_IMPACTS); // rotate each wave
            for (int i = 0; i < waveSize; i++) {
                double angle = angleShift + (2 * Math.PI / waveSize) * i;
                double dist  = BOMBARDMENT_RADIUS * (0.5 + level.random.nextDouble() * 0.5);
                Vec3 offset  = new Vec3(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                wave.add(Vec3.atBottomCenterOf(level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(aimCentre.add(offset)))));
            }
            burstWaves.add(wave);
            remaining -= waveSize;
            waveIndex++;
        }

        level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.PLAYERS, 1.2f, 0.9f);
        level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_AMBIENT,
                SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    protected void onReleaseTick() {
        if (burstWaves.isEmpty() || currentWave >= burstWaves.size()) return;

        List<Vec3> wave = burstWaves.get(currentWave);
        int tickInWave = releaseTick % WAVE_DURATION; // 0 … WAVE_DURATION-1 within this wave

        // Telegraph phase — draw warning rings around each incoming point.
        if (tickInWave < TELEGRAPH_DURATION) {
            for (Vec3 pt : wave) {
                spawnParticleArcInWorld(level, pt.add(0, 0.2, 0),
                        IMPACT_RADIUS, 360, 10, 0, 90, ParticleTypes.SOUL_FIRE_FLAME);
            }
        }

        // Descent phase — purple bolts fall from the sky.
        if (tickInWave >= TELEGRAPH_DURATION) {
            double descentProgress = (double)(tickInWave - TELEGRAPH_DURATION) / DESCENT_DURATION;
            for (Vec3 pt : wave) {
                Vec3 skyPos     = pt.add(0, 25, 0);
                Vec3 particlePos = skyPos.lerp(pt, descentProgress);
                level.sendParticles(PURPLE_SLASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        3, 0.1, 0.1, 0.1, 0.0);
            }
        }

        // Impact — fires once at the very last tick of this wave.
        if (tickInWave == WAVE_DURATION - 1 && !waveImpactsTriggered) {
            waveImpactsTriggered = true;
            for (Vec3 pt : wave) {
                level.playSound(null, pt.x, pt.y, pt.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.8f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                dealAOEDamage(user, pt, IMPACT_RADIUS, DAMAGE, KNOCKBACK, false, true);
                destroyBlocksInRadius(level, new BlockPos(pt), IMPACT_RADIUS, 4, false, user);
                if (this.crescentTandem) {
                    leaveSoulFire(pt);
                }
            }
            // Advance to the next wave.
            currentWave++;
            waveImpactsTriggered = false;
        }

        releaseTick++;
    }

    @Override
    protected void onChargeEnd() {
        // Nothing to clean up.
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void leaveSoulFire(Vec3 impactPoint) {
        BlockPos center = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(impactPoint));
        int rSqr = SOUL_FIRE_RADIUS * SOUL_FIRE_RADIUS;

        for (int x = -SOUL_FIRE_RADIUS; x <= SOUL_FIRE_RADIUS; x++) {
            for (int z = -SOUL_FIRE_RADIUS; z <= SOUL_FIRE_RADIUS; z++) {
                if (x * x + z * z > rSqr || level.random.nextFloat() > 0.72f) continue;

                BlockPos surface = level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(x, 0, z));
                BlockPos base = surface.below();
                if (level.getBlockState(surface).isAir() && !level.getBlockState(base).isAir()) {
                    level.setBlock(base, Blocks.SOUL_SOIL.defaultBlockState(), 11);
                    level.setBlock(surface, Blocks.SOUL_FIRE.defaultBlockState(), 11);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            surface.getX() + 0.5, surface.getY() + 0.15, surface.getZ() + 0.5,
                            4, 0.15, 0.05, 0.15, 0.02);
                }
            }
        }
    }
}

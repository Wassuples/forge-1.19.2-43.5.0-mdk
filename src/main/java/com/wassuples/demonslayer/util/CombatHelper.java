package com.wassuples.demonslayer.util;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class CombatHelper {
    /**
     * The single source of truth for damage.
     * @param user The entity dealing the damage.
     * @param baseDamage The base damage of the attack before any modifiers.
     * @return The final calculated damage.
     */
    public static float calculateFinalDamage(LivingEntity user, float baseDamage) {
        // Using an array wrapper to modify the value inside lambdas
        final float[] finalDamage = { baseDamage };

        // --- DEMON-SPECIFIC MODIFIERS ---
        user.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;

            // Apply Blood Art Potency Passive
            if (demon.isPassiveActive(DemonPassive.BLOOD_ART_POTENCY)) {
                int tier = demon.getPassiveTier(DemonPassive.BLOOD_ART_POTENCY);
                finalDamage[0] *= (1.0f + (tier * 0.05f)); // +5% damage per tier
            }

            // Apply Crescent BDA (from Moon Breathing) bonus
            if (demon.getEquippedBda() == BloodDemonArts.CRESCENT) {
                finalDamage[0] *= 1.3f; // +30% damage
            }

            // TODO: Apply BDA Tier scaling here if you want a global buff for all abilities
        });

        // --- SLAYER-SPECIFIC MODIFIERS ---
        user.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            // TODO: Apply Slayer Rank damage bonus
            // Rank rank = progression.getRank();
            // finalDamage[0] *= (1.0f + (rank.ordinal() * 0.02f)); // +2% damage per rank
        });

        return finalDamage[0];
    }

    /**
     * The single source of truth for range.
     * @param user The entity dealing the damage.
     * @param baseRange The base range of the attack before any modifiers.
     * @return The final calculated range.
     */
    public static double calculateFinalRange(LivingEntity user, double baseRange) {
        final double[] finalRange = { baseRange };

        user.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;

            // Apply Crescent BDA (from Moon Breathing) bonus
            if (demon.getEquippedBda() == BloodDemonArts.CRESCENT) {
                finalRange[0] *= 1.2; // +20% range
            }
        });

        return finalRange[0];
    }

    /**
     * A centralized helper to apply damage and knockback to a target.
     * @param user The attacker.
     * @param target The entity being damaged.
     * @param damage The final, calculated damage to apply.
     * @param source The DamageSource to use.
     * @param knockbackDir The direction of the knockback. Can be null.
     * @param knockbackStrength The strength of the knockback.
     */
    private static void applyDamageToTarget(LivingEntity user, LivingEntity target, float damage, DamageSource source, @Nullable Vec3 knockbackDir, float knockbackStrength) {
        if (!target.hurt(source, damage)) {
            return; // Don't apply knockback if the damage event was cancelled
        }
        if (knockbackDir == null || knockbackStrength <= 0) {
            return; // No knockback to apply
        }

        target.knockback(knockbackStrength, knockbackDir.x, knockbackDir.z);
    }

    /**
     * Gets the appropriate DamageSource for the user.
     * @param user The entity dealing damage.
     * @return DamageSource.playerAttack or DamageSource.mobAttack.
     */
    public static DamageSource getDamageSource(LivingEntity user) {
        return (user instanceof Player player) ? DamageSource.playerAttack(player) : DamageSource.mobAttack(user);
    }

    /**
     * Gets the appropriate DamageSource for the user, with optional properties.
     * @param user The entity dealing damage.
     * @param isFire Whether the damage should be fire-based.
     * @param isExplosion Whether the damage should count as an explosion.
     * @return The configured DamageSource.
     */
    public static DamageSource getDamageSource(LivingEntity user, boolean isFire, boolean isExplosion) {
        DamageSource source = getDamageSource(user);
        if (isFire) source.setIsFire();
        if (isExplosion) source.setExplosion();
        return source;
    }

    // =================================================================================
    // Damage Dealing Methods
    // =================================================================================

    /**
     * Deals damage to targets in a forward-facing cone.
     */
    public static void dealDirectionalDamage(LivingEntity user, double baseRange, float baseDamage, double angleThreshold) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRange = calculateFinalRange(user, baseRange);
        DamageSource source = getDamageSource(user);
        Vec3 lookVec = user.getLookAngle().normalize();

        AABB attackAABB = user.getBoundingBox().inflate(finalRange);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackAABB, entity ->
                entity != user && entity.isAlive() && user.hasLineOfSight(entity));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(user.position()).normalize();
            if (lookVec.dot(toTarget) < angleThreshold) continue;

            Vec3 knockback = toTarget.scale(0.5);
            applyDamageToTarget(user, target, finalDamage, source, knockback, 0.5f);
        }
    }

    /**
     * Deals damage in a cone in a specific direction, defined by yaw.
     */
    public static void dealDirectionalDamageInYaw(LivingEntity user, double baseRange, float baseDamage, float knockback, float yaw) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRange = calculateFinalRange(user, baseRange);
        DamageSource source = getDamageSource(user);

        // Calculate the direction vector from yaw and pitch
        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-user.getXRot());
        Vec3 lookVec = new Vec3(Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad));

        // Find all targets in a single large box
        Vec3 endPos = user.getEyePosition().add(lookVec.scale(finalRange));
        AABB broadPhaseBox = new AABB(user.getEyePosition(), endPos).inflate(2.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, entity ->
                !entity.is(user) && entity.isAlive() && user.hasLineOfSight(entity));

        for (LivingEntity target : targets) {
            // No need for dot product check as this is a narrow "beam" attack style
            applyDamageToTarget(user, target, finalDamage, source, new Vec3(-lookVec.x, 0, -lookVec.z), knockback);
        }
    }

    /**
     * Deals damage to all valid targets within a spherical radius, with optional flags.
     */
    public static void dealAOEDamage(LivingEntity user, Vec3 centerPos, double baseRange, float baseDamage, float knockbackStrength, boolean isFire, boolean isExplosion) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRange = calculateFinalRange(user, baseRange);
        DamageSource source = getDamageSource(user, isFire, isExplosion);

        AABB attackAABB = new AABB(centerPos, centerPos).inflate(finalRange);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackAABB, entity -> entity != user && entity.isAlive());

        for (LivingEntity target : targets) {
            Vec3 knockbackDir = centerPos.vectorTo(target.position()).normalize();
            applyDamageToTarget(user, target, finalDamage, source, knockbackDir, knockbackStrength);
        }
    }

    /**
     * Overloaded version of dealAOEDamage for non-special damage types.
     */
    public static void dealAOEDamage(LivingEntity user, Vec3 centerPos, double range, float baseDamage, float knockbackStrength) {
        dealAOEDamage(user, centerPos, range, baseDamage, knockbackStrength, false, false);
    }

    /**
     * Deals damage to all entities within a vertical column.
     */
    public static void dealVerticalColumnDamage(LivingEntity user, Vec3 bottomCenter, double baseRadius, double baseHeight, float baseDamage) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRadius = calculateFinalRange(user, baseRadius);
        double finalHeight = calculateFinalRange(user, baseHeight); // Height can also be considered a "range"
        DamageSource source = getDamageSource(user);

        AABB damageZone = new AABB(bottomCenter, bottomCenter).inflate(finalRadius, 0, finalRadius).expandTowards(0, finalHeight, 0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());

        for (LivingEntity target : targets) {
            applyDamageToTarget(user, target, finalDamage, source, null, 0);
        }
    }

    /**
     * Deals damage in a straight line (a thrust).
     */
    public static void dealThrustDamage(LivingEntity user, double baseRange, float baseDamage) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRange = calculateFinalRange(user, baseRange);
        DamageSource source = getDamageSource(user);
        Vec3 start = user.getEyePosition();
        Vec3 direction = user.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(finalRange));

        AABB lineAABB = new AABB(start, end).inflate(1.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, lineAABB, entity ->
                entity != user && entity.isAlive() && isEntityInLineOfSight(user, entity, lineAABB));

        for (LivingEntity target : targets) {
            applyDamageToTarget(user, target, finalDamage, source, direction, 0.5f);
        }
    }

    /**
     * Deals damage and sets entities on fire along a vector path.
     */
    public static void dealLineDamage(LivingEntity user, Vec3 startPos, Vec3 endPos, float baseDamage, int fireSeconds) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        DamageSource source = getDamageSource(user, fireSeconds > 0, false);

        AABB damageAABB = new AABB(startPos, endPos).inflate(2.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageAABB, entity -> entity != user && entity.isAlive());

        for (LivingEntity target : targets) {
            applyDamageToTarget(user, target, finalDamage, source, null, 0);
            if (fireSeconds > 0) {
                target.setSecondsOnFire(fireSeconds);
            }
        }
    }

    /**
     * Deals damage in multiple arcs, simulating claws.
     */
    public static void dealClawDamage(LivingEntity user, ParticleOptions particle, float baseDamage, double baseRange, int clawCount, double spreadAngle, float knockback) {
        if (!(user.level instanceof ServerLevel level)) return;

        float finalDamage = calculateFinalDamage(user, baseDamage);
        double finalRange = calculateFinalRange(user, baseRange);
        DamageSource source = getDamageSource(user);

        Vec3 userPos = user.position();
        float yawRad = -user.getYRot() * Mth.DEG_TO_RAD;
        Vec3 horizontalDir = new Vec3(Mth.sin(yawRad), 0, Mth.cos(yawRad)).normalize();

        AABB broadPhaseBox = user.getBoundingBox().inflate(finalRange + 2.0, 4.0, finalRange + 2.0);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, entity -> entity != user && entity.isAlive());
        if (potentialTargets.isEmpty()) return;

        Set<LivingEntity> hitTargets = new HashSet<>();

        for (int clawIndex = 0; clawIndex < clawCount; clawIndex++) {
            double angleOffset = -(spreadAngle / 2) + (clawIndex * (spreadAngle / (clawCount - 1)));
            Vec3 slashDirection = horizontalDir.yRot((float) Math.toRadians(angleOffset));

            BlockPos endGroundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, user.blockPosition().offset(slashDirection.scale(finalRange).x, 0, slashDirection.scale(finalRange).z));
            Vec3 arcEnd = Vec3.atBottomCenterOf(endGroundPos);

            for (int i = 0; i <= 15; i++) {
                double progress = i / 15.0;
                Vec3 linePoint = user.getEyePosition().lerp(arcEnd, progress);
                double arcHeight = Math.sin(progress * Math.PI) * 3.0;
                Vec3 currentPoint = linePoint.add(0, arcHeight, 0);

                level.sendParticles(particle, currentPoint.x, currentPoint.y, currentPoint.z, 1, 0, 0, 0, 0);

                for (LivingEntity potentialTarget : potentialTargets) {
                    if (potentialTarget.getBoundingBox().inflate(0.5).contains(currentPoint)) {
                        hitTargets.add(potentialTarget);
                    }
                }
            }
        }

        for (LivingEntity target : hitTargets) {
            Vec3 knockbackDir = userPos.vectorTo(target.position()).normalize();
            applyDamageToTarget(user, target, finalDamage, source, knockbackDir, knockback);
        }
    }

    /**
     * This is the master function for the Nichirin Sword's basic attack effect.
     * It can be called from anywhere to trigger the style-based visual and damage cone.
     */
    public static void performNichirinSwingEffect(LivingEntity user) {
        user.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            BreathingStyles currentStyle = style.getCurrentStyles();
            ParticleOptions particle = switch (currentStyle) {
                case WIND -> ParticleTypes.CLOUD;
                case INSECT -> new DustParticleOptions(new Vector3f(0.8f, 0.4f, 1.0f), 1.0f);
                case STONE -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
                case WATER -> ParticleTypes.SPLASH;
                case LOVE -> new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
                case SOUND -> new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 1.0f);
                case BEAST -> new DustParticleOptions(new Vector3f(0.6f, 0.4f, 0.2f), 1.0f);
                case SERPENT -> new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.8f), 1.0f);
                case FLOWER -> new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.8f), 1.0f);
                case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
                case FLAME -> ParticleTypes.FLAME;
                case MIST -> new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);
                case MOON -> new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
                case SUN -> new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 1.0f);
                case WOOD -> new DustParticleOptions(new Vector3f(0.4f, 0.6f, 0.2f), 1.0f);
                case FROST -> new DustParticleOptions(new Vector3f(0.2f, 0.8f, 1.0f), 1.0f);
                case SAND -> new DustParticleOptions(new Vector3f(0.8f, 0.7f, 0.5f), 1.0f);
                default -> ParticleTypes.SWEEP_ATTACK;
            };

            // Unleash the extra effect: a small directional damage cone and a particle slash.
            dealDirectionalDamage(user, 3.0, 4.0f, 0.7);
            spawnParticleArc(user, particle, 2.0, 120, 20);
        });
    }

    public static void performDemonAttack(LivingEntity user) {
        user.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;

            BloodDemonArts currentBda = demon.getEquippedBda();

            ParticleOptions particle = switch (currentBda) {
                case CRESCENT -> new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
                case CRYO -> ParticleTypes.SNOWFLAKE;
                case DESTRUCTIVE_DEATH -> new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.0f);
                case EMOTION -> new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 1.0f);
                case FISH -> new DustParticleOptions(new Vector3f(0.2f, 0.8f, 0.9f), 1.0f);
                case SASH -> new DustParticleOptions(new Vector3f(0.9f, 0.4f, 0.8f), 1.0f);
                case HAEMO -> new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 1.0f);
                case SLEEP -> new DustParticleOptions(new Vector3f(0.6f, 0.6f, 0.9f), 1.0f);
                case THREADS -> ParticleTypes.SPIT;
                default -> ParticleTypes.SWEEP_ATTACK;
            };

            dealDirectionalDamage(user, 3.0, 4.0f, 0.7);
            spawnParticleArc(user, particle, 2.0, 120, 20);
        });
    }

    /**
     * Plays a sound and swings the player's hand.
     */
    public static void performGenericAttack(LivingEntity user, SoundEvent sound, float volume, float pitch) {
        user.getLevel().playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        user.swing(InteractionHand.MAIN_HAND);
    }

    public static boolean isMoonBdaActive(LivingEntity user) {
        return user.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(demon -> demon.getEquippedBda() == BloodDemonArts.CRESCENT)
                .orElse(false);
    }

    /**
     * Checks if the player is holding a NichirinSwordItem in either hand.
     */
    public static boolean isPlayerHoldingNichirin(Player player) {
        return player.getMainHandItem().getItem() instanceof SwordItem ||
                player.getOffhandItem().getItem() instanceof SwordItem;
    }

    /**
     * Checks if a damage source can be parried.
     */
    public static boolean isParryable(DamageSource source) {
        return (source.getDirectEntity() instanceof LivingEntity || source.isProjectile())
                && !source.isExplosion()
                && !source.isFire()
                && !source.isMagic()
                && !source.isCreativePlayer();
    }

    // =================================================================================
    // General Utilities
    // =================================================================================

    /**
     * A more robust line-of-sight check that considers the entity's bounding box.
     */
    private static boolean isEntityInLineOfSight(LivingEntity user, Entity entity, AABB boundingBox) {
        return user.hasLineOfSight(entity) || entity.getBoundingBox().intersects(boundingBox);
    }

    /**
     * Finds the living entity the player is looking at within a given range.
     * @return The entity, or null if no entity is found.
     */
    @Nullable
    public static LivingEntity getLookedAtEntity(LivingEntity user, double range) {
        List<Entity> entities = user.level.getEntities(user, user.getBoundingBox().expandTowards(user.getLookAngle().scale(range)).inflate(1.0D), e -> e instanceof LivingEntity && !e.isSpectator());
        LivingEntity closestEntity = null;
        double closestDist = Double.MAX_VALUE;

        Vec3 eyePos = user.getEyePosition();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingTarget) {
                double dist = livingTarget.getEyePosition().distanceToSqr(eyePos);
                if (dist < closestDist && user.hasLineOfSight(livingTarget)) {
                    closestDist = dist;
                    closestEntity = livingTarget;
                }
            }
        }
        return closestEntity;
    }

    /**
     * Rotates a vector by a given pitch and yaw.
     * @param vec The vector to rotate.
     * @param pitch The pitch in degrees.
     * @param yaw The yaw in degrees.
     * @return The rotated vector.
     */
    public static Vec3 rotateVec(Vec3 vec, float pitch, float yaw) {
        float p_rad = pitch * Mth.DEG_TO_RAD;
        float y_rad = yaw * Mth.DEG_TO_RAD;
        vec = vec.xRot(p_rad);
        vec = vec.yRot(-y_rad); // We use negative yaw for standard rotation
        return vec;
    }
}

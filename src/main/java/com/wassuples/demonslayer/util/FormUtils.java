package com.wassuples.demonslayer.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.effects.ForcedSleepEffect;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.*;
import com.wassuples.demonslayer.gamerule.ModGameRules;
import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiFunction;

public class FormUtils {

    // =================================================================================
    // Damage Dealing Methods
    // =================================================================================

    /**
     * Deals damage in a cone in front of the player.
     */
    public static void dealDirectionalDamage(LivingEntity user, double range, float damage, double angleThreshold) {
        ServerLevel level = (ServerLevel) user.getLevel();
        Vec3 lookVec = user.getLookAngle().normalize();
        AABB attackAABB = user.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackAABB, entity ->
                entity != user && entity.isAlive() && user.hasLineOfSight(entity));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(user.position()).normalize();
            double dot = lookVec.dot(toTarget);
            if (dot > angleThreshold) {
                if(user instanceof Player player) {
                    target.hurt(DamageSource.playerAttack(player), damage);
                } else {
                    target.hurt(DamageSource.mobAttack(user), damage);
                }
                Vec3 knockback = toTarget.scale(0.5);
                target.push(knockback.x, 0.1, knockback.z);
            }
        }
    }

    /**
     * Deals damage in a cone in a specific direction, defined by yaw.
     */
    public static void dealDirectionalDamageInYaw(LivingEntity user, double range, float damage, float knockback, float yaw) {
        Vec3 eyePos = user.getEyePosition();
        // Calculate a look vector from the given yaw
        float f = (float) Math.toRadians(-yaw);
        float f1 = (float) Math.toRadians(-user.getXRot());
        Vec3 lookVec = new Vec3(Mth.sin(f) * Mth.cos(f1), -Mth.sin(f1), Mth.cos(f) * Mth.cos(f1));

        for (int i = 0; i < 8; ++i) { // Check multiple points for better accuracy
            Vec3 targetPos = eyePos.add(lookVec.scale(range * i / 8.0));
            AABB attackAABB = new AABB(targetPos, targetPos).inflate(2.0, 2.0, 2.0);
            List<LivingEntity> targets = user.level.getEntitiesOfClass(LivingEntity.class, attackAABB, entity ->
                    !entity.is(user) && entity.isAlive() && user.hasLineOfSight(entity));

            for (LivingEntity target : targets) {
                if(user instanceof Player player) {
                    target.hurt(DamageSource.playerAttack(player), damage);
                } else {
                    target.hurt(DamageSource.mobAttack(user), damage);
                }
                target.knockback(knockback, -lookVec.x, -lookVec.z);
            }
        }
    }

    /**
     * Deals damage to all entities within a radius around the player.
     */
    public static void dealAOEDamage(LivingEntity user, Vec3 centerPos, double range, float damage, float knockbackStrength, DamageSource damageSource) {
        ServerLevel level = (ServerLevel) user.getLevel();
        AABB attackAABB = new AABB(centerPos, centerPos).inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackAABB, entity ->
                entity != user && entity.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(damageSource, damage);
            double d0 = centerPos.x() - target.getX();
            double d1;
            for (d1 = centerPos.z() - target.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                d0 = (Math.random() - Math.random()) * 0.01D;
            }
            target.knockback(knockbackStrength, d0, d1);
        }
    }

    /**
     * Deals damage to all entities within a vertical column.
     */
    public static void dealVerticalColumnDamage(LivingEntity user, Vec3 bottomCenter, double radius, double height, float damage, DamageSource damageSource) {
        ServerLevel level = (ServerLevel) user.getLevel();
        AABB damageZone = new AABB(bottomCenter, bottomCenter).inflate(radius, 0, radius).expandTowards(0, height, 0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());

        for (LivingEntity target : targets) {
            // Using lightningBolt damage source is very thematic for Thunder Breathing.
            target.hurt(damageSource, damage);
        }
    }

    /**
     * Deals damage in a straight line (a thrust).
     */
    public static void dealThrustDamage(LivingEntity user, double range, float damage) {
        ServerLevel level = (ServerLevel) user.getLevel();
        Vec3 start = user.getEyePosition();
        Vec3 direction = user.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));
        AABB lineAABB = new AABB(start, end).inflate(1.0);

        if(user instanceof Player player) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, lineAABB, entity ->
                    entity != player && entity.isAlive() && isEntityInLineOfSight(player, entity, lineAABB));

            for (LivingEntity target : targets) {
                target.hurt(DamageSource.playerAttack(player), damage);

                Vec3 knockback = direction.scale(0.5);
                target.push(knockback.x, 0.1, knockback.z);
            }
        } else {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, lineAABB, entity ->
                    entity != user && entity.isAlive() && user.hasLineOfSight(entity));

            for(LivingEntity target : targets) {
                target.hurt(DamageSource.mobAttack(user), damage);
                Vec3 knockback = direction.scale(0.5);
                target.push(knockback.x, 0.1, knockback.z);
            }
        }
    }

    /**
     * Deals damage and sets entities on fire along a vector path.
     */
    public static void dealLineDamage(LivingEntity user, Vec3 startPos, Vec3 endPos, float damage, int fireSeconds) {
        ServerLevel level = (ServerLevel) user.getLevel();
        AABB damageAABB = new AABB(startPos, endPos).inflate(2.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageAABB, entity ->
                entity != user && entity.isAlive());

        for (LivingEntity target : targets) {
            if(user instanceof Player player) {
                target.hurt(DamageSource.playerAttack(player), damage);
            } else {
                target.hurt(DamageSource.mobAttack(user), damage);
            }
            if (fireSeconds > 0) {
                target.setSecondsOnFire(fireSeconds);
            }
        }
    }

    /**
     * Deals damage in multiple arcs, simulating claws.
     */
    public static void dealClawDamage(LivingEntity user, float damage, double range, int clawCount, double spreadAngle, float knockback) {
        ServerLevel level = (ServerLevel) user.getLevel();
        Vec3 playerPos = user.position();
        float yawRad = -user.getYRot() * ((float) Math.PI / 180F);
        Vec3 horizontalDir = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();

        for (int clawIndex = 0; clawIndex < clawCount; clawIndex++) {
            double angleOffset = -(spreadAngle / 2) + (clawIndex * (spreadAngle / (clawCount - 1)));
            Vec3 slashDirection = horizontalDir.yRot((float) Math.toRadians(angleOffset));

            BlockPos endGroundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, user.blockPosition().offset(slashDirection.scale(range).x, 0, slashDirection.scale(range).z));
            Vec3 arcEnd = Vec3.atBottomCenterOf(endGroundPos);

            for (int i = 0; i <= 15; i++) {
                double progress = i / 15.0;
                Vec3 linePoint = user.getEyePosition().lerp(arcEnd, progress);
                double arcHeight = Math.sin(progress * Math.PI) * 3.0;
                Vec3 currentPoint = linePoint.add(0, arcHeight, 0);

                AABB damageBox = new AABB(currentPoint, currentPoint).inflate(1.8);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageBox, entity -> entity != user && entity.isAlive());

                for (LivingEntity target : targets) {
                    if(user instanceof Player player) {
                        if (target.hurt(DamageSource.playerAttack(player), damage)) {
                            Vec3 knockbackDir = target.position().subtract(playerPos).normalize();
                            target.push(knockbackDir.x * knockback, 0.4, knockbackDir.z * knockback);
                        }
                    } else {
                        if (target.hurt(DamageSource.mobAttack(user), damage)) {
                            Vec3 knockbackDir = target.position().subtract(playerPos).normalize();
                            target.push(knockbackDir.x * knockback, 0.4, knockbackDir.z * knockback);
                        }
                    }
                }
            }
        }
    }

    /**
     * This is the master function for the Nichirin Sword's basic attack effect.
     * It can be called from anywhere to trigger the style-based visual and damage cone.
     */
    public static void performNichirinSwingEffect(LivingEntity user) {
        user.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            BreathingStyles currentStyle = style.getCurrentStyles();

            // Choose a particle effect based on the current style.
            ParticleOptions particle = switch (currentStyle) {
                case WATER -> ParticleTypes.SPLASH;
                case FLAME -> ParticleTypes.FLAME;
                case WIND -> ParticleTypes.CLOUD;
                case MIST -> new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);
                case STONE -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
                case INSECT -> new DustParticleOptions(new Vector3f(0.8f, 0.4f, 1.0f), 1.0f);
                case LOVE -> new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
                case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
                case SERPENT -> new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.8f), 1.0f);
                case FLOWER -> new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.8f), 1.0f);
                default -> ParticleTypes.SWEEP_ATTACK;
            };

            // Unleash the extra effect: a small directional damage cone and a particle slash.
            dealDirectionalDamage(user, 3.0, 4.0f, 0.7);
            spawnParticleArc(user, particle, 2.0, 120, 20);
        });
    }

    public static void performDemonAttack(LivingEntity user) {
        user.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            BreathingStyles currentStyle = style.getCurrentStyles();

            ParticleOptions particle = switch (currentStyle) {
                case THREADS -> ParticleTypes.SPIT;
                default -> ParticleTypes.SWEEP_ATTACK;
            };

            dealDirectionalDamage(user, 3.0, 4.0f, 0.7);
            spawnParticleArc(user, particle, 2.0, 120, 20);
        });
    }

    // =================================================================================
    // Particle Spawning Methods
    // =================================================================================

    /**
     * Spawns particles in a straight line.
     */
    public static void spawnLineParticles(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, int count) {
        int segments = count - 1;
        if (segments <= 0) {
            level.sendParticles(particle, start.x, start.y, start.z, 1, 0, 0, 0, 0);
            return;
        }
        for (int i = 0; i <= segments; i++) {
            Vec3 point = start.lerp(end, (double) i / segments);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawns a particle arc with a specific "roll" or tilt.
     */
    public static void spawnParticleArc(LivingEntity user, ParticleOptions particle, double radius, double arcDegrees, int particleCount, float rollDegrees) {
        ServerLevel level = (ServerLevel) user.getLevel();
        Vec3 eyePos = user.getEyePosition();
        Vec3 lookVec = user.getLookAngle();

        // Create a quaternion for the roll rotation around the look vector
        Quaternion rollQuat = new Quaternion(new Vector3f(lookVec), rollDegrees, true);

        for (int i = 0; i <= particleCount; i++) {
            double progress = (double)i / particleCount;
            double angle = Math.toRadians(-(arcDegrees / 2) + (arcDegrees * progress));

            // 1. Start with the forward vector
            Vec3 particleVec = lookVec;

            // 2. Rotate it sideways for the arc
            particleVec = particleVec.yRot((float)angle);

            // 3. Apply the roll rotation using the quaternion
            Vector3f tempVec = new Vector3f((float)particleVec.x, (float)particleVec.y, (float)particleVec.z);
            tempVec.transform(rollQuat);
            particleVec = new Vec3(tempVec.x(), tempVec.y(), tempVec.z());

            // 4. Add a slight upward curve
            double yOffset = Math.sin(progress * Math.PI) * 0.5;

            // 5. Calculate final position
            Vec3 particlePos = eyePos.add(particleVec.scale(radius)).add(0, yOffset, 0);

            level.sendParticles(particle, particlePos.x, particlePos.y, particlePos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * Overloaded method to spawn a particle arc with a RANDOMIZED roll.
     */
    public static void spawnParticleArc(LivingEntity user, ParticleOptions particle, double radius, double arcDegrees, int particleCount) {
        // Generate a random roll from -45 to +45 degrees.
        float randomRoll = (user.level.random.nextFloat() * 90.0F) - 45.0F;

        // Call the more specific method with our random roll.
        spawnParticleArc(user, particle, radius, arcDegrees, particleCount, randomRoll);
    }

    /**
     * Spawns a particle arc at a specific point in the world with a given yaw and pitch.
     */
    public static void spawnParticleArcInWorld(ServerLevel level, Vec3 center, double radius, double arcDegrees, int particleCount, float yaw, float pitch, ParticleOptions particle) {
        float yawRad = -yaw * Mth.DEG_TO_RAD;
        float pitchRad = -pitch * Mth.DEG_TO_RAD;

        for (int i = 0; i < particleCount; i++) {
            double angle = Math.toRadians((180.0 - arcDegrees) / 2 + (arcDegrees / (particleCount - 1)) * i);
            Vec3 particlePos = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(radius);
            particlePos = particlePos.xRot(pitchRad);
            particlePos = particlePos.yRot(yawRad);
            level.sendParticles(particle,
                    center.x + particlePos.x,
                    center.y + particlePos.y,
                    center.z + particlePos.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void spawnParticleStreamWithWeave(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, double weaveAmplitude, double weaveFrequency, int particleCount) {
        int segments = particleCount - 1;
        if (segments <= 0) return;

        Vec3 direction = start.vectorTo(end);
        Vec3 sideVector = direction.cross(new Vec3(0, 1, 0)).normalize();
        if (sideVector.lengthSqr() < 0.01) {
            sideVector = new Vec3(1, 0, 0);
        }

        for (int i = 0; i <= segments; i++) {
            double progress = (double) i / segments;
            Vec3 point = start.lerp(end, progress);
            double weaveOffset = Math.sin(progress * Math.PI * weaveFrequency) * weaveAmplitude;

            level.sendParticles(particle,
                    point.x + sideVector.x * weaveOffset,
                    point.y + sideVector.y * weaveOffset,
                    point.z + sideVector.z * weaveOffset,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a swirling particle effect around a central point.
     */
    public static void spawnCycloneParticles(ServerLevel level, ParticleOptions particle1, ParticleOptions particle2, Vec3 center, double radius, double height, int count, int tick) {
        for (int i = 0; i < count; i++) {
            double angle = (tick * 0.4) + (i * (2 * Math.PI / count));
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            double yOffset = (level.random.nextDouble() - 0.5) * height;

            Vec3 particlePos = center.add(xOffset, yOffset, zOffset);
            level.sendParticles(i % 3 == 0 ? particle2 : particle1, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a vortex particle effect at a location, pulling inwards.
     */
    public static void spawnVortexParticles(ServerLevel level, BlockPos targetPos, double radius, int count, int tick) {
        if (tick % 5 == 0) {
            level.playSound(null, targetPos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 2.0f, 1.0f);
        }

        for (int i = 0; i < count; i++) {
            double angle = (i * (2 * Math.PI / count)) + (tick * 0.1);
            Vec3 particlePos = Vec3.atCenterOf(targetPos).add(Math.cos(angle) * radius, 0.5 + Math.sin(angle * 5) * 0.2, Math.sin(angle) * radius);
            Vec3 velocity = Vec3.atCenterOf(targetPos).subtract(particlePos).normalize().scale(0.8);
            level.sendParticles(ParticleTypes.CLOUD, particlePos.x, particlePos.y, particlePos.z, 1, velocity.x, velocity.y, velocity.z, 0.1);
        }
    }


    // =================================================================================
    // World Interaction Methods
    // =================================================================================

    /**
     * Carves a trench in the ground, setting it on fire.
     */
    public static void carveFieryTrench(ServerLevel level, Vec3 startPos, Vec3 endPos,  int width, int depth) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) {
            return;
        }

        int segments = (int) startPos.distanceTo(endPos);
        if (segments <= 0 || width <= 0 || depth <= 0) return;

        Vec3 direction = endPos.subtract(startPos).normalize();

        for (int i = 0; i < segments; i++) {
            Vec3 currentPoint = startPos.add(direction.scale(i));
            BlockPos centerPos = new BlockPos(currentPoint);

            int radius = width / 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerPos.offset(x, 0, z));

                    for(int d = 1; d <= depth; d++) {
                        BlockPos currentTrenchPos = groundPos.below(d);
                        BlockState currentState = level.getBlockState(currentTrenchPos);

                        if (currentState.getDestroySpeed(level, currentTrenchPos) < 0) {
                            continue;
                        }

                        if (d < depth) {
                            level.setBlock(currentTrenchPos, Blocks.AIR.defaultBlockState(), 3);
                        } else {
                            level.setBlock(currentTrenchPos, level.random.nextFloat() > 0.6f ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
                        }

                        BlockPos newGroundPos = groundPos.below(depth);
                        BlockPos firePos = newGroundPos.above();
                        if (level.getBlockState(firePos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                    BlockPos pos1 = groundPos.below();
                    BlockPos pos2 = groundPos.below(2);

                    if(level.getBlockState(pos1).is(Blocks.BEDROCK)) { continue; }
                    if (!level.getBlockState(pos1).isAir()) {
                        level.setBlock(pos1, Blocks.AIR.defaultBlockState(), 3);
                    }
                    if (!level.getBlockState(pos2).isAir()) {
                        level.setBlock(pos2, level.random.nextFloat() > 0.6f ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
                    }

                    BlockPos firePos = pos2.above();
                    if (level.getBlockState(firePos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                        level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Destroys blocks in a radius around a center point.
     */
    public static void destroyBlocksInRadius(ServerLevel level, BlockPos center, double horizontalRadius, int verticalRadius, boolean dropBlocks, Entity owner) {
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING)) {
            return;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int hRadius = Mth.ceil(horizontalRadius);

        for (int x = -hRadius; x <= hRadius; x++) {
            for (int z = -hRadius; z <= hRadius; z++) {
                if (x == 0 && z == 0) continue; // Skip column directly under center
                if (x * x + z * z <= horizontalRadius * horizontalRadius) {
                    for (int y = -verticalRadius; y <= verticalRadius; y++) {
                        if (level.random.nextFloat() < 0.4f) {
                            mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                            if (level.getBlockState(mutablePos).getDestroySpeed(level, mutablePos) >= 0) {
                                level.destroyBlock(mutablePos, dropBlocks, owner);
                            }
                        }
                    }
                }
            }
        }
    }

    // =================================================================================
    // Player Movement & State
    // =================================================================================

    /**
     * Pushes the player forward based on their look vector.
     */
    public static void pushPlayer(LivingEntity user, double speed, boolean maintainVertical) {
        Vec3 lookVec = user.getLookAngle();
        double yVel = maintainVertical ? user.getDeltaMovement().y : lookVec.y * speed;
        Vec3 velocity = new Vec3(lookVec.x * speed, yVel, lookVec.z * speed);
        user.setDeltaMovement(velocity);
        user.hurtMarked = true;
    }

    /**
     * Temporarily makes the player immobile and immune to knockback.
     */
    public static void applyImmobility(LivingEntity user, int durationTicks) {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            if (attribute.getModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID) == null) {
                AttributeModifier modifier = new AttributeModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID, "Form knockback immunity", 1.0D, AttributeModifier.Operation.ADDITION);
                attribute.addTransientModifier(modifier);
            }
        }
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 255, false, false, false));
        user.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, -128, false, false, false));
    }

    /**
     * Removes the immobility effect.
     */
    public static void removeImmobility(LivingEntity user) {
        AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            AttributeModifier modifier = new AttributeModifier(ActiveFormManager.KNOCKBACK_IMMUNITY_UUID, "Form knockback immunity", 1.0D, AttributeModifier.Operation.ADDITION);
            if (!attribute.hasModifier(modifier)) {
                attribute.addTransientModifier(modifier);
            }
        }
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        user.removeEffect(MobEffects.JUMP);
    }


    // =================================================================================
    // General Utilities
    // =================================================================================

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

    /**
     * A more robust line-of-sight check that considers the entity's bounding box.
     */
    private static boolean isEntityInLineOfSight(Player player, Entity entity, AABB boundingBox) {
        return player.hasLineOfSight(entity) || entity.getBoundingBox().intersects(boundingBox);
    }

    /**
     * Plays a sound and swings the player's hand.
     */
    public static void performGenericAttack(LivingEntity user, SoundEvent sound, float volume, float pitch) {
        user.getLevel().playSound(null, user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        user.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * Finds the living entity the player is looking at within a given range.
     * @return The entity, or null if no entity is found.
     */
    @Nullable
    public static LivingEntity getLookedAtEntity(LivingEntity user, double range) {
        List<Entity> entities = user.level.getEntities(user, user.getBoundingBox().expandTowards(user.getLookAngle().scale(range)).inflate(1.0D), e -> e instanceof  LivingEntity && !e.isSpectator());
        LivingEntity closestEntity = null;
        double closestDist = Double.MAX_VALUE;

        Vec3 eyePos = user.getEyePosition();
        for(Entity entity : entities)  {
            if (entity instanceof LivingEntity livingTarget) {
                double dist = livingTarget.getEyePosition().distanceToSqr(eyePos);
                if(dist < closestDist && user.hasLineOfSight(livingTarget)) {
                    closestDist = dist;
                    closestEntity = livingTarget;
                }
            }
        }
        return closestEntity;
    }

    /**
     * Checks if the player is holding a NichirinSwordItem in either hand.
     */
    public static boolean isPlayerHoldingNichirin(Player player) {
        return player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                player.getOffhandItem().getItem() instanceof NichirinSwordItem;
    }

    /**
     * Rotates a vector by a given pitch and yaw.
     * @param vec The vector to rotate.
     * @param pitch The pitch in degrees.
     * @param yaw The yaw in degrees.
     * @return The rotated vector.
     */
    private static Vec3 rotateVec(Vec3 vec, float pitch, float yaw) {
        float p_rad = pitch * Mth.DEG_TO_RAD;
        float y_rad = yaw * Mth.DEG_TO_RAD;
        vec = vec.xRot(p_rad);
        vec = vec.yRot(-y_rad); // We use negative yaw for standard rotation
        return vec;
    }


    // =================================================================================
    // Active Form Manager
    // =================================================================================

    @Mod.EventBusSubscriber(modid = "demonslayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ActiveFormManager {
        private static final Map<UUID, ActiveForm> ACTIVE_FORMS = new HashMap<>();
        private static final Map<BreathingForms, BiFunction<LivingEntity, BreathingForms, ActiveForm>> FORM_FACTORIES;
        public static final UUID KNOCKBACK_IMMUNITY_UUID = UUID.fromString("d8a6a6dc-34d3-4e42-8c19-3c871343399b");

        // --- Manager Public API ---

        public static void start(LivingEntity user, BreathingForms form) {
            // Prevent starting a new form if one is already active that shouldn't be interrupted.
            if (ACTIVE_FORMS.containsKey(user.getUUID()) && !ACTIVE_FORMS.get(user.getUUID()).isInterruptible()) {
                return;
            }
            stop(user); // Stop any previous form

            BiFunction<LivingEntity, BreathingForms, ActiveForm> factory = FORM_FACTORIES.get(form);
            if (factory != null) {
                ActiveForm activeForm = factory.apply(user, form);
                ACTIVE_FORMS.put(user.getUUID(), activeForm);
                activeForm.onStart();
            }
        }

        public static void stop(LivingEntity user) {
            ActiveForm form = ACTIVE_FORMS.remove(user.getUUID());
            if (form != null) {
                form.onStop();
            }
        }

        public static void startCharge(LivingEntity user, BreathingForms form) {
            if (form.getInputType() != BreathingForms.InputType.HOLD) return;
            start(user, form);
        }

        public static void releaseCharge(LivingEntity user, boolean wasFullyCharged) {
            ActiveForm activeForm = ACTIVE_FORMS.get(user.getUUID());
            if (activeForm instanceof ChargeableForm chargeable) {
                chargeable.release(wasFullyCharged);
            }
        }

        public static ActiveForm getActiveForm(LivingEntity user) {
            return ACTIVE_FORMS.get(user.getUUID());
        }

        // --- Event Handlers ---

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                ActiveForm activeForm = ACTIVE_FORMS.get(player.getUUID());
                if (activeForm != null) {
                    activeForm.tick();
                }
            }
        }

        @SubscribeEvent
        public static void onEntityHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            if (target.hasEffect(ModEffects.FORCED_SLEEP.get())) {
                UUID casterUUID = ForcedSleepEffect.getCasterUUID(target);
                Entity attacker = event.getSource().getEntity();

                if (attacker != null && !attacker.getUUID().equals(casterUUID)) {
                    target.removeEffect(ModEffects.FORCED_SLEEP.get());
                    target.level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.5f);
                    event.setCanceled(true);
                    return;
                } else {
                    event.setCanceled(true);
                    return;
                }
            }

            if (target instanceof ServerPlayer player) {
                ActiveForm activeForm = ACTIVE_FORMS.get(player.getUUID());
                if (activeForm != null) {
                    activeForm.onHurt(event);
                }
            }

            ActiveForm activeForm = getActiveForm(target);
            if (activeForm instanceof CrystallineDivineChildForm) {
                Level level = target.level;
                List<CrystallineDivineChildEntity> clones = level.getEntitiesOfClass(CrystallineDivineChildEntity.class,
                        target.getBoundingBox().inflate(128), e -> e.isOwner(target) && e.isAlive());

                if (!clones.isEmpty()) {
                    CrystallineDivineChildEntity unluckyClone = clones.get(level.random.nextInt(clones.size()));
                    float originalDamage = event.getAmount();
                    event.setCanceled(true);
                    unluckyClone.hurt(event.getSource(), originalDamage * 4.0F);

                    level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD, target.getEyePosition().x, target.getEyePosition().y, target.getEyePosition().z, 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    return;
                }
            }
        }

        // --- Static Initializer for Form Factories ---

        static {
            FORM_FACTORIES = new ImmutableMap.Builder<BreathingForms, BiFunction<LivingEntity, BreathingForms, ActiveForm>>()
                    // WATER
                    .put(BreathingForms.WATER_2, WaterWheelForm::new)
                    .put(BreathingForms.WATER_3, FlowingDanceForm::new)
                    .put(BreathingForms.WATER_6, WhirlpoolForm::new)
                    .put(BreathingForms.WATER_8, WaterfallBasinForm::new)
                    .put(BreathingForms.WATER_10, ConstantFluxForm::new)
                    .put(BreathingForms.WATER_11, DeadCalmParry::new)
                    // FLAME
                    .put(BreathingForms.FLAME_1, UnknowingFireForm::new)
                    .put(BreathingForms.FLAME_3, BlazingUniverseForm::new)
                    .put(BreathingForms.FLAME_4, FlameUndulationParry::new)
                    .put(BreathingForms.FLAME_5, FlameTigerForm::new)
                    .put(BreathingForms.FLAME_9, RengokuForm::new)
                    // WIND
                    .put(BreathingForms.WIND_1, DustWhirlwindCutterForm::new)
                    .put(BreathingForms.WIND_3, ClearStormWindTreeParry::new)
                    .put(BreathingForms.WIND_4, RisingDustStormForm::new)
                    .put(BreathingForms.WIND_5, ColdMountainWindForm::new)
                    .put(BreathingForms.WIND_7, GaleSuddenGustsForm::new)
                    .put(BreathingForms.WIND_8, PrimaryGaleSlashForm::new)
                    .put(BreathingForms.WIND_9, IdatenTyphoonForm::new)
                    // THUNDER
                    .put(BreathingForms.THUNDER_1, ThunderclapAndFlashForm::new)
                    .put(BreathingForms.THUNDER_2, RiceSpiritForm::new)
                    .put(BreathingForms.THUNDER_3, ThunderSwarmForm::new)
                    .put(BreathingForms.THUNDER_4, DistantThunderForm::new)
                    .put(BreathingForms.THUNDER_6, RumbleAndFlashForm::new)
                    .put(BreathingForms.THUNDER_7, HonoikazuchiNoKamiForm::new)
                    // MIST
                    .put(BreathingForms.MIST_2, EightLayeredMistForm::new)
                    .put(BreathingForms.MIST_3, ScatteringMistParry::new)
                    .put(BreathingForms.MIST_4, ShiftingFlowSlashForm::new)
                    .put(BreathingForms.MIST_5, SeaOfCloudsForm::new)
                    .put(BreathingForms.MIST_6, LunarDispersingMist::new)
                    // SOUND
                    .put(BreathingForms.SOUND_1, RoarForm::new)
                    .put(BreathingForms.SOUND_4, ResoundingSlashesForm::new)
                    .put(BreathingForms.SOUND_5, StringPerformanceForm::new)
                    // LOVE
                    .put(BreathingForms.LOVE_1, ShiversOfFirstLoveForm::new)
                    .put(BreathingForms.LOVE_2, LovePangsForm::new)
                    .put(BreathingForms.LOVE_3, CatloveShowerForm::new)
                    .put(BreathingForms.LOVE_5, SwayingLoveWildclawForm::new)
                    .put(BreathingForms.LOVE_6, CatLeggedWindsForm::new)
                    // INSECT
                    .put(BreathingForms.INSECT_1, ButterflyDanceCapriceForm::new)
                    .put(BreathingForms.INSECT_2, BeeStingFlutterForm::new)
                    .put(BreathingForms.INSECT_3, DragonflyDanceForm::new)
                    .put(BreathingForms.INSECT_4, CentipedeDanceForm::new)
                    // STONE
                    .put(BreathingForms.STONE_1, SerpentiniteBipolarForm::new)
                    .put(BreathingForms.STONE_2, UpperSmashForm::new)
                    .put(BreathingForms.STONE_3, StoneSkinParry::new)
                    .put(BreathingForms.STONE_4, VolcanicRockForm::new)
                    .put(BreathingForms.STONE_5, ArcsOfJusticeForm::new)
                    // FLOWER
                    .put(BreathingForms.FLOWER_2, HonorableShadowPlumParry::new)
                    .put(BreathingForms.FLOWER_6, WhirlingPeachForm::new)
                    .put(BreathingForms.FLOWER_7, VermilionEyeForm::new)
                    // SERPENT
                    .put(BreathingForms.SERPENT_2, VenomFangsForm::new)
                    .put(BreathingForms.SERPENT_3, CoilChokeForm::new)
                    .put(BreathingForms.SERPENT_5, SlitheringSerpentForm::new)
                    // BDA, Rui
                    .put(BreathingForms.THREADS_1, CuttingThreadCageForm::new)
                    .put(BreathingForms.THREADS_2, MurderousEyeBasketForm::new)
                    .put(BreathingForms.THREADS_3, CuttingThreadRotationForm::new)
                    // BDA, Enmu
                    .put(BreathingForms.SLEEP_1, WhispersHypnosisForm::new)
                    .put(BreathingForms.SLEEP_2, EyesOfSleepForm::new)
                    // BDA, Daki
                    .put(BreathingForms.SASH_1, EightLayeredObiSlashForm::new)
                    // BDA, Gyutaro
                    .put(BreathingForms.HAEMO_1, FlyingBloodSicklesForm::new)
                    .put(BreathingForms.HAEMO_2, RampantArcRampageParry::new)
                    .put(BreathingForms.HAEMO_3, RotatingCircularSlashesForm::new)
                    // BDA, Gyokko
                    .put(BreathingForms.FISH_1, ThousandNeedleFishKillForm::new)
                    .put(BreathingForms.FISH_2, WaterPrisonPotForm::new)
                    .put(BreathingForms.FISH_3, OctopusVaseHellForm::new)
                    .put(BreathingForms.FISH_4, TenThousandFishBarrageForm::new)
                    .put(BreathingForms.FISH_5, KillerFishScalesForm::new)
                    // BDA, Hantengu
                    .put(BreathingForms.EMOTION_1, WeepingSpearsForm::new)
                    .put(BreathingForms.EMOTION_2, CrazedCryOfThunderdeathForm::new)
                    .put(BreathingForms.EMOTION_3, CountlessStrikingTreesForm::new)
                    .put(BreathingForms.EMOTION_4, CompressedSoundWavesForm::new)
                    // BDA, Akaza
                    .put(BreathingForms.DESTRUCTIVE_DEATH_1, CompassNeedleForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_2, AirTypeForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_3, DisorderForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_4, AnnihilationTypeForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_5, CrownSplitterForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_6, ExplosiveFlurryForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_7, FlyingPlanetThousandWheelsForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_8, EightLayeredDemonCoreForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_9, TenThousandLeavesWillowForm::new)
                    .put(BreathingForms.DESTRUCTIVE_DEATH_10, FinalFormChaoticAfterglowForm::new)
                    // BDA, Doma
                    .put(BreathingForms.CRYO_1, FrozenLotusForm::new)
                    .put(BreathingForms.CRYO_2, BarrenHangingGardenForm::new)
                    .put(BreathingForms.CRYO_3, FreezingCloudsForm::new)
                    .put(BreathingForms.CRYO_4, LotusVinesForm::new)
                    .put(BreathingForms.CRYO_5, ColdWhitePrincessesForm::new)
                    .put(BreathingForms.CRYO_6, WintryIciclesForm::new)
                    .put(BreathingForms.CRYO_7, ScatteringLotusesForm::new)
                    .put(BreathingForms.CRYO_8, CrystallineDivineChildForm::new)
                    .build();
        }
    }


    // =================================================================================
    // ActiveForm Base Classes
    // =================================================================================

    public static abstract class ActiveForm {
        protected final LivingEntity user;
        protected final ServerLevel level;
        protected final BreathingForms form;
        protected final int startTick;
        protected int ticksActive;

        public ActiveForm(LivingEntity user, BreathingForms form) {
            this.user = user;
            this.level = (ServerLevel) user.getLevel();
            this.form = form;
            this.startTick = user.tickCount;
            this.ticksActive = 0;
        }

        public final void tick() {
            if(user instanceof ServerPlayer player) {
                boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                        .map(PlayerDemon::isDemon)
                        .orElse(false);

                // Is anyone trying to use a breathing style without a sword?
                if (!isPlayerHoldingNichirin(player) && !this.form.IsBDA()) {
                    ActiveFormManager.stop(player);
                    return;
                }
                // Is a human trying to use BDA?
                if (this.form.IsBDA() && !isDemon) {
                    ActiveFormManager.stop(player);
                    return;
                }
            }

            this.ticksActive = user.tickCount - startTick;
            onTick();
        }

        // --- Methods to be overridden by implementations ---
        protected void onStart() {}
        protected abstract void onTick();
        protected void onStop() {}
        public void onHurt(LivingHurtEvent event) {}
        public boolean isInterruptible() { return true; }
        public BreathingForms getForm() { return form; }
    }

    public static abstract class TimedForm extends ActiveForm {
        protected final int duration;
        public TimedForm(LivingEntity user, BreathingForms form, int durationTicks) {
            super(user, form);
            this.duration = durationTicks;
        }
        @Override
        protected void onTick() {
            if (ticksActive >= duration) {
                ActiveFormManager.stop(user);
            }
        }
    }

    public static abstract class ParryForm extends TimedForm {
        public ParryForm(LivingEntity user, BreathingForms form, int parryWindow) {
            super(user, form, parryWindow);
        }

        @Override
        public void onHurt(LivingHurtEvent event) {
            if (isParryable(event.getSource())) {
                event.setCanceled(true);
                onParrySuccess(event.getSource().getDirectEntity());
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 2.0f);
        }

        protected abstract void onParrySuccess(Entity attacker);
    }

    public static abstract class ChargeableForm extends ActiveForm {
        protected final int chargeGoal;
        protected int chargeTicks = 0;
        protected boolean released = false;

        public ChargeableForm(LivingEntity user, BreathingForms form) {
            super(user, form);
            this.chargeGoal = form.getChargeTicks();
        }

        @Override
        protected void onStart() {
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, chargeGoal + 40, 255, false, false));
            onChargeStart();
        }

        @Override
        protected void onTick() {
            if (!released) {
                if (chargeTicks < chargeGoal) {
                    chargeTicks++;
                    onChargingTick();
                }
            } else {
                onReleaseTick();
            }
        }

        @Override
        protected void onStop() {
            user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            onChargeEnd();
        }

        public void release(boolean wasFullyCharged) {
            this.released = true;
            this.ticksActive = 0; // Reset ticks for the release phase
            user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            onChargeRelease(wasFullyCharged);
        }

        protected abstract void onChargeStart();
        protected abstract void onChargingTick();
        protected abstract void onChargeRelease(boolean fullyCharged);
        protected abstract void onReleaseTick();
        protected abstract void onChargeEnd();
    }


    // =================================================================================
    // Concrete Form Implementations
    // =================================================================================

    // --- WATER FORMS ---
    public static class WaterWheelForm extends ActiveForm {
        public WaterWheelForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

            Vec3 lookVec = user.getLookAngle();
            Vec3 forwardMotion = new Vec3(lookVec.x, 0, lookVec.z).normalize().scale(0.8);
            user.setDeltaMovement(user.getDeltaMovement().add(forwardMotion.x, 0.8, forwardMotion.z));
            user.hurtMarked = true;

            Vec3 playerCenter = user.position().add(0, user.getBbHeight() / 2.0, 0);
            double radius = 2.2;
            int particleCount = 60;

            Vec3 forwardVector = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();

            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI / particleCount) * i;
                Vec3 verticalOffset = new Vec3(0, 1, 0).scale(radius * Math.cos(angle));
                Vec3 longitudinalOffset = forwardVector.scale(radius * Math.sin(angle));
                Vec3 particlePos = playerCenter.add(verticalOffset).add(longitudinalOffset);
                level.sendParticles(ParticleTypes.SPLASH, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0.05);
                if (i % 2 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0.05);
                }
            }
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 5.0, 8.0f, 0.5f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), 5.0, 8.0f, 0.5f, DamageSource.mobAttack(user));
            }
        }

        @Override
        protected void onTick() {
            // Do nothing.
        }
    }

    public static class FlowingDanceForm extends TimedForm {
        public FlowingDanceForm(LivingEntity user, BreathingForms form) { super(user, form, 50); }
        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        }
        @Override
        protected void onTick() {
            super.onTick(); // Handles timeout
            if (ticksActive % 5 == 0) {
                pushPlayer(user, 1.8, true);
                dealDirectionalDamage(user, 3.0, 6.0f, 0.5);
                spawnParticleArc(user, ParticleTypes.SPLASH, 1.5, 90, 10);
            }
        }
    }

    public static class WhirlpoolForm extends TimedForm {
        private float startYaw;
        public WhirlpoolForm(LivingEntity user, BreathingForms form) { super(user, form, 30); }
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            this.startYaw = user.getYRot();
            user.setDeltaMovement(user.getDeltaMovement().add(0, 1.2, 0));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 1.0f);
            applyImmobility(user, duration);
        }
        @Override
        protected void onTick() {
            super.onTick();
            float progress = (float) ticksActive / duration;
            float newYaw = startYaw + (360 * 3 * progress);
            user.setYRot(newYaw);
            user.setYHeadRot(newYaw);

            spawnParticleArc(user, ParticleTypes.SPLASH, 2.5, 360, 20);
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 3.0, 3.0f, 1.2f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), 3.0, 3.0f, 1.2f, DamageSource.mobAttack(user));
            }
        }
        @Override protected void onStop() { removeImmobility(user); }
    }

    private static class WaterfallBasinForm extends ActiveForm {
        public WaterfallBasinForm(LivingEntity user, BreathingForms form) { super(user, form); }
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onTick() {
            if (user.isOnGround() || user.isInWater()) {
                float fallDistance = user.fallDistance;
                float damage = 5.0f + Mth.clamp(fallDistance, 0, 50) * 0.8f;
                double radius = Mth.clamp(2.0 + fallDistance * 0.15, 2.0, 10.0);
                float knockback = 0.5f + fallDistance * 0.05f;

                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), radius, damage, knockback, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), radius, damage, knockback, DamageSource.mobAttack(user));
                }
                level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, user.getX(), user.getY(), user.getZ(), 1, 0, 0, 0, 0);

                final DustParticleOptions waterMist = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 1.0f), 1.2f);
                int particleCount = (int)(40 * radius); // More particles for a bigger splash

                for (int i = 0; i < particleCount; i++) {
                    // Splash particles that fly outwards from the impact point
                    double motionX = (level.random.nextDouble() - 0.5) * radius * 0.4;
                    double motionY = level.random.nextDouble() * 0.6;
                    double motionZ = (level.random.nextDouble() - 0.5) * radius * 0.4;
                    level.sendParticles(ParticleTypes.SPLASH, user.getX(), user.getY() + 0.2, user.getZ(), 0, motionX, motionY, motionZ, 0.7);

                    // Blue mist particles that rise up slowly
                    if (i % 4 == 0) { // Spawn fewer mist particles
                        double mistX = user.getX() + (level.random.nextDouble() - 0.5) * radius;
                        double mistY = user.getY() + level.random.nextDouble() * 1.5;
                        double mistZ = user.getZ() + (level.random.nextDouble() - 0.5) * radius;
                        level.sendParticles(waterMist, mistX, mistY, mistZ, 0, 0, 0.1, 0, 1);
                    }
                }

                ActiveFormManager.stop(user);
            }
        }
    }

    public static class ConstantFluxForm extends ActiveForm {
        private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.7f, 1.0f), 1.2f);
        public ConstantFluxForm(LivingEntity user, BreathingForms form) { super(user, form); }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        @Override
        protected void onTick() {
            if (!user.isSprinting()) {
                ActiveFormManager.stop(user);
                return;
            }
            int speedAmp = Math.min(ticksActive / 40, 4);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, speedAmp, false, false, false));

            int damageInterval = Math.max(20 - (ticksActive / 10), 5);
            if (ticksActive % damageInterval == 0) {
                float damage = Math.min(6.0f + (ticksActive / 25f), 15.0f);
                dealDirectionalDamage(user, 3.0, damage, 0.7);
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.5f);
            }
            // Particle Dragon
            int particleCount = Math.min(5 + (ticksActive / 10), 30);
            for (int i = 0; i < particleCount; i++) {
                double angle = (user.tickCount + i * 18) * 0.1;
                double radius = 1.0 + Mth.clamp(ticksActive / 100.0, 0, 2.0);
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;
                double yOffset = Math.sin((user.tickCount + i * 45) * 0.05) * 0.5 + 1.0;
                level.sendParticles(BLUE_DUST, user.getX() + xOffset, user.getY() + yOffset, user.getZ() + zOffset, 1, 0, 0, 0, 0);
            }
        }
        @Override
        protected void onStop() {
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }

    private static class DeadCalmParry extends ParryForm {
        public DeadCalmParry(LivingEntity user, BreathingForms form) { super(user, form, 30); }
        @Override
        protected void onParrySuccess(Entity attacker) {
            applyImmobility(user, 10);
            level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 2.0f);
            level.sendParticles(ParticleTypes.CRIT, user.getX(), user.getEyeY(), user.getZ(), 15, 0.3, 0.3, 0.3, 0.5);
            if (attacker instanceof LivingEntity living) {
                living.knockback(1.0, user.getX() - living.getX(), user.getZ() - living.getZ());
            }
        }
        @Override protected void onStop() { removeImmobility(user); }
    }

    // --- FLAME FORMS ---
    private static class UnknowingFireForm extends TimedForm {
        private final Vec3 direction;
        private static final int DASH_DURATION = 8;
        private static final int SLASH_DELAY = 3;
        public UnknowingFireForm(LivingEntity user, BreathingForms form) {
            super(user, form, DASH_DURATION + SLASH_DELAY + 2);
            this.direction = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        }
        @Override public boolean isInterruptible() { return false; }
        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
        @Override
        protected void onTick() {
            super.onTick();
            if (ticksActive < DASH_DURATION) {
                user.setDeltaMovement(direction.scale(2.5));
                user.hurtMarked = true;
                level.sendParticles(ParticleTypes.SMOKE, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0.1);

                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 1.5, 2.0f, 0.1f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), 1.5, 2.0f, 0.1f, DamageSource.mobAttack(user));
                }
            }
                else if (ticksActive == DASH_DURATION) {
                user.setDeltaMovement(Vec3.ZERO);
                user.hurtMarked = true;
            } else if (ticksActive == DASH_DURATION + SLASH_DELAY) {
                float yaw = (float)(Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90.0F;
                user.setYRot(yaw);
                user.setYHeadRot(yaw);
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                dealDirectionalDamage(user, 3.0, 8.0f, 0.7);
                spawnParticleArc(user, ParticleTypes.FLAME, 2.0, 140, 30);
            }
        }
    }

    private static class BlazingUniverseForm extends ActiveForm {
        public BlazingUniverseForm(LivingEntity user, BreathingForms form) { super(user, form); }
        @Override public boolean isInterruptible() { return false; }
        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
        @Override
        protected void onTick() {
            user.setDeltaMovement(user.getDeltaMovement().add(0, -0.2, 0)); // a bit more gravity
            if (user.isOnGround()) {
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 4.0, 10.0f, 0.8f, DamageSource.playerAttack(player));
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 50, 2.0, 0.5, 2.0, 0);
                    level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 100, 2.0, 0.5, 2.0, 0.1);
                    ActiveFormManager.stop(player);
                } else {
                    dealAOEDamage(user, user.position(), 4.0, 10.0f, 0.8f, DamageSource.mobAttack(user));
                    level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    level.sendParticles(ParticleTypes.LAVA, user.getX(), user.getY() + 0.5, user.getZ(), 50, 2.0, 0.5, 2.0, 0);
                    level.sendParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 0.5, user.getZ(), 100, 2.0, 0.5, 2.0, 0.1);
                    ActiveFormManager.stop(user);
                }
            }
        }
    }

    private static class FlameUndulationParry extends ParryForm {
        public FlameUndulationParry(LivingEntity user, BreathingForms form) { super(user, form, 30); }
        @Override
        protected void onParrySuccess(Entity attacker) {
            applyImmobility(user, 10);
            performGenericAttack(user, SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 4.0, 10.0f, 0.8f, DamageSource.playerAttack(player));
                spawnParticleArc(player, ParticleTypes.FLAME, 4.0, 360, 60);
                level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 20, 1.5, 0.5, 1.5, 0);
            } else {
                dealAOEDamage(user, user.position(), 4.0, 10.0f, 0.8f, DamageSource.mobAttack(user));
                spawnParticleArc(user, ParticleTypes.FLAME, 4.0, 360, 60);
                level.sendParticles(ParticleTypes.LAVA, user.getX(), user.getY() + 0.5, user.getZ(), 20, 1.5, 0.5, 1.5, 0);
            }
        }
        @Override protected void onStop() { removeImmobility(user); }
    }

    public static class FlameTigerForm extends ActiveForm {
        private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.0f), 1.0f);
        private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.1f, 0.1f), 1.2f);
        public FlameTigerForm(LivingEntity user, BreathingForms form) { super(user, form); }

        @Override protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
        @Override
        protected void onTick() {
            if (!user.isSprinting()) {
                ActiveFormManager.stop(user);
                return;
            }

            int speedAmplifier = Math.min(ticksActive / 40, 4);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, speedAmplifier, false, false, false));
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, ticksActive / 60, false, false));
            user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, false, false));

            if (ticksActive % 15 == 0) {
                float damage = 3.0f + (ticksActive / 50f);
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 2.5, damage, 0.2f, DamageSource.playerAttack(player));
                    level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.5f);
                } else {
                    dealAOEDamage(user, user.position(), 2.5, damage, 0.2f, DamageSource.mobAttack(user));
                    level.playSound(null, user.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.5f);
                }
            }
            // Particle aura
            int particleCount = Math.min(10 + (ticksActive / 20), 40);
            for (int i = 0; i < particleCount; i++) {
                double angle = (user.tickCount + i * 22) * 0.08;
                double r_x = Math.cos(angle) * (1.2 + (Math.random() - 0.5) * 0.4);
                double r_z = Math.sin(angle) * (1.2 + (Math.random() - 0.5) * 0.4);
                double yOffset = Math.random() * 2.0;
                level.sendParticles(i % 4 == 0 ? BLACK_DUST : ORANGE_DUST, user.getX() + r_x, user.getY() + yOffset, user.getZ() + r_z, 1, 0, 0, 0, 0);
            }
        }
        @Override
        protected void onStop() {
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.removeEffect(MobEffects.DAMAGE_BOOST);
            user.removeEffect(MobEffects.FIRE_RESISTANCE);
            level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.0f);
        }
    }

    private static class RengokuForm extends ChargeableForm {
        private static final int DASH_DURATION = 20;
        private Vec3 direction, prevPos;
        private int releaseTicks = 0;

        public RengokuForm(LivingEntity user, BreathingForms form) { super(user, form); }
        @Override public boolean isInterruptible() { return false; }
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
            if(fullyCharged) {
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
            if (releaseTicks < DASH_DURATION) {
                releaseTicks++;
                user.setDeltaMovement(direction.scale(3.5));
                user.hurtMarked = true;

                Vec3 currentPos = user.position();
                dealLineDamage(user, prevPos, currentPos, 35.0f, 10);
                carveFieryTrench(level, prevPos, currentPos, 3, 2);
                this.prevPos = currentPos;
            } else {
                ActiveFormManager.stop(user);
            }
        }
        @Override
        protected void onChargeEnd() {
            level.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
    }

    // --- WIND FORMS ---
    private static class DustWhirlwindCutterForm extends TimedForm {
        private static final DustParticleOptions WIND_DUST = new DustParticleOptions(new Vector3f(0.8f, 1.0f, 0.8f), 1.0f);
        private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.6f, 0.2f), 1.0f);
        private final Vec3 direction;
        public DustWhirlwindCutterForm(LivingEntity user, BreathingForms form) {
            super(user, form, 10);
            this.direction = Vec3.directionFromRotation(0, user.getYRot()).normalize();
        }
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        @Override
        protected void onTick() {
            super.onTick();
            user.setDeltaMovement(direction.scale(1.8));
            user.hurtMarked = true;
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 2.5, 5.0f, 0.4f, DamageSource.playerAttack(player));
                spawnCycloneParticles(level, WIND_DUST, GREEN_DUST, player.position().add(0, 1.1, 0), 2.0, 2.2, 8, ticksActive);
            } else {
                dealAOEDamage(user, user.position(), 2.5, 5.0f, 0.4f, DamageSource.mobAttack(user));
                spawnCycloneParticles(level, WIND_DUST, GREEN_DUST, user.position().add(0, 1.1, 0), 2.0, 2.2, 8, ticksActive);
            }
        }
        @Override
        protected void onStop() {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        }
    }

    private static class ClearStormWindTreeParry extends ParryForm {
        public ClearStormWindTreeParry(LivingEntity user, BreathingForms form) { super(user, form, 30); }

        @Override
        protected void onParrySuccess(Entity attacker) {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.5f);
            destroyBlocksInRadius(level, user.blockPosition(), 4.0, 1, false, user);
            if (attacker instanceof LivingEntity living) {
                living.knockback(2.5F, user.getX() - living.getX(), user.getZ() - living.getZ());
            }
            for(int i = 0; i < 100; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 3.0;
                double x = user.getX() + Math.cos(angle) * radius;
                double z = user.getZ() + Math.sin(angle) * radius;
                double y = user.getY() + Math.random() * 2.5;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static class RisingDustStormForm extends TimedForm {
        private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 1.5f);
        public RisingDustStormForm(LivingEntity user, BreathingForms form) { super(user, form, 80); }

        @Override
        protected void onStart() {
            user.setDeltaMovement(user.getDeltaMovement().add(0, -2.5, 0));
            user.hurtMarked = true;
            performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        }
        @Override
        protected void onTick() {
            super.onTick();
            BlockPos stormCenter = user.blockPosition();
            double radius = 4.0; double height = 15.0;
            AABB damageZone = new AABB(stormCenter).expandTowards(0, height, 0).inflate(radius, 0, radius);

            if (ticksActive % 10 == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());
                for (LivingEntity target : targets) {
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), 6.0f);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), 6.0f);
                    }
                    Vec3 pullDir = new Vec3(stormCenter.getX() + 0.5 - target.getX(), 2.0, stormCenter.getZ() + 0.5 - target.getZ()).normalize();
                    target.push(pullDir.x * 0.2, pullDir.y * 1.2, pullDir.z * 0.2);
                }
            }
            spawnCycloneParticles(level, ParticleTypes.CLOUD, GREEN_DUST, Vec3.atCenterOf(stormCenter), radius, height, 20, ticksActive);
        }
    }

    private static class ColdMountainWindForm extends TimedForm {
        private static final DustParticleOptions ICE_DUST = new DustParticleOptions(new Vector3f(0.7f, 0.9f, 1.0f), 1.0f);
        public ColdMountainWindForm(LivingEntity user, BreathingForms form) { super(user, form, 60); }

        @Override
        protected void onStart() {

            user.setDeltaMovement(user.getDeltaMovement().add(0, 2.5, 0));
            user.hurtMarked = true;
            performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        }
        @Override
        protected void onTick() {
            super.onTick();
            BlockPos playerPos = user.blockPosition();
            double radius = 5.0; double depth = 15.0;
            AABB damageZone = new AABB(playerPos).inflate(radius, 0, radius).move(0, -depth, 0);

            if (ticksActive % 10 == 0) {
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageZone, e -> e != user && e.isAlive());
                for (LivingEntity target : targets) {
                    if(user instanceof  ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), 8.0f);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), 8.0f);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    target.push(0, -1.5, 0);
                }
                level.playSound(null, playerPos, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            spawnCycloneParticles(level, ICE_DUST, ParticleTypes.SNOWFLAKE, user.position().add(0,-depth/2, 0), radius, depth, 25, -ticksActive);
        }
    }

    private static class GaleSuddenGustsForm extends ActiveForm {
        public GaleSuddenGustsForm(LivingEntity user, BreathingForms form) { super(user, form); }
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);
            pushPlayer(user, 5.0, false);
        }
        @Override
        protected void onTick() {
            if (user.isOnGround() || user.horizontalCollision) {
                ActiveFormManager.stop(user);
                return;
            }
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 3.5, 2.0f, 1.2f, DamageSource.playerAttack(player));
                spawnParticleArc(player, ParticleTypes.SWEEP_ATTACK, 3.5, 360, 5);
            } else {
                dealAOEDamage(user, user.position(), 3.5, 2.0f, 1.2f, DamageSource.mobAttack(user));
                spawnParticleArc(user, ParticleTypes.SWEEP_ATTACK, 3.5, 360, 5);
            }
            if (ticksActive % 8 == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
    }

    public static class PrimaryGaleSlashForm extends ActiveForm {
        private Vec3 pos, vel;
        private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.1f), 0.8f);

        public PrimaryGaleSlashForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.8f);
            user.setDeltaMovement(user.getLookAngle().scale(-1.8).add(0, 0.6, 0));
            user.hurtMarked = true;
            this.pos = user.getEyePosition();
            this.vel = user.getLookAngle().scale(3.0);
        }

        @Override
        protected void onTick() {
            // --- Projectile Simulation ---
                if (ticksActive > 40) {
                    ActiveFormManager.stop(user);
                    return;
                }

                Vec3 nextPos = pos.add(vel);
                vel = vel.add(0, -0.05, 0);
                AABB checkZone = new AABB(pos, nextPos).inflate(1.5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, checkZone, e -> e != user && e.isAlive());

                if (!targets.isEmpty() || !level.getBlockState(new BlockPos(nextPos)).isAir()) {
                    LivingEntity hitTarget = targets.isEmpty() ? null : targets.get(0);
                    if (hitTarget != null) {
                        if(user instanceof ServerPlayer player) {
                            hitTarget.hurt(DamageSource.playerAttack(player), 25.0f);
                        } else {
                            hitTarget.hurt(DamageSource.mobAttack(user), 25.0f);
                        }
                    }
                    level.playSound(null, new BlockPos(nextPos), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.5f);
                    level.sendParticles(ParticleTypes.EXPLOSION, nextPos.x, nextPos.y, nextPos.z, 1, 0, 0, 0, 0);

                    ActiveFormManager.stop(user);
                } else {
                    pos = nextPos;
                    spawnCycloneParticles(level, GREEN_DUST, ParticleTypes.CLOUD, pos, 1.2, 0, 25, ticksActive * 10);
                }
        }
    }

    public static class IdatenTyphoonForm extends ActiveForm {
        private final BlockPos targetPos;
        private static final int VORTEX_DURATION = 20;

        public IdatenTyphoonForm(LivingEntity user, BreathingForms form) {
            super(user, form);
            this.targetPos = user.blockPosition();
        }

        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
            user.setDeltaMovement(user.getLookAngle().scale(-1.8).add(0, 1.2, 0));
            user.hurtMarked = true;
        }

        @Override
        protected void onTick() {
            if (ticksActive <= VORTEX_DURATION) {
                // Vortex logic...
                double radius = 10.0;
                AABB effectZone = new AABB(targetPos).inflate(radius, 3, radius);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());
                for (LivingEntity target : targets) {
                    Vec3 pullDir = Vec3.atCenterOf(targetPos).subtract(target.position()).normalize();
                    target.push(pullDir.x * 0.5, pullDir.y * 0.5 + 0.2, pullDir.z * 0.5);
                }
                spawnVortexParticles(level, targetPos, radius, 50, ticksActive);
            } else if (ticksActive == VORTEX_DURATION + 1) {
                // Explosion logic...
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, Vec3.atCenterOf(targetPos), 10.0, 40.0f, 2.0f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, Vec3.atCenterOf(targetPos), 10.0, 40.0f, 2.0f, DamageSource.mobAttack(user));
                }
                level.playSound(null, targetPos, SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.2f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 3, 0, 0, 0, 0);

                ActiveFormManager.stop(user);
            }
        }
    }

    // --- THUNDER FORMS ---
    private static class ThunderclapAndFlashForm extends TimedForm {
        private static final int CHARGE_PAUSE_DURATION = 10;
        private static final int DASH_DURATION_TICKS = 6;
        private static final double DASH_SPEED = 4.5;
        private static final float ATTACK_DAMAGE = 15.0f;
        private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

        private Vec3 dashDirection;

        public ThunderclapAndFlashForm(LivingEntity user, BreathingForms form) {
            // We pass this form's total duration to the parent TimedForm.
            // It's the dash duration + 1 tick for the slash itself.
            super(user, form, CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS + 1);
        }

        // Don't interrupt me!
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            // The "Thunderclap" sound effect
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.2f);

            // Store the direction at the start so the player can't turn mid-dash.
            // We only care about the horizontal direction.
            this.dashDirection = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();

            // Prevent the player from being knocked around during the dash.
            applyImmobility(user, duration);
        }

        private void performSlash() {
            user.setDeltaMovement(Vec3.ZERO); // Stop the player
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
            dealThrustDamage(user, 5.0, ATTACK_DAMAGE);
            spawnParticleArc(user, YELLOW_LIGHTNING, 3.0, 140, 40);
        }

        @Override
        protected void onTick() {
//            super.onTick(); // This important. It stops the form when the duration is up. // NEVERMIND WE ARE MANUALLY STOPPING THE FORM NOW.

            if (ticksActive <= CHARGE_PAUSE_DURATION) {
                user.setDeltaMovement(Vec3.ZERO);
                user.hurtMarked = true;

                if (level.random.nextFloat() < 0.6f) {
                    Vec3 particlePos = user.position().add(Math.random() - 0.5, user.getBbHeight() * Math.random(), Math.random() - 0.5);
                    level.sendParticles(YELLOW_LIGHTNING, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            } else if(ticksActive < CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS) {
                // --- The Dash Phase ---
                if (user.isCrouching()) {
                    performSlash();
                    ActiveFormManager.stop(user);
                    return;
                }

                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 1.5, 3.0f, 0.1f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), 1.5, 3.0f, 0.1f, DamageSource.mobAttack(user));
                }
                user.setDeltaMovement(dashDirection.scale(DASH_SPEED));
                user.hurtMarked = true;

                // Spawn a trail of lightning particles
                Vec3 particlePos = user.position().add(Math.random() - 0.5, 1.0 + (Math.random() * 0.5), Math.random() - 0.5);
                level.sendParticles(YELLOW_LIGHTNING, particlePos.x, particlePos.y, particlePos.z, 5, 0, 0, 0, 0);
            } else {
                // --- The Slash Phase ---
                performSlash();
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onStop() {
            // Clean up the effects we applied onStart.
            removeImmobility(user);
        }
    }

    private static class RiceSpiritForm extends TimedForm {
        private static final int NUMBER_OF_SLASHES = 5;
        private static final int TICKS_BETWEEN_SLASHES = 2; // 0.1s
        private static final float DAMAGE_PER_SLASH = 12.0f;
        private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

        public RiceSpiritForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
        }

        @Override
        protected void onTick() {
            super.onTick();

            // a slash every 2 ticks.
            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
                // Sound
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.8f);
                level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.2f, 2.0f);

                // Animation
                user.swing(InteractionHand.MAIN_HAND);

                // Damage
                dealDirectionalDamage(user, 4.0, DAMAGE_PER_SLASH, 0.6);

                // Visuals
                spawnParticleArc(user, YELLOW_LIGHTNING, 2.5, 140, 25);
                spawnParticleArc(user, ParticleTypes.ELECTRIC_SPARK, 2.5, 140, 5);
            }
        }
    }

    public static class ThunderSwarmForm extends TimedForm {
        // --- 1. UPDATED: Faster and more numerous attacks ---
        private static final int NUMBER_OF_DASHES = 8;
        private static final int TICKS_PER_PHASE = 4; // An attack every 0.2 seconds
        private static final double DASH_DISTANCE = 3.0;
        private static final float DAMAGE_PER_HIT = 7.0f;
        private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

        private LivingEntity target;

        public ThunderSwarmForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_DASHES * TICKS_PER_PHASE);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            this.target = getLookedAtEntity(user, 16.0);

            if (this.target == null) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (target == null || !target.isAlive() || target.distanceTo(user) > 32) {
                ActiveFormManager.stop(user);
                return;
            }

            if (ticksActive > 0 && ticksActive % TICKS_PER_PHASE == 0) {
                // Generate a random 3D position around the target
                Vec3 teleportPos;
                int attempts = 0;
                do {
                    // Generate a random direction.
                    double x = level.random.nextGaussian();
                    double y = level.random.nextGaussian();
                    double z = level.random.nextGaussian();
                    Vec3 randomDir = new Vec3(x, y, z).normalize();

                    // Calculate the position around the target's center (their eyes).
                    teleportPos = target.getEyePosition().add(randomDir.scale(DASH_DISTANCE));
                    attempts++;
                } while (!level.getBlockState(new BlockPos(teleportPos)).isAir() && attempts < 5); // Safety check

                // The Blink
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getX(), user.getY() + 1, user.getZ(), 30, 0.3, 0.5, 0.3, 0.5);
                user.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getX(), user.getY() + 1, user.getZ(), 30, 0.3, 0.5, 0.3, 0.5);

                // The Attack
                user.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 2.0f);
                dealDirectionalDamage(user, 4.0, DAMAGE_PER_HIT, 0.85);
                spawnParticleArc(user, YELLOW_LIGHTNING, 3.0, 120, 15);
            }
        }
    }

    public static class DistantThunderForm extends TimedForm {
        private static final int DURATION = 100; // 5 seconds
        private static final double STORM_RADIUS = 8.0;
        private static final int BOLTS_PER_WAVE = 3;
        private static final int TICKS_PER_WAVE = 20; // A wave of lightning every second
        private static final float DAMAGE_PER_BOLT = 7.0f;
        private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

        public DistantThunderForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            applyImmobility(user,  this.duration + 5);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            // Logic for Firing Bolts
            if (ticksActive > 0 && ticksActive % TICKS_PER_WAVE == 0) {
                // Find all potential targets in the storm's radius.
                AABB searchBox = user.getBoundingBox().inflate(STORM_RADIUS);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != user && e.isAlive());

                if (targets.isEmpty()) return;

                level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6f, 2.0f);

                // Fire off the set number of bolts for this wave.
                for (int i = 0; i < BOLTS_PER_WAVE; i++) {
                    LivingEntity target = targets.get(level.random.nextInt(targets.size()));
                    target.hurt(DamageSource.LIGHTNING_BOLT, DAMAGE_PER_BOLT);

                    spawnLineParticles(level, YELLOW_LIGHTNING, user.getEyePosition(), target.getEyePosition(), 20);
                }
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
        }
    }

    public static class RumbleAndFlashForm extends TimedForm {
        private static final int DURATION = 80; // 4 seconds channeling
        private static final double MAX_RANGE = 32.0;
        private static final int TICKS_PER_BOLT = 15; // every 0.75 seconds
        private static final float DAMAGE_PER_BOLT = 8.0f;
        private static final DustParticleOptions YELLOW_LIGHTNING = new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);

        private LivingEntity target;

        public RumbleAndFlashForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        // This method was missing and is essential for cleanup.
        @Override
        protected void onStop() {
            // Free the player from their stance when the form ends.
            removeImmobility(user);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
        }

        @Override
        protected void onStart() {
            this.target = getLookedAtEntity(user, MAX_RANGE);

            if (this.target == null) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }

            applyImmobility(user, this.duration + 5);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (target == null || !target.isAlive() || target.distanceTo(user) > MAX_RANGE + 4.0) {
                ActiveFormManager.stop(user);
                return;
            }

            if (ticksActive % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, user.getRandomX(0.8), user.getRandomY(), user.getRandomZ(0.8), 2, 0, 0, 0, 0);
            }

            if (ticksActive > 0 && ticksActive % TICKS_PER_BOLT == 0) {
                Vec3 skyPos = target.getEyePosition().add(0, 15, 0);
                target.hurt(DamageSource.LIGHTNING_BOLT, DAMAGE_PER_BOLT);

                level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 2.0f);
                spawnLineParticles(level, YELLOW_LIGHTNING, skyPos, target.getEyePosition(), 30);
            }
        }
    }

    public static class HonoikazuchiNoKamiForm extends TimedForm {
        private static final int CHARGE_PAUSE_DURATION = 15;
        private static final int DASH_DURATION_TICKS = 10;
        private static final double DASH_SPEED = 64.0; // a stack
        private static final float DASH_DAMAGE = 10.0f;
        private static final float FINAL_ATTACK_DAMAGE = 40.0f;
        private static final DustParticleOptions DRAGON_PARTICLE = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.4f), 1.5f);

        private final Vec3 dashDirection;

        public HonoikazuchiNoKamiForm(LivingEntity user, BreathingForms form) {
            super(user, form, CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS + 1);
            this.dashDirection = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            // More epic, intimidating sound effects for an ultimate move
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
            applyImmobility(user, this.duration + 10);
        }

        private void performSlash() {
            // The final, devastating slash
            user.setDeltaMovement(Vec3.ZERO);
            performGenericAttack(user, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f);
            dealThrustDamage(user, 6.0, FINAL_ATTACK_DAMAGE);
            spawnParticleArc(user, DRAGON_PARTICLE, 4.0, 160, 60);
            spawnParticleArc(user, ParticleTypes.ELECTRIC_SPARK, 4.5, 180, 20);
        }

        @Override
        protected void onTick() {
            if (ticksActive < CHARGE_PAUSE_DURATION) {
                user.setDeltaMovement(Vec3.ZERO);
                user.hurtMarked = true;

                for (int i = 0; i < 10; i++) {
                    double angle = (ticksActive * 0.5) + (i * (Math.PI / 5.0));
                    double radius = 2.5 - (ticksActive / (double)CHARGE_PAUSE_DURATION * 2.5); // Radius shrinks to zero
                    Vec3 offset = new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                    Vec3 particlePos = user.position().add(offset).add(0, user.getBbHeight() / 2.0, 0);
                    level.sendParticles(DRAGON_PARTICLE, particlePos.x, particlePos.y, particlePos.z, 1, -offset.x * 0.1, -offset.y * 0.1, -offset.z * 0.1, 0);
                }
            }
            else if (ticksActive < CHARGE_PAUSE_DURATION + DASH_DURATION_TICKS) {
                if (user.isCrouching()) {
                    performSlash();
                    ActiveFormManager.stop(user);
                    return;
                }

                user.setDeltaMovement(dashDirection.scale(DASH_SPEED));
                user.hurtMarked = true;
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 2.0, DASH_DAMAGE, 0.2f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), 2.0, DASH_DAMAGE, 0.2f, DamageSource.mobAttack(user));
                }
                Vec3 sideVector = dashDirection.cross(new Vec3(0, 1, 0));
                Vec3 upVector = new Vec3(0, 1, 0);

                // The "Head" of the dragon
                Vec3 headPos = user.getEyePosition().add(dashDirection.scale(1.5));
                level.sendParticles(DRAGON_PARTICLE, headPos.x, headPos.y, headPos.z, 10, 0.2, 0.2, 0.2, 0.1);

                // The "Body" of the dragon
                for (int i = 0; i < 3; i++) {
                    double serpentAngle = (ticksActive * 1.5) + (i * Math.PI / 1.5);
                    double serpentRadius = 1.2;
                    Vec3 offset = upVector.scale(Math.sin(serpentAngle) * serpentRadius)
                            .add(sideVector.scale(Math.cos(serpentAngle) * serpentRadius));

                    // Spawn particles behind the player to create the tail
                    Vec3 particlePos = user.position().add(offset).add(0, 1, 0).subtract(dashDirection.scale(i * 0.5));
                    level.sendParticles(DRAGON_PARTICLE, particlePos.x, particlePos.y, particlePos.z, 2, 0, 0, 0, 0);
                }
            }
            else {
                performSlash();
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    // --- MIST FORMS ---
    public static class EightLayeredMistForm extends TimedForm {
        private static final int NUMBER_OF_SLASHES = 8;
        private static final int TICKS_BETWEEN_SLASHES = 3; // A very fast slash every 0.15 seconds
        private static final float DAMAGE_PER_SLASH = 4.0f; // Total damage = 32
        private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

        public EightLayeredMistForm(LivingEntity user, BreathingForms form) {
            // Total duration is 8 slashes * 3 ticks each = 24 ticks (~1.2 seconds)
            super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
        }

        @Override
        protected void onTick() {
            super.onTick(); // This is important! It handles stopping the form when the duration is up.

            // Use the modulo operator to trigger a slash every 3 ticks.
            // We check ticksActive > 0 to prevent a slash on the very first tick.
            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
                // --- Perform a single slash ---

                // Play a quick swoosh sound, pitched up to sound faster.
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 2.0f);

                // Deal damage in a cone in front of the player.
                dealDirectionalDamage(user, 3.5, DAMAGE_PER_SLASH, 0.6);

                // Spawn an arc of mist particles. This will build up with each slash.
                spawnParticleArc(user, ParticleTypes.CLOUD, 2.0, 140, 15);
                spawnParticleArc(user, DENSE_MIST, 2.0, 140, 10);
            }
        }
    }

    public static class ScatteringMistParry extends ParryForm {
        private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

        public ScatteringMistParry(LivingEntity user, BreathingForms form) {
            super(user, form, 30); // A 1-second parry window
        }

        @Override
        protected void onParrySuccess(Entity attacker) {
            // --- Offensive Circular Slash ---
            performGenericAttack(user, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.5f);
            // Damage and strongly knock back nearby enemies
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), 4.0, 7.0f, 1.5f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), 4.0, 7.0f, 1.5f, DamageSource.mobAttack(user));
            }
            // Create the 360-degree mist visual
            spawnParticleArc(user, DENSE_MIST, 3.5, 360, 40);
            spawnParticleArc(user, ParticleTypes.CLOUD, 4.0, 360, 40);

            // --- Defensive Projectile Deflection ---
            // Find all projectiles in a 5-block radius
            AABB deflectionBox = user.getBoundingBox().inflate(5.0);
            List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, deflectionBox);

            for (Projectile projectile : projectiles) {
                // Change the projectile's owner to the player
                projectile.setOwner(user);
                // Calculate a vector pointing away from the player
                Vec3 awayVector = projectile.position().subtract(user.getEyePosition()).normalize();
                // Give the projectile a strong push outwards
                projectile.setDeltaMovement(awayVector.scale(1.5));
            }
        }
    }

    public static class ShiftingFlowSlashForm extends TimedForm {
        // --- Constants ---
        private static final int PAUSE_DURATION = 4;
        private static final int DASH_DURATION = 10;
        private static final double TARGETING_RANGE = 16.0;
        private static final double DASH_DISTANCE = 10.0; // The distance for a non-targeted dash
        private static final double S_CURVE_AMPLITUDE = 2.0;
        private static final float SLASH_DAMAGE = 14.0f;
        private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

        // --- State ---
        // The target is now nullable (it can be empty)
        @Nullable
        private final LivingEntity target;
        private final Vec3 startPos;
        private final Vec3 endPos;

        public ShiftingFlowSlashForm(LivingEntity user, BreathingForms form) {
            super(user, form, PAUSE_DURATION + DASH_DURATION);
            this.target = getLookedAtEntity(user, TARGETING_RANGE);
            this.startPos = user.position();

            // --- NEW: Optional Target Logic ---
            if (this.target != null) {
                // If we have a target, the end position is in front of them.
                this.endPos = this.target.position().add(this.target.getLookAngle().scale(-1.5));
            } else {
                // If we have NO target, the end position is 10 blocks straight ahead.
                this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
            }
        }

        @Override
        public boolean isInterruptible() { return true; }

        @Override
        protected void onStart() {
            // We no longer need to check for a null target here.
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            // --- Phase 1: The "Low Stance" Pause ---
            if (ticksActive < PAUSE_DURATION) {
                user.setDeltaMovement(Vec3.ZERO);
                user.hurtMarked = true;
                level.sendParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 0.1, user.getZ(), 2, 0.3, 0.0, 0.3, 0.02);
            }
            // --- Phase 2: The S-Curve Dash ---
            else {
                int dashTicks = ticksActive - PAUSE_DURATION;
                double dashProgress = Math.min((double) dashTicks / DASH_DURATION, 1.0);

                // The movement logic is the same, but now uses the dynamic endPos.
                Vec3 forwardDir = endPos.subtract(startPos).normalize();
                Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 straightPos = startPos.lerp(endPos, dashProgress);
                double sineAngle = dashProgress * Math.PI * 2.0;
                double sineOffset = S_CURVE_AMPLITUDE * Math.sin(sineAngle);
                Vec3 curvePos = straightPos.add(sideDir.scale(sineOffset));
                user.setPos(curvePos.x, curvePos.y, curvePos.z);
                Vec3 nextCurvePos = startPos.lerp(endPos, Math.min((double)(dashTicks + 1) / DASH_DURATION, 1.0)).add(sideDir.scale(S_CURVE_AMPLITUDE * Math.sin(Math.min((double)(dashTicks + 1) / DASH_DURATION, 1.0) * Math.PI * 2.0)));
                Vec3 velocityHint = nextCurvePos.subtract(curvePos);
                user.setDeltaMovement(velocityHint);
                user.hurtMarked = true;
//                user.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(user));

                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 2.0, 2.0f, 0.1f, DamageSource.playerAttack(player));
                    level.sendParticles(DENSE_MIST, player.getX(), player.getY() + 1, player.getZ(), 2, 0, 0, 0, 0);
                } else {
                    dealAOEDamage(user, user.position(), 2.0, 2.0f, 0.1f, DamageSource.mobAttack(user));
                    level.sendParticles(DENSE_MIST, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0);
                }
                if (dashTicks >= DASH_DURATION) {
                    // The final slash will use whatever direction the player is currently looking.
                    performGenericAttack(user, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.2f);
                    dealDirectionalDamage(user, 3.0, SLASH_DAMAGE, 0.85);
                    spawnParticleArc(user, DENSE_MIST, 2.5, 160, 30);
                }
            }
        }
    }

    public static class SeaOfCloudsForm extends TimedForm {
        // --- Constants ---
        private static final int DASH_DURATION = 20; // A slighly shorter dash
        private static final double DASH_DISTANCE = 15.0; // The distance for the dash
        private static final double ZIGZAG_AMPLITUDE = 2.0;
        private static final double ZIGZAG_FREQUENCY = 10.0;
        private static final float DAMAGE_PER_TICK = 1.5f;
        private static final double DAMAGE_RADIUS = 2.5;

        // --- State ---
        private final Vec3 startPos;
        private final Vec3 endPos;

        public SeaOfCloudsForm(LivingEntity user, BreathingForms form) {
            super(user, form, DASH_DURATION);
            this.startPos = user.position();
            this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
        }

        @Override
        public boolean isInterruptible() { return true; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            double dashProgress = Math.min((double) ticksActive / DASH_DURATION, 1.0);

            Vec3 forwardDir = endPos.subtract(startPos).normalize();
            Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 straightPos = startPos.lerp(endPos, dashProgress);
            double zigzagOffset = ZIGZAG_AMPLITUDE * Math.signum(Math.sin(dashProgress * Math.PI * ZIGZAG_FREQUENCY));
            Vec3 currentCurvePos = straightPos.add(sideDir.scale(zigzagOffset));

            user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

            double nextDashProgress = Math.min((double)(ticksActive + 1) / DASH_DURATION, 1.0);
            Vec3 nextStraightPos = startPos.lerp(endPos, nextDashProgress);
            double nextZigzagOffset = ZIGZAG_AMPLITUDE * Math.signum(Math.sin(nextDashProgress * Math.PI * ZIGZAG_FREQUENCY));
            Vec3 nextCurvePos = nextStraightPos.add(sideDir.scale(nextZigzagOffset));
            Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);

            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f, DamageSource.mobAttack(user));
            }
            if (ticksActive > 0 && ticksActive % 2 == 0) {
                BlindingMistCloudEntity cloud = ModEntityTypes.BLINDING_MIST_CLOUD.get().create(level);
                if(cloud != null) {
                    cloud.setOwner(user);
                    cloud.setPos(user.position());
                    cloud.setParticle(ParticleTypes.CLOUD);
                    cloud.setRadius(3.0f);
                    cloud.setDuration(100);
                    cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    level.addFreshEntity(cloud);
                }
            }
        }
    }

    public static class LunarDispersingMist extends TimedForm {
        // --- Constants ---
        private static final int DURATION = 40; // 2 seconds of air time
        private static final float LEAP_STRENGTH = 1.2f;
        private static final float FORWARD_STRENGTH = 0.7f;
        private static final double AOE_RADIUS = 5.0;
        private static final float DAMAGE_PER_TICK = 2.0f;
        private static final DustParticleOptions DENSE_MIST = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.5f);

        public LunarDispersingMist(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            // Launch the player upwards and forwards into the fight.
            Vec3 forward = user.getLookAngle().multiply(FORWARD_STRENGTH, 0, FORWARD_STRENGTH);
            Vec3 leap = forward.add(0, LEAP_STRENGTH, 0);
            user.setDeltaMovement(user.getDeltaMovement().add(leap));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.3f);
        }

        @Override
        protected void onTick() {
            super.onTick(); // This handles stopping the form after its duration.

            // --- Continuous AOE Barrage ---
            // Deal damage every tick to all nearby enemies.
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.2f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.2f, DamageSource.mobAttack(user));
            }
            // --- Visuals: The Obscuring Clouds and Slashes ---
            // 1. Spawn a large, dense sphere of mist around the player.
            for (int i = 0; i < 20; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
                double offsetY = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
                double offsetZ = (level.random.nextDouble() - 0.5) * (AOE_RADIUS * 2);
                Vec3 particlePos = user.position().add(offsetX, 1.0, offsetZ);

                if (user.position().distanceToSqr(particlePos) <= (AOE_RADIUS * AOE_RADIUS)) {
                    level.sendParticles(ParticleTypes.CLOUD, particlePos.x, user.getY() + offsetY, particlePos.z, 1, 0, 0, 0, 0.1);
                }
            }

            // 2. Every 4 ticks, create a visual "slash" effect.
            if (ticksActive % 4 == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f);
                // The sweep particle creates a great 360-degree slash visual.
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getEyeY(), user.getZ(), 3, 0, 0, 0, 0);
            }
        }
    }

    // --- SOUND FORMS ---
    public static class RoarForm extends TimedForm {
        private static final int SLAM_DELAY = 10;
        private static final float SLAM_DAMAGE = 15.0f;
        private static final double SLAM_RADIUS = 15.0;
        private static final float SLAM_KNOCKBACK = 1.8f;

        public RoarForm(LivingEntity user, BreathingForms form) {
            super(user, form, SLAM_DELAY + 5);
        }

        @Override
        protected void onStart() {
            applyImmobility(user, this.duration);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive < SLAM_DELAY) {
                for (int i = 0; i < 3; i++) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double x = user.getX() + Math.cos(angle) * 1.2;
                    double z = user.getZ() + Math.sin(angle) * 1.2;
                    level.sendParticles(ParticleTypes.NOTE, x, user.getY() + 1.0, z, 1, 0, 0, 0, 0);
                }
            }
            if (ticksActive == SLAM_DELAY) {
                // Damage and Sound logic (now uses the new 10-block radius)
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), SLAM_RADIUS, SLAM_DAMAGE, SLAM_KNOCKBACK, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), SLAM_RADIUS, SLAM_DAMAGE, SLAM_KNOCKBACK, DamageSource.mobAttack(user));
                }
                level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
                level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.2f);

                // Particle logic
                level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getY() + 0.5, user.getZ(), 1, 0, 0, 0, 0);

                BlockState groundState = level.getBlockState(user.blockPosition().below());
                if (!groundState.isAir()) {
                    for (int i = 0; i < 50; i++) {
                        double angle = level.random.nextDouble() * 2 * Math.PI;
                        double radius = 0.5 + level.random.nextDouble() * (SLAM_RADIUS - 1.0);
                        double x = user.getX() + Math.cos(angle) * radius;
                        double z = user.getZ() + Math.sin(angle) * radius;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), x, user.getY() + 0.2, z, 1, 0.2, 0.1, 0.2, 0.1);
                    }
                }
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    public static class ResoundingSlashesForm extends TimedForm {
        // --- Constants ---
        private static final int DURATION = 100; // 5 seconds
        private static final int DAMAGE_TICK_RATE = 10; // A damage pulse every half-second
        private static final float DAMAGE = 5.0f;
        private static final double RADIUS = 3.5;
        private static final float KNOCKBACK = 1.2f;

        private float rotationAngle = 0;

        public ResoundingSlashesForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        public boolean isInterruptible() { return true; }

        @Override
        protected void onStart() {
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.duration + 5, 4));
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            AABB defenseBox = user.getBoundingBox().inflate(RADIUS);
            List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, defenseBox);

            for (Projectile projectile : projectiles) {
                level.playSound(null, projectile.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 2.0f);
                level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.1, 0.1, 0.1, 0.5);
                projectile.discard();
            }

            if (ticksActive % DAMAGE_TICK_RATE == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.mobAttack(user));
                }
            }

            this.rotationAngle += 35.0f;
            spawnParticleArcInWorld(level, user.getEyePosition(), RADIUS - 0.5, 160, 15, this.rotationAngle, 0, ParticleTypes.SWEEP_ATTACK);
            spawnParticleArcInWorld(level, user.getEyePosition(), RADIUS - 0.5, 160, 15, this.rotationAngle + 180, 0, ParticleTypes.SWEEP_ATTACK);
        }

        @Override
        protected void onStop() {
            user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.8f);
        }
    }

    public static class StringPerformanceForm extends TimedForm {
        private static final int DURATION = 60; // 3 seconds of charging
        private static final float FORWARD_SPEED = 1.3f;
        private static final float DAMAGE = 3.5f;
        private static final double RADIUS = 3.0;
        private static final float KNOCKBACK = 0.4f;

        private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("a8a0e842-7a88-4a41-8f43-059935677a21");


        private float rotationAngle = 0;

        public StringPerformanceForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (attribute != null) {
                attribute.removeModifier(KNOCKBACK_RESISTANCE_UUID);
                AttributeModifier modifier = new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "String Performance Knockback Immunity", 1.0, AttributeModifier.Operation.ADDITION);
                attribute.addTransientModifier(modifier);
            }

            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            Vec3 lookVec = user.getLookAngle();
            user.setDeltaMovement(lookVec.x * FORWARD_SPEED, user.getDeltaMovement().y, lookVec.z * FORWARD_SPEED);
            user.hurtMarked = true;
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.mobAttack(user));
            }

            this.rotationAngle += 45.0f;
            Vec3 discCenter = user.getEyePosition().add(user.getLookAngle().scale(1.5));

            // Spawn a ring of particles.
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * (360.0 / 6) + this.rotationAngle);
                Vec3 particleOffset = new Vec3(Math.cos(angle) * (RADIUS - 1.0), Math.sin(angle) * (RADIUS - 1.0), 0);
                particleOffset = rotateVec(particleOffset, user.getXRot(), user.getYRot());
                Vec3 particlePos = discCenter.add(particleOffset);

                level.sendParticles(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }

        @Override
        protected void onStop() {
            AttributeInstance attribute = user.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (attribute != null) {
                // We remove the specific modifier we added by its unique ID.
                attribute.removeModifier(KNOCKBACK_RESISTANCE_UUID);
            }
        }
    }

    // --- LOVE FORMS ---
    public static class ShiversOfFirstLoveForm extends TimedForm {
        // --- Constants ---
        private static final int NUMBER_OF_SLASHES = 6;
        private static final int TICKS_BETWEEN_SLASHES = 4; // every 0.2 seconds
        private static final float DAMAGE_PER_SLASH = 4.5f;
        private static final double FORWARD_SPEED = 0.6;
        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
        private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

        public ShiversOfFirstLoveForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
        }

        @Override
        protected void onTick() {
            super.onTick();

            Vec3 lookVec = user.getLookAngle();
            user.setDeltaMovement(lookVec.x * FORWARD_SPEED, user.getDeltaMovement().y, lookVec.z * FORWARD_SPEED);
            user.hurtMarked = true;

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
                user.swing(InteractionHand.MAIN_HAND);
                dealDirectionalDamage(user, 4.5, DAMAGE_PER_SLASH, 0.6);

                spawnParticleArc(user, PINK_SPARK, 3.0, 140, 15);
                spawnParticleArc(user, GREEN_SPARK, 3.0, 140, 15);
                level.sendParticles(ParticleTypes.HEART, user.getX(), user.getEyeY(), user.getZ(), 3, 1, 1, 1, 0.1);
            }
        }
    }

    public static class LovePangsForm extends TimedForm {
        private static final double MAX_RANGE = 12.0;
        private static final double WHIP_SPEED = 2.0; // The speed in blocks per tick
        private static final float DAMAGE_PER_HIT = 2.5f;
        private static final int HIT_COUNT = 6; // Total damage = 15
        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
        private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

        private final Vec3 direction;
        private Vec3 whipTipPos;
        private boolean hasHit = false;

        public LovePangsForm(LivingEntity user, BreathingForms form) {
            super(user, form, (int)(MAX_RANGE / WHIP_SPEED));
            this.direction = user.getLookAngle();
            this.whipTipPos = user.getEyePosition();
        }

        @Override
        protected void onStart() {
            // Play a "whip crack" sound on activation
            performGenericAttack(user, SoundEvents.ARROW_SHOOT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.hasHit) return;
            this.whipTipPos = this.whipTipPos.add(this.direction.scale(WHIP_SPEED));
            level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK,
                    this.whipTipPos.x, this.whipTipPos.y, this.whipTipPos.z,
                    2, 0.1, 0.1, 0.1, 0);

            AABB hitBox = new AABB(this.whipTipPos, this.whipTipPos).inflate(1.2);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

            if (!potentialTargets.isEmpty()) {
                // We have a hit!
                this.hasHit = true;
                LivingEntity hitTarget = potentialTargets.get(0);
                for (int i = 0; i < HIT_COUNT; i++) {
                    if(user instanceof ServerPlayer player) {
                        hitTarget.hurt(DamageSource.playerAttack(player), DAMAGE_PER_HIT);
                    } else {
                        hitTarget.hurt(DamageSource.mobAttack(user), DAMAGE_PER_HIT);
                    }
                }

                level.playSound(null, hitTarget.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.5f);
                if (hitTarget.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                }
                ActiveFormManager.stop(user);
            }
        }
    }

    public static class CatloveShowerForm extends TimedForm {
        private static final int SLASH_DELAY = 5; // The slashes trigger 0.25 seconds into the leap
        private static final float LEAP_STRENGTH = 0.9f;
        private static final float DAMAGE = 18.0f;
        private static final double RADIUS = 6.0;
        private static final float KNOCKBACK = 0.7f;
        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);

        public CatloveShowerForm(LivingEntity user, BreathingForms form) {
            super(user, form, 20);
        }

        @Override
        protected void onStart() {
            user.setDeltaMovement(user.getDeltaMovement().add(0, LEAP_STRENGTH, 0));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            if (ticksActive == SLASH_DELAY) {
                level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.5f, 1.8f);
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), RADIUS, DAMAGE, KNOCKBACK, DamageSource.mobAttack(user));
                }

                level.sendParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1, user.getZ(), 30, RADIUS / 2, RADIUS / 2, RADIUS / 2, 0.1);

                Vec3 center = user.getEyePosition();
                spawnParticleArcInWorld(level, center, RADIUS * 0.8, 70, 20, user.getYRot() + 0, 45, PINK_SPARK); // Forward
                spawnParticleArcInWorld(level, center, RADIUS * 0.8, 70, 20, user.getYRot() + 90, 45, PINK_SPARK); // Left
                spawnParticleArcInWorld(level, center, RADIUS * 0.8, 70, 20, user.getYRot() + 180, 45, PINK_SPARK); // Back
                spawnParticleArcInWorld(level, center, RADIUS * 0.8, 70, 20, user.getYRot() + 270, 45, PINK_SPARK); // Right
            }
        }
    }

    public static class SwayingLoveWildclawForm extends TimedForm {
        private static final int DURATION = 60; // Tornado lasts for 3 seconds
        private static final float LEAP_STRENGTH = 1.1f;
        private static final double MAX_RANGE = 20.0;
        private static final double TORNADO_RADIUS = 3.5;
        private static final double TORNADO_HEIGHT = 7.0;
        private static final float DAMAGE_PER_PULSE = 7.0f;
        private static final int DAMAGE_TICK_RATE = 10; // A damage pulse every half-second

        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
        private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

        private final Vec3 tornadoCenter;

        public SwayingLoveWildclawForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);

            if (hitResult.getType() == HitResult.Type.MISS) {
                this.tornadoCenter = null; // Mark as failed if aiming at the sky or too far.
            } else {
                this.tornadoCenter = Vec3.atBottomCenterOf(hitResult.getBlockPos());
            }
        }

        @Override
        protected void onStart() {
            if (this.tornadoCenter == null) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }
            user.setDeltaMovement(user.getDeltaMovement().add(0, LEAP_STRENGTH, 0));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.tornadoCenter == null) return;

            if (ticksActive % DAMAGE_TICK_RATE == 0) {
                if(user instanceof ServerPlayer player) {
                    dealVerticalColumnDamage(player, this.tornadoCenter, TORNADO_RADIUS, TORNADO_HEIGHT, DAMAGE_PER_PULSE, DamageSource.playerAttack(player));
                } else {
                    dealVerticalColumnDamage(user, this.tornadoCenter, TORNADO_RADIUS, TORNADO_HEIGHT, DAMAGE_PER_PULSE, DamageSource.mobAttack(user));
                }
                level.playSound(null, tornadoCenter.x, tornadoCenter.y, tornadoCenter.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0f, 1.8f);
            }

            for (int i = 0; i < 15; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double radius = level.random.nextDouble() * TORNADO_RADIUS;
                double y = level.random.nextDouble() * TORNADO_HEIGHT;
                Vec3 particlePos = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius).add(this.tornadoCenter);

                Vec3 centerToParticle = particlePos.subtract(this.tornadoCenter);
                Vec3 tangentialVel = new Vec3(-centerToParticle.z, 0, centerToParticle.x).normalize().scale(0.4);
                Vec3 upwardVel = new Vec3(0, 0.2, 0);

                ParticleOptions particle = (i % 2 == 0) ? PINK_SPARK : GREEN_SPARK;
                level.sendParticles(particle, particlePos.x, particlePos.y, particlePos.z, 1, tangentialVel.x, upwardVel.y, tangentialVel.z, 0);

                if (level.random.nextInt(10) == 0) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static class CatLeggedWindsForm extends TimedForm {
        private enum Phase { OUTWARD, INWARD }
        private static final double MAX_RANGE = 10.0;
        private static final double WHIP_SPEED = 2.0; // The speed in blocks per tick
        private static final float DAMAGE = 9.0f;
        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
        private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

        private Phase currentPhase = Phase.OUTWARD;
        private final Vec3 direction;
        private Vec3 whipTipPos;
        private final List<UUID> hitInPhase = new ArrayList<>();

        public CatLeggedWindsForm(LivingEntity user, BreathingForms form) {
            super(user, form, (int)(MAX_RANGE / WHIP_SPEED) * 2);
            this.direction = user.getLookAngle();
            this.whipTipPos = user.getEyePosition();
        }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.currentPhase == Phase.OUTWARD) {
                this.whipTipPos = this.whipTipPos.add(this.direction.scale(WHIP_SPEED));

                if (user.position().distanceToSqr(this.whipTipPos) >= MAX_RANGE * MAX_RANGE) {
                    this.currentPhase = Phase.INWARD;
                    this.hitInPhase.clear();
                }
            } else {
                Vec3 returnDirection = user.getEyePosition().subtract(this.whipTipPos).normalize();
                this.whipTipPos = this.whipTipPos.add(returnDirection.scale(WHIP_SPEED));
            }

            level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK,
                    this.whipTipPos.x, this.whipTipPos.y, this.whipTipPos.z,
                    3, 0.1, 0.1, 0.1, 0);

            AABB hitBox = new AABB(this.whipTipPos, this.whipTipPos).inflate(1.5);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != user && e.isAlive() && !hitInPhase.contains(e.getUUID()));

            for (LivingEntity target : potentialTargets) {
                if(user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }
                this.hitInPhase.add(target.getUUID());
                level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    // --- INSECT FORMS ---
    public static class ButterflyDanceCapriceForm extends TimedForm {
        private static final int DASH_DURATION = 15;
        private static final double DASH_SPEED = 1.5;
        private static final float STING_DAMAGE = 1.0f;
        private static final int POISON_DURATION = 140;
        private static final int POISON_AMPLIFIER = 1;

        private static final DustParticleOptions PINK_SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.9f), 1.0f);
        private static final DustParticleOptions GREEN_SPARK = new DustParticleOptions(new Vector3f(0.6f, 1.0f, 0.6f), 1.0f);

        private final Vec3 direction;
        private boolean hasHit = false;

        public ButterflyDanceCapriceForm(LivingEntity user, BreathingForms form) {
            super(user, form, DASH_DURATION);
            this.direction = user.getLookAngle();
        }

        @Override
        protected void onStart() {
            user.setDeltaMovement(user.getDeltaMovement().add(0, 0.6, 0));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 2.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            user.setDeltaMovement(this.direction.x * DASH_SPEED, user.getDeltaMovement().y, this.direction.z * DASH_SPEED);
            user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            Vec3 forwardDir = user.getDeltaMovement().normalize();
            Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
            double serpentAngle = ticksActive * 1.3;
            double serpentRadius = 0.9;
            Vec3 offset = sideDir.scale(Math.sin(serpentAngle) * serpentRadius).add(0, Math.cos(serpentAngle) * serpentRadius, 0);
            Vec3 particlePos = user.position().add(offset);
            level.sendParticles(ticksActive % 2 == 0 ? PINK_SPARK : GREEN_SPARK, particlePos.x, particlePos.y + 1, particlePos.z, 1, 0, 0, 0, 0);

            if (!this.hasHit) {
                AABB hitBox = user.getBoundingBox().inflate(1.0);
                List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                if (!potentialTargets.isEmpty()) {
                    this.hasHit = true;
                    LivingEntity hitTarget = potentialTargets.get(0);

                    if(user instanceof ServerPlayer player) {
                        hitTarget.hurt(DamageSource.playerAttack(player), STING_DAMAGE);
                    } else {
                        hitTarget.hurt(DamageSource.mobAttack(user), STING_DAMAGE);
                    }
                    hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));

                    level.playSound(null, hitTarget.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.8f);
                    if (hitTarget.level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 20, 0.4, 0.4, 0.4, 0.5);
                    }
                    ActiveFormManager.stop(user);
                }
            }
        }
    }

    public static class BeeStingFlutterForm extends TimedForm {
        private enum Phase { DASHING, RECOILING }
        private static final int DURATION = 10;
        private static final double DASH_SPEED = 3.0;
        private static final double RECOIL_STRENGTH = 1.5;
        private static final float STING_DAMAGE = 1.0f;
        private static final int POISON_DURATION = 80;
        private static final int POISON_AMPLIFIER = 3;

        // --- State ---
        private Phase currentPhase = Phase.DASHING;
        private final Vec3 direction;

        public BeeStingFlutterForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.direction = user.getLookAngle();
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            switch (this.currentPhase) {
                case DASHING:
                    user.setDeltaMovement(this.direction.x * DASH_SPEED, user.getDeltaMovement().y, this.direction.z * DASH_SPEED);
                    user.hurtMarked = true;
//                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

                    level.sendParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.1);

                    AABB hitBox = user.getBoundingBox().inflate(0.8);
                    List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                    if (!potentialTargets.isEmpty()) {
                        LivingEntity hitTarget = potentialTargets.get(0);

                        if(user instanceof ServerPlayer player) {
                            hitTarget.hurt(DamageSource.playerAttack(player), STING_DAMAGE);
                        } else {
                            hitTarget.hurt(DamageSource.mobAttack(user), STING_DAMAGE);
                        }
                        hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));

                        level.playSound(null, hitTarget.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.2f, 1.0f);
                        level.sendParticles(ParticleTypes.CRIT, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 30, 0.4, 0.4, 0.4, 0.5);

                        Vec3 recoilVec = user.position().subtract(hitTarget.position()).normalize().scale(RECOIL_STRENGTH);
                        user.setDeltaMovement(recoilVec.x, 0.4, recoilVec.z);
                        user.hurtMarked = true;
//                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

                        // Transition to next phase
                        this.currentPhase = Phase.RECOILING;
                    }
                    break;

                case RECOILING:
                    break;
            }
        }
    }

    public static class DragonflyDanceForm extends TimedForm {
        // --- Constants ---
        private static final int STRIKES = 6;
        private static final int TICKS_PER_STRIKE = 3; // A very fast stab every 0.15 seconds
        private static final double HEXAGON_DISTANCE = 1.5; // How far in front of the player the hexagon is
        private static final double HEXAGON_RADIUS = 0.6;
        private static final float DAMAGE_PER_STRIKE = 2.0f;
        private static final int POISON_DURATION = 60; // 3 seconds per sting
        private static final int POISON_AMPLIFIER = 0; // Wisteria Poison I

        // --- State ---
        private int strikeIndex = 0;

        public DragonflyDanceForm(LivingEntity user, BreathingForms form) {
            super(user, form, STRIKES * TICKS_PER_STRIKE + 2);
        }

        @Override
        protected void onTick() {
            super.onTick(); // Handles stopping the form

            // Trigger a strike every few ticks
            if (ticksActive > 0 && ticksActive % TICKS_PER_STRIKE == 0 && strikeIndex < STRIKES) {

                // --- NEW: Calculate the strike position dynamically ---
                // 1. Get the player's CURRENT position and view direction.
                Vec3 playerLook = user.getLookAngle();
                Vec3 playerEyePos = user.getEyePosition();

                // 2. Calculate the center of the hexagon in front of the player.
                Vec3 hexagonCenter = playerEyePos.add(playerLook.scale(HEXAGON_DISTANCE));
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 side = playerLook.cross(up).normalize();

                // 3. Calculate the angle for the CURRENT strike in the hexagon pattern.
                double angle = (2 * Math.PI / STRIKES) * this.strikeIndex;
                Vec3 strikePos = hexagonCenter.add(
                        side.scale(Math.cos(angle) * HEXAGON_RADIUS)
                ).add(
                        up.scale(Math.sin(angle) * HEXAGON_RADIUS)
                );
                user.swing(InteractionHand.MAIN_HAND);

                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, strikePos, 0.8, DAMAGE_PER_STRIKE, 0.1f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, strikePos, 0.8, DAMAGE_PER_STRIKE, 0.1f, DamageSource.mobAttack(user));
                }
                List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, new AABB(strikePos, strikePos).inflate(0.8));
                for(LivingEntity target : hitTargets) {
                    if(target != user) {
                        target.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), POISON_DURATION, POISON_AMPLIFIER));
                    }
                }

                level.playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 2.0f);
                level.sendParticles(ParticleTypes.CRIT, strikePos.x, strikePos.y, strikePos.z, 15, 0.1, 0.1, 0.1, 0.5);

                this.strikeIndex++;
            }
        }
    }

    public static class CentipedeDanceForm extends TimedForm {
        private static final int DASH_DURATION = 25;
        private static final double DASH_DISTANCE = 18.0;
        private static final double ZIGZAG_AMPLITUDE = 1.5; // How wide the zig-zags are
        private static final double ZIGZAG_FREQUENCY = 12.0; // How fast the zig-zags are

        private static final float FINISHER_DAMAGE = 8.0f;
        private static final int FINISHER_POISON_DURATION = 120;
        private static final int FINISHER_POISON_AMPLIFIER = 4;  // Wisteria Poison V

        private static final DustParticleOptions BUTTERFLY_PARTICLE = new DustParticleOptions(new Vector3f(0.8f, 0.4f, 1.0f), 1.0f);

        private final Vec3 startPos;
        private final Vec3 endPos;
        private boolean hasHit = false;

        public CentipedeDanceForm(LivingEntity user, BreathingForms form) {
            super(user, form, DASH_DURATION);
            this.startPos = user.position();
            this.endPos = this.startPos.add(user.getLookAngle().scale(DASH_DISTANCE));
        }

        @Override
        public boolean isInterruptible() { return true; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2f, 1.6f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            if (this.hasHit) return;

            double dashProgress = Math.min((double) ticksActive / DASH_DURATION, 1.0);
            Vec3 forwardDir = endPos.subtract(startPos).normalize();
            Vec3 sideDir = forwardDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 straightPos = startPos.lerp(endPos, dashProgress);
            double zigzagOffset = ZIGZAG_AMPLITUDE * Math.sin(dashProgress * Math.PI * ZIGZAG_FREQUENCY);
            Vec3 currentCurvePos = straightPos.add(sideDir.scale(zigzagOffset));

            double nextDashProgress = Math.min((double)(ticksActive + 1) / DASH_DURATION, 1.0);
            Vec3 nextStraightPos = startPos.lerp(endPos, nextDashProgress);
            double nextZigzagOffset = ZIGZAG_AMPLITUDE * Math.sin(nextDashProgress * Math.PI * ZIGZAG_FREQUENCY);
            Vec3 nextCurvePos = nextStraightPos.add(sideDir.scale(nextZigzagOffset));
            Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);

            user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);
            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            level.sendParticles(BUTTERFLY_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getY() + 1, user.getZ(), 1, 0, 0, 0, 0);

            AABB hitBox = user.getBoundingBox().inflate(0.8);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

            if (!potentialTargets.isEmpty()) {
                this.hasHit = true;
                LivingEntity hitTarget = potentialTargets.get(0);

                if(user instanceof ServerPlayer player) {
                    hitTarget.hurt(DamageSource.playerAttack(player), FINISHER_DAMAGE);
                } else {
                    hitTarget.hurt(DamageSource.mobAttack(user), FINISHER_DAMAGE);
                }
                hitTarget.addEffect(new MobEffectInstance(ModEffects.WISTERIA_POISON.get(), FINISHER_POISON_DURATION, FINISHER_POISON_AMPLIFIER));
                level.playSound(null, hitTarget.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.5f, 1.0f);
                level.sendParticles(ParticleTypes.EXPLOSION, hitTarget.getX(), hitTarget.getEyeY(), hitTarget.getZ(), 2, 0, 0, 0, 0);

                ActiveFormManager.stop(user);
            }
        }
    }

    // --- STONE FORMS ---
    public static class SerpentiniteBipolarForm extends TimedForm {
        private static final double MAX_RANGE = 12.0;
        private static final double TRAVEL_SPEED = 1.0;
        private static final float ROTATION_SPEED = 40.0f; // degrees per tick
        private static final double DRILL_RADIUS = 1.5; // The distance of the axe/flail from the center
        private static final float DAMAGE_PER_TICK = 6.0f;

        private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);
        private final Vec3 direction;
        private Vec3 drillCenterPos;
        private float rotationAngle = 0;

        public SerpentiniteBipolarForm(LivingEntity user, BreathingForms form) {
            super(user, form, (int)(MAX_RANGE / TRAVEL_SPEED));
            this.direction = user.getLookAngle();
            this.drillCenterPos = user.getEyePosition();
        }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.GRINDSTONE_USE, 1.0f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            this.drillCenterPos = this.drillCenterPos.add(this.direction.scale(TRAVEL_SPEED));

            AABB hitBox = new AABB(this.drillCenterPos, this.drillCenterPos).inflate(DRILL_RADIUS);
            List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

            for (LivingEntity target : hitTargets) {
                if(user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
                }
                target.setDeltaMovement(this.direction.scale(TRAVEL_SPEED));
            }
            this.rotationAngle += ROTATION_SPEED;
            Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 upDir = sideDir.cross(this.direction).normalize();
            double angleRad = Math.toRadians(this.rotationAngle);

            Vec3 axePos = this.drillCenterPos.add(sideDir.scale(Math.cos(angleRad) * DRILL_RADIUS)).add(upDir.scale(Math.sin(angleRad) * DRILL_RADIUS));
            Vec3 flailPos = this.drillCenterPos.add(sideDir.scale(-Math.cos(angleRad) * DRILL_RADIUS)).add(upDir.scale(-Math.sin(angleRad) * DRILL_RADIUS));

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), axePos.x, axePos.y, axePos.z, 2, 0.1, 0.1, 0.1, 0.1);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 2, 0.1, 0.1, 0.1, 0.1);
            spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 4);
        }
    }

    public static class UpperSmashForm extends ActiveForm {
        private enum Phase { LAUNCHING, WAITING, SMASHING }
        private static final int LAUNCH_DURATION = 10; // 0.5s for the pincer to fly out
        private static final int WAIT_DURATION = 8;    // ~0.4s pause
        private static final int SMASH_DURATION = 4;   // ~0.2s for the final smash
        private static final double MAX_RANGE = 10.0;
        private static final double PINCER_ANGLE = 25.0; // How wide the V is
        private static final float SMASH_DAMAGE = 20.0f;
        private static final double SMASH_RADIUS = 3.0;

        private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

        private Phase currentPhase = Phase.LAUNCHING;
        private final Vec3 direction;
        private Vec3 axePos;
        private Vec3 flailPos;
        private int phaseTicks = 0;

        public UpperSmashForm(LivingEntity user, BreathingForms form) {
            super(user, form);
            this.direction = user.getLookAngle();
            this.axePos = user.getEyePosition();
            this.flailPos = user.getEyePosition();
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.ANVIL_USE, 1.0f, 0.9f);
        }

        @Override
        protected void onTick() {
            this.phaseTicks++;

            switch (this.currentPhase) {
                case LAUNCHING:
                    double speed = MAX_RANGE / LAUNCH_DURATION;
                    Vec3 forwardVec = this.direction.scale(speed);
                    Vec3 sideVec = this.direction.cross(new Vec3(0, 1, 0)).normalize().scale(Math.tan(Math.toRadians(PINCER_ANGLE)) * speed);

                    this.axePos = this.axePos.add(forwardVec).add(sideVec);
                    this.flailPos = this.flailPos.add(forwardVec).subtract(sideVec);

                    AABB pincerBox = new AABB(this.axePos, this.flailPos).inflate(1.5);
                    List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, pincerBox, e -> e != user && e.isAlive());

                    if (!hitTargets.isEmpty() || this.phaseTicks >= LAUNCH_DURATION) {
                        this.currentPhase = Phase.WAITING;
                        this.phaseTicks = 0;
                    }
                    break;

                case WAITING:
                    // HE'S JUST STANDING THERE, MENACINGLY!!
                    level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.5f, 1.2f);
                    if (this.phaseTicks >= WAIT_DURATION) {
                        this.currentPhase = Phase.SMASHING;
                        this.phaseTicks = 0;
                    }
                    break;

                case SMASHING:
                    Vec3 smashCenter = this.axePos.add(this.flailPos).scale(0.5);
                    double smashSpeed = this.flailPos.distanceTo(smashCenter) / SMASH_DURATION;
                    Vec3 smashDirection = smashCenter.subtract(this.flailPos).normalize();

                    this.flailPos = this.flailPos.add(smashDirection.scale(smashSpeed));

                    if (this.phaseTicks >= SMASH_DURATION) {
                        if(user instanceof ServerPlayer player) {
                            dealAOEDamage(player, smashCenter, SMASH_RADIUS, SMASH_DAMAGE, 1.5f, DamageSource.playerAttack(player));
                        } else {
                            dealAOEDamage(user, smashCenter, SMASH_RADIUS, SMASH_DAMAGE, 1.5f, DamageSource.mobAttack(user));
                        }
                        level.playSound(null, smashCenter.x, smashCenter.y, smashCenter.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5f, 1.0f);
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), smashCenter.x, smashCenter.y, smashCenter.z, 100, 1.5, 1.5, 1.5, 0.5);
                        ActiveFormManager.stop(user);
                    }
                    break;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), axePos.x, axePos.y, axePos.z, 1, 0, 0, 0, 0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 1, 0, 0, 0, 0);
            spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 5);
        }
    }

    public static class StoneSkinParry extends ParryForm {
        private static final int PARRY_WINDOW = 25; // 1.25 second parry window
        private static final float COUNTER_DAMAGE = 25.0f;
        private static final double COUNTER_RADIUS = 6.0;
        private static final float COUNTER_KNOCKBACK = 2.5f;
        private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);
        private float rotationAngle = 0;

        public StoneSkinParry(LivingEntity user, BreathingForms form) {
            super(user, form, PARRY_WINDOW);
        }

        @Override
        protected void onStart() {
            super.onStart();
            level.playSound(null, user.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            this.rotationAngle += 35.0f;
            double angleRad = Math.toRadians(this.rotationAngle);
            double radius = 1.5;

            Vec3 axePos = user.getEyePosition().add(new Vec3(Math.cos(angleRad) * radius, 0, Math.sin(angleRad) * radius));
            Vec3 flailPos = user.getEyePosition().add(new Vec3(Math.cos(angleRad + Math.PI) * radius, 0, Math.sin(angleRad + Math.PI) * radius));

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), axePos.x, axePos.y, axePos.z, 1, 0, 0, 0, 0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), flailPos.x, flailPos.y, flailPos.z, 1, 0, 0, 0, 0);
            spawnLineParticles(level, CHAIN_PARTICLE, axePos, flailPos, 3);
        }

        @Override
        protected void onParrySuccess(Entity attacker) {
            level.playSound(null, user.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);

            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK, DamageSource.mobAttack(user));
            }
            BlockState groundState = level.getBlockState(user.blockPosition().below());
            if (!groundState.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), user.getX(), user.getY() + 0.2, user.getZ(), 100, COUNTER_RADIUS * 0.8, 0.2, COUNTER_RADIUS * 0.8, 0.2);
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getY() + 0.5, user.getZ(), 1, 0,0,0,0);
        }
    }

    public static class VolcanicRockForm extends TimedForm {
        private static final int SWING_DURATION = 10; // The swing takes half a second to complete
        private static final double ATTACK_REACH = 6.0;
        private static final double IMPACT_RADIUS = 2.0; // The hitbox of each weapon head
        private static final float DAMAGE = 18.0f;
        private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

        private final List<UUID> hitEntities = new ArrayList<>();

        public VolcanicRockForm(LivingEntity user, BreathingForms form) {
            super(user, form, SWING_DURATION);
        }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.ANVIL_USE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            double progress = (double) ticksActive / SWING_DURATION;
            double currentReach = ATTACK_REACH * progress;

            Vec3 playerPos = user.getEyePosition();
            Vec3 lookDir = user.getLookAngle();
            Vec3 sideDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

            Vec3 rightAxePos = playerPos.add(sideDir.scale(currentReach));
            Vec3 leftFlailPos = playerPos.add(sideDir.scale(-currentReach));

            AABB damageZone = new AABB(rightAxePos, leftFlailPos).inflate(IMPACT_RADIUS);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                    e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

            for(LivingEntity target : potentialTargets) {
                if(user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                }
                hitEntities.add(target.getUUID());
            }

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), rightAxePos.x, rightAxePos.y, rightAxePos.z, 5, 0.2, 0.2, 0.2, 0.1);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), leftFlailPos.x, leftFlailPos.y, leftFlailPos.z, 5, 0.2, 0.2, 0.2, 0.1);
            spawnLineParticles(level, CHAIN_PARTICLE, playerPos, rightAxePos, 5);
            spawnLineParticles(level, CHAIN_PARTICLE, playerPos, leftFlailPos, 5);
        }
    }

    public static class ArcsOfJusticeForm extends TimedForm {
        // --- Constants ---
        private static final int AXE_IMPACT_TICK = 8;  // Axe hits quickly (0.4s)
        private static final int FLAIL_IMPACT_TICK = 14; // Flail hits later (0.7s)
        private static final double MAX_RANGE = 16.0;
        private static final float AXE_DAMAGE = 12.0f;
        private static final float FLAIL_DAMAGE = 20.0f;
        private static final double IMPACT_RADIUS = 2.0;

        private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

        @Nullable // The target can be null if we are aiming at the ground
        private final LivingEntity target;
        private Vec3 destinationPoint;
        private Vec3 axeStartPos, flailStartPos;
        private boolean axeHasHit = false;
        private boolean flailHasHit = false;

        public ArcsOfJusticeForm(LivingEntity user, BreathingForms form) {
            super(user, form, FLAIL_IMPACT_TICK + 5);
            this.axeStartPos = user.getEyePosition().add(user.getLookAngle().cross(new Vec3(0,1,0)).normalize().scale(1.5));
            this.flailStartPos = user.getEyePosition().add(user.getLookAngle().cross(new Vec3(0,1,0)).normalize().scale(-1.5));

            this.target = getLookedAtEntity(user, MAX_RANGE);
            if (this.target != null) {
                this.destinationPoint = target.position();
            } else {
                BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
                if (hitResult.getType() == HitResult.Type.MISS) {
                    this.destinationPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
                } else {
                    this.destinationPoint = Vec3.atCenterOf(hitResult.getBlockPos());
                }
            }
        }

        @Override
        protected void onStart() {
            performGenericAttack(user, SoundEvents.ANVIL_LAND, 1.2f, 0.7f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            // If we have a living target, update the destination point so the attack tracks them.
            if (this.target != null && this.target.isAlive()) {
                this.destinationPoint = this.target.position();
            }

            // Axe travels faster in a direct line
            double axeProgress = Math.min((double) ticksActive / AXE_IMPACT_TICK, 1.0);
            Vec3 currentAxePos = this.axeStartPos.lerp(this.destinationPoint, axeProgress);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), currentAxePos.x, currentAxePos.y, currentAxePos.z, 2, 0,0,0,0);

            // Flail travels slower in a wider arc
            double flailProgress = Math.min((double) ticksActive / FLAIL_IMPACT_TICK, 1.0);
            Vec3 flailPathCenter = this.flailStartPos.lerp(this.destinationPoint, flailProgress);
            double arcHeight = Math.sin(flailProgress * Math.PI) * 3.0; // The arc goes 3 blocks high at its peak
            Vec3 currentFlailPos = flailPathCenter.add(0, arcHeight, 0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), currentFlailPos.x, currentFlailPos.y, currentFlailPos.z, 2, 0,0,0,0);

            // Axe Impact
            if (!axeHasHit && ticksActive >= AXE_IMPACT_TICK) {
                axeHasHit = true;
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, this.destinationPoint, IMPACT_RADIUS, AXE_DAMAGE, 0.5f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, this.destinationPoint, IMPACT_RADIUS, AXE_DAMAGE, 0.5f, DamageSource.mobAttack(user));
                }
                level.playSound(null, destinationPoint.x, destinationPoint.y, destinationPoint.z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.5f, 0.8f);
            }

            // Flail Impact
            if (!flailHasHit && ticksActive >= FLAIL_IMPACT_TICK) {
                flailHasHit = true;
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, this.destinationPoint, IMPACT_RADIUS * 1.5, FLAIL_DAMAGE, 1.0f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, this.destinationPoint, IMPACT_RADIUS * 1.5, FLAIL_DAMAGE, 1.0f, DamageSource.mobAttack(user));
                }
                level.playSound(null, destinationPoint.x, destinationPoint.y, destinationPoint.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.5f, 1.0f);
            }
        }
    }


    // --- FLOWER FORMS ---
    public static class HonorableShadowPlumParry extends ParryForm {
        private static final int PARRY_WINDOW = 20; // 1 second parry window
        private static final float COUNTER_DAMAGE = 15.0f;
        private static final double COUNTER_REACH = 5.0;
        public HonorableShadowPlumParry(LivingEntity user, BreathingForms form) {
            super(user, form, PARRY_WINDOW);
        }

        @Override
        protected void onStart() {
            super.onStart();
            level.playSound(null, user.blockPosition(), SoundEvents.SPORE_BLOSSOM_FALL, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            if (level.random.nextInt(2) == 0) {
                level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                        user.getX() + (level.random.nextDouble() - 0.5) * 3.0,
                        user.getY() + 2.5,
                        user.getZ() + (level.random.nextDouble() - 0.5) * 3.0,
                        1, 0, -0.08, 0, 1);
            }
        }

        @Override
        protected void onParrySuccess(Entity attacker) {
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.8f);
            level.sendParticles(ParticleTypes.HEART, user.getX(), user.getEyeY(), user.getZ(), 10, 0.5, 0.5, 0.5, 0.1);

            for (int i = 0; i < 4; i++) {
                float yaw = user.getYRot() + (i * 90);
                dealDirectionalDamageInYaw(user, COUNTER_REACH, COUNTER_DAMAGE, 0.4f, yaw);
                spawnParticleArcInWorld(level, user.getEyePosition(), COUNTER_REACH, 140, 20, yaw, 0, ParticleTypes.FALLING_SPORE_BLOSSOM);
            }
        }
    }

    public static class WhirlingPeachForm extends TimedForm {
        private static final int DURATION = 20; // A quick 1-second evasive move
        private static final float LEAP_STRENGTH = 0.7f;
        private static final float BACKWARD_STRENGTH = 1.0f;
        private static final double AOE_RADIUS = 3.5;
        private static final float DAMAGE_PER_TICK = 3.0f;
        private static final float ROTATION_SPEED = 45.0f; // degrees per tick

        private static final DustParticleOptions PINK_PETAL = new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.8f), 1.2f);
        private static final DustParticleOptions GREEN_PETAL = new DustParticleOptions(new Vector3f(0.5f, 1.0f, 0.5f), 1.2f);

        private final float startYaw;

        public WhirlingPeachForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.startYaw = user.getYRot();
        }

        @Override
        protected void onStart() {
            Vec3 backward = user.getLookAngle().scale(-1);
            Vec3 leap = new Vec3(backward.x * BACKWARD_STRENGTH, LEAP_STRENGTH, backward.z * BACKWARD_STRENGTH);
            user.setDeltaMovement(user.getDeltaMovement().add(leap));
            user.hurtMarked = true;
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            float newYaw = this.startYaw + (ticksActive * ROTATION_SPEED);
            user.setYRot(newYaw);
            user.setYHeadRot(newYaw);
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.3f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), AOE_RADIUS, DAMAGE_PER_TICK, 0.3f, DamageSource.mobAttack(user));
            }
            for (int i = 0; i < 10; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double radius = level.random.nextDouble() * AOE_RADIUS;
                Vec3 offset = new Vec3(Math.cos(angle) * radius, (level.random.nextDouble() - 0.5) * 2.5, Math.sin(angle) * radius);

                ParticleOptions particle = level.random.nextBoolean() ? PINK_PETAL : GREEN_PETAL;
                level.sendParticles(particle, user.getX() + offset.x, user.getY() + 1.0 + offset.y, user.getZ() + offset.z, 1, 0, 0, 0, 0);
                if (level.random.nextInt(5) == 0) {
                    level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, user.getX() + offset.x, user.getY() + 2.0, user.getZ() + offset.z, 1, 0, -0.1, 0, 1);
                }
            }
        }
    }

    public static class VermilionEyeForm extends TimedForm {
        private static final int DURATION = 200; // 10 seconds of super mode
        private static final int BURNOUT_DURATION = 300; // 15 seconds of debuffs after it ends
        private static final double AURA_RADIUS = 12.0;

        // Effect Amplifiers (0=I, 1=II, etc.)
        private static final int SPEED_AMP = 3;      // Speed IV
        private static final int STRENGTH_AMP = 2;   // Strength III
        private static final int HASTE_AMP = 3;      // Haste IV
        private static final int SLOWNESS_AMP = 3;   // Slowness IV
        private static final int FATIGUE_AMP = 3;    // Mining Fatigue IV

        public VermilionEyeForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION + 40, SPEED_AMP, false, false, true));
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION + 40, STRENGTH_AMP, false, false, true));
            user.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, DURATION + 40, HASTE_AMP, false, false, true));
            level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 1.2f);

            if(user instanceof ServerPlayer player) {
                ModMessages.sendToPlayer(new VignetteS2CPacket(true), player);
            }
        }

        @Override
        protected void onTick() {
            super.onTick();

            AABB auraBox = user.getBoundingBox().inflate(AURA_RADIUS);
            List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != user && e.isAlive());

            for (LivingEntity entity : nearbyEntities) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, SLOWNESS_AMP, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, FATIGUE_AMP, false, false, true));
            }
        }

        @Override
        protected void onStop() {
            level.playSound(null, user.blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);

            user.hurt(DamageSource.MAGIC, 2.0f);
            user.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BURNOUT_DURATION, 0, false, false, true));
            user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BURNOUT_DURATION, 1, false, false, true));

            if(user instanceof ServerPlayer player) {
                ModMessages.sendToPlayer(new VignetteS2CPacket(false), player);
            }
        }
    }


    // -- SERPENT FORMS ---
    public static class VenomFangsForm extends TimedForm {
        private static final int DURATION = 12; // A very short duration for a quick dash/attack
        private static final double DASH_SPEED = 2.5;
        private static final int SLASH_DELAY = 3; // Slash happens 3 ticks after teleporting
        private static final float STAB_DAMAGE = 8.0f;

        private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.8f), 1.0f);

        @Nullable
        private final LivingEntity target;

        public VenomFangsForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.target = getLookedAtEntity(user, 12.0);
        }

        @Override
        public boolean isInterruptible() { return true; }

        @Override
        protected void onStart() {
            if (this.target != null) {
                level.playSound(null, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.sendParticles(ParticleTypes.POOF, user.getX(), user.getY(), user.getZ(), 20, 0.3, 0.5, 0.3, 0.2);

                Vec3 teleportPos = target.position().add(target.getLookAngle().scale(-2.0));
                user.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
                user.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            } else {
                Vec3 dashVec = user.getLookAngle().scale(DASH_SPEED);
                user.setDeltaMovement(dashVec);
                user.hurtMarked = true;
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.target != null) {
                if (!this.target.isAlive()) {
                    ActiveFormManager.stop(user);
                    return;
                }

                if (ticksActive == SLASH_DELAY) {
                    performGenericAttack(user, SoundEvents.BEE_STING, 1.2f, 1.0f);
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), STAB_DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), STAB_DAMAGE);
                    }
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
                }
            } else {
                level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 2, 0, 0, 0, 0);
            }
        }
    }

    public static class CoilChokeForm extends TimedForm {
        private static final int DURATION = 60;
        private static final float DAMAGE_PER_PULSE = 3.0f;
        private static final int DAMAGE_TICK_RATE = 5;
        private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);

        private static final double ORBIT_RADIUS = 3.0;
        private static final float ORBIT_SPEED = 20.0f;

        private static final double DASH_DISTANCE = 12.0;
        private static final double COIL_START_RADIUS = 2.5;
        private static final float COIL_ROTATION_SPEED = 40.0f;

        @Nullable
        private final LivingEntity target;
        private float currentAngle;
        private final Vec3 startPos;
        private final Vec3 direction;

        public CoilChokeForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.target = getLookedAtEntity(user, 10.0);
            this.startPos = user.position();
            this.direction = user.getLookAngle();
            this.currentAngle = user.getYRot();
        }

        @Override
        protected void onStart() {
            if (!user.isOnGround()) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }
            if (this.target != null) {
                // Root the player for the orbital assault.
                applyImmobility(user, this.duration + 5);
            }
            // Use a better sound that isn't the Enderman stare.
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.target != null) {
                // --- TARGETED "ORBITAL ASSAULT" (With Smooth Movement) ---
                if (!this.target.isAlive() || this.target.distanceTo(user) > 32) {
                    ActiveFormManager.stop(user);
                    return;
                }

                // --- 1. Hybrid Movement Logic ---
                // Calculate the IDEAL position for the CURRENT and NEXT ticks on the circle.
                this.currentAngle += ORBIT_SPEED;
                double currentAngleRad = Math.toRadians(this.currentAngle);
                double nextAngleRad = Math.toRadians(this.currentAngle + ORBIT_SPEED);

                Vec3 currentOffset = new Vec3(Math.cos(currentAngleRad), 0, Math.sin(currentAngleRad)).scale(ORBIT_RADIUS);
                Vec3 nextOffset = new Vec3(Math.cos(nextAngleRad), 0, Math.sin(nextAngleRad)).scale(ORBIT_RADIUS);

                Vec3 currentOrbitPos = this.target.position().add(currentOffset);
                Vec3 nextOrbitPos = this.target.position().add(nextOffset);

                // Set player's server-side position directly.
                user.setPos(currentOrbitPos.x, currentOrbitPos.y, currentOrbitPos.z);
                // Calculate the velocity hint for the client.
                Vec3 velocityHint = nextOrbitPos.subtract(currentOrbitPos);
                user.setDeltaMovement(velocityHint);
                user.hurtMarked = true;
//                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            } else {
                // --- UNTARGETED "COILING SERPENT DASH" (With Smooth Movement) ---
                double progress = (double) ticksActive / DURATION;
                double nextProgress = Math.min((double)(ticksActive + 1) / DURATION, 1.0);

                // --- 2. Hybrid Movement Logic (Same pattern as ShiftingFlowSlash) ---
                // Calculate IDEAL position for CURRENT tick
                Vec3 forwardPos = this.startPos.add(this.direction.scale(progress * DASH_DISTANCE));
                double currentCoilRadius = COIL_START_RADIUS * (1.0 - progress);
                this.currentAngle += COIL_ROTATION_SPEED;
                double angleRad = Math.toRadians(this.currentAngle);
                Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 offset = sideDir.scale(Math.cos(angleRad) * currentCoilRadius);
                Vec3 currentCurvePos = forwardPos.add(offset);
                user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

                // Calculate IDEAL position for NEXT tick
                Vec3 nextForwardPos = this.startPos.add(this.direction.scale(nextProgress * DASH_DISTANCE));
                double nextCoilRadius = COIL_START_RADIUS * (1.0 - nextProgress);
                double nextAngleRad = Math.toRadians(this.currentAngle + COIL_ROTATION_SPEED);
                Vec3 nextOffset = sideDir.scale(Math.cos(nextAngleRad) * nextCoilRadius);
                Vec3 nextCurvePos = nextForwardPos.add(nextOffset);

                // Set velocity hint for the client
                Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);
                user.setDeltaMovement(velocityHint);
                user.hurtMarked = true;
//                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            }

            // --- Shared Damage and Visuals (Unchanged) ---
            if (ticksActive % DAMAGE_TICK_RATE == 0) {
                if(user instanceof ServerPlayer player) {
                    dealAOEDamage(player, player.position(), 3.0, DAMAGE_PER_PULSE, 0.2f, DamageSource.playerAttack(player));
                } else {
                    dealAOEDamage(user, user.position(), 3.0, DAMAGE_PER_PULSE, 0.2f, DamageSource.mobAttack(user));
                }
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
            }
            level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    public static class SlitheringSerpentForm extends TimedForm {
        private static final int DURATION = 30; // 1.5 second dash
        private static final double DASH_DISTANCE = 16.0;
        private static final double COIL_START_RADIUS = 3.0;
        private static final float COIL_ROTATION_SPEED = 30.0f;
        private static final float DAMAGE_PER_TICK = 4.0f;
        private static final double DAMAGE_RADIUS = 4.0;

        private static final DustParticleOptions SERPENT_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.9f), 1.0f);

        private final Vec3 startPos;
        private final Vec3 direction;
        private float currentAngle;

        public SlitheringSerpentForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.startPos = user.position();
            this.direction = user.getLookAngle();
            this.currentAngle = user.getYRot();
        }

        @Override
        protected void onTick() {
            super.onTick();
            double progress = (double) ticksActive / DURATION;
            Vec3 forwardPos = this.startPos.add(this.direction.scale(progress * DASH_DISTANCE));
            double currentCoilRadius = COIL_START_RADIUS * (1.0 - progress);
            this.currentAngle += COIL_ROTATION_SPEED;
            double angleRad = Math.toRadians(this.currentAngle);
            Vec3 sideDir = this.direction.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 offset = sideDir.scale(Math.cos(angleRad) * currentCoilRadius);
            Vec3 currentCurvePos = forwardPos.add(offset);
            user.setPos(currentCurvePos.x, currentCurvePos.y, currentCurvePos.z);

            double nextProgress = Math.min((double)(ticksActive + 1) / DURATION, 1.0);
            Vec3 nextForwardPos = this.startPos.add(this.direction.scale(nextProgress * DASH_DISTANCE));
            double nextCoilRadius = COIL_START_RADIUS * (1.0 - nextProgress);
            double nextAngleRad = Math.toRadians(this.currentAngle + COIL_ROTATION_SPEED);
            Vec3 nextOffset = sideDir.scale(Math.cos(nextAngleRad) * nextCoilRadius);
            Vec3 nextCurvePos = nextForwardPos.add(nextOffset);

            Vec3 velocityHint = nextCurvePos.subtract(currentCurvePos);
            user.setDeltaMovement(velocityHint);
            user.hurtMarked = true;
//            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, player.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f, DamageSource.playerAttack(player));
            } else {
                dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_TICK, 0.1f, DamageSource.mobAttack(user));
            }
            level.sendParticles(SERPENT_PARTICLE, user.getX(), user.getY() + 1, user.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            if (ticksActive % 5 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, user.getX(), user.getEyeY(), user.getZ(), 2, 1.5, 0.1, 1.5, 0.0);
            }
        }
    }

    // -- BDA, Rui ---
    public static class CuttingThreadCageForm extends TimedForm {
        private static final int DURATION = 5; // A very short form, as the cage does the work

        public CuttingThreadCageForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            Vec3 targetPos = null;
            LivingEntity lookedAtEntity = getLookedAtEntity(user, 16.0D);

            if (lookedAtEntity != null) {
                targetPos = lookedAtEntity.position();
            } else {
                BlockHitResult hitResult = (BlockHitResult) user.pick(16.0D, 0, false);
                if (hitResult.getType() != HitResult.Type.MISS) {
                    targetPos = hitResult.getLocation();
                }
            }

            if (targetPos != null) {
                user.level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.8F);

                CuttingThreadCageEntity cage = ModEntityTypes.CUTTING_THREAD_CAGE.get().create(level);
                if (cage != null) {
                    cage.setPos(targetPos);
                    level.addFreshEntity(cage);
                }
            } else {
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onTick() {
            super.onTick(); // Handles stopping the form
        }
    }

    public static class CuttingThreadRotationForm extends TimedForm {
        public CuttingThreadRotationForm(LivingEntity user, BreathingForms form) {
            super(user, form, 5);
        }

        @Override
        protected void onStart() {
            user.level.playSound(null, user.blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 1.2F, 0.7F);

            ThreadWheelEntity wheel = ModEntityTypes.THREAD_WHEEL.get().create(level);
            if (wheel != null) {
                Vec3 spawnPos = user.position().add(user.getLookAngle().scale(1.5));
                wheel.moveTo(spawnPos.x, spawnPos.y + 0.5, spawnPos.z, user.getYRot(), user.getXRot());

                final double SPEED = 0.4;
                wheel.setDirection(user.getLookAngle(), SPEED);

                level.addFreshEntity(wheel);
            }
        }

        @Override
        protected void onTick() {
            super.onTick();
        }
    }

    public static class MurderousEyeBasketForm extends ActiveForm {
        private enum Phase { FORMATION, TRAPPED, CONTRACTION }

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


        public MurderousEyeBasketForm(LivingEntity user, BreathingForms form) {
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
        protected void onStart() {
            if (this.cageCenter == null) {
                ActiveFormManager.stop(user);
                return;
            }
            level.playSound(null, cageCenter.x, cageCenter.y, cageCenter.z, SoundEvents.SPIDER_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
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
                        dealAOEDamage(user, this.cageCenter, 1.0, CONTRACTION_BASE_DAMAGE + CONTRACTION_RAMP_DAMAGE, 0.5f, DamageSource.MAGIC);
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

                for(LivingEntity target : targets) {
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

    // -- BDA, Enmu --
    public static class WhispersHypnosisForm extends ActiveForm {
        private static final int SLEEP_DURATION = 1200;
        private static final double MAX_RANGE = 16.0;

        public WhispersHypnosisForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        protected void onStart() {
            LivingEntity target = getLookedAtEntity(user, MAX_RANGE);
            if (target != null) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);

                target.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), SLEEP_DURATION, 0));
                ForcedSleepEffect.setCaster(target, user);

                final DustParticleOptions whisperParticle = new DustParticleOptions(new Vector3f(0.2f, 0.1f, 0.3f), 1.0f);
                spawnLineParticles(level, whisperParticle, user.getEyePosition(), target.getEyePosition(), 15);

                if (target instanceof ServerPlayer sleepingPlayer) {
                    List<Integer> keys = new ArrayList<>();
                    keys.add(GLFW.GLFW_KEY_W);
                    keys.add(GLFW.GLFW_KEY_A);
                    keys.add(GLFW.GLFW_KEY_S);
                    keys.add(GLFW.GLFW_KEY_D);

                    Collections.shuffle(keys);
                    ModMessages.sendToPlayer(new StartSleepQTES2CPacket(keys), sleepingPlayer);
                }
            } else {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() { }
    }

    public static class EyesOfSleepForm extends ActiveForm {
        private static final double MAX_SPAWN_RANGE = 20.0;

        public EyesOfSleepForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_SPAWN_RANGE, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }

            BlockPos spawnPos = hitResult.getBlockPos().above();

            MotherEyeEntity motherEye = ModEntityTypes.MOTHER_EYE.get().create(level);
            if (motherEye != null) {
                motherEye.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                motherEye.setCaster(user);
                level.addFreshEntity(motherEye);
                user.startRiding(motherEye, true);

                level.playSound(null, spawnPos, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.5f, 0.8f);
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() {  }
    }

    // -- BDA, Daki --
    public static class EightLayeredObiSlashForm extends TimedForm {
        private static final int NUMBER_OF_SLASHES = 8;
        private static final int TICKS_BETWEEN_SLASHES = 4; // Slightly slower than mist for a heavier feel
        private static final float DAMAGE_PER_SLASH = 5.0f; // Total damage = 40
        private static final double RANGE = 12.0; // Long-ranged attack

        private static final DustParticleOptions OBI_PINK = new DustParticleOptions(new Vector3f(0.9f, 0.4f, 0.8f), 1.0f);
        private static final DustParticleOptions OBI_PATTERN = new DustParticleOptions(new Vector3f(0.1f, 0.1f, 0.1f), 1.2f);


        public EightLayeredObiSlashForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_SLASHES * TICKS_BETWEEN_SLASHES);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SLASHES == 0) {
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.8f);
                level.playSound(null, user.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);

                final int segments = 20;
                Vec3 eyePos = user.getEyePosition();
                Vec3 lookDir = user.getLookAngle();
                Vec3 origin = eyePos.subtract(lookDir.scale(1.5));
                Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

                for (int i = 0; i <= segments; i++) {
                    double progress = (double) i / segments;
                    Vec3 pathPoint = origin.add(lookDir.scale(progress * RANGE));

                    double wind_amplitude = 1.2;
                    double wind_frequency = 2.0 + level.random.nextDouble();
                    double windOffset = Math.sin(progress * Math.PI * wind_frequency) * wind_amplitude;
                    Vec3 particlePos = pathPoint.add(sideDir.scale(windOffset));

                    level.sendParticles(OBI_PINK, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                    if (i % 3 == 0) {
                        level.sendParticles(OBI_PATTERN, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                    }
                }
                dealThrustDamage(user, RANGE, DAMAGE_PER_SLASH);
            }
        }
    }

    // -- BDA, Gyutaro --
    public static class FlyingBloodSicklesForm extends TimedForm {
        private static final int SICKLE_COUNT = 6;
        private static final int TICKS_BETWEEN_SICKLES = 3;
        private final LivingEntity target;

        public FlyingBloodSicklesForm(LivingEntity user, BreathingForms form) {
            super(user, form, SICKLE_COUNT * TICKS_BETWEEN_SICKLES);
            this.target = getLookedAtEntity(user, 32.0);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_SICKLES == 0) {
                performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 2.0f);
                BloodSickleProjectileEntity sickle = new BloodSickleProjectileEntity(level, user, this.target);
                Vec3 spread = new Vec3(
                        level.random.nextGaussian() * 0.1,
                        level.random.nextGaussian() * 0.1,
                        level.random.nextGaussian() * 0.1
                );

                sickle.shoot(user.getLookAngle().x, user.getLookAngle().y, user.getLookAngle().z, 1.5f, 5.0f);
                sickle.setDeltaMovement(sickle.getDeltaMovement().add(spread));

                level.addFreshEntity(sickle);
            }
        }
    }

    public static class RampantArcRampageParry extends ParryForm {
        private static final int PARRY_WINDOW = 25;
        private static final float COUNTER_DAMAGE = 20.0f;
        private static final double COUNTER_RADIUS = 7.0;
        private static final float COUNTER_KNOCKBACK = 1.8f;
        private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 1.0f);

        private float rotation = 0;

        public RampantArcRampageParry(LivingEntity user, BreathingForms form) {
            super(user, form, PARRY_WINDOW);
        }

        @Override
        protected void onStart() {
            super.onStart();
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.7f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            this.rotation += 40.0f;
            double radius = 2.0;

            spawnParticleArcInWorld(level, user.getEyePosition(), radius, 360, 10, this.rotation, 0, BLOOD_PARTICLE);
            spawnParticleArcInWorld(level, user.getEyePosition(), radius, 360, 10, -this.rotation, 90, ParticleTypes.SWEEP_ATTACK);
        }

        @Override
        protected void onParrySuccess(Entity attacker) {
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.5f);

            dealAOEDamage(user, user.position(), COUNTER_RADIUS, COUNTER_DAMAGE, COUNTER_KNOCKBACK, DamageSource.mobAttack(user));

            for (int i = 0; i < 200; i++) {
                Vec3 randomDir = new Vec3(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5).normalize();
                Vec3 particlePos = user.getEyePosition().add(randomDir.scale(level.random.nextDouble() * COUNTER_RADIUS));
                level.sendParticles(i % 3 == 0 ? ParticleTypes.SWEEP_ATTACK : BLOOD_PARTICLE,
                        particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static class RotatingCircularSlashesForm extends TimedForm {
        private static final int WAVE_DURATION = 30; // The wave expands over 1.5 seconds
        private static final double MAX_RANGE = 15.0; // The wave travels 15 blocks
        private static final double WAVE_HEIGHT = 4.0;  // The wave is 4 blocks tall
        private static final float DAMAGE = 15.0f;
        private static final DustParticleOptions BLOOD_PARTICLE = new DustParticleOptions(new Vector3f(0.7f, 0.1f, 0.1f), 1.0f);

        private float rotation = 0;
        private final List<UUID> hitEntities = new ArrayList<>();

        public RotatingCircularSlashesForm(LivingEntity user, BreathingForms form) {
            super(user, form, WAVE_DURATION);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.5f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            float progress = (float) ticksActive / WAVE_DURATION;
            float prevProgress = (float) (ticksActive - 1) / WAVE_DURATION;
            double currentRadius = MAX_RANGE * progress;
            double previousRadius = MAX_RANGE * prevProgress;

            AABB damageZone = user.getBoundingBox().inflate(currentRadius, WAVE_HEIGHT / 2.0, currentRadius);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                    e -> e != user && !hitEntities.contains(e.getUUID()));

            for (LivingEntity target : potentialTargets) {
                double dist = target.distanceTo(user);
                if (dist <= currentRadius && dist > previousRadius) {
                    target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    hitEntities.add(target.getUUID());

                    Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                    target.push(knockbackDir.x * 0.8, 0.2, knockbackDir.z * 0.8);
                }
            }

            this.rotation += 30.0f; // Rotation speed
            for (int i = 0; i < 36; i++) {
                double angle = Math.toRadians(i * 10 + this.rotation);
                for (double y = 0; y <= WAVE_HEIGHT; y += 0.5) {
                    Vec3 particlePos = user.position().add(
                            Math.cos(angle) * currentRadius,
                            y,
                            Math.sin(angle) * currentRadius
                    );
                    level.sendParticles(level.random.nextFloat() > 0.3f ? BLOOD_PARTICLE : ParticleTypes.SWEEP_ATTACK,
                            particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // -- BDA, Gyokko --
    public static class ThousandNeedleFishKillForm extends ActiveForm {
        private static final int FISH_COUNT = 3;
        private static final double SPAWN_RADIUS = 3.0;

        public ThousandNeedleFishKillForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);

            for (int i = 0; i < FISH_COUNT; i++) {
                double angle = (2 * Math.PI / FISH_COUNT) * i;
                double x = user.getX() + Math.cos(angle) * SPAWN_RADIUS;
                double z = user.getZ() + Math.sin(angle) * SPAWN_RADIUS;
                double y = user.getY() + 1.0;

                KillerGoldfishEntity fish = ModEntityTypes.KILLER_GOLDFISH.get().create(level);
                if (fish != null) {
                    fish.setPos(x, y, z);
                    fish.setOwner(user);
                    level.addFreshEntity(fish);
                }
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() {
        }
    }

    public static class WaterPrisonPotForm extends ActiveForm {
        private static final double MAX_RANGE = 25.0;

        public WaterPrisonPotForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            LivingEntity target = getLookedAtEntity(user, MAX_RANGE);

            if (target != null) {
                WaterPrisonEntity prison = ModEntityTypes.WATER_PRISON_POT.get().create(level);
                if (prison != null) {
                    prison.setPos(target.position());
                    applyImmobility(target, 220);
                    prison.setTrappedEntity(target);

                    level.addFreshEntity(prison);
                    level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 1.0f);
                }
            } else {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
            }
            ActiveFormManager.stop(user);
        }
        @Override
        protected void onTick() { }
    }

    public static class OctopusVaseHellForm extends ActiveForm {
        private static final double MAX_SPAWN_RANGE = 20.0;

        public OctopusVaseHellForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_SPAWN_RANGE, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }

            BlockPos spawnPos = hitResult.getBlockPos().above();
            OctopusPotEntity pot = ModEntityTypes.OCTOPUS_POT.get().create(level);
            if (pot != null) {
                pot.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                pot.setOwner(user);
                level.addFreshEntity(pot);
                level.playSound(null, spawnPos, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 2.0f, 0.7f);
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() { }
    }

    public static class TenThousandFishBarrageForm extends ActiveForm {
        private static final float DAMAGE_PER_HIT = 7.0f;
        private static final double RANGE = 25.0;
        private static final double IMPACT_RADIUS = 2.0;

        public TenThousandFishBarrageForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }
        @Override
        public boolean isInterruptible() { return false; }
        @Override
        protected void onStart() {
            Vec3 targetPoint;
            LivingEntity lookedAtEntity = getLookedAtEntity(user, RANGE);
            if (lookedAtEntity != null) {
                targetPoint = lookedAtEntity.position().add(0, lookedAtEntity.getBbHeight() / 2.0, 0);
            } else {
                targetPoint = user.getEyePosition().add(user.getLookAngle().scale(RANGE));
            }
            List<Vec3> origins = List.of(
                    new Vec3(0.6, 1.4, 0.4), new Vec3(-0.6, 1.4, 0.4),
                    new Vec3(0.8, 1.0, 0.6), new Vec3(-0.8, 1.0, 0.6),
                    new Vec3(1.0, 0.7, 0.8), new Vec3(-1.0, 0.7, 0.8)
            );
            level.playSound(null, user.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 1.5f, 0.8f);
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            for (Vec3 localOrigin : origins) {
                Vec3 startPoint = user.position().add(localOrigin.yRot(-user.getYRot() * Mth.DEG_TO_RAD));
                double spread = 4.0;
                Vec3 finalTargetPoint = targetPoint.add(
                        (level.random.nextDouble() - 0.5) * spread,
                        (level.random.nextDouble() - 0.5) * spread,
                        (level.random.nextDouble() - 0.5) * spread
                );
                final DustParticleOptions fishParticle = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 0.6f), 0.7f);
                double weaveAmplitude = 0.4 + level.random.nextDouble() * 0.4;
                double weaveFrequency = 3.0 + level.random.nextDouble() * 2.0;
                spawnParticleStreamWithWeave(level, fishParticle, startPoint, finalTargetPoint, weaveAmplitude, weaveFrequency, 60);
                dealAOEDamage(user, finalTargetPoint, IMPACT_RADIUS, DAMAGE_PER_HIT, 0.5f, DamageSource.mobAttack(user));
            }
            ActiveFormManager.stop(user);
        }
        @Override
        protected void onTick() { }
    }

    public static class KillerFishScalesForm extends TimedForm {
        private static final int DURATION = 100;
        private static final int DASH_COOLDOWN = 10;
        private static final int MAX_BOUNCES = 6;
        private static final double DASH_RANGE = 15.0;
        private static final float DAMAGE_PER_HIT = 12.0f;
        private static final double DAMAGE_RADIUS = 3.0;

        @Nullable
        private LivingEntity currentTarget;
        private int bouncesLeft;
        private int dashTicks;

        public KillerFishScalesForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
            this.bouncesLeft = MAX_BOUNCES;
            this.dashTicks = 0;
            this.currentTarget = getLookedAtEntity(user, DASH_RANGE);
        }

        @Override
        protected void onStart() {
            if (this.currentTarget == null) {
                level.playSound(null, user.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.5f);
                ActiveFormManager.stop(user);
                return;
            }
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION + 5, 2));
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION + 5, 1));
            level.playSound(null, user.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.5f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            this.dashTicks--;

            final DustParticleOptions scaleParticle = new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.7f), 1.0f);
            for (int i = 0; i < 5; i++) {
                double angle = (ticksActive * 0.5) + (i * Math.PI / 2.5);
                Vec3 offset = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(1.2);
                level.sendParticles(scaleParticle, user.getX() + offset.x, user.getY() + 1.0, user.getZ() + offset.z, 1, 0, 0.1, 0, 0);
            }

            if (this.dashTicks <= 0) {
                if (this.currentTarget == null || !this.currentTarget.isAlive() || this.bouncesLeft <= 0) {
                    ActiveFormManager.stop(user);
                    return;
                }
                user.teleportTo(this.currentTarget.getX(), this.currentTarget.getY(), this.currentTarget.getZ());
                dealAOEDamage(user, user.position(), DAMAGE_RADIUS, DAMAGE_PER_HIT, 0.8f, DamageSource.mobAttack(user));
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
                level.sendParticles(ParticleTypes.EXPLOSION, user.getX(), user.getY(), user.getZ(), 1, 0,0,0,0);
                this.bouncesLeft--;

                List<LivingEntity> potentialNextTargets = level.getEntitiesOfClass(LivingEntity.class,
                        user.getBoundingBox().inflate(DASH_RANGE),
                        e -> e != user && e != this.currentTarget && e.isAlive() && !(e instanceof Player && ((Player) e).isCreative()));

                if (potentialNextTargets.isEmpty()) {
                    this.currentTarget = null;
                } else {
                    this.currentTarget = potentialNextTargets.get(level.random.nextInt(potentialNextTargets.size()));
                }
                this.dashTicks = DASH_COOLDOWN;
            }
        }

        @Override
        protected void onStop() {
            user.removeEffect(MobEffects.MOVEMENT_SPEED);
            user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
    }

    // -- BDA, Hantengu --
    public static class WeepingSpearsForm extends ActiveForm {
        private static final int SPEAR_COUNT = 6;
        private static final float SPREAD = 3.0f;

        public WeepingSpearsForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5f, 0.8f);
            performGenericAttack(user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

            for (int i = 0; i < SPEAR_COUNT; i++) {
                WeepingSpearProjectileEntity spear = new WeepingSpearProjectileEntity(level, user);
                Vec3 lookAngle = user.getLookAngle();
                spear.setPos(user.getEyePosition());
                spear.shoot(lookAngle.x, lookAngle.y, lookAngle.z, 2.5f, SPREAD);

                level.addFreshEntity(spear);
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() { }
    }

    public static class CrazedCryOfThunderdeathForm extends ActiveForm {
        private static final int WIND_UP_TICKS = 60;
        private static final int LIGHTNING_START_TICKS = 80;
        private static final int LIGHTNING_TICK_RATE = 5;
        private static final double ARENA_RADIUS = 25.0;

        private boolean soundWaveFired = false;
        private final Vec3 arenaCenter;

        public CrazedCryOfThunderdeathForm(LivingEntity user, BreathingForms form) {
            super(user, form);
            this.arenaCenter = user.position().add(user.getLookAngle().scale(ARENA_RADIUS));
        }

        @Override
        public boolean isInterruptible() {
            return true;
        }

        @Override
        protected void onStart() {
            applyImmobility(user, Integer.MAX_VALUE);
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 1.2f);
        }

        @Override
        protected void onTick() {
            if (user.isCrouching()) {
                ActiveFormManager.stop(user);
                return;
            }

            final DustParticleOptions woodParticle = new DustParticleOptions(new Vector3f(0.4f, 0.2f, 0.1f), 1.0f);
            double angle1 = (ticksActive * 0.05);
            Vec3 dragon1Pos = arenaCenter.add(Math.cos(angle1) * ARENA_RADIUS, 10, Math.sin(angle1) * ARENA_RADIUS);
            level.sendParticles(woodParticle, dragon1Pos.x, dragon1Pos.y, dragon1Pos.z, 10, 1, 1, 1, 0);

            double angle2 = (ticksActive * 0.05) + Math.PI;
            Vec3 dragon2Pos = arenaCenter.add(Math.cos(angle2) * ARENA_RADIUS, 10, Math.sin(angle2) * ARENA_RADIUS);
            level.sendParticles(woodParticle, dragon2Pos.x, dragon2Pos.y, dragon2Pos.z, 10, 1, 1, 1, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, dragon2Pos.x, dragon2Pos.y, dragon2Pos.z, 5, 0.5, 0.5, 0.5, 0.1);

            if (!soundWaveFired && ticksActive >= WIND_UP_TICKS) {
                soundWaveFired = true;
                level.playSound(null, arenaCenter.x, arenaCenter.y, arenaCenter.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.8f);
                level.sendParticles(ParticleTypes.SONIC_BOOM, arenaCenter.x, arenaCenter.y, arenaCenter.z, 1, 0,0,0,0);

                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(arenaCenter, arenaCenter).inflate(ARENA_RADIUS));
                for(LivingEntity target : targets) {
                    if(target != user) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2));
                        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                    }
                }
            }

            if (soundWaveFired && ticksActive >= LIGHTNING_START_TICKS) {
                if (ticksActive % LIGHTNING_TICK_RATE == 0) {
                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(arenaCenter, arenaCenter).inflate(ARENA_RADIUS),
                            e -> e != user);

                    if (!targets.isEmpty()) {
                        LivingEntity randomTarget = targets.get(level.random.nextInt(targets.size()));
                        EntityType.LIGHTNING_BOLT.spawn(level, null, null, randomTarget.blockPosition(), MobSpawnType.TRIGGERED, true, false);
                    }
                }
            }
        }
        
        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    public static class CountlessStrikingTreesForm extends ActiveForm {
        private static final int STRIKE_TICK_RATE = 8;
        private static final double AOE_RADIUS = 35.0;
        private static final double DAMAGE_RADIUS = 3.0;
        private static final float DAMAGE = 18.0f;

        public CountlessStrikingTreesForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        protected void onStart() {
            applyImmobility(user, Integer.MAX_VALUE);
            level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
        }

        @Override
        protected void onTick() {
            if (ticksActive % 20 == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS, 2.0f, 0.5f);
            }
            if (ticksActive > 0 && ticksActive % STRIKE_TICK_RATE == 0) {
                Vec3 strikeTargetPos;
                List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class,
                        user.getBoundingBox().inflate(AOE_RADIUS),
                        e -> e != user && e.isAlive() && !(e instanceof Player && ((Player)e).isCreative()));

                if (!potentialTargets.isEmpty()) {
                    LivingEntity randomTarget = potentialTargets.get(level.random.nextInt(potentialTargets.size()));
                    strikeTargetPos = randomTarget.position();
                } else {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double distance = 5 + level.random.nextDouble() * (AOE_RADIUS - 5);
                    double x = user.getX() + Math.cos(angle) * distance;
                    double z = user.getZ() + Math.sin(angle) * distance;
                    strikeTargetPos = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, user.getY(), z)));
                }
                triggerDragonStrike(strikeTargetPos);
            }
        }

        private void triggerDragonStrike(Vec3 position) {
            // This could be made more complex later, e.g., with a telegraph particle first
            // For now, it's an instant eruption.

            level.playSound(null, position.x, position.y, position.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.5f, 1.0f);
            level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);

            final BlockParticleOption woodParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_WOOD.defaultBlockState());
            for (int i = 0; i < 50; i++) {
                double motionY = 0.5 + level.random.nextDouble() * 0.5;
                level.sendParticles(woodParticle, position.x, position.y, position.z, 3, 0.8, 1.5, 0.8, motionY);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y + 1.0, position.z, 1, 0,0,0,0);
            dealAOEDamage(user, position, DAMAGE_RADIUS, DAMAGE, 1.5f, DamageSource.mobAttack(user));
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    public static class CompressedSoundWavesForm extends ActiveForm {
        private static final double MAX_RANGE = 30.0;
        private static final int KNOCKOUT_DURATION = 140; // 7 seconds

        public CompressedSoundWavesForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            Vec3 start = user.getEyePosition();
            Vec3 end = start.add(user.getLookAngle().scale(MAX_RANGE));
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, user, start, end, new AABB(start, end), e -> e instanceof LivingEntity && !e.is(user));
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.2f);

            if (entityHit != null) {
                LivingEntity target = (LivingEntity) entityHit.getEntity();
                target.hurt(DamageSource.sonicBoom(user), 25.0F);
                target.addEffect(new MobEffectInstance(ModEffects.FORCED_SLEEP.get(), KNOCKOUT_DURATION, 0));
                ForcedSleepEffect.setCaster(target, user);
                ModMessages.sendToClients(new SpawnSonicBeamS2CPacket(start, entityHit.getLocation()));
            } else {
                ModMessages.sendToClients(new SpawnSonicBeamS2CPacket(start, end));
            }
            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() { }
    }


    // -- BDA, Akaza --
    public static class CompassNeedleForm extends ActiveForm {
        private Set<LivingEntity> currentTargets;
        private int rescanTimer = 0;

        public CompassNeedleForm(LivingEntity user, BreathingForms form) {
            super(user, form);
            this.currentTargets = new HashSet<>();
        }
        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            if (!level.isClientSide && user instanceof ServerPlayer caster) {
                applyImmobility(caster, 40);
                updateTargets(caster); // Run the scan once immediately.
                level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);

                CompassNeedleEntity effect = new CompassNeedleEntity(level, caster.getX(), caster.getY() + 0.01, caster.getZ());
                effect.setYRot(caster.getYRot());
                level.addFreshEntity(effect);
            }
        }

        @Override
        protected void onStop() {
            if (!level.isClientSide && user instanceof ServerPlayer caster) {
                removeImmobility(caster);
                if (this.currentTargets == null) return;

                for (LivingEntity target : this.currentTargets) {
                    if (target.isAlive()) {
                        target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                            if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(caster.getUUID())) {
                                cap.clear();
                                ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                            }
                        });
                    }
                }
                level.playSound(null, user.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        }

        @Override
        protected void onTick() {
            if (level.isClientSide || !(user instanceof ServerPlayer caster)) {
                return;
            }

            rescanTimer++;
            if (rescanTimer >= 20) {
                rescanTimer = 0; // Reset the timer
                updateTargets(caster); // Run the update logic.
            }
        }

        private void updateTargets(ServerPlayer caster) {
            Set<LivingEntity> newTargets = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class,
                    caster.getBoundingBox().inflate(64),
                    entity -> entity != caster && entity.isAlive()));
            Set<LivingEntity> targetsToAdd = new HashSet<>(newTargets);
            targetsToAdd.removeAll(this.currentTargets);

            String glowColor = "#FF5555";
            for (LivingEntity target : targetsToAdd) {
                target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                    cap.setGlowColor(glowColor);
                    cap.setExclusiveViewer(caster.getUUID());
                    cap.setGlowing(true);
                    ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), glowColor, true, caster.getUUID()));
                });
            }
            Set<LivingEntity> targetsToRemove = new HashSet<>(this.currentTargets);
            targetsToRemove.removeAll(newTargets);

            for (LivingEntity target : targetsToRemove) {
                if (target.isAlive()) {
                    target.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                        if (cap.hasExclusiveViewer() && cap.getExclusiveViewer().equals(caster.getUUID())) {
                            cap.clear();
                            ModMessages.sendToClients(new UpdateGlowDataPacket(target.getId(), "", false, null));
                        }
                    });
                }
            }
            this.currentTargets = newTargets;
        }
    }

    public static class AirTypeForm extends TimedForm {
        // --- Constants ---
        private static final double MAX_RANGE = 25.0;
        private static final double TRAVEL_SPEED = 5.0;       // Blocks per tick
        private static final double HITBOX_SIZE = 2.0;
        private static final float DAMAGE = 8.0f;
        private static final float KNOCKBACK_STRENGTH = 1.8f;

        private final Vec3 direction;
        private Vec3 currentPos;
        private final List<UUID> hitEntities = new ArrayList<>();

        public AirTypeForm(LivingEntity user, BreathingForms form) {
            // The duration is calculated based on how long it takes to travel the max range
            super(user, form, (int)(MAX_RANGE / TRAVEL_SPEED));
            this.direction = user.getLookAngle();
            this.currentPos = user.getEyePosition(); // Start from the player's eyes
        }

        @Override
        protected void onStart() {
            // Play a sharp, punchy sound effect
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();
            this.currentPos = this.currentPos.add(this.direction.scale(TRAVEL_SPEED));

            level.sendParticles(ParticleTypes.SONIC_BOOM, this.currentPos.x, this.currentPos.y, this.currentPos.z, 1, 0, 0, 0, 0);

            AABB hitBox = new AABB(this.currentPos, this.currentPos).inflate(HITBOX_SIZE);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

            if (!potentialTargets.isEmpty()) {
                for (LivingEntity target : potentialTargets) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    }
                    target.knockback(KNOCKBACK_STRENGTH, -direction.x, -direction.z);
                    this.hitEntities.add(target.getUUID());
                }

                level.playSound(null, this.currentPos.x, this.currentPos.y, this.currentPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.2f);
                ActiveFormManager.stop(user);
                return;
            }

            if (!level.getBlockState(new BlockPos(this.currentPos)).isAir()) {
                ActiveFormManager.stop(user);
            }
        }
    }

    public static class DisorderForm extends ActiveForm {
        private static final double CONE_RANGE = 15.0;
        private static final double CONE_ANGLE_DEGREES = 45.0;
        private static final int PULSE_RATE = 4;
        private static final float KNOCKBACK_STRENGTH = 1.2F;
        private static final double CONE_ANGLE_COS = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES));
        private final Set<UUID> knockedBackEntities = new HashSet<>();

        public DisorderForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        public boolean isInterruptible() { return false; }

        @Override
        protected void onStart() {
            applyImmobility(user, Integer.MAX_VALUE);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        protected void onTick() {
            if (ticksActive > 0 && ticksActive % PULSE_RATE == 0) {
                float damagePerPulse = 6.0f + (ticksActive / 40f);
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);

                AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
                List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
                Vec3 userEyePos = user.getEyePosition();
                Vec3 userLookDir = user.getLookAngle();

                for (LivingEntity target : potentialTargets) {
                    Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
                    double dotProduct = vectorToTarget.dot(userLookDir);

                    if (dotProduct > CONE_ANGLE_COS) {
                        if (user instanceof ServerPlayer player) {
                            target.hurt(DamageSource.playerAttack(player), damagePerPulse);
                        } else {
                            target.hurt(DamageSource.mobAttack(user), damagePerPulse);
                        }

                        if (!knockedBackEntities.contains(target.getUUID())) {
                            Vec3 knockbackVec = userLookDir.normalize().scale(KNOCKBACK_STRENGTH);
                            target.setDeltaMovement(target.getDeltaMovement().add(knockbackVec.x, 0.4, knockbackVec.z));
                            target.hurtMarked = true;

                            knockedBackEntities.add(target.getUUID());
                        }
                    }
                }
                int particlesPerPulse = 5 + (ticksActive / 30);
                for (int i = 0; i < particlesPerPulse; i++) {
                    Vec3 randomPosInCone = getRandomPointInCone(user, CONE_RANGE, CONE_ANGLE_DEGREES);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, randomPosInCone.x, randomPosInCone.y, randomPosInCone.z, 1, 0, 0, 0, 0);
                }
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_AGITATED, SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        private Vec3 getRandomPointInCone(LivingEntity entity, double maxRange, double angleDegrees) {
            Vec3 lookDir = entity.getLookAngle();
            Vec3 startPos = entity.getEyePosition();
            float randomYawOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float)angleDegrees;
            float randomPitchOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float)angleDegrees;
            Vec3 randomDir = lookDir.yRot((float) Math.toRadians(randomYawOffset)).xRot((float) Math.toRadians(randomPitchOffset));
            double randomDist = level.random.nextDouble() * maxRange;
            return startPos.add(randomDir.scale(randomDist));
        }
    }

    private static class AnnihilationTypeForm extends ChargeableForm {
        private static final int DASH_DURATION = 15; // Shorter, faster dash than Rengoku
        private static final double DASH_SPEED = 4.0;   // Faster than Rengoku's 3.5
        private static final float IMPACT_DAMAGE = 60.0f;
        private static final double IMPACT_RADIUS = 7.0;

        private static final DustParticleOptions BLUE_AURA = new DustParticleOptions(new Vector3f(0.2f, 0.5f, 1.0f), 1.0f);
        private static final DustParticleOptions PURPLE_AURA = new DustParticleOptions(new Vector3f(0.6f, 0.2f, 1.0f), 1.2f);

        private Vec3 direction;
        private int releaseTicks = 0;

        public AnnihilationTypeForm(LivingEntity user, BreathingForms form) { super(user, form); }
        @Override public boolean isInterruptible() { return false; }

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
        protected void onChargeRelease(boolean fullyCharged) {
            removeImmobility(user);
            if (fullyCharged) {
                level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
                level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
                this.direction = user.getLookAngle();
            } else {
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onReleaseTick() {
            if (releaseTicks < DASH_DURATION) {
                releaseTicks++;
                user.setDeltaMovement(direction.scale(DASH_SPEED));
                user.hurtMarked = true;

                AABB impactBox = user.getBoundingBox().inflate(1.0).move(direction.scale(1.5));
                List<LivingEntity> hitTargets = level.getEntitiesOfClass(LivingEntity.class, impactBox, e -> e != user && e.isAlive());

                if (!hitTargets.isEmpty()) {
                    triggerExplosion(hitTargets.get(0).position());
                    ActiveFormManager.stop(user);
                }
            } else {
                triggerExplosion(user.position());
                ActiveFormManager.stop(user);
            }
        }

        @Override
        protected void onChargeEnd() {
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }

        private void triggerExplosion(Vec3 position) {
            if(user instanceof ServerPlayer player) {
                dealAOEDamage(player, position, IMPACT_RADIUS, IMPACT_DAMAGE, 3.0f, DamageSource.playerAttack(player).setExplosion());
            } else {
                dealAOEDamage(user, position, IMPACT_RADIUS, IMPACT_DAMAGE, 3.0f, DamageSource.mobAttack(user).setExplosion());
            }
            level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.8f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z, 2, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, position.x, position.y, position.z, 2, 0, 0, 0, 0);
        }
    }

    public static class CrownSplitterForm extends TimedForm {
        private static final double ATTACK_RANGE = 5.0;
        private static final float PRIMARY_DAMAGE = 40.0f;
        private static final float LAUNCH_STRENGTH_VERTICAL = 1f;
        private static final float LAUNCH_STRENGTH_HORIZONTAL = 2f;
        private static final double SHOCKWAVE_RADIUS = 5.0;
        private static final float SHOCKWAVE_DAMAGE = 15.0f;
        private static final float SHOCKWAVE_KNOCKBACK = 1.0f;
        private static final double LUNGE_SPEED = 1.5;
        private boolean isLunge = false;
        private Vec3 lungeDirection;

        public CrownSplitterForm(LivingEntity user, BreathingForms form) {
            super(user, form, 10);
        }

        @Override
        protected void onStart() {
            LivingEntity target = getLookedAtEntity(user, ATTACK_RANGE);

            if (target != null) {
                this.isLunge = false;
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.2f, 1.2f);

                Vec3 strikePos = target.position().subtract(user.getLookAngle().scale(1.5));
                double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int)strikePos.x, (int)strikePos.z);
                user.teleportTo(strikePos.x, groundY, strikePos.z);

                if (user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), PRIMARY_DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), PRIMARY_DAMAGE);
                }

                Vec3 horizontalPush = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize()
                        .scale(LAUNCH_STRENGTH_HORIZONTAL);

                target.setDeltaMovement(horizontalPush.add(0, LAUNCH_STRENGTH_VERTICAL, 0));
                target.hurtMarked = true;

                Vec3 shockwaveCenter = target.position();
                level.sendParticles(ParticleTypes.SONIC_BOOM, shockwaveCenter.x, shockwaveCenter.y + 1.0, shockwaveCenter.z, 1, 0, 0, 0, 0);

                List<LivingEntity> shockwaveTargets = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(shockwaveCenter, shockwaveCenter).inflate(SHOCKWAVE_RADIUS),
                        e -> e != user && e != target && e.isAlive());

                for (LivingEntity secondaryTarget : shockwaveTargets) {
                    if (user instanceof ServerPlayer player) {
                        secondaryTarget.hurt(DamageSource.playerAttack(player), SHOCKWAVE_DAMAGE);
                    } else {
                        secondaryTarget.hurt(DamageSource.mobAttack(user), SHOCKWAVE_DAMAGE);
                    }
                    Vec3 knockbackDir = secondaryTarget.position().subtract(shockwaveCenter).normalize();
                    secondaryTarget.knockback(SHOCKWAVE_KNOCKBACK, knockbackDir.x, knockbackDir.z);
                }

            } else {
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
                this.isLunge = true;
                Vec3 direction = user.getLookAngle();
                this.lungeDirection = new Vec3(direction.x, 0, direction.z).normalize();
            }
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (this.isLunge) {
                user.setDeltaMovement(this.lungeDirection.scale(LUNGE_SPEED));
                user.hurtMarked = true;
            }
        }
    }

    public static class ExplosiveFlurryForm extends TimedForm {
        private static final int NUMBER_OF_KICKS = 5;
        private static final int TICKS_BETWEEN_KICKS = 3;
        private static final float DAMAGE_PER_KICK = 10.0f;

        private static final float FINAL_KICK_DAMAGE = 25.0f;
        private static final float FINAL_KICK_KNOCKBACK = 3.5f;

        private static final DustParticleOptions AFTERIMAGE_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.6f, 1.0f), 0.8f);

        public ExplosiveFlurryForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_KICKS * TICKS_BETWEEN_KICKS);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_KICKS == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8f, 1.5f);
                level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.3f, 2.0f);

                dealDirectionalDamage(user, 4.0, DAMAGE_PER_KICK, 0.2);

                spawnParticleArc(user, ParticleTypes.SONIC_BOOM, 1.0, 60, 1);
                spawnAfterimageVFX();
            }
        }

        @Override
        protected void onStop() {
            LivingEntity target = getLookedAtEntity(user, 4.5);
            if (target != null) {
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
                level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getEyeY(), target.getZ(), 1, 0,0,0,0);

                if(user instanceof ServerPlayer player) {
                    target.hurt(DamageSource.playerAttack(player), FINAL_KICK_DAMAGE);
                } else {
                    target.hurt(DamageSource.mobAttack(user), FINAL_KICK_DAMAGE);
                }

                Vec3 launchVec = user.getLookAngle().scale(FINAL_KICK_KNOCKBACK);
                target.setDeltaMovement(launchVec.x, 0.8, launchVec.z);
                target.hurtMarked = true;
            }
        }

        private void spawnAfterimageVFX() {
            Vec3 pos = user.position();
            for (double y = 0; y <= user.getBbHeight(); y += 0.2) {
                for (int i = 0; i < 4; i++) {
                    level.sendParticles(AFTERIMAGE_PARTICLE,
                            pos.x + (level.random.nextDouble() - 0.5) * user.getBbWidth(),
                            pos.y + y,
                            pos.z + (level.random.nextDouble() - 0.5) * user.getBbWidth(),
                            1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static class FlyingPlanetThousandWheelsForm extends TimedForm {
        private static final int DURATION = 12;
        private static final double ATTACK_RADIUS = 3.5;
        private static final double ATTACK_HEIGHT = 4.0;
        private static final float DAMAGE = 20.0f;
        private static final float LAUNCH_STRENGTH = 1.8f;
        private static final DustParticleOptions VORTEX_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.6f, 1.0f), 1.0f);
        private final Set<UUID> hitEntities = new HashSet<>();

        public FlyingPlanetThousandWheelsForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            double progress = (double) ticksActive / DURATION;
            double currentHeight = ATTACK_HEIGHT * progress;

            Vec3 attackCenter = user.position()
                    .add(user.getLookAngle().scale(1.5 + progress))
                    .add(0, currentHeight, 0);

            double particleRadius = 1.0 + (ATTACK_RADIUS * progress * 0.5);
            spawnParticleArcInWorld(level, attackCenter, particleRadius, 360, 20, (float)(progress * 180), 90, VORTEX_PARTICLE);
            if (ticksActive % 4 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, attackCenter.x, attackCenter.y, attackCenter.z, 1, 0,0,0,0);
            }

            AABB hitBox = new AABB(attackCenter, attackCenter).inflate(ATTACK_RADIUS, 1.0, ATTACK_RADIUS);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

            for (LivingEntity target : potentialTargets) {
                Vec3 directionToTarget = target.position().subtract(user.position()).normalize();
                double dotProduct = user.getLookAngle().dot(directionToTarget);

                if (dotProduct > 0) {
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    }
                    target.setDeltaMovement(0, LAUNCH_STRENGTH, 0);
                    target.hurtMarked = true;

                    hitEntities.add(target.getUUID());
                }
            }
        }
    }

    public static class EightLayeredDemonCoreForm extends TimedForm {
        private static final int NUMBER_OF_PUNCHES = 8;
        private static final int TICKS_BETWEEN_PUNCHES = 5;

        private static final float BASE_DAMAGE = 8.0f;
        private static final float DAMAGE_RAMP_PER_PUNCH = 5.0f;

        private static final float FINAL_PUNCH_KNOCKBACK = 2.0f;

        private static final double CONE_RANGE = 12.0;
        private static final double CONE_ANGLE_DEGREES = 40.0;
        private static final double CONE_ANGLE_COS = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES));

        public EightLayeredDemonCoreForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_PUNCHES * TICKS_BETWEEN_PUNCHES);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_PUNCHES == 0) {
                int punchNumber = ticksActive / TICKS_BETWEEN_PUNCHES; // (1 through 8)
                float currentDamage = BASE_DAMAGE + ((punchNumber - 1) * DAMAGE_RAMP_PER_PUNCH);

                level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.8f + (punchNumber * 0.05f), 1.2f + (punchNumber * 0.1f));
                level.sendParticles(ParticleTypes.SONIC_BOOM,
                        user.getX() + user.getLookAngle().x * 3,
                        user.getEyeY() + user.getLookAngle().y * 3,
                        user.getZ() + user.getLookAngle().z * 3,
                        1, 0, 0, 0, 0);

                AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
                List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
                Vec3 userEyePos = user.getEyePosition();
                Vec3 userLookDir = user.getLookAngle();

                for (LivingEntity target : potentialTargets) {
                    Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
                    double dotProduct = vectorToTarget.dot(userLookDir);

                    if (dotProduct > CONE_ANGLE_COS) {
                        if (user instanceof ServerPlayer player) {
                            target.hurt(DamageSource.playerAttack(player), currentDamage);
                        } else {
                            target.hurt(DamageSource.mobAttack(user), currentDamage);
                        }
                    }
                }
            }
        }

        @Override
        protected void onStop() {
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.0f);

            AABB broadPhaseBox = user.getBoundingBox().inflate(CONE_RANGE);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
            Vec3 userEyePos = user.getEyePosition();
            Vec3 userLookDir = user.getLookAngle();

            for (LivingEntity target : potentialTargets) {
                Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos).normalize();
                double dotProduct = vectorToTarget.dot(userLookDir);

                if (dotProduct > CONE_ANGLE_COS) {
                    Vec3 horizontalPush = new Vec3(userLookDir.x, 0, userLookDir.z).normalize().scale(FINAL_PUNCH_KNOCKBACK);
                    target.setDeltaMovement(target.getDeltaMovement().add(horizontalPush.add(0, 0.5, 0)));
                    target.hurtMarked = true;
                }
            }
        }
    }

    public static class TenThousandLeavesWillowForm extends TimedForm {
        private static final int DURATION = 20;
        private static final double MAX_RADIUS = 12.0;
        private static final float DAMAGE = 25.0f;
        private static final float KNOCKBACK = 1.5f;

        private final Set<UUID> hitEntities = new HashSet<>();

        public TenThousandLeavesWillowForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            applyImmobility(user, DURATION + 5);
            level.playSound(null, user.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.2f, 0.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            double progress = (double) ticksActive / DURATION;
            double previousProgress = (double) (ticksActive - 1) / DURATION;

            double currentRadius = MAX_RADIUS * progress;
            double previousRadius = MAX_RADIUS * previousProgress;

            AABB damageZone = user.getBoundingBox().inflate(currentRadius);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, damageZone,
                    e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

            for (LivingEntity target : potentialTargets) {
                double distanceToUser = target.distanceTo(user);

                if (distanceToUser <= currentRadius && distanceToUser > previousRadius) {
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player).setExplosion(), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user).setExplosion(), DAMAGE);
                    }

                    Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                    target.knockback(KNOCKBACK, knockbackDir.x, knockbackDir.z);

                    hitEntities.add(target.getUUID());
                }
            }

            BlockState groundState = level.getBlockState(user.blockPosition().below());
            if (groundState.isAir()) {
                groundState = Blocks.STONE.defaultBlockState();
            }
            spawnParticleArcInWorld(level, user.position(), currentRadius, 360, 40, 0, 0, new BlockParticleOption(ParticleTypes.BLOCK, groundState));
            if(ticksActive % 4 == 0) {
                spawnParticleArcInWorld(level, user.position(), currentRadius, 360, 5, 0, 0, ParticleTypes.SWEEP_ATTACK);
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }

    public static class FinalFormChaoticAfterglowForm extends TimedForm {
        private static final int DURATION = 30;
        private static final double ATTACK_RADIUS = 16.0;
        private static final float DAMAGE = 240.0f;

        private static final DustParticleOptions BLUE_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.8f, 1.0f), 1.0f);
        private static final DustParticleOptions SILVER_PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.9f, 0.95f), 1.0f);

        public FinalFormChaoticAfterglowForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            applyImmobility(user, DURATION);

            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 1.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive < DURATION - 5) {
                double progress = (double)ticksActive / (DURATION - 5);
                double radius = 5.0 * (1.0 - progress);
                for (int i = 0; i < 20; i++) {
                    double angle = (level.random.nextDouble() * 2 * Math.PI);
                    Vec3 offset = new Vec3(Math.cos(angle), (level.random.nextDouble() - 0.5) * 4.0, Math.sin(angle)).scale(radius);
                    level.sendParticles(level.random.nextBoolean() ? BLUE_PARTICLE : SILVER_PARTICLE, user.getX() + offset.x, user.getY() + 1.0, user.getZ() + offset.z, 1, 0,0,0,0);
                }
            }

            if (ticksActive == DURATION - 5) {
                level.playSound(null, user.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 1.2f);
                level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);

                AABB effectZone = user.getBoundingBox().inflate(ATTACK_RADIUS);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());
                DamageSource unblockableSource = DamageSource.mobAttack(user).setIsFall().setMagic();

                for (LivingEntity target : targets) {
                    target.hurt(unblockableSource, DAMAGE);

                    for (int i = 0; i < 50; i++) {
                        Vec3 randomStart = target.position().add(new Vec3(
                                (level.random.nextDouble() - 0.5) * 6.0,
                                (level.random.nextDouble() - 0.5) * 6.0,
                                (level.random.nextDouble() - 0.5) * 6.0
                        ));
                        spawnLineParticles(level, level.random.nextBoolean() ? BLUE_PARTICLE : ParticleTypes.CRIT, randomStart, target.getEyePosition(), 3);
                    }
                }

                level.sendParticles(ParticleTypes.FLASH, user.getX(), user.getEyeY(), user.getZ(), 1, 0,0,0,0);
                level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getEyeY(), user.getZ(), 5, 0,0,0,0);
            }
        }

        @Override
        protected void onStop() {
            removeImmobility(user);
        }
    }


    // -- BDA, Doma --
    public static class FrozenLotusForm extends TimedForm {
        // --- Constants ---
        private static final int DURATION = 20; // Form lasts 1s to let visuals linger
        private static final double ATTACK_RANGE = 7.0;
        private static final double ATTACK_ANGLE_DEGREES = 60.0; // A wide 120-degree cone
        private static final double ATTACK_ANGLE_COS = Math.cos(Math.toRadians(ATTACK_ANGLE_DEGREES));

        private static final float DAMAGE = 18.0f;
        private static final int CHILL_DURATION = 80; // 4 seconds of slowness
        private static final int CHILL_AMPLIFIER = 2; // Slowness III

        public FrozenLotusForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.8f);

            AABB broadPhaseBox = user.getBoundingBox().inflate(ATTACK_RANGE);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, broadPhaseBox, e -> e != user && e.isAlive());
            Vec3 userEyePos = user.getEyePosition();
            Vec3 userLookDir = user.getLookAngle();

            for (LivingEntity target : potentialTargets) {
                Vec3 vectorToTarget = target.getEyePosition().subtract(userEyePos);
                if (vectorToTarget.lengthSqr() > ATTACK_RANGE * ATTACK_RANGE) continue;

                double dotProduct = vectorToTarget.normalize().dot(userLookDir);
                if (dotProduct > ATTACK_ANGLE_COS) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_DURATION, CHILL_AMPLIFIER));
                }
            }

            spawnParticleArc(user, ParticleTypes.SNOWFLAKE, ATTACK_RANGE, 120, 50);
            spawnParticleArc(user, ParticleTypes.SWEEP_ATTACK, ATTACK_RANGE, 120, 5);

            for (int i = 0; i < 4; i++) {
                Vec3 randomPosInCone = getRandomPointInCone(user, ATTACK_RANGE * 0.8, ATTACK_ANGLE_DEGREES);
                BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(randomPosInCone)).above();
                spawnIceLotusVFX(level, Vec3.atCenterOf(groundPos));
            }
        }

        private void spawnIceLotusVFX(ServerLevel level, Vec3 center) {
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI / 6) * i;
                for (double r = 0.5; r <= 1.5; r += 0.2) {
                    Vec3 petalPos = center.add(new Vec3(Math.cos(angle) * r, 0.2 + (r * 0.2), Math.sin(angle) * r));
                    level.sendParticles(ParticleTypes.END_ROD, petalPos.x, petalPos.y, petalPos.z, 1, 0,0,0,0);
                }
            }
        }

        private Vec3 getRandomPointInCone(LivingEntity entity, double maxRange, double angleDegrees) {
            Vec3 lookDir = entity.getLookAngle();
            Vec3 startPos = entity.getEyePosition();
            float randomYawOffset = (float) (level.random.nextDouble() - 0.5) * 2 * (float)angleDegrees;
            Vec3 randomDir = lookDir.yRot((float) Math.toRadians(randomYawOffset));
            double randomDist = 1.0 + (level.random.nextDouble() * (maxRange - 1.0));
            return startPos.add(randomDir.scale(randomDist));
        }

        @Override
        protected void onTick() {
            super.onTick();
        }
    }

    public static class BarrenHangingGardenForm extends TimedForm {
        private static final int NUMBER_OF_WAVES = 8;
        private static final int TICKS_BETWEEN_WAVES = 2;
        private static final double WAVE_RADIUS = 6.0;
        private static final float DAMAGE_PER_WAVE = 8.0f;
        private static final float KNOCKBACK_PER_WAVE = 0.8f;

        private static final int CHILL_DURATION = 60; // 3 seconds of slowness
        private static final int CHILL_AMPLIFIER = 1; // Slowness II

        public BarrenHangingGardenForm(LivingEntity user, BreathingForms form) {
            super(user, form, NUMBER_OF_WAVES * TICKS_BETWEEN_WAVES);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive > 0 && ticksActive % TICKS_BETWEEN_WAVES == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.8f);

                spawnParticleArcInWorld(level, user.position(), WAVE_RADIUS, 360, 40, 0, 0, ParticleTypes.SNOWFLAKE);
                spawnParticleArcInWorld(level, user.position(), WAVE_RADIUS, 360, 5, 0, 0, ParticleTypes.SWEEP_ATTACK);

                AABB effectZone = user.getBoundingBox().inflate(WAVE_RADIUS);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, effectZone, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    if (user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_WAVE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_WAVE);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHILL_DURATION, CHILL_AMPLIFIER));

                    Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                    target.knockback(KNOCKBACK_PER_WAVE, knockbackDir.x, knockbackDir.z);
                }
            }
        }

        @Override
        protected void onStop() {}
    }

    public static class FreezingCloudsForm extends TimedForm {
        private static final int DURATION = 40;
        private static final double CLOUD_RANGE = 30.0;
        private static final double CLOUD_WIDTH = 10.0;
        private static final float DAMAGE = 2.0f;

        private static final int DEBUFF_DURATION = 100; // 5 seconds
        private static final int SLOWNESS_AMPLIFIER = 4;  // Slowness V
        private static final int WEAKNESS_AMPLIFIER = 1;  // Weakness II

        private final Set<UUID> hitEntities = new HashSet<>();

        public FreezingCloudsForm(LivingEntity user, BreathingForms form) {
            super(user, form, DURATION);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5f, 1.8f);
            level.playSound(null, user.blockPosition(), SoundEvents.POWDER_SNOW_FALL, SoundSource.PLAYERS, 2.0f, 0.8f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            double progress = (double) ticksActive / DURATION;
            double currentDistance = CLOUD_RANGE * progress;

            Vec3 lookDir = user.getLookAngle();
            Vec3 cloudCenter = user.position().add(lookDir.scale(currentDistance));
            Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

            for (int i = 0; i < 40; i++) {
                double offset = (level.random.nextDouble() - 0.5) * (CLOUD_WIDTH * 2);
                Vec3 particlePos = cloudCenter.add(sideDir.scale(offset));
                particlePos = particlePos.add(0, (level.random.nextDouble() - 0.5) * 4.0, 0); // Add some vertical randomness

                level.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                if (i % 5 == 0) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
                }
            }

            AABB hitBox = new AABB(cloudCenter, cloudCenter).inflate(CLOUD_WIDTH, 2.0, CLOUD_WIDTH);
            List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != user && e.isAlive() && !hitEntities.contains(e.getUUID()));

            for(LivingEntity target : potentialTargets) {
                if (target.distanceToSqr(cloudCenter) < (CLOUD_WIDTH * CLOUD_WIDTH)) {
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    }

                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, SLOWNESS_AMPLIFIER));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_DURATION, WEAKNESS_AMPLIFIER));
                    // We need to add a "Frozen" effect here that handles the visual overlay

                    level.playSound(null, target.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    hitEntities.add(target.getUUID());
                }
            }
        }

        @Override
        protected void onStop() {}
    }

    public static class LotusVinesForm extends TimedForm {
        private static final int VINE_TRAVEL_DURATION = 15; // Vines take 0.75s to reach target
        private static final int GARDEN_LINGER_DURATION = 100; // The hazard lasts for 5 seconds
        private static final double MAX_RANGE = 20.0;
        private static final double GARDEN_RADIUS = 4.0;

        private static final float DAMAGE_PER_TICK = 3.0f; // Damage for standing in the garden
        private static final int SLOWNESS_AMPLIFIER = 3; // Slowness IV

        private final Vec3 targetPoint;
        private final Vec3 startPoint;
        private boolean hasBloomed = false;

        public LotusVinesForm(LivingEntity user, BreathingForms form) {
            super(user, form, VINE_TRAVEL_DURATION + GARDEN_LINGER_DURATION);
            this.startPoint = user.position();

            Vec3 finalTarget = null;

            Vec3 eyePos = user.getEyePosition();
            Vec3 lookVec = user.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.scale(MAX_RANGE));
            AABB searchBox = user.getBoundingBox().expandTowards(lookVec.scale(MAX_RANGE)).inflate(1.0D);

            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(level, user, eyePos, endPos, searchBox, e -> e instanceof LivingEntity && !e.is(user));

            if (entityHitResult != null) {
                finalTarget = entityHitResult.getEntity().position();
            } else {
                BlockHitResult blockHitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
                if (blockHitResult.getType() == HitResult.Type.MISS) {
                    finalTarget = eyePos.add(lookVec.scale(MAX_RANGE));
                } else {
                    finalTarget = Vec3.atCenterOf(blockHitResult.getBlockPos());
                }
            }
            this.targetPoint = finalTarget;
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.FROGSPAWN_HATCH, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive <= VINE_TRAVEL_DURATION) {
                double progress = (double) ticksActive / VINE_TRAVEL_DURATION;

                for (int i = 0; i < 3; i++) {
                    Vec3 vinePath = startPoint.lerp(targetPoint, progress);

                    double weave_amplitude = 1.2;
                    double weave_frequency = 2.0;
                    double weave_offset = Math.sin(progress * Math.PI * weave_frequency + (i * 2)) * weave_amplitude;
                    Vec3 sideDir = targetPoint.subtract(startPoint).cross(new Vec3(0, 1, 0)).normalize();
                    Vec3 particlePos = vinePath.add(sideDir.scale(weave_offset));

                    level.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y + 0.2, particlePos.z, 1, 0,0,0,0);
                }

            } else {
                if (!hasBloomed) {
                    hasBloomed = true;
                    level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.5f, 1.8f);
                    for (int i = 0; i < 5; i++) {
                        Vec3 lotusPos = targetPoint.add(new Vec3(
                                (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2),
                                0.2,
                                (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2)
                        ));
                        spawnIceLotusVFX(level, lotusPos);
                    }
                }

                AABB gardenBox = new AABB(targetPoint, targetPoint).inflate(GARDEN_RADIUS, 2.0, GARDEN_RADIUS);
                List<LivingEntity> targetsInGarden = level.getEntitiesOfClass(LivingEntity.class, gardenBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targetsInGarden) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, SLOWNESS_AMPLIFIER));
                    if (ticksActive % 10 == 0) {
                        if(user instanceof ServerPlayer player) {
                            target.hurt(DamageSource.playerAttack(player), DAMAGE_PER_TICK);
                        } else {
                            target.hurt(DamageSource.mobAttack(user), DAMAGE_PER_TICK);
                        }
                    }
                }

                if (ticksActive % 2 == 0) {
                    Vec3 mistPos = targetPoint.add(new Vec3(
                            (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2),
                            0.5 + level.random.nextDouble(),
                            (level.random.nextDouble() - 0.5) * (GARDEN_RADIUS * 2)
                    ));
                    level.sendParticles(ParticleTypes.WHITE_ASH, mistPos.x, mistPos.y, mistPos.z, 1,0,0,0,0);
                }
            }
        }

        private void spawnIceLotusVFX(ServerLevel level, Vec3 center) {
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI / 6) * i;
                for (double r = 0.5; r <= 1.5; r += 0.2) {
                    Vec3 petalPos = center.add(new Vec3(Math.cos(angle) * r, 0.2 + (r * 0.2), Math.sin(angle) * r));
                    level.sendParticles(ParticleTypes.END_ROD, petalPos.x, petalPos.y, petalPos.z, 1, 0,0,0,0);
                }
            }
        }
    }

    public static class ColdWhitePrincessesForm extends ActiveForm {
        private static final double SPAWN_DISTANCE = 4.0;

        public ColdWhitePrincessesForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.5f);
            level.playSound(null, user.blockPosition(), SoundEvents.AMBIENT_CAVE, SoundSource.PLAYERS, 1.0f, 1.8f);

            Vec3 lookDir = user.getLookAngle();
            Vec3 sideDir = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

            Vec3 pos1 = user.position().add(sideDir.scale(SPAWN_DISTANCE));
            Vec3 pos2 = user.position().add(sideDir.scale(-SPAWN_DISTANCE));

            spawnPrincessAt(pos1);
            spawnPrincessAt(pos2);
            ActiveFormManager.stop(user);
        }

        private void spawnPrincessAt(Vec3 position) {
            ColdWhitePrincessEntity princess = ModEntityTypes.COLD_WHITE_PRINCESS.get().create(level);
            if (princess != null) {
                BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(position));
                princess.setPos(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
                princess.setOwner(user);
                level.addFreshEntity(princess);
            }
        }

        @Override
        protected void onTick() { }
    }

    public static class WintryIciclesForm extends TimedForm {
        private static final int TELEGRAPH_DURATION = 15;
        private static final double MAX_RANGE = 25.0;
        private static final double IMPACT_RADIUS = 2.5;
        private static final float DAMAGE = 35.0f;
        private final Vec3 targetPoint;

        public WintryIciclesForm(LivingEntity user, BreathingForms form) {
            super(user, form, TELEGRAPH_DURATION + 10);

            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS) {
                Vec3 endPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
                this.targetPoint = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(endPoint)));
            } else {
                this.targetPoint = Vec3.atBottomCenterOf(hitResult.getBlockPos());
            }
        }

        @Override
        protected void onStart() {
            level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0f, 2.0f);
        }

        @Override
        protected void onTick() {
            super.onTick();

            if (ticksActive <= TELEGRAPH_DURATION) {
                double angle = (ticksActive * 0.4);
                double radius = IMPACT_RADIUS * 0.8;
                Vec3 particlePos = targetPoint.add(new Vec3(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius));
                level.sendParticles(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 2, 0,0,0,0);
            }

            if (ticksActive == TELEGRAPH_DURATION) {
                level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);
                level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 1.2f, 1.0f);
                int icicleCount = 7;
                for (int i = 0; i < icicleCount; i++) {
                    Vec3 icicleBasePos = targetPoint.add(new Vec3(
                            (level.random.nextDouble() - 0.5) * (IMPACT_RADIUS),
                            0.1,
                            (level.random.nextDouble() - 0.5) * (IMPACT_RADIUS)
                    ));
                    spawnIcicleVFX(level, icicleBasePos);
                }

                AABB eruptionBox = new AABB(targetPoint, targetPoint).inflate(IMPACT_RADIUS, 2.0, IMPACT_RADIUS);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, eruptionBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    if(user instanceof ServerPlayer player) {
                        target.hurt(DamageSource.playerAttack(player), DAMAGE);
                    } else {
                        target.hurt(DamageSource.mobAttack(user), DAMAGE);
                    }
                    target.hurtMarked = true;
                }
            }
        }

        private void spawnIcicleVFX(ServerLevel level, Vec3 basePos) {
            final DustParticleOptions iceParticle = new DustParticleOptions(new Vector3f(0.7f, 0.9f, 1.0f), 1.0f);
            for (int i = 0; i < 20; i++) {
                double height = i * 0.25;
                double motionY = 0.8;
                level.sendParticles(iceParticle, basePos.x, basePos.y + height, basePos.z, 1, 0, motionY, 0, 0);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, basePos.x, basePos.y + height, basePos.z, 1, 0, motionY, 0, 0);
                }
            }
        }
    }

    public static class ScatteringLotusesForm extends ActiveForm {
        private static final int PROJECTILE_COUNT = 10;
        private static final float SPREAD = 12.0f; // Higher number = wider spread
        private static final float PROJECTILE_SPEED = 2.0f;

        public ScatteringLotusesForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override
        protected void onStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, user.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);

            for (int i = 0; i < PROJECTILE_COUNT; i++) {
                 LotusShardProjectileEntity shard = new LotusShardProjectileEntity(level, user);
                 Vec3 lookAngle = user.getLookAngle();

                 shard.shoot(lookAngle.x, lookAngle.y, lookAngle.z, PROJECTILE_SPEED, SPREAD);
                 level.addFreshEntity(shard);
            }

            ActiveFormManager.stop(user);
        }

        @Override
        protected void onTick() {}
    }

    public static class CrystallineDivineChildForm extends ChargeableForm {
        private static final int CLONE_CHARGE_TIME = 20; // 1s/clone
        private static final int MAX_CLONES = 6;

        public CrystallineDivineChildForm(LivingEntity user, BreathingForms form) {
            super(user, form);
        }

        @Override public boolean isInterruptible() { return false; }

        @Override
        protected void onChargeStart() {
            level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        }

        @Override
        protected void onChargingTick() {
            int clonesToSpawn = Math.min(chargeTicks / CLONE_CHARGE_TIME, MAX_CLONES);
            for (int i = 0; i < clonesToSpawn; i++) {
                double angle = (user.tickCount * 0.05) + (i * (2 * Math.PI / Math.max(1, clonesToSpawn)));
                double radius = 2.5;
                Vec3 offset = new Vec3(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                Vec3 particlePos = user.position().add(offset);
                spawnIceLotusVFX(level, particlePos);
            }
        }

        @Override
        protected void onChargeRelease(boolean fullyCharged) {
            int clonesToSpawn = Math.min(chargeTicks / CLONE_CHARGE_TIME, MAX_CLONES);
            if (clonesToSpawn > 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.5f, 1.5f);

                for (int i = 0; i < clonesToSpawn; i++) {
                    double angle = (level.random.nextDouble() * 2 * Math.PI);
                    double dist = 2.0 + level.random.nextDouble() * 3.0;
                    Vec3 spawnPos = user.position().add(new Vec3(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
                    spawnCloneAt(spawnPos);
                }
            }
        }

        private void spawnCloneAt(Vec3 position) {
            CrystallineDivineChildEntity clone = ModEntityTypes.CRYSTALLINE_DIVINE_CHILD.get().create(level);
            if (clone != null) {
                BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(position));
                clone.setPos(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
                clone.setOwner(this.user);

                if (this.user instanceof Player playerOwner) {
                    clone.setOwnerProfile(playerOwner.getGameProfile());
                } else {
                    clone.setIsNpcClone(true);
                }

                level.addFreshEntity(clone);
            }
        }

        private void spawnIceLotusVFX(ServerLevel level, Vec3 center) {
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI / 3) * i + (user.tickCount * 0.1);
                Vec3 petalPos = center.add(new Vec3(Math.cos(angle) * 0.5, 0.2, Math.sin(angle) * 0.5));
                level.sendParticles(ParticleTypes.END_ROD, petalPos.x, petalPos.y, petalPos.z, 1, 0,0,0,0);
            }
        }

        @Override protected void onReleaseTick() { }
        @Override protected void onChargeEnd() { }
    }




    // =================================================================================
    // Client-Side Parry Indicator Logic
    // =================================================================================
    @Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, value = Dist.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static class Client {
        private static final Map<BreathingForms, BiFunction<Level, Vec3, Void>> INDICATOR_ACTIONS = new ImmutableMap.Builder<BreathingForms, BiFunction<Level, Vec3, Void>>()
                .put(BreathingForms.WATER_11, Client::showWaterIndicator)
                .put(BreathingForms.FLAME_4, Client::showFlameIndicator)
                .put(BreathingForms.WIND_3, Client::showWindIndicator)
                .put(BreathingForms.MIST_3, Client::showMistIndicator)
                .put(BreathingForms.STONE_3, Client::showStoneIndicator)
                .put(BreathingForms.FLOWER_2, Client::showFlowerIndicator)
                // BDA
                .put(BreathingForms.HAEMO_2, Client::showHaemoIndicator)
                .build();

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            mc.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                BreathingForms currentForm = style.getCurrentForms();
                if (!INDICATOR_ACTIONS.containsKey(currentForm)) return;

                AABB scanBox = mc.player.getBoundingBox().inflate(10.0);
                List<Entity> nearbyEntities = mc.level.getEntities(mc.player, scanBox,
                        entity -> !entity.isAlliedTo(mc.player) && (entity instanceof Projectile || entity instanceof LivingEntity));

                for (Entity threat : nearbyEntities) {
                    checkThreatAndShowIndicator(mc.player, threat, currentForm);
                }
            });
        }

        private static void checkThreatAndShowIndicator(Player player, Entity threat, BreathingForms form) {
            Vec3 indicatorPos = null;

            if (threat instanceof Projectile projectile) {
                Vec3 pPos = projectile.position();
                Vec3 playerPos = player.getEyePosition();

                double distanceToImpact = pPos.distanceTo(playerPos);
                if (distanceToImpact > 1.0 && distanceToImpact < 4.0) {
                    indicatorPos = projectile.position();
                }
            } else if (threat instanceof LivingEntity living) {
                boolean isHostile = false;
                if (living instanceof Mob mob) {
                    if (mob.getTarget() == player) {
                        isHostile = true;
                    }
                } else if (!(living instanceof Animal)) {
                    isHostile = true;
                }
                if (isHostile && living.swinging && living.distanceTo(player) < 5.0) {
                    indicatorPos = living.getEyePosition().add(living.getLookAngle().scale(1.2));
                }
            }

            if(indicatorPos != null) {
                INDICATOR_ACTIONS.get(form).apply(player.level, indicatorPos);
            }
        }

        private static Void showWaterIndicator(Level level, Vec3 position) {
            level.addParticle(ParticleTypes.END_ROD, position.x, position.y, position.z, 0, 0, 0);
            playSound(level, position, SoundEvents.NOTE_BLOCK_BELL, 1.0f, 2.0f);
            return null;
        }

        private static Void showFlameIndicator(Level level, Vec3 position) {
            level.addParticle(ParticleTypes.FLAME, position.x, position.y, position.z, 0, 0, 0);
            playSound(level, position, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.8f);
            return null;
        }

        private static Void showWindIndicator(Level level, Vec3 position) {
            level.addParticle(ParticleTypes.CLOUD, position.x, position.y, position.z, 0, 0, 0);
            playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 2.0f);
            return null;
        }

        private static Void showMistIndicator(Level level, Vec3 position) {
            level.addParticle(ParticleTypes.CLOUD, position.x, position.y, position.z, 0.1, 0.1, 0.1);
            playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
            return null;
        }

        private static Void showStoneIndicator(Level level, Vec3 position) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), position.x, position.y, position.z, 0.1, 0.1, 0.1);
            playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
            return null;
        }

        private static Void showFlowerIndicator(Level level, Vec3 position) {
            level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, position.x, position.y, position.z, 0.1, 0.1, 0.1);
            playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
            return null;
        }

        private static Void showHaemoIndicator(Level level, Vec3 position) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), position.x, position.y, position.z, 0.1, 0.1, 0.1);
            playSound(level, position, SoundEvents.HORSE_BREATHE, 1.0f, 1.8f);
            return null;
        }

        private static void playSound(Level level, Vec3 pos, SoundEvent sound, float vol, float pitch) {
            level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, vol, pitch, false);
        }
    }
}
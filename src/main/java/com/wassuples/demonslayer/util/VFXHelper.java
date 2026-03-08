package com.wassuples.demonslayer.util;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.wassuples.demonslayer.util.CombatHelper.isMoonBdaActive;

/**
 * Static visual effects utility methods for particle and sound spawning.
 *
 * <p>Provides server-side particle spawning helpers used by form implementations:
 * <ul>
 *   <li><b>Arcs</b> — {@code spawnParticleArc} spawns particles in a sweeping arc in front of the user</li>
 *   <li><b>Lines</b> — {@code spawnLineParticles} draws a particle line between two points</li>
 *   <li><b>Rings &amp; spirals</b> — various geometric particle patterns for ability VFX</li>
 *   <li><b>Moon BDA overlay</b> — automatically adds purple moon particles when Crescent BDA is active</li>
 * </ul>
 *
 * @see CombatHelper
 * @see MovementHelper
 */

/**
 * VFXHelper.
 */
public class VFXHelper {
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

        if (isMoonBdaActive(user)) {
            final DustParticleOptions moonParticle = new DustParticleOptions(new Vector3f(0.5f, 0.2f, 0.8f), 1.0f);
            spawnParticleArcInWorld(level, user.getEyePosition(), radius * 1.1, arcDegrees, particleCount / 3, rollDegrees + 15f, 0f, moonParticle);
        }

        for (int i = 0; i <= particleCount; i++) {
            double progress = (double) i / particleCount;
            double angle = Math.toRadians(-(arcDegrees / 2) + (arcDegrees * progress));

            // 1. Start with the forward vector
            Vec3 particleVec = lookVec;

            // 2. Rotate it sideways for the arc
            particleVec = particleVec.yRot((float) angle);

            // 3. Apply the roll rotation using the quaternion
            Vector3f tempVec = new Vector3f((float) particleVec.x, (float) particleVec.y, (float) particleVec.z);
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

    public static void spawnHeartShape(ServerLevel level, LivingEntity user, Vec3 targetCenter, double size, int particleCount) {
        Vec3 lookVec = new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        Vec3 sideVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();

        for (int i = 0; i < particleCount; i++) {
            double t = 2 * Math.PI * i / particleCount;
            double x_shape = size * 16 * Math.pow(Math.sin(t), 3);
            double z_shape = size * (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));

            Vec3 offset = sideVec.scale(x_shape * 0.1).add(lookVec.scale(z_shape * -0.1));
            Vec3 particlePos = targetCenter.add(offset).add(0, 0.2, 0);
            level.sendParticles(ParticleTypes.HEART, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0);
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
}

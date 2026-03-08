package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.util.WisteriaHelper;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Simple "flee" goal that prevents demon mobs from walking into wisteria leaves.
 *
 * This doesn't require pathfinding to a specific block; it just pushes the mob away
 * while wisteria is nearby.
 */
public class AvoidWisteriaLeavesGoal extends Goal {
    private final PathfinderMob mob;
    private final int scanRadius;
    private final double fleeSpeed;

    public AvoidWisteriaLeavesGoal(PathfinderMob mob, int scanRadius, double fleeSpeed) {
        this.mob = mob;
        this.scanRadius = scanRadius;
        this.fleeSpeed = fleeSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive() || mob.level.isClientSide()) return false;
        // Throttle scanning a bit.
        if (mob.tickCount % 10 != 0) return false;
        return WisteriaHelper.isWisteriaLeavesNearby(mob.level, mob.blockPosition(), scanRadius);
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive() || mob.level.isClientSide()) return false;
        return WisteriaHelper.isWisteriaLeavesNearby(mob.level, mob.blockPosition(), scanRadius);
    }

    @Override
    public void start() {
        // Push away from the center of the wisteria "cloud" by moving opposite current motion.
        // If stationary, just pick a direction away from current look.
        var look = mob.getLookAngle();
        double dx = -look.x;
        double dz = -look.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            dx = mob.getRandom().nextDouble() - 0.5;
            dz = mob.getRandom().nextDouble() - 0.5;
            len = Math.sqrt(dx * dx + dz * dz);
        }
        dx /= len;
        dz /= len;

        // Move ~8 blocks away.
        double targetX = mob.getX() + dx * 8.0;
        double targetZ = mob.getZ() + dz * 8.0;
        mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, fleeSpeed);
    }

    @Override
    public void tick() {
        if (mob.getNavigation().isDone()) {
            start();
        }
    }
}


package com.wassuples.demonslayer.entity.ai;

import com.wassuples.demonslayer.util.WisteriaHelper;
import net.minecraft.core.BlockPos;
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
    private BlockPos nearestLeafPos;

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
        nearestLeafPos = WisteriaHelper.findNearestWisteriaLeaves(mob.level, mob.blockPosition(), scanRadius);
        return nearestLeafPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive() || mob.level.isClientSide()) return false;
        if (mob.tickCount % 10 == 0) {
            nearestLeafPos = WisteriaHelper.findNearestWisteriaLeaves(mob.level, mob.blockPosition(), scanRadius);
        }
        return nearestLeafPos != null;
    }

    @Override
    public void start() {
        fleeFromNearestLeaf();
    }

    private void fleeFromNearestLeaf() {
        BlockPos leaf = nearestLeafPos != null
                ? nearestLeafPos
                : WisteriaHelper.findNearestWisteriaLeaves(mob.level, mob.blockPosition(), scanRadius);
        if (leaf == null) {
            return;
        }

        // Flee directly away from the closest leaf block.
        double dx = mob.getX() - (leaf.getX() + 0.5D);
        double dz = mob.getZ() - (leaf.getZ() + 0.5D);
        double len = Math.sqrt(dx * dx + dz * dz);

        // If we're basically centered, choose a random escape heading.
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
        if (mob.tickCount % 10 == 0) {
            nearestLeafPos = WisteriaHelper.findNearestWisteriaLeaves(mob.level, mob.blockPosition(), scanRadius);
        }

        if (mob.getNavigation().isDone()) {
            fleeFromNearestLeaf();
        }
    }
}


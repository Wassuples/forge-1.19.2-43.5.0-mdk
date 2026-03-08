package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Server-safe helpers for wisteria-related mechanics.
 */
public final class WisteriaHelper {
    private WisteriaHelper() {}

    /**
     * Checks for any {@link com.wassuples.demonslayer.block.ModBlocks#WISTERIA_LEAVES} within a cube radius.
     *
     * Kept intentionally small + allocation-free for tick usage.
     */
    public static boolean isWisteriaLeavesNearby(Level level, BlockPos center, int radius) {
        if (radius <= 0) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(pos).is(ModBlocks.WISTERIA_LEAVES.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}


package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.mixin.FireBlockAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;

/**
 * Registers flammability for this mod's blocks.
 */
public final class ModFlammableBlocks {
    private ModFlammableBlocks() {}

    public static void register() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        FireBlockAccessor accessor = (FireBlockAccessor) fire;

        accessor.demonslayer$invokeSetFlammable(ModBlocks.WISTERIA_LOG.get(), 5, 5);
        accessor.demonslayer$invokeSetFlammable(ModBlocks.WISTERIA_LEAVES.get(), 30, 60);
        accessor.demonslayer$invokeSetFlammable(ModBlocks.WISTERIA_SAPLING.get(), 60, 100);
    }
}

package com.wassuples.demonslayer.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor to call the (mappings) non-public method that registers flammability on FireBlock.
 */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {
    @Invoker("setFlammable")
    void demonslayer$invokeSetFlammable(Block block, int encouragement, int flammability);
}


package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Wisteria canopy: ordinary leaves that shed drifting wisteria petals from their undersides. */
public class WisteriaLeavesBlock extends LeavesBlock {
    public WisteriaLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(14) != 0) return;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isAir() && !(level.getBlockState(below).getBlock() instanceof HangingWisteriaBlock)) return;
        level.addParticle(ModParticles.WISTERIA_PETAL.get(), pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                0.0, 0.0, 0.0);
    }
}

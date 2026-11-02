package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tōrō stone lantern: base, post, a fire box with a glowing paper window, and a wide hat. Usually stands on a stone
 * wall or pedestal. Gives off a faint ember now and then.
 */
public class StoneLanternBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),     // base
            Block.box(6, 2, 6, 10, 6, 10),     // post
            Block.box(4, 6, 4, 12, 11, 12),    // fire box
            Block.box(1, 11, 1, 15, 13, 15),   // hat
            Block.box(5, 13, 5, 11, 16, 11));  // jewel

    public StoneLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        level.addParticle(ModParticles.LANTERN_EMBER.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                pos.getY() + 0.62, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
    }
}

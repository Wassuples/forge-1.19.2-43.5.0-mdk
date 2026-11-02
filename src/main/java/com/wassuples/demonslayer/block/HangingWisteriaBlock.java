package com.wassuples.demonslayer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.Nullable;

/**
 * A raceme of wisteria blossoms hanging from leaves, logs or other blossoms. Stacks downward into curtains; the lowest
 * block shows the pale tip ({@link #BOTTOM}). Breaks if whatever it hangs from is removed. Climbable, shearable, and
 * counts as wisteria for demons (see {@code WisteriaHelper}).
 */
public class HangingWisteriaBlock extends Block implements IForgeShearable {
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public HangingWisteriaBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(BOTTOM, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOTTOM);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        return above.is(this) || above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS) || above.isFaceSturdy(level, up, Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState().setValue(BOTTOM, !ctx.getLevel().getBlockState(ctx.getClickedPos().below()).is(this));
        return state.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (dir == Direction.DOWN) {
            return state.setValue(BOTTOM, !neighbor.is(this));
        }
        return state;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(BOTTOM) || random.nextInt(10) != 0) return;
        level.addParticle(com.wassuples.demonslayer.particle.ModParticles.WISTERIA_PETAL.get(),
                pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.1, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
    }
}

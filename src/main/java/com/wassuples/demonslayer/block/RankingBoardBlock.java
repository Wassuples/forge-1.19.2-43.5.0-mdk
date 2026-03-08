package com.wassuples.demonslayer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Custom block: Ranking Board Block.
 * Rankings are now accessed via the Kasugai Crow instead.
 *
 * @see com.wassuples.demonslayer.block.ModBlocks
 */
public class RankingBoardBlock extends Block {
    public RankingBoardBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return InteractionResult.PASS;
    }
}

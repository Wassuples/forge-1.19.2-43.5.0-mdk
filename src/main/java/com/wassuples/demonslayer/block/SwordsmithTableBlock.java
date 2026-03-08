package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.worldgen.inventory.SwordsmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Custom block: Swordsmith Table Block.
 *
 * @see com.wassuples.demonslayer.block.ModBlocks
 */
public class SwordsmithTableBlock extends Block {
    public SwordsmithTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            pPlayer.openMenu(pState.getMenuProvider(pLevel, pPos));
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider((pId, pInventory, pPlayer) -> {
            return new SwordsmithTableMenu(pId, pInventory, ContainerLevelAccess.create(pLevel, pPos));
        }, Component.literal("Swordsmith's Table"));
    }
}


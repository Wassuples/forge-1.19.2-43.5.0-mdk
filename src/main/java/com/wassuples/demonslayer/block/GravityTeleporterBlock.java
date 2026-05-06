package com.wassuples.demonslayer.block;
import com.wassuples.demonslayer.events.InfinityCastleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public class GravityTeleporterBlock extends Block {
    public GravityTeleporterBlock(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(InfinityCastleEvents.INFINITY_CASTLE_DIMENSION);
            if (targetLevel != null) {
                player.changeDimension(targetLevel, new net.minecraftforge.common.util.ITeleporter() {
                    @Override
                    public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, java.util.function.Function<Boolean, net.minecraft.world.entity.Entity> repositionEntity) {
                        entity.setPos(0, 100, 0);
                        return repositionEntity.apply(false);
                    }
                });
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }
}

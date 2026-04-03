package com.wassuples.demonslayer.item.custom;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeSpawnEggItem;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Spawn egg that resolves a hierarchy demon identity first, then spawns that exact Nemesis demon.
 */
public class HierarchySpawnEggItem extends ForgeSpawnEggItem {
    @Nullable
    private final DemonRank fixedRank;

    public HierarchySpawnEggItem(@Nullable DemonRank fixedRank, int backgroundColor, int highlightColor, Properties properties) {
        super(ModEntityTypes.NEMESIS_DEMON, backgroundColor, highlightColor, properties);
        this.fixedRank = fixedRank;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        DemonHierarchyManager manager = DemonHierarchyManager.get(serverLevel);
        Optional<UUID> targetId = this.fixedRank == null
                ? manager.getRandomFodderDemonId()
                : manager.getDemonIdForRank(this.fixedRank);

        if (targetId.isEmpty()) {
            sendFailure(context.getPlayer(), this.fixedRank == null
                    ? "No fodder demons are available in the Nemesis pool."
                    : "No demon currently occupies that Kizuki rank.");
            return InteractionResult.FAIL;
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        boolean spawned = manager.spawnDemonFromData(targetId.get(), serverLevel, spawnPos, "Spawn Egg");
        if (!spawned) {
            sendFailure(context.getPlayer(), "That hierarchy demon is already loaded in this world.");
            return InteractionResult.FAIL;
        }

        consumeEgg(context.getItemInHand(), context.getPlayer());
        return InteractionResult.CONSUME;
    }

    private static void consumeEgg(ItemStack stack, @Nullable Player player) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void sendFailure(@Nullable net.minecraft.world.entity.player.Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(message), true);
        }
    }
}

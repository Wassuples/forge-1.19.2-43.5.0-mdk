package com.wassuples.demonslayer.item.custom;

import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.reputation.ReputationService;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable Muzan blood that converts humans into demons or strengthens existing demons.
 */
public class MuzansBloodItem extends Item {
    public MuzansBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (level.isClientSide || !(livingEntity instanceof ServerPlayer player)) {
            return result;
        }

        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                int bloodPoints = DemonProgressionHelper.rollMuzanBloodInfusion(player);
                demon.addBloodPoints(bloodPoints);
                ModUtils.syncDemon(player);
                player.sendSystemMessage(Component.literal("Muzan's blood surges through you. +" + bloodPoints + " Blood Points.")
                        .withStyle(ChatFormatting.DARK_RED));
                return;
            }

            DemonProgressionHelper.initializeNewDemon(player, demon);
            ReputationService.onDemonPathAccepted(player);
            player.sendSystemMessage(Component.literal("Muzan's blood rewrites your body.").withStyle(ChatFormatting.DARK_PURPLE));
        });

        return result;
    }
}

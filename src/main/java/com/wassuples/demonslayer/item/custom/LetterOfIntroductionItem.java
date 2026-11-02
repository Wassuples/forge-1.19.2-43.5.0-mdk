package com.wassuples.demonslayer.item.custom;

import com.wassuples.demonslayer.client.mission.ClientLetterHandler;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A letter of introduction to the Ubuyashiki household. Reading it (use) opens the letter; holding it shows a compass
 * to the shrine on Mt. Fujikasane, where the Final Selection is held. The shrine's position is written into the item
 * ({@code Shrine}: x, y, z) by the server.
 */
public class LetterOfIntroductionItem extends Item {
    public static final String TARGET = "Shrine";

    public LetterOfIntroductionItem(Properties properties) {
        super(properties);
    }

    /** Writes the shrine's position into the letter (if the world has a Mt. Fujikasane). */
    public static void address(ItemStack stack, ServerPlayer player) {
        FujikasaneLayout layout = FujikasaneSites.find(player.server);
        if (layout == null) return;
        BlockPos p = layout.plazaCenter();
        CompoundTag t = new CompoundTag();
        t.putInt("x", p.getX());
        t.putInt("y", p.getY());
        t.putInt("z", p.getZ());
        stack.getOrCreateTag().put(TARGET, t);
    }

    @Nullable
    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET)) return null;
        CompoundTag t = tag.getCompound(TARGET);
        return new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
    }

    public static ItemStack create(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        address(stack, player);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (target(stack) == null) address(stack, sp);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLetterHandler.open(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.demonslayer.letter_of_introduction.tooltip"));
    }
}

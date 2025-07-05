package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record ItemReward(Supplier<Item> itemSupplier, int count) implements MissionReward {
    @Override
    public Component getDescription() {
        return Component.literal(count + "x ").append(new ItemStack(itemSupplier.get()).getHoverName());
    }

    @Override
    public void grant(ServerPlayer player) {
        player.getInventory().add(new ItemStack(itemSupplier.get(), count));
    }
}

package com.wassuples.demonslayer.missions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Mission system: Item Reward.
 *
 * @see com.wassuples.demonslayer.missions.MissionRegistry
 */
public record ItemReward(ResourceLocation itemId, int count) implements MissionReward {
    public ItemReward(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readInt());
    }

    public ItemReward(Item item, int count) {
        this(ForgeRegistries.ITEMS.getKey(Objects.requireNonNull(item)), count);
    }

    public ItemReward(Supplier<Item> supplier, int count) {
        this(Objects.requireNonNull(supplier.get()), count);
    }

    public ItemReward(RegistryObject<? extends Item> registryObject, int count) {
        this(Objects.requireNonNull(registryObject.getId()), count);
    }

    private ItemStack createStack() {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return item != null ? new ItemStack(item, count) : ItemStack.EMPTY;
    }

    @Override
    public Component getDescription() {
        ItemStack stack = createStack();
        return stack.isEmpty()
                ? Component.literal(count + "x <missing item>")
                : Component.literal(count + "x ").append(stack.getHoverName());
    }

    @Override
    public void grant(ServerPlayer player) {
        ItemStack stack = createStack();
        if (!stack.isEmpty()) {
            player.getInventory().add(stack);
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeInt(count);
    }

    @Override
    public RewardType<?> getType() {
        return RewardType.ITEM;
    }

    public static ItemReward fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        int count = GsonHelper.getAsInt(json, "count", 1);
        return new ItemReward(id, count);
    }
}

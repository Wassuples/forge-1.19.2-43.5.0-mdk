package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RewardType<T extends MissionReward> {
    public static final Map<ResourceLocation, RewardType<?>> REGISTRY = new HashMap<>();

    public static final RewardType<ItemReward> ITEM = register("item", buf -> new ItemReward(buf));
    public static final RewardType<RankReward> RANK = register("rank", buf -> new RankReward(buf));
    public static final RewardType<StyleMasteryReward> STYLE_MASTERY = register("style_mastery", StyleMasteryReward::new);

    private final ResourceLocation id;
    private final Function<FriendlyByteBuf, T> factory;

    public RewardType(ResourceLocation id, Function<FriendlyByteBuf, T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public ResourceLocation getId() {
        return id;
    }


    public static <T extends MissionReward> RewardType<T> register(String name, Function<FriendlyByteBuf, T> factory) {
        RewardType<T> type = new RewardType<>(new ResourceLocation("demonslayer", name), factory);
        REGISTRY.put(type.getId(), type);
        return type;
    }

    public static MissionReward fromBytes(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        RewardType<?> type = REGISTRY.get(id);
        if (type != null) {
            return type.factory.apply(buf);
        }
        throw new IllegalArgumentException("Unknown reward type: " + id);
    }
}

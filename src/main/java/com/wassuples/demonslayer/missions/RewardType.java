package com.wassuples.demonslayer.missions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RewardType<T extends MissionReward> {
    public static final Map<ResourceLocation, RewardType<?>> REGISTRY = new HashMap<>();

    public static final RewardType<ItemReward> ITEM = register("item", ItemReward::new, ItemReward::fromJson);
    public static final RewardType<RankReward> RANK = register("rank", RankReward::new, RankReward::fromJson);
    public static final RewardType<StyleMasteryReward> STYLE_MASTERY = register("style_mastery", StyleMasteryReward::new, StyleMasteryReward::fromJson);

    private final ResourceLocation id;
    private final Function<FriendlyByteBuf, T> factory;
    private final Function<JsonObject, T> jsonFactory;

    public RewardType(ResourceLocation id, Function<FriendlyByteBuf, T> factory, Function<JsonObject, T> jsonFactory) {
        this.id = id;
        this.factory = factory;
        this.jsonFactory = jsonFactory;
    }

    public ResourceLocation getId() {
        return id;
    }


    public static <T extends MissionReward> RewardType<T> register(String name, Function<FriendlyByteBuf, T> factory, Function<JsonObject, T> jsonFactory) {
        RewardType<T> type = new RewardType<>(new ResourceLocation("demonslayer", name), factory, jsonFactory);
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

    public T deserialize(JsonObject json) {
        return jsonFactory.apply(json);
    }
}

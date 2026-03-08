package com.wassuples.demonslayer.missions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ObjectiveType<T extends MissionObjective> {
    public static final Map<ResourceLocation, ObjectiveType<?>> REGISTRY = new HashMap<>();

    public static final ObjectiveType<KillObjective> KILL = register("kill", KillObjective::new, KillObjective::fromJson);
    public static final ObjectiveType<SurvivalObjective> SURVIVAL = register("survival", SurvivalObjective::new, SurvivalObjective::fromJson);

    private final ResourceLocation id;
    private final Function<FriendlyByteBuf, T> factory;
    private final Function<JsonObject, T> jsonFactory;

    public ObjectiveType(ResourceLocation id, Function<FriendlyByteBuf, T> factory, Function<JsonObject, T> jsonFactory) {
        this.id = id;
        this.factory = factory;
        this.jsonFactory = jsonFactory;
    }

    public ResourceLocation getId() {
        return id;
    }

    public static <T extends MissionObjective> ObjectiveType<T> register(String name, Function<FriendlyByteBuf, T> factory, Function<JsonObject, T> jsonFactory) {
        ObjectiveType<T> type = new ObjectiveType<>(new ResourceLocation("demonslayer", name), factory, jsonFactory);
        REGISTRY.put(type.getId(), type);
        return type;
    }

    public static MissionObjective fromBytes(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        ObjectiveType<?> type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown objective type: " + id);
        }
        return type.factory.apply(buf);
    }

    public T deserialize(JsonObject json) {
        return jsonFactory.apply(json);
    }
}

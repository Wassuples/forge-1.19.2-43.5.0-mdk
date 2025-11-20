package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ObjectiveType<T extends MissionObjective> {
    public static final Map<ResourceLocation, ObjectiveType<?>> REGISTRY = new HashMap<>();

    public static final ObjectiveType<KillObjective> KILL = register("kill", KillObjective::new);
    public static final ObjectiveType<SurviveObjective> SURVIVE = register("survive", SurviveObjective::new);
    public static final ObjectiveType<SurviveInBiomeObjective> SURVIVE_IN_BIOME = register("survive_in_biome", SurviveInBiomeObjective::new);
    public static final ObjectiveType<SurviveWhileFightingObjective> SURVIVE_WHILE_FIGHTING = register("survive_while_fighting", SurviveWhileFightingObjective::new);

    private final ResourceLocation id;
    private final Function<FriendlyByteBuf, T> factory;

    public ObjectiveType(ResourceLocation id, Function<FriendlyByteBuf, T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public ResourceLocation getId() {
        return id;
    }

    public static <T extends MissionObjective> ObjectiveType<T> register(String name, Function<FriendlyByteBuf, T> factory) {
        ObjectiveType<T> type = new ObjectiveType<>(new ResourceLocation("demonslayer", name), factory);
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
}

package com.wassuples.demonslayer.demon.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class DemonHierarchyManager extends SavedData {
    private final Map<DemonRank, UUID> kizukiSlots = new EnumMap<>(DemonRank.class);
    private final List<UUID> rankedFodder = new ArrayList<>();
    private final Map<UUID, DemonTrait> demonDataCache = new HashMap<>();

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag) {
        return null;
    }

    public static get(ServerLevel level) {
        // TODO implement
    }
}

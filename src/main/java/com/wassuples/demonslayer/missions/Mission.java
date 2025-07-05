package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record Mission (
    ResourceLocation id,
    MissionType type,
    Component title,
    List<Component> description,
    List<MissionObjective> objectives,
    List<MissionReward> rewards,
    Rank requirement
) {
    // Helper methods later
}

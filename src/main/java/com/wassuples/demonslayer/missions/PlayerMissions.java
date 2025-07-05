package com.wassuples.demonslayer.missions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.*;

public class PlayerMissions {
    private final Map<ResourceLocation, Map<String, Integer>> activeMissions = new HashMap<>();
    private final Set<ResourceLocation> completedMissions = new HashSet<>();

    public boolean hasCompleted(ResourceLocation missionId) {
        return completedMissions.contains(missionId);
    }

    public boolean isActive(ResourceLocation missionId) {
        return activeMissions.containsKey(missionId);
    }

    public void acceptMission(Mission mission, ServerPlayer player) {
        if (!isActive(mission.id()) && !hasCompleted(mission.id())) {
            Map<String, Integer> progress = new HashMap<>();

            // Special handling for survival objectives
            for (MissionObjective objective : mission.objectives()) {
                if (objective instanceof SurviveObjective) {
                    progress.put(objective.getProgressKey(), (int)player.level.getGameTime());
                }
                if (objective instanceof KillObjective killObjective) {
                    int startingKills = player.getStats().getValue(Stats.ENTITY_KILLED, killObjective.target());
                    progress.put(objective.getProgressKey(), startingKills);
                }
            }
            activeMissions.put(mission.id(), progress);
        }
    }

    public void abandonMission(ResourceLocation missionId) {
        activeMissions.remove(missionId);
    }

    public void completeMission(Mission mission) {
        activeMissions.remove(mission.id());
        if (mission.type() != MissionType.RADIANT_QUEST) {
            completedMissions.add(mission.id());
        }
    }

    public Set<ResourceLocation> getCompletedMissionIds() {
        return Collections.unmodifiableSet(this.completedMissions);
    }

    public Set<ResourceLocation> getActiveMissionIds() {
        return activeMissions.keySet();
    }

    public Map<String, Integer> getProgress(ResourceLocation missionId) {
        return activeMissions.getOrDefault(missionId, Collections.emptyMap());
    }

    public void copyFrom(PlayerMissions source) {
        this.activeMissions.clear();
        this.activeMissions.putAll(source.activeMissions);
        this.completedMissions.clear();
        this.completedMissions.addAll(source.completedMissions);
    }

    public void saveNBTData(CompoundTag nbt) {
        ListTag activeList = new ListTag();
        activeMissions.forEach((id, progressMap) -> {
            CompoundTag missionTag = new CompoundTag();
            missionTag.putString("id", id.toString());
            CompoundTag progressTag = new CompoundTag();
            progressMap.forEach(progressTag::putInt);
            missionTag.put("progress", progressTag);
            activeList.add(missionTag);
        });
        nbt.put("activeMissions", activeList);

        ListTag completedList = new ListTag();
        completedMissions.forEach(id -> {
            CompoundTag idTag = new CompoundTag();
            idTag.putString("id", id.toString());
            completedList.add(idTag);
        });
        nbt.put("completedMissions", completedList);
    }

    public void loadNBTData(CompoundTag nbt) {
        activeMissions.clear();
        ListTag activeList = nbt.getList("activeMissions", 10);
        for (int i = 0; i < activeList.size(); i++) {
            CompoundTag missionTag = activeList.getCompound(i);
            ResourceLocation id = new ResourceLocation(missionTag.getString("id"));
            CompoundTag progressTag = missionTag.getCompound("progress");
            Map<String, Integer> progressMap = new HashMap<>();
            progressTag.getAllKeys().forEach(key -> progressMap.put(key, progressTag.getInt(key)));
            activeMissions.put(id, progressMap);
        }

        completedMissions.clear();
        ListTag completedList = nbt.getList("completedMissions", 10);
        for (int i = 0; i < completedList.size(); i++) {
            completedMissions.add(new ResourceLocation(completedList.getCompound(i).getString("id")));
        }
    }
}

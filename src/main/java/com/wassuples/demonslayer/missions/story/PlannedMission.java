package com.wassuples.demonslayer.missions.story;

import com.wassuples.demonslayer.missions.Arc;
import com.wassuples.demonslayer.missions.MissionContext;
import com.wassuples.demonslayer.missions.MissionTracker;
import com.wassuples.demonslayer.missions.PlayerMissions;
import com.wassuples.demonslayer.missions.StoryMission;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Placeholder for a story mission that isn't built yet, so the crow's list shows the whole story in order.
 * Never playable.
 */
public final class PlannedMission extends StoryMission {
    public PlannedMission(String path, int number, Arc arc) {
        super(path, number, arc);
    }

    @Override
    public boolean isPlayable() {
        return false;
    }

    @Override
    public Component summary() {
        return Component.translatable("mission.demonslayer.planned.summary");
    }

    @Override
    public Component location() {
        return arc().title();
    }

    @Override
    public List<Component> briefing() {
        return List.of();
    }

    @Override
    public boolean isUnlocked(ServerPlayer player, PlayerMissions missions) {
        return false;
    }

    @Override
    public void onAccept(MissionContext ctx) {
    }

    @Override
    public void tick(MissionContext ctx) {
    }

    @Override
    public MissionTracker tracker(MissionContext ctx) {
        return new MissionTracker(id(), arc().label(), title(), List.of(), Component.empty(), false);
    }
}

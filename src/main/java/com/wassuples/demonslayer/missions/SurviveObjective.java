package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public record SurviveObjective(int durationTicks) implements MissionObjective {
    @Override
    public Component getDescription() {
        // 20 ticks = 1 second. 24000 ticks = 1 day
        int days = durationTicks / 24000;
        return Component.literal("Survive for " + days + " day(s)");
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        // The check for this will be handled by the PlayerMissions capability
        return false; // This is checked by a different logic path
    }

    @Override
    public String getProgressKey() {
        return "survive_objective"; // A unique key for this type
    }
}
package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public record SurviveObjective(int durationTicks) implements MissionObjective {
    @Override
    public Component getDescription() {
        // 20 ticks = 1 second. 24000 ticks = 1 day.
        int days = durationTicks / 24000;
        String dayString = days > 1 ? " days" : " day";
        return Component.literal("Survive for " + days + dayString);
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        // --- THIS IS THE FIX ---
        // Get the GameTime when the mission started, which was stored when the mission was accepted.
        int startTime = progress.getOrDefault(getProgressKey(), (int)player.level.getGameTime());

        // Get the current GameTime.
        long currentTime = player.level.getGameTime();

        // If the difference is greater than or equal to the required duration, the objective is complete.
        return (currentTime - startTime) >= durationTicks;
    }

    @Override
    public String getProgressKey() {
        return "survive_objective"; // A unique key for this type
    }
}
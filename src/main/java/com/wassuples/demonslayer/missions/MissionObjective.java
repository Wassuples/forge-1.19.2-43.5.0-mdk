package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface MissionObjective {
    // Description, e.g. "Kill 10 Demons"
    Component getDescription();

    // Check if mission is complete
    boolean isComplete(ServerPlayer player, Map<String, Integer> progress);

    // A unique key to track progress (e.g., "kill.zombie")
    String getProgressKey();
}

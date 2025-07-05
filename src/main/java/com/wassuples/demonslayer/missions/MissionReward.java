package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface MissionReward {
    // A description of the reward (e.g., "500 Style Mastery")
    Component getDescription();

    // Grants the reward to the player on the server
    void grant(ServerPlayer player);
}

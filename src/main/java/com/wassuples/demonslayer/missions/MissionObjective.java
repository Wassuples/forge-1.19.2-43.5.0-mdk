package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface MissionObjective {
    Component getDescription();
    boolean isComplete(ServerPlayer player, Map<String, Integer> progress);
    String getProgressKey();
    void toBytes(FriendlyByteBuf buf);
    ObjectiveType<?> getType();
}


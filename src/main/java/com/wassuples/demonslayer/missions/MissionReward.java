package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface MissionReward {
    Component getDescription();
    void grant(ServerPlayer player);
    void toBytes(FriendlyByteBuf buf);
    RewardType<?> getType();
}


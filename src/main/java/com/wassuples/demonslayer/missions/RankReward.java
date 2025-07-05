package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public record RankReward(Rank newRank) implements MissionReward{
    @Override
    public Component getDescription() {
        return Component.literal("Rank up to ").append(newRank.getDisplayName());
    }

    @Override
    public void grant(ServerPlayer player) {
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            progression.setRank(newRank);
        });
    }
}

package com.wassuples.demonslayer.missions;

import com.google.gson.JsonObject;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public record RankReward(Rank rank) implements MissionReward {
    public RankReward(FriendlyByteBuf buf) {
        this(buf.readEnum(Rank.class));
    }

    public static RankReward fromJson(JsonObject json) {
        Rank rank = Rank.valueOf(GsonHelper.getAsString(json, "rank").toUpperCase());
        return new RankReward(rank);
    }

    @Override
    public Component getDescription() {
        return Component.literal("Rank up to ").append(rank.getDisplayName());
    }

    @Override
    public void grant(ServerPlayer player) {
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                .ifPresent(progress -> progress.setRank(rank));
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(rank);
    }

    @Override
    public RewardType<?> getType() {
        return RewardType.RANK;
    }
}

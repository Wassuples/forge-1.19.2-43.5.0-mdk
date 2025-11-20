package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public record KillObjective(EntityType<?> target, int count) implements MissionObjective {
    public KillObjective(FriendlyByteBuf buf) {
        this(EntityType.byString(buf.readUtf()).orElse(null), buf.readInt());
    }

    @Override
    public Component getDescription() {
        return Component.literal("Defeat " + count + "x ").append(target.getDescription());
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        int startingKills = progress.getOrDefault(getProgressKey(), 0);
        int currentKills = player.getStats().getValue(Stats.ENTITY_KILLED, target);
        return (currentKills - startingKills) >= count;
    }

    @Override
    public String getProgressKey() {
        return EntityType.getKey(target).toString();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(EntityType.getKey(target));
        buf.writeInt(count);
    }

    @Override
    public ObjectiveType<?> getType() {
        return ObjectiveType.KILL;
    }
}

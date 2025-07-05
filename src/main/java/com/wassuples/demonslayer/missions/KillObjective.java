package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public record KillObjective(EntityType<?> target, int count) implements MissionObjective {
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
}

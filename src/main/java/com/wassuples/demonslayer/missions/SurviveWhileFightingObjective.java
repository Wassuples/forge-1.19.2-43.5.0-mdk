package com.wassuples.demonslayer.missions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.Map;

public record SurviveWhileFightingObjective(EntityType<?> bossType, int durationTicks, double radius) implements MissionObjective {
    @Override
    public Component getDescription() {
        return Component.literal("Survive the encounter with ").append(bossType.getDescription().copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        int startTime = progress.getOrDefault(getProgressKey(), (int)player.level.getGameTime());
        // Check if enough time has passed
        if (player.level.getGameTime() - startTime >= durationTicks) {
            return true;
        }

        // Also fail if the boss is no longer nearby (player ran away or killed it early)
        AABB searchBox = player.getBoundingBox().inflate(radius);
        if (player.level.getEntities(bossType, searchBox, e -> true).isEmpty()) {
            // Player might have run away, we can optionally fail the mission here if desired.
            // For now, we just require them to stay engaged for the duration.
        }

        return false;
    }

    @Override
    public String getProgressKey() {
        // A unique key for this objective type
        return "survive_fight_" + EntityType.getKey(bossType);
    }
}
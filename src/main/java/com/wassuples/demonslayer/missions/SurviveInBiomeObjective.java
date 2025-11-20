package com.wassuples.demonslayer.missions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public record SurviveInBiomeObjective(ResourceLocation biomeId, int durationTicks) implements MissionObjective {
    public SurviveInBiomeObjective(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readInt());
    }

    @Override
    public Component getDescription() {
        int days = durationTicks / 24000;
        String dayString = days > 1 ? " days" : " day";
        return Component.literal("Survive for " + days + dayString + " in the ")
                .append(Component.literal(biomeId.getPath().replace("_", " ")).withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        // The completion logic is now simpler and correct.
        int startTime = progress.getOrDefault(getProgressKey(), (int)player.level.getGameTime());
        long currentTime = player.level.getGameTime();
        boolean timeSurvived = (currentTime - startTime) >= durationTicks;
        boolean bossDefeated = progress.getOrDefault("boss_defeated", 0) == 1;

        return timeSurvived && bossDefeated;
    }

    @Override
    public String getProgressKey() {
        return "survive_in_biome_" + biomeId.toString().replace(":", "_");
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(biomeId);
        buf.writeInt(durationTicks);
    }

    @Override
    public ObjectiveType<?> getType() {
        return ObjectiveType.SURVIVE_IN_BIOME;
    }
}
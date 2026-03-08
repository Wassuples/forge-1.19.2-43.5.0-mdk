package com.wassuples.demonslayer.missions;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Objects;

/**
 * Unified survival objective that can optionally constrain biome, bosses, and radius checks.
 */
public class SurvivalObjective implements MissionObjective {
    private final int durationTicks;
    private final ResourceLocation biome;
    private final EntityType<?> bossType;
    private final double radius;
    private final boolean requireBossAlive;

    public SurvivalObjective(FriendlyByteBuf buf) {
        this.durationTicks = buf.readVarInt();
        this.biome = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.bossType = buf.readBoolean() ? EntityType.byString(buf.readUtf()).orElse(null) : null;
        this.radius = buf.readDouble();
        this.requireBossAlive = buf.readBoolean();
    }

    public SurvivalObjective(int durationTicks, ResourceLocation biome, EntityType<?> bossType, double radius, boolean requireBossAlive) {
        this.durationTicks = durationTicks;
        this.biome = biome;
        this.bossType = bossType;
        this.radius = radius;
        this.requireBossAlive = requireBossAlive;
    }

    public static SurvivalObjective fromJson(JsonObject json) {
        int duration = GsonHelper.getAsInt(json, "duration_ticks", 0);
        if (duration <= 0) {
            throw new IllegalArgumentException("Survival objective requires duration_ticks > 0");
        }

        ResourceLocation biome = json.has("biome") ? new ResourceLocation(GsonHelper.getAsString(json, "biome")) : null;
        EntityType<?> boss = json.has("boss") ? EntityType.byString(GsonHelper.getAsString(json, "boss")).orElseThrow(() -> new IllegalArgumentException("Unknown boss entity")) : null;
        double radius = GsonHelper.getAsDouble(json, "radius", 16);
        boolean requireBossAlive = GsonHelper.getAsBoolean(json, "require_boss_alive", boss != null);
        return new SurvivalObjective(duration, biome, boss, radius, requireBossAlive);
    }

    @Override
    public Component getDescription() {
        MutableComponent base = Component.literal(formatDuration(durationTicks));
        if (biome != null) {
            base.append(Component.literal(" in ")).append(Component.literal(biome.getPath().replace('_', ' ')).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (bossType != null) {
            base.append(Component.literal(" while facing ")).append(bossType.getDescription().copy().withStyle(ChatFormatting.RED));
        }
        return base;
    }

    private static String formatDuration(int ticks) {
        int days = ticks / 24000;
        if (days > 0) {
            return "Survive for " + days + (days == 1 ? " day" : " days");
        }
        int seconds = ticks / 20;
        return "Survive for " + seconds + "s";
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        int start = progress.getOrDefault(getProgressKey(), (int) player.level.getGameTime());
        if (player.level.getGameTime() - start < durationTicks) {
            return false;
        }
        if (biome != null && !isPlayerInBiome(player, biome)) {
            return false;
        }
        if (bossType != null && requireBossAlive && !isBossNearby(player)) {
            return false;
        }
        return true;
    }

    private boolean isPlayerInBiome(ServerPlayer player, ResourceLocation biomeId) {
        ServerLevel level = player.server.getLevel(player.level.dimension());
        if (level == null) {
            return false;
        }
        BlockPos pos = player.blockPosition();
        ResourceKey<Biome> key = level.getBiome(pos).unwrapKey().orElse(null);
        return key != null && Objects.equals(key.location(), biomeId);
    }

    private boolean isBossNearby(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(radius);
        return !player.level.getEntities(bossType, box, Entity::isAlive).isEmpty();
    }

    @Override
    public String getProgressKey() {
        StringBuilder builder = new StringBuilder("survival:");
        if (biome != null) {
            builder.append("biome=").append(biome).append(";");
        }
        if (bossType != null) {
            builder.append("boss=").append(EntityType.getKey(bossType)).append(";");
        }
        builder.append("duration=").append(durationTicks);
        return builder.toString();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(durationTicks);
        buf.writeBoolean(biome != null);
        if (biome != null) {
            buf.writeResourceLocation(biome);
        }
        buf.writeBoolean(bossType != null);
        if (bossType != null) {
            buf.writeUtf(EntityType.getKey(bossType).toString());
        }
        buf.writeDouble(radius);
        buf.writeBoolean(requireBossAlive);
    }

    @Override
    public ObjectiveType<?> getType() {
        return ObjectiveType.SURVIVAL;
    }
}

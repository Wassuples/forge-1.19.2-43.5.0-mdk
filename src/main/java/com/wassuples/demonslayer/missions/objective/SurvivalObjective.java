package com.wassuples.demonslayer.missions.objective;

import com.google.gson.JsonObject;
import com.wassuples.demonslayer.missions.MissionObjective;
import com.wassuples.demonslayer.missions.ObjectiveType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
 * Mission system: Survival Objective.
 *
 * @see com.wassuples.demonslayer.missions.MissionRegistry
 */
public final class SurvivalObjective implements MissionObjective {
    private static final String START_KEY = "start_ticks";
    private static final String BIOME_TICKS_KEY = "biome_ticks";
    private static final String BOSS_SPAWNED_KEY = "boss_spawned";
    private static final String BIOME_TIMER_KEY = "biome_timer";

    private final int durationTicks;
    private final ResourceLocation biome;
    private final EntityType<?> bossType;
    private final double radius;
    private final boolean requireBossAlive;
    private final boolean spawnBossAtEnd;
    private final boolean resetOnLeaveBiome;
    private final int bossSpawnThresholdTicks;

    public SurvivalObjective(FriendlyByteBuf buf) {
        this.durationTicks = buf.readVarInt();
        this.biome = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.bossType = buf.readBoolean() ? EntityType.byString(buf.readUtf()).orElse(null) : null;
        this.radius = buf.readDouble();
        this.requireBossAlive = buf.readBoolean();
        this.spawnBossAtEnd = buf.readBoolean();
        this.resetOnLeaveBiome = buf.readBoolean();
        this.bossSpawnThresholdTicks = buf.readVarInt();
    }

    public SurvivalObjective(int durationTicks, ResourceLocation biome, EntityType<?> bossType, double radius, boolean requireBossAlive, boolean spawnBossAtEnd, boolean resetOnLeaveBiome, int bossSpawnThresholdTicks) {
        this.durationTicks = durationTicks;
        this.biome = biome;
        this.bossType = bossType;
        this.radius = radius;
        this.requireBossAlive = requireBossAlive;
        this.spawnBossAtEnd = spawnBossAtEnd;
        this.resetOnLeaveBiome = resetOnLeaveBiome;
        this.bossSpawnThresholdTicks = bossSpawnThresholdTicks;
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
        boolean spawnBoss = GsonHelper.getAsBoolean(json, "spawn_boss_at_end", false);
        boolean reset = GsonHelper.getAsBoolean(json, "reset_on_leave_biome", false);
        int spawnThreshold = GsonHelper.getAsInt(json, "boss_spawn_threshold_ticks", duration - 24000);
        return new SurvivalObjective(duration, biome, boss, radius, requireBossAlive, spawnBoss, reset, spawnThreshold);
    }

    private void spawnBoss(ServerPlayer player) {
        if (bossType == null) {
            return;
        }
        var entity = bossType.create(player.level);
        if (entity != null) {
            entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.level.addFreshEntity(entity);
        }
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
        if (bossType == null) {
            return true;
        }
        AABB box = player.getBoundingBox().inflate(radius);
        return !player.level.getEntities(bossType, box, Entity::isAlive).isEmpty();
    }

    @Override
    public Component getDescription() {
        return null;
    }

    @Override
    public boolean isComplete(ServerPlayer player, Map<String, Integer> progress) {
        if (biome != null && !isPlayerInBiome(player, biome)) {
            if (resetOnLeaveBiome) {
                progress.put(BIOME_TICKS_KEY, 0);
            }
            return false;
        }

        if (bossType != null && requireBossAlive && spawnBossAtEnd) {
            AABB box = player.getBoundingBox().inflate(radius);
            boolean bossPresent = !player.level.getEntities(bossType, box, Entity::isAlive).isEmpty();
            if (requireBossAlive && !bossPresent) {
                return false;
            }

            if (spawnBossAtEnd && !bossPresent) {
                int elapsed = progress.getOrDefault(BIOME_TICKS_KEY, 0);
                if (elapsed >= bossSpawnThresholdTicks && progress.getOrDefault(BOSS_SPAWNED_KEY, 0) == 0) {
                    spawnBoss(player);
                    progress.put(BOSS_SPAWNED_KEY, 1);
                }
                return false;
            }
        }

        return true;
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
        buf.writeBoolean(spawnBossAtEnd);
        buf.writeBoolean(resetOnLeaveBiome);
        buf.writeVarInt(bossSpawnThresholdTicks);
    }

    @Override
    public ObjectiveType<?> getType() {
        return null;
    }
}

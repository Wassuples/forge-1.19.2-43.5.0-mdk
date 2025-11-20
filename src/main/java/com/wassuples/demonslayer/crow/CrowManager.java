package com.wassuples.demonslayer.crow;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.entity.custom.CrowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CrowManager {
    private static final double DEFAULT_OFFSET_BACK = 5.0D;
    private static final double DEFAULT_OFFSET_UP = 4.0D;

    private CrowManager() {}

    public static void ensureCrowFor(ServerPlayer player) {
        if (player.level.isClientSide) return;
        player.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(data -> {
            data.setOwnerDimension(player.level.dimension());
            if (data.hasCrow()) {
                Entity entity = ((ServerLevel) player.level).getEntity(data.getCrowUuid());
                if (entity instanceof CrowEntity crow) {
                    crow.bindToOwner(player);
                    return;
                }
                data.setCrowUuid(null);
                data.setNeedsRespawn(true);
            }

            if (!data.hasCrow() && !data.needsRespawn()) {
                spawnCrow(player, data);
            }
        });
    }

    public static void handlePlayerSleep(ServerPlayer player) {
        if (player.level.isClientSide) return;
        player.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(data -> {
            data.setOwnerDimension(player.level.dimension());
            if (data.needsRespawn()) {
                spawnCrow(player, data);
            }
        });
    }

    public static void handleCrowDeath(CrowEntity crow) {
        if (!(crow.level instanceof ServerLevel serverLevel)) return;
        if (crow.getOwnerUUID() == null) return;
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(crow.getOwnerUUID());
        if (owner == null) return;
        owner.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(data -> {
            data.setCrowUuid(null);
            data.setNeedsRespawn(true);
        });
    }

    private static void spawnCrow(ServerPlayer player, PlayerCrowData data) {
        ServerLevel level = player.getLevel();
        CrowEntity crow = ModEntityTypes.CROW.get().create(level);
        if (crow == null) return;
        Vec3 spawnPos = getDefaultSpawnPos(player);
        crow.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0F);
        crow.bindToOwner(player);
        level.addFreshEntity(crow);
        data.setCrowUuid(crow.getUUID());
        data.setNeedsRespawn(false);
        data.setOwnerDimension(player.level.dimension());
    }

    private static Vec3 getDefaultSpawnPos(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        if (look.lengthSqr() == 0) {
            look = new Vec3(0, 0, 1);
        }
        Vec3 offset = look.scale(-DEFAULT_OFFSET_BACK).add(0, DEFAULT_OFFSET_UP, 0);
        return player.position().add(offset);
    }
}

package com.wassuples.demonslayer.util;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class ModUtils {
    private ModUtils() {}

    public static void syncProgression(ServerPlayer player) {
        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(
                    player.getId(),
                    progression.getRank(),
                    progression.getUnlockedStyles(),
                    progression.getStyleMasteryMap(),
                    progression.getFormMasteryMap()
            ), player);
        });
    }

    public static void syncMissions(ServerPlayer player) {
        player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            CompoundTag nbt = new CompoundTag();
            missions.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(nbt), player);
        });
    }

    public static void syncStyle(ServerPlayer player) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), player);
        });
    }

    public static void syncBreath(ServerPlayer player) {
        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player);
        });
    }

    public static void syncDemon(ServerPlayer player) {
        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            CompoundTag nbt = new CompoundTag();
            demon.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(nbt), player);
        });
    }

    public static void syncAll(ServerPlayer player) {
        syncProgression(player);
        syncMissions(player);
        syncStyle(player);
        syncBreath(player);
        syncDemon(player);
    }
}
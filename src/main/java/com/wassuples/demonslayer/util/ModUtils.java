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

/**
 * Miscellaneous helper utilities used across the mod.
 *
 * <p>This class primarily contains <b>server-side</b> synchronization helpers which read a capability's
 * current server state and send the appropriate S2C sync packet(s) to the owning player.
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>These helpers should be called on the logical server.</li>
 *   <li>They are intentionally tolerant: if a capability is missing, they do nothing.</li>
 * </ul>
 */
public final class ModUtils {
    private ModUtils() {}

    /**
     * Sends the player's progression state to the client.
     *
     * <p>Includes rank, unlocked styles, and mastery maps.
     */
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

    /**
     * Sends the player's mission data to the client.
     *
     * <p>Serialization is performed via the missions capability NBT.
     */
    public static void syncMissions(ServerPlayer player) {
        player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            CompoundTag nbt = new CompoundTag();
            missions.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(nbt), player);
        });
    }

    /** Sends the player's currently selected breathing style/form to the client. */
    public static void syncStyle(ServerPlayer player) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), player);
        });
    }

    /** Sends the player's current breath state to the client for UI display. */
    public static void syncBreath(ServerPlayer player) {
        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player);
        });
    }

    /** Sends the player's demon state (BDA, passives, etc) to the client. */
    public static void syncDemon(ServerPlayer player) {
        player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            CompoundTag nbt = new CompoundTag();
            demon.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(nbt), player);
        });
    }

    /** Convenience method that calls all sync helpers for the player. */
    public static void syncAll(ServerPlayer player) {
        syncProgression(player);
        syncMissions(player);
        syncStyle(player);
        syncBreath(player);
        syncDemon(player);
    }
}

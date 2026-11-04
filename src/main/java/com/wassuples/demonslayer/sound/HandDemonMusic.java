package com.wassuples.demonslayer.sound;

import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.HandDemonMusicS2CPacket;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side of the Hand Demon's theme: who hears it (everyone within the mountain's radius of its centre) and the
 * "silence" it imposes on the mountain's other demons while it plays. The music itself is chosen and sequenced on each
 * client by {@code HandDemonConductor}.
 */
public final class HandDemonMusic {
    private HandDemonMusic() {}

    /** Heard this far (horizontally) from the mountain's centre: the mountain's own footprint. */
    public static final int RADIUS = FujikasaneLayout.R;

    private static final Map<ResourceKey<Level>, Long> ACTIVE = new HashMap<>();

    /** Called every tick by a Hand Demon whose theme is playing (fighting, or dying). */
    public static void markActive(ServerLevel level) {
        ACTIVE.put(level.dimension(), level.getGameTime());
    }

    /** While the Hand Demon's theme plays, the mountain's other demons make no sound. */
    public static boolean silencing(Level level) {
        if (level.isClientSide) return false;
        Long at = ACTIVE.get(level.dimension());
        if (at == null) return false;
        long d = level.getGameTime() - at;
        return d >= 0 && d <= 40;
    }

    /** Sends {@code packet} to every player within {@link #RADIUS} of the mountain's centre (or of {@code origin}). */
    public static void broadcast(ServerLevel level, BlockPos origin, HandDemonMusicS2CPacket packet) {
        FujikasaneLayout layout = FujikasaneSites.known(level.getServer());
        double cx = layout != null ? layout.centerX + 0.5 : origin.getX() + 0.5;
        double cz = layout != null ? layout.centerZ + 0.5 : origin.getZ() + 0.5;
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - cx, dz = p.getZ() - cz;
            if (dx * dx + dz * dz <= (double) RADIUS * RADIUS) ModMessages.sendToPlayer(packet, p);
        }
    }
}

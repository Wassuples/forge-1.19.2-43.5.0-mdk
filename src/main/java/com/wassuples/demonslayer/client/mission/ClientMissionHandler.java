package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.client.gui.CrowDispatchScreen;
import com.wassuples.demonslayer.missions.MissionTracker;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.packet.OpenCrowDispatchS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only side of the mission packets. Kept out of the packet classes so a dedicated server never loads
 * screen classes.
 */
public final class ClientMissionHandler {
    /** Tracker for the active mission (null when none). Read by {@link MissionTrackerOverlay}. */
    @Nullable
    static volatile MissionTracker tracker;

    /** The live tracker of the player's active mission (null when none), for screens outside this package. */
    public static MissionTracker currentTracker() {
        return tracker;
    }

    private ClientMissionHandler() {
    }

    public static void applySync(CompoundTag data, @Nullable MissionTracker newTracker) {
        tracker = newTracker;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(m -> m.loadNBTData(data));
        }
    }

    public static void openDispatch(OpenCrowDispatchS2CPacket packet) {
        Minecraft.getInstance().setScreen(new CrowDispatchScreen(packet.kizuki, packet.fodder, packet.missions));
    }
}

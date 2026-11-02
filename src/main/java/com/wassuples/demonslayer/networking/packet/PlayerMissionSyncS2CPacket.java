package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.ClientMissionHandler;
import com.wassuples.demonslayer.missions.MissionTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Story progress (completed missions, flags, perks) plus the HUD tracker for the active mission, if any.
 */
public class PlayerMissionSyncS2CPacket {
    private final CompoundTag missionData;
    @Nullable
    private final MissionTracker tracker;

    public PlayerMissionSyncS2CPacket(CompoundTag missionData, @Nullable MissionTracker tracker) {
        this.missionData = missionData;
        this.tracker = tracker;
    }

    public PlayerMissionSyncS2CPacket(FriendlyByteBuf buf) {
        this.missionData = buf.readNbt();
        this.tracker = buf.readBoolean() ? MissionTracker.decode(buf) : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.missionData);
        buf.writeBoolean(tracker != null);
        if (tracker != null) tracker.encode(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMissionHandler.applySync(missionData, tracker)));
        context.setPacketHandled(true);
        return true;
    }
}

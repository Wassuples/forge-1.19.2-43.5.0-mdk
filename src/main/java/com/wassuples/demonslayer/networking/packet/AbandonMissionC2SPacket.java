package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.missions.MissionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client forfeits its active story mission. Ignored unless {@code missionId} is the active one.
 */
public class AbandonMissionC2SPacket {
    private final ResourceLocation missionId;

    public AbandonMissionC2SPacket(ResourceLocation missionId) {
        this.missionId = missionId;
    }

    public AbandonMissionC2SPacket(FriendlyByteBuf buf) {
        this.missionId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.missionId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) MissionManager.abandon(player, missionId, false);
        });
        context.setPacketHandled(true);
        return true;
    }
}

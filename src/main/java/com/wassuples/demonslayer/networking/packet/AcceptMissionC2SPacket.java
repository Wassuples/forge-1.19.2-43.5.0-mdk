package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.missions.MissionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client asks to start a story mission. Every condition is re-checked on the server ({@link MissionManager#tryAccept}).
 */
public class AcceptMissionC2SPacket {
    private final ResourceLocation missionId;

    public AcceptMissionC2SPacket(ResourceLocation missionId) {
        this.missionId = missionId;
    }

    public AcceptMissionC2SPacket(FriendlyByteBuf buf) {
        this.missionId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(missionId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Component refusal = MissionManager.tryAccept(player, missionId, false);
            if (refusal != null) MissionManager.notify(player, refusal, true);
        });
        context.setPacketHandled(true);
        return true;
    }
}

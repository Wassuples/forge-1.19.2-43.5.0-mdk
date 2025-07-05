package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbandonMissionC2SPacket {
    private final ResourceLocation missionId;

    public AbandonMissionC2SPacket(ResourceLocation missionId) { this.missionId = missionId; }
    public AbandonMissionC2SPacket(FriendlyByteBuf buf) { this.missionId = buf.readResourceLocation(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeResourceLocation(this.missionId); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                    missions.abandonMission(this.missionId);

                    CompoundTag missionData = new CompoundTag();
                    missions.saveNBTData(missionData);
                    ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), player);
                });
            }
        });
        return true;
    }
}
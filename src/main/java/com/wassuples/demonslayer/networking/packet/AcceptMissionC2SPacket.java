package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AcceptMissionC2SPacket {
    private final ResourceLocation missionId;

    public AcceptMissionC2SPacket(ResourceLocation missionId) { this.missionId = missionId; }
    public AcceptMissionC2SPacket(FriendlyByteBuf buf) { this.missionId = buf.readResourceLocation(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeResourceLocation(missionId); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            Mission mission = MissionRegistry.getMission(this.missionId);
            if (player != null && mission != null) {
                player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                    missions.acceptMission(mission, player);

                    CompoundTag missionData = new CompoundTag();
                    missions.saveNBTData(missionData);
                    ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), player);
                });
            }
        });
        return true;
    }
}
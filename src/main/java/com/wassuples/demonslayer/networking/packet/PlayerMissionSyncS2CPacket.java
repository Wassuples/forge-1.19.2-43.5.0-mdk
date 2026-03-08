package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client packet: player mission sync.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class PlayerMissionSyncS2CPacket {

    private final CompoundTag missionData;

    public PlayerMissionSyncS2CPacket(CompoundTag missionData) {
        this.missionData = missionData;
    }

    public PlayerMissionSyncS2CPacket(FriendlyByteBuf buf) {
        this.missionData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.missionData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE
            // When the client receives this packet, update their mission capability with the new data.
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
                    missions.loadNBTData(this.missionData);
                });
            }
        });
        return true;
    }
}

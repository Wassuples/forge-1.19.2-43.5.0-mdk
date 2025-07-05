package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PlayerDemonSyncS2CPacket {
    private final CompoundTag demonData;

    public PlayerDemonSyncS2CPacket(CompoundTag nbt) {
        this.demonData = nbt;
    }

    public PlayerDemonSyncS2CPacket(FriendlyByteBuf buf) {
        this.demonData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.demonData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    demon.loadNBTData(this.demonData);
                });
            }
        });
        return true;
    }
}
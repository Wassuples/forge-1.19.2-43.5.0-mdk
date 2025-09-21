package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerDemonSyncS2CPacket {
    private final CompoundTag nbtData;

    public PlayerDemonSyncS2CPacket(PlayerDemon demon) {
        this.nbtData = new CompoundTag();
        demon.saveNBTData(this.nbtData);
    }

    public PlayerDemonSyncS2CPacket(CompoundTag nbt) {
        this.nbtData = nbt;
    }

    public PlayerDemonSyncS2CPacket(FriendlyByteBuf buf) {
        this.nbtData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbtData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    demon.loadNBTData(this.nbtData);
                });
            }
        });
        return true;
    }
}
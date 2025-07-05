package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.events.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VignetteS2CPacket {
    private final boolean active;

    public VignetteS2CPacket(boolean active) {
        this.active = active;
    }

    public VignetteS2CPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.active);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE: Update our static flag in ClientEvents.
            ClientEvents.ClientForgeEvents.isVignetteActive = this.active;
        });
        return true;
    }
}
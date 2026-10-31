package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.ToonPocketOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToonPocketS2CPacket(int durationTicks, int result) {
    public ToonPocketS2CPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(durationTicks);
        buffer.writeVarInt(result);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ToonPocketOverlay.activate(durationTicks, result));
        return true;
    }
}

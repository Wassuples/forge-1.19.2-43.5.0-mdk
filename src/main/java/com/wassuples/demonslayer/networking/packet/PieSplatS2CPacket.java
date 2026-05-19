package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.PieSplatOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PieSplatS2CPacket {
    private final int durationTicks;

    public PieSplatS2CPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public PieSplatS2CPacket(FriendlyByteBuf buf) {
        this.durationTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.durationTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> PieSplatOverlay.activate(this.durationTicks));
        return true;
    }
}


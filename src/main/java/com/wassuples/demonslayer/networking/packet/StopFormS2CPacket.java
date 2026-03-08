package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.events.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

// This packet is sent from SERVER to CLIENT

/**
 * Server-to-client packet: stop form.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class StopFormS2CPacket {

    public StopFormS2CPacket() {
    }

    public StopFormS2CPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientEvents.resetAbilityKeyState();
        });
        return true;
    }
}
package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.DemonThreatEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: updates demon intimidation threat intensity.
 * Intensity is clamped to [0, 1].
 */
public class DemonThreatS2CPacket {
    private final float intensity;

    public DemonThreatS2CPacket(float intensity) {
        this.intensity = intensity;
    }

    public DemonThreatS2CPacket(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.intensity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DemonThreatEffects.setThreatIntensity(this.intensity));
        return true;
    }
}


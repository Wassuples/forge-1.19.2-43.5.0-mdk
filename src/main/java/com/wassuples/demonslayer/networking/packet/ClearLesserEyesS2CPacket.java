package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.LesserEyeRendererManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ClearLesserEyesS2CPacket {

    // This packet needs no data.
    public ClearLesserEyesS2CPacket() {
    }

    public ClearLesserEyesS2CPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LesserEyeRendererManager.clearEyePositions();
            });
        });
        return true;
    }
}
package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.LesserEyeRendererManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * S2C Packet to sync the COMPLETE list of active Lesser Eye positions to the client.
 */
public class SyncLesserEyesS2CPacket {

    private final List<BlockPos> eyePositions;

    public SyncLesserEyesS2CPacket(List<BlockPos> positions) {
        this.eyePositions = positions;
    }

    public SyncLesserEyesS2CPacket(FriendlyByteBuf buf) {
        this.eyePositions = buf.readList(FriendlyByteBuf::readBlockPos);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(this.eyePositions, FriendlyByteBuf::writeBlockPos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Pass the new list to our client-side manager
                LesserEyeRendererManager.updateEyePositions(this.eyePositions);
            });
        });
        return true;
    }
}
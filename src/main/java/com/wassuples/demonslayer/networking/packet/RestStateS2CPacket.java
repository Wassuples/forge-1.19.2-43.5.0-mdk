package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.RestPoseHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A player lay down on (or got up from) a bedroll: everyone around draws them lying on it, head towards
 * {@code headDir} (a horizontal {@link net.minecraft.core.Direction} 2D value; -1 when there is no mat to lie along).
 */
public class RestStateS2CPacket {
    public final int entityId;
    public final boolean resting;
    public final int headDir;

    public RestStateS2CPacket(int entityId, boolean resting, int headDir) {
        this.entityId = entityId;
        this.resting = resting;
        this.headDir = headDir;
    }

    public RestStateS2CPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readBoolean(), buf.readByte());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(resting);
        buf.writeByte(headDir);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RestPoseHandler.set(entityId, resting, headDir)));
        context.setPacketHandled(true);
        return true;
    }
}

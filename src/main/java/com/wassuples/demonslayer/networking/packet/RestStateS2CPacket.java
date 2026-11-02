package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.RestPoseHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A player lay down on (or got up from) a bedroll: everyone around draws them lying down. */
public class RestStateS2CPacket {
    public final int entityId;
    public final boolean resting;

    public RestStateS2CPacket(int entityId, boolean resting) {
        this.entityId = entityId;
        this.resting = resting;
    }

    public RestStateS2CPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(resting);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RestPoseHandler.set(entityId, resting)));
        context.setPacketHandled(true);
        return true;
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.QuestMarkerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Which characters around the player have something for them right now (a side errand to offer, one under way, one
 * ready to hand in), drawn in the world as paper talismans over their heads ({@link QuestMarkerRenderer}). Replaces
 * the whole set each time; empty clears it.
 */
public class QuestMarkersS2CPacket {
    public static final byte OFFER = 1, ACTIVE = 2, READY = 3;
    public final Map<Integer, Byte> markers;

    public QuestMarkersS2CPacket(Map<Integer, Byte> markers) {
        this.markers = markers;
    }

    public QuestMarkersS2CPacket(FriendlyByteBuf buf) {
        int n = Math.min(64, buf.readVarInt());
        this.markers = new HashMap<>();
        for (int i = 0; i < n; i++) markers.put(buf.readVarInt(), buf.readByte());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(markers.size());
        markers.forEach((id, kind) -> {
            buf.writeVarInt(id);
            buf.writeByte(kind);
        });
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> QuestMarkerRenderer.set(markers)));
        context.setPacketHandled(true);
        return true;
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player takes the dying Hand Demon's outstretched hand (only when Tanjiro is not there to). */
public class TakeHandC2SPacket {
    private final int entityId;

    public TakeHandC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public TakeHandC2SPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Entity e = player.level.getEntity(entityId);
            if (e instanceof HandDemonEntity hand) hand.takeHand(player);
        });
        context.setPacketHandled(true);
        return true;
    }
}

package com.wassuples.demonslayer.networking.packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayPlayerAnimationS2CPacket {
    private final int entityId;
    private final String animationId;

    public PlayPlayerAnimationS2CPacket(int entityId, String animationId) {
        this.entityId = entityId;
        this.animationId = animationId;
    }

    public PlayPlayerAnimationS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.animationId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeUtf(this.animationId, 64);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this.entityId, this.animationId)));
        return true;
    }

    private static class ClientPacketHandler {
        private static void handle(int entityId, String animationId) {
            com.wassuples.demonslayer.client.animation.ClientFormAnimations.play(entityId, animationId);
        }
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.glow.GlowCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet: update glow data.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class UpdateGlowDataPacket {
    private final int entityId;
    private final boolean isGlowing;
    private final boolean hasViewer;
    private final UUID viewerId;

    public UpdateGlowDataPacket(int entityId, String color, boolean isGlowing, UUID viewerId) {
        this.entityId = entityId;
        this.isGlowing = isGlowing;
        this.hasViewer = viewerId != null;
        this.viewerId = viewerId == null ? new UUID(0, 0) : viewerId;
    }

    public UpdateGlowDataPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.isGlowing = buf.readBoolean();
        this.hasViewer = buf.readBoolean();
        this.viewerId = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(isGlowing);
        buf.writeBoolean(hasViewer);
        buf.writeUUID(viewerId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            Entity entity = level.getEntity(this.entityId);
            if (entity != null) {
                entity.getCapability(GlowCapabilityProvider.GLOW_CAPABILITY).ifPresent(cap -> {
                    cap.setGlowing(this.isGlowing);
                    cap.setExclusiveViewer(this.hasViewer ? this.viewerId : null);
                });
            }
        });
        return true;
    }
}
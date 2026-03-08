package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.effects.HeatLightningClientEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-only packet that marks an entity to be rendered with the Heat Lightning "cracked skin" overlay for a duration.
 */
public class HeatLightningCracksS2CPacket {
    private final int entityId;
    private final int durationTicks;

    public HeatLightningCracksS2CPacket(int entityId, int durationTicks) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
    }

    public HeatLightningCracksS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.durationTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(durationTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            Entity e = level.getEntity(entityId);
            if (e != null) {
                HeatLightningClientEffects.markCracked(e.getId(), durationTicks);
            }
        });
        return true;
    }
}

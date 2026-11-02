package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.ClientCooldowns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client packet: the player's form cooldowns as remaining ticks (form id -> ticks), so the HUD and the
 * form wheel can show them. Sent by CooldownSyncEvents only when something changed.
 */
public class CooldownSyncS2CPacket {
    private final CompoundTag remaining;

    public CooldownSyncS2CPacket(CompoundTag remaining) {
        this.remaining = remaining;
    }

    public CooldownSyncS2CPacket(FriendlyByteBuf buf) {
        this.remaining = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.remaining);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCooldowns.accept(this.remaining)));
        return true;
    }
}

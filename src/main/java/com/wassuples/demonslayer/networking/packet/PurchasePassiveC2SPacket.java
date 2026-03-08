package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet: purchase passive.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class PurchasePassiveC2SPacket {
    private final DemonPassive passiveToPurchase;

    public PurchasePassiveC2SPacket(DemonPassive passive) {
        this.passiveToPurchase = passive;
    }

    public PurchasePassiveC2SPacket(FriendlyByteBuf buf) {
        this.passiveToPurchase = buf.readEnum(DemonPassive.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.passiveToPurchase);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Server-side logic
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    DemonProgressionHelper.tryUnlockPassive(player, demon, passiveToPurchase);
                });
            }
        });
        return true;
    }
}
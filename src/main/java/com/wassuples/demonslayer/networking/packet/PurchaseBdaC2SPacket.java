package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PurchaseBdaC2SPacket {
    private final BloodDemonArts bdaToPurchase;

    public PurchaseBdaC2SPacket(BloodDemonArts bda) {
        this.bdaToPurchase = bda;
    }

    public PurchaseBdaC2SPacket(FriendlyByteBuf buf) {
        this.bdaToPurchase = buf.readEnum(BloodDemonArts.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.bdaToPurchase);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    DemonProgressionHelper.tryPurchaseBda(player, demon, bdaToPurchase);
                });
            }
        });
        return true;
    }
}
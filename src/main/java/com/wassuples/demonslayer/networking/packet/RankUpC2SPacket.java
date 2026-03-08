package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet: rank up.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class RankUpC2SPacket {

    public RankUpC2SPacket() {}

    public RankUpC2SPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Server-side logic
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    DemonProgressionHelper.tryRankUp(player, demon);
                });
            }
        });
        return true;
    }
}
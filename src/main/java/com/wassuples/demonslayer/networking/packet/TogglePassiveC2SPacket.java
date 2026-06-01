package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet: toggle passive.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class TogglePassiveC2SPacket {
    private final DemonPassive passive;

    public TogglePassiveC2SPacket(DemonPassive passive) {
        this.passive = passive;
    }

    public TogglePassiveC2SPacket(FriendlyByteBuf buf) {
        this.passive = buf.readEnum(DemonPassive.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.passive);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                    if (demon.getPassiveTier(this.passive) <= 0) {
                        return;
                    }
                    demon.setPassiveActive(this.passive, !demon.isPassiveActive(this.passive));
                    ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(demon), player);
                });
            }
        });
        return true;
    }
}

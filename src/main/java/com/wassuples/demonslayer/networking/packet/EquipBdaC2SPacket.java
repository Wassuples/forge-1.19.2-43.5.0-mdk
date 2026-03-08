package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.wassuples.demonslayer.util.ModUtils.syncDemon;

/**
 * Client-to-server packet: equip bda.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class EquipBdaC2SPacket {
    private final BloodDemonArts bda;

    public EquipBdaC2SPacket(BloodDemonArts bda) {
        this.bda = bda;
    }

    public EquipBdaC2SPacket(FriendlyByteBuf buf) {
        this.bda = buf.readEnum(BloodDemonArts.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.bda);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                // Server-side validation: Only allow equipping if the player is a demon.
                if (demon.isDemon()) {
                    demon.setEquippedBda(this.bda);
                    syncDemon(player);
                }
            });
        });
        return true;
    }
}
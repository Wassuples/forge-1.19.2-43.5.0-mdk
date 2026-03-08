package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server (C2S) packet that changes the player's selected {@code BreathingStyles} index.
 *
 * <p>Typical source: Breathing Menu GUI.
 *
 * <p>Contract:
 * <ul>
 *   <li>Runs on the server thread (executed via {@code context.enqueueWork(...)}).</li>
 *   <li>Updates the {@code PlayerStyle} capability.</li>
 *   <li>Sends {@link PlayerStyleSyncS2CPacket} back to the player so the client GUI updates immediately.</li>
 * </ul>
 */
public class SwitchStyleC2SPacket {
    private final int styleIndex;

    public SwitchStyleC2SPacket(int styleIndex) {
        this.styleIndex = styleIndex;
    }

    public SwitchStyleC2SPacket(FriendlyByteBuf buf) {
        this.styleIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.styleIndex);
    }

    /** Handles the packet on the server side when it is received. */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // This code is executed on the server thread.
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    // Change the style on the server
                    style.changeStyle(this.styleIndex);

                    // Sync the change back to the client immediately so their GUI updates
                    ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), player);
                });
            }
        });
        return true;
    }
}

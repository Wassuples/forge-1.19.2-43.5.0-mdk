package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet that cycles the player's currently selected form.
 *
 * <p>Routing:
 *
 * <ul>
 *   <li>If the player is in BDA mode, cycles {@code DemonForms} via the demon capability.</li>
 *   <li>Otherwise, cycles {@code BreathingForms} via the style capability.</li>
 * </ul>
 *
 * <p>This packet only changes selection; it does not execute the form.
 */
public class ChangeFormC2SPacket {
    public ChangeFormC2SPacket() {

    }

    public ChangeFormC2SPacket(FriendlyByteBuf buf) {

    }

    public void toBytes(FriendlyByteBuf buf) {

    }

    /**
     * Handles the packet on the server.
     *
     * <p>Selection changes are performed server-side to keep the client authoritative state in sync.
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // WE ARE NOW ON THE SERVER, EVEN THOUGH THIS IS ON THE CLIENT!!
            ServerPlayer player = context.getSender();
            if(player == null)return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isBdaModeActive()) {
                    // --- BDA MODE IS ACTIVE ---
                    demon.cycleDemonForm();
                    player.sendSystemMessage(Component.literal("Switched to " + demon.getCurrentDemonForm().getDisplayName()), true);
                } else {
                    // --- BREATHING STYLE MODE IS ACTIVE ---
                    player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                        style.nextForm();
                        player.sendSystemMessage(Component.literal("Switched to " + style.getCurrentForms().getDisplayName()), true);
                    });
                }
            });
        });
        return true;
    }
}

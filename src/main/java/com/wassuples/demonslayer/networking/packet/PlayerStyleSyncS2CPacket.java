package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.events.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerStyleSyncS2CPacket {
    private final int formIndex;
    private final int styleIndex;

    public PlayerStyleSyncS2CPacket(int formIndex, int styleIndex) {
        this.formIndex = formIndex;
        this.styleIndex = styleIndex;
    }

    public PlayerStyleSyncS2CPacket(FriendlyByteBuf buf) {
        this.formIndex = buf.readInt();
        this.styleIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(formIndex);
        buf.writeInt(styleIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // This code runs on the CLIENT
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // ADD THIS LINE - Update our static tracker with the correct value from the server.
                ClientEvents.ClientForgeEvents.lastSyncedStyle = this.styleIndex;

                player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    // This part remains the same from the last fix
                    style.setStyleIndex(this.styleIndex);
                    style.setFormIndex(this.formIndex);
                });
            }
        });
        return true;
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import software.bernie.shadowed.eliotlash.mclib.math.functions.limit.Min;

import java.util.function.Supplier;

/**
 * Server-to-client packet: player breath sync.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class PlayerBreathSyncS2CPacket {
    private final int breath;
    private final int maxBreath;

    public PlayerBreathSyncS2CPacket(int breath, int maxBreath) {
        this.breath = breath;
        this.maxBreath = maxBreath;
    }

    public PlayerBreathSyncS2CPacket(FriendlyByteBuf buf) {
        this.breath = buf.readInt();
        this.maxBreath = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.breath);
        buf.writeInt(this.maxBreath);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
                // This code will run on the CLIENT side
                // We get the player's breath capability and update it with the values from the server.
                Minecraft.getInstance().player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(playerBreath -> {
                    playerBreath.setBreath(this.breath);
                    playerBreath.setMaxBreath(this.maxBreath);
                });
        });
        return true;
    }
}
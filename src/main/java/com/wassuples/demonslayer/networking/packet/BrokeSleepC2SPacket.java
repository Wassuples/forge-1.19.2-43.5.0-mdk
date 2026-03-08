package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.effects.ModEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet: broke sleep.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class BrokeSleepC2SPacket {
    public BrokeSleepC2SPacket() {
    }

    public BrokeSleepC2SPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (player.hasEffect(ModEffects.FORCED_SLEEP.get())) {
                player.removeEffect(ModEffects.FORCED_SLEEP.get());

                player.level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 1.5f);
            }
        });
        return true;
    }
}

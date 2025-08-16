package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.wassuples.demonslayer.util.ModUtils.syncDemon;

public class ToggleBdaModeC2SPacket {

    public ToggleBdaModeC2SPacket() {
    }

    public ToggleBdaModeC2SPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon()) {
                    boolean newState = !demon.isBdaModeActive();
                    demon.setBdaModeActive(newState);

                    syncDemon(player);

                    // Send feedback to the player
                    if (newState) {
                        player.sendSystemMessage(Component.literal("Blood Demon Art Activated.").withStyle(ChatFormatting.RED));
                        player.level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
                    } else {
                        player.sendSystemMessage(Component.literal("Blood Demon Art Deactivated.").withStyle(ChatFormatting.GRAY));
                        player.level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
                    }
                }
            });
        });
        return true;
    }
}
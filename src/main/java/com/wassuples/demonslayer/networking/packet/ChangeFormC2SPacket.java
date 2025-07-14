package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.styles.BreathingForms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChangeFormC2SPacket {
    public ChangeFormC2SPacket() {

    }

    public ChangeFormC2SPacket(FriendlyByteBuf buf) {

    }

    public void toBytes(FriendlyByteBuf buf) {

    }

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

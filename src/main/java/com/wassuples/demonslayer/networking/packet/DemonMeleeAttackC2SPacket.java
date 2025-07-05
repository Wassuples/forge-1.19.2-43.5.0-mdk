package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DemonMeleeAttackC2SPacket {

    public DemonMeleeAttackC2SPacket() { }

    public DemonMeleeAttackC2SPacket(FriendlyByteBuf buf) { }

    public void toBytes(FriendlyByteBuf buf) { }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // SERVER-SIDE
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isDemon()) {
                    FormUtils.performDemonAttack(player);

                    player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(playerProgression -> {
                        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(playerStyle -> {
                            BreathingStyles currentStyle = playerStyle.getCurrentStyles();
                            playerProgression.addStyleMastery(currentStyle, 1);
                            ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(player.getId(), playerProgression.getRank(), playerProgression.getUnlockedStyles(), playerProgression.getStyleMasteryMap(), playerProgression.getFormMasteryMap()), player);
                        });
                    });
                }
            });
        });
        return true;
    }
}
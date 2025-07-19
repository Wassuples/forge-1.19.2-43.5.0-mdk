package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChargeFormC2SPacket {

    public enum ChargeType {
        START, RELEASE, CANCEL
    }

    private final ChargeType type;

    public ChargeFormC2SPacket(ChargeType type) {
        this.type = type;
    }

    public ChargeFormC2SPacket(FriendlyByteBuf buf) {
        this.type = buf.readEnum(ChargeType.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            System.out.println("----------------------------------------------------");
            System.out.println("[DEBUG] Received ChargeFormC2SPacket with type: " + this.type);

            if (this.type == ChargeType.RELEASE) {
                FormUtils.ActiveFormManager.releaseCharge(player);
                return;
            }

            if (this.type == ChargeType.CANCEL) {
                FormUtils.ActiveFormManager.stop(player);
                return;
            }

            if (this.type == ChargeType.START) {
                IForm formToUse = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                        .filter(PlayerDemon::isBdaModeActive)
                        .map(PlayerDemon::getCurrentDemonForm)
                        .map(IForm.class::cast)
                        .orElse(player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                                .map(PlayerStyle::getCurrentForms)
                                .orElse(null)
                        );

                if (formToUse == null || formToUse.getInputType() != IForm.InputType.HOLD) {
                    return;
                }

                boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(PlayerDemon::isDemon).orElse(false);
                if (!isDemon && !formToUse.IsBDA() && !FormUtils.isPlayerHoldingNichirin(player)) {
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                    return;
                }

                FormUtils.ActiveFormManager.startCharge(player, formToUse);
            }
        });
        return true;
    }
}
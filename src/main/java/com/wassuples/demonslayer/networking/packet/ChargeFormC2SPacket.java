package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChargeFormC2SPacket {
    public enum ChargeType { START, RELEASE, CANCEL }

    private final ChargeType type;
    private final int chargeTime;

    public ChargeFormC2SPacket(ChargeType type) {
        this.type = type;
        this.chargeTime = 0;
    }

    public ChargeFormC2SPacket(ChargeType type, int chargeTime) {
        this.type = type;
        this.chargeTime = chargeTime;
    }

    public ChargeFormC2SPacket(FriendlyByteBuf buf) {
        this.type = buf.readEnum(ChargeType.class);
        this.chargeTime = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.type);
        buf.writeInt(this.chargeTime);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

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
            boolean isHoldingNichirin = player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                    player.getOffhandItem().getItem() instanceof NichirinSwordItem;

            if (!isDemon && !isHoldingNichirin) {
                if (this.type == ChargeType.START) {
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                }
                return;
            }

            switch (this.type) {
                case START:
                    FormUtils.ActiveFormManager.startCharge(player, formToUse);
                    break;
                case RELEASE:
                    boolean wasFullyCharged = this.chargeTime >= formToUse.getChargeTicks();
                    FormUtils.ActiveFormManager.releaseCharge(player, wasFullyCharged);
                    if (!wasFullyCharged) {
                        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.5f);
                    }
                    break;
                case CANCEL:
                    FormUtils.ActiveFormManager.stop(player);
                    break;
            }
        });
        return true;
    }
}
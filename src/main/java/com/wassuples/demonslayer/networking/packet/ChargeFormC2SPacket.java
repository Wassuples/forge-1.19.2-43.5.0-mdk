package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.InputType;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.text.Normalizer;
import java.util.function.Supplier;

public class ChargeFormC2SPacket {
    public enum ChargeType { START, RELEASE, CANCEL }

    private final ChargeType type;
    private final int chargeTime;

    // Constructor for START
    public ChargeFormC2SPacket(ChargeType type) {
        this.type = type;
        this.chargeTime = 0;
    }

    // Constructor for RELEASE/CANCEL
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
            if(player == null) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                boolean isDemon = demon.isDemon();
                if(!(player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                        player.getOffhandItem().getItem() instanceof  NichirinSwordItem) ||
                        !isDemon) {
                    if(this.type == ChargeType.START) {
                        player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                    }
                    return;
                }
            });

            player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                BreathingForms currentForm = style.getCurrentForms();
                if(currentForm.getInputType() != BreathingForms.InputType.HOLD) { return; }
                if(this.type == ChargeType.START) {
                    FormUtils.ActiveFormManager.startCharge(player, currentForm);
                } else if (this.type == ChargeType.RELEASE) {
                    boolean wasFullyCharged = this.chargeTime >= currentForm.getChargeTicks();
                    FormUtils.ActiveFormManager.releaseCharge(player, wasFullyCharged);
                    if(!wasFullyCharged) {
                        player.getLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.5f);
                    }
                } else if (this.type == ChargeType.CANCEL) {
                    FormUtils.ActiveFormManager.stop(player);
                }
            });
        });
        return true;
    }
}

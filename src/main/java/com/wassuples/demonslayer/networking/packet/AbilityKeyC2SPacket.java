package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityKeyC2SPacket {

    public enum KeyState {
        PRESS, RELEASE
    }

    private final KeyState keyState;

    public AbilityKeyC2SPacket(KeyState state) {
        this.keyState = state;
    }

    public AbilityKeyC2SPacket(FriendlyByteBuf buf) {
        this.keyState = buf.readEnum(KeyState.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.keyState);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // KEY RELEASE
            if (this.keyState == KeyState.RELEASE) {
                FormUtils.ActiveForm activeForm = FormUtils.ActiveFormManager.getActiveForm(player);
                if (activeForm instanceof FormUtils.ChargeableForm chargeable) {
                    boolean wasFullyCharged = chargeable.chargeTicks >= chargeable.chargeGoal;
                    chargeable.release(wasFullyCharged);
                }
                return;
            }

            // KEY PRESS
            if (this.keyState == KeyState.PRESS) {
                IForm formToUse = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                        .filter(PlayerDemon::isBdaModeActive)
                        .map(PlayerDemon::getCurrentDemonForm)
                        .map(IForm.class::cast)
                        .orElseGet(() -> player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                                .map(PlayerStyle::getCurrentForms)
                                .orElse(null));

                if (formToUse == null) return;

                if (formToUse.isBuff()) {
                    formToUse.performAction(player, (ServerLevel) player.level);

                } else {
                    // Guard: Is a combat move already in progress? If so, block the new one
                    if (FormUtils.ActiveFormManager.getActiveForm(player) != null) {
                        return;
                    }

                    // Guard: Is the form on cooldown?
                    PlayerCooldowns cooldowns = player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS).orElse(null);
                    if (cooldowns == null) {
                        DemonSlayer.LOGGER.error("CRITICAL: Player {} is missing PlayerCooldowns capability!", player.getName().getString());
                        return;
                    }
                    if (cooldowns.isOnCooldown(formToUse)) {
                        int ticksLeft = cooldowns.getCooldownRemainingTicks(formToUse);
                        player.sendSystemMessage(Component.literal(formToUse.getDisplayName() + " is on cooldown for " + String.format("%.1f", ticksLeft / 20.0) + "s"), true);
                        return;
                    }

                    // Guard: Validation checks
                    boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(PlayerDemon::isDemon).orElse(false);
                    if (!isDemon && !formToUse.IsBDA() && !FormUtils.isPlayerHoldingNichirin(player)) {
                        player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                        return;
                    }

                    // All checks passed, perform the combat move.
                    formToUse.performAction(player, (ServerLevel) player.level);

                    // Put the combat move on cooldown.
                    int cooldownTicks = formToUse.getCooldownTicks(player);
                    cooldowns.setOnCooldown(formToUse, cooldownTicks);
                }
            }
        });
        return true;
    }
}

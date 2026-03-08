package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.wassuples.demonslayer.util.CombatHelper.isPlayerHoldingNichirin;

public class AbilityKeyC2SPacket {
    private static final boolean DEBUG_FORMS = false;

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

            PlayerCooldowns cooldowns = player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS)
                    .orElseThrow(() -> new IllegalStateException("Missing PlayerCooldowns capability for player " + player.getName().getString()));

            // Handle key RELEASE for chargeable forms
            if (this.keyState == KeyState.RELEASE) {
                ActiveForm activeForm = ActiveFormManager.getActiveForm(player);

                if (activeForm instanceof ChargeableForm chargeable) {
                    boolean wasFullyCharged = chargeable.chargeTicks >= chargeable.chargeGoal;
                    IForm form = chargeable.getForm();

                    chargeable.release(wasFullyCharged);

                    if (wasFullyCharged) {
                        int cooldownTicks = form.getCooldownTicks(player);
                        cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

                        if (cooldownTicks > 0) {
                            cooldowns.setOnCooldown(player.level, form, cooldownTicks);
                        }
                    }
                }
                return;
            }

            // Handle key PRESS
            if (this.keyState == KeyState.PRESS) {
                IForm formToUse = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                        .filter(PlayerDemon::isBdaModeActive)
                        .map(PlayerDemon::getCurrentDemonForm)
                        .map(IForm.class::cast)
                        .orElseGet(() -> player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                                .map(PlayerStyle::getCurrentForms)
                                .orElse(null));

                if (formToUse == null) {
                    return;
                }

                ActiveForm activeForm = ActiveFormManager.getActiveForm(player);

                // --- DEACTIVATION LOGIC ---
                // Only allow 'press again to toggle' behavior for forms that are intended to be toggled (buffs).
                // For non-buff active forms (like WATER_2 TimedForm), pressing again should NOT re-trigger the action.
                if (activeForm != null && activeForm.getForm() == formToUse) {
                    if (activeForm.getForm().isBuff()) {
                        formToUse.performAction(player, (ServerLevel) player.level);
                    }
                    return;
                }

                // --- ACTIVATION LOGIC ---
                if (activeForm != null && !activeForm.getForm().isBuff()) {
                    return;
                }

                if (cooldowns.isOnCooldown(player.level, formToUse)) {
                    int ticksLeft = cooldowns.getCooldownRemainingTicks(player.level, formToUse);
                    player.sendSystemMessage(Component.literal(formToUse.getDisplayName() + " is on cooldown for " + String.format("%.1f", ticksLeft / 20.0) + "s"), true);
                    return;
                }

                boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(PlayerDemon::isDemon).orElse(false);
                if (!isDemon && (formToUse instanceof com.wassuples.demonslayer.styles.BreathingForms) && !isPlayerHoldingNichirin(player)) {
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                    return;
                }

                formToUse.performAction(player, (ServerLevel) player.level);

                if (formToUse.getInputType() == IForm.InputType.PRESS) {
                    int cooldownTicks = formToUse.getCooldownTicks(player);
                    cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

                    if (cooldownTicks > 0) {
                        cooldowns.setOnCooldownAtLeastOneTick(player.level, formToUse, cooldownTicks);
                    }
                }
            }
        });
        return true;
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.forms.ActiveFormManager;
import com.wassuples.demonslayer.forms.baseForms.ActiveForm;
import com.wassuples.demonslayer.forms.baseForms.ChargeableForm;
import com.wassuples.demonslayer.item.custom.HashiraSwordHelper;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.wassuples.demonslayer.util.CombatHelper.isPlayerHoldingNichirin;

/**
 * Client-to-server packet: ability key.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
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

            PlayerCooldowns cooldowns = player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS)
                    .orElseThrow(() -> new IllegalStateException("Missing PlayerCooldowns capability for player " + player.getName().getString()));

            // Handle key RELEASE for chargeable forms
            if (this.keyState == KeyState.RELEASE) {
                ActiveForm activeForm = ActiveFormManager.getActiveForm(player);

                if (activeForm instanceof ChargeableForm chargeable) {
                    boolean wasFullyCharged = chargeable.chargeTicks >= chargeable.chargeGoal;
                    IForm form = chargeable.getForm();

                    // Deduct breath cost on successful charge release
                    if (wasFullyCharged && form instanceof BreathingForms breathingForm) {
                        int breathCost = breathingForm.getBreathCost();
                        boolean hasEnoughBreath = player.getCapability(PlayerBreathProvider.PLAYER_BREATH)
                                .map(breath -> breath.getBreath() >= breathCost)
                                .orElse(false);
                        if (!hasEnoughBreath) {
                            player.sendSystemMessage(Component.literal("You do not have enough breath. Requires: " + breathCost)
                                    .withStyle(ChatFormatting.DARK_AQUA));
                            ActiveFormManager.stop(player);
                            return;
                        }
                        player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> breath.subBreath(breathCost));
                        ModUtils.syncBreath(player);
                    }

                    chargeable.release(wasFullyCharged);

                    if (wasFullyCharged) {
                        int cooldownTicks = form.getCooldownTicks(player);
                        cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

                        if (cooldownTicks > 0) {
                            cooldowns.setOnCooldown(player.level, form, cooldownTicks);
                        }

                        // Grant mastery on successful charge
                        PlayerProgressionProvider.grantMasteryOnUse(player, form);
                        ModUtils.syncProgression(player);
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

                // --- DEACTIVATION / REUSE LOGIC ---
                // Keep tap-to-toggle behavior only for buff forms.
                if (activeForm != null && activeForm.getForm() == formToUse) {
                    if (activeForm.getForm().isBuff()) {
                        formToUse.performAction(player, (ServerLevel) player.level);
                    }
                    return;
                }

                // --- ACTIVATION LOGIC ---
                // Allow swapping to a different form if the current one is interruptible.
                // Non-interruptible forms still hard-block new activations until they end.
                if (activeForm != null && !activeForm.isInterruptible()) {
                    return;
                }

                if (cooldowns.isOnCooldown(player.level, formToUse)) {
                    int ticksLeft = cooldowns.getCooldownRemainingTicks(player.level, formToUse);
                    player.sendSystemMessage(Component.literal(formToUse.getDisplayName() + " is on cooldown for " + String.format("%.1f", ticksLeft / 20.0) + "s"), true);
                    return;
                }

                boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(PlayerDemon::isDemon).orElse(false);
                boolean isBdaMode = player.getCapability(PlayerDemonProvider.PLAYER_DEMON).map(PlayerDemon::isBdaModeActive).orElse(false);

                if (!isDemon && (formToUse instanceof BreathingForms) && !isPlayerHoldingNichirin(player)) {
                    player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                    return;
                }

                // --- Breathing form checks (non-BDA) ---
                if (formToUse instanceof BreathingForms breathingForm && !isBdaMode) {
                    // Underwater check
                    boolean isUnderwaterBlocked = player.isUnderWater() && player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                            .map(p -> p.getRank().ordinal() < Rank.TSUCHINOTO.ordinal())
                            .orElse(true);
                    if (isUnderwaterBlocked) {
                        player.sendSystemMessage(Component.literal("You struggle to maintain your breath underwater.").withStyle(ChatFormatting.DARK_AQUA));
                        player.level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        return;
                    }

                    // Form unlock check
                    boolean isUnlocked = player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION)
                            .map(progression -> {
                                Object styleObj = breathingForm.getStyle();
                                if (styleObj instanceof BreathingStyles bs) {
                                    return progression.getStyleMastery(bs) >= breathingForm.getStyleMasteryToUnlock();
                                }
                                return false;
                            }).orElse(false);
                    if (!isUnlocked) {
                        player.sendSystemMessage(Component.literal("This form hasn't been unlocked yet.").withStyle(ChatFormatting.RED));
                        return;
                    }

                    // Breath cost check
                    int breathCost = breathingForm.getBreathCost();
                    boolean hasEnoughBreath = player.getCapability(PlayerBreathProvider.PLAYER_BREATH)
                            .map(breath -> breath.getBreath() >= breathCost)
                            .orElse(false);
                    if (!hasEnoughBreath) {
                        player.sendSystemMessage(Component.literal("You do not have enough breath. Requires: " + breathCost)
                                .withStyle(ChatFormatting.DARK_AQUA));
                        player.level.playSound(null, player.getOnPos(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                        return;
                    }

                    // Deduct breath
                    player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> breath.subBreath(breathCost));
                }

                // --- BDA form checks (demon mode) ---
                if (formToUse instanceof DemonForms demonForm && isBdaMode) {
                    int hungerCost = Math.max(0, demonForm.getBreathCost());
                    if (hungerCost > 0 && !player.getAbilities().instabuild) {
                        FoodData foodData = player.getFoodData();
                        int currentFood = foodData.getFoodLevel();
                        if (currentFood < hungerCost) {
                            player.sendSystemMessage(Component.literal("You are too hungry/weak to use " + demonForm.getDisplayName() + ". Requires: " + hungerCost)
                                    .withStyle(ChatFormatting.RED), true);
                            player.level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                            return;
                        }
                        foodData.setFoodLevel(Math.max(0, currentFood - hungerCost));
                        foodData.setSaturation(0.0F);
                    }
                }

                formToUse.performAction(player, (ServerLevel) player.level);

                if (formToUse.getInputType() == IForm.InputType.PRESS) {
                    int cooldownTicks = formToUse.getCooldownTicks(player);

                    // Hashira sword synergy for breathing forms
                    if (formToUse instanceof BreathingForms) {
                        boolean hashiraSynergy = player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                                .map(style -> HashiraSwordHelper.doesSwordMatchStyle(player.getMainHandItem(), style.getCurrentStyles())
                                        || HashiraSwordHelper.doesSwordMatchStyle(player.getOffhandItem(), style.getCurrentStyles()))
                                .orElse(false);
                        if (hashiraSynergy) {
                            cooldownTicks = Math.max(1, Math.round(cooldownTicks * 0.80f));
                        }
                    }

                    cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

                    if (cooldownTicks > 0) {
                        cooldowns.setOnCooldownAtLeastOneTick(player.level, formToUse, cooldownTicks);
                    }
                }

                // Grant mastery and sync
                PlayerProgressionProvider.grantMasteryOnUse(player, formToUse);
                if (formToUse instanceof BreathingForms) {
                    ModUtils.syncBreath(player);
                }
                ModUtils.syncProgression(player);
            }
        });
        return true;
    }
}

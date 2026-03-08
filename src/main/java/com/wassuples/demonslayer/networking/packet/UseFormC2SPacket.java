package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.cooldown.PlayerCooldowns;
import com.wassuples.demonslayer.cooldown.PlayerCooldownsProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.HashiraSwordHelper;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.styles.IForm;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseFormC2SPacket {
    private static final boolean DEBUG_FORMS = false;

    public UseFormC2SPacket() { }
    public UseFormC2SPacket(FriendlyByteBuf buf) { }
    public void toBytes(FriendlyByteBuf buf) { }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerCooldowns cooldowns = player.getCapability(PlayerCooldownsProvider.PLAYER_COOLDOWNS)
                    .orElseThrow(() -> new IllegalStateException("Missing PlayerCooldowns capability for player " + player.getName().getString()));

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isBdaModeActive()) {
                    handleBdaForm(player, demon, cooldowns);
                } else {
                    handleBreathingForm(player, cooldowns);
                }
            });
        });
        return true;
    }

    private void handleBdaForm(ServerPlayer player, PlayerDemon demon, PlayerCooldowns cooldowns) {
        DemonForms currentForm = demon.getCurrentDemonForm();
        if (currentForm == null) return;

        // Cooldown guard
        if (cooldowns.isOnCooldown(player.level, currentForm)) {
            int ticksLeft = cooldowns.getCooldownRemainingTicks(player.level, currentForm);
            player.sendSystemMessage(Component.literal(currentForm.getDisplayName() + " is on cooldown for " + String.format("%.1f", ticksLeft / 20.0) + "s"), true);
            return;
        }

        // BDA resource cost: drain hunger (not breath)
        int hungerCost = Math.max(0, currentForm.getBreathCost());
        if (hungerCost > 0 && !player.getAbilities().instabuild) {
            FoodData foodData = player.getFoodData();
            int currentFood = foodData.getFoodLevel();

            if (currentFood < hungerCost) {
                player.sendSystemMessage(Component.literal("You are too hungry/weak to use " + currentForm.getDisplayName() + ". Requires: " + hungerCost)
                        .withStyle(ChatFormatting.RED), true);
                player.level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                return;
            }

            // Consume hunger. We intentionally *don't* touch breath here. Only hunger.
            foodData.setFoodLevel(Math.max(0, currentFood - hungerCost));
            // Clear saturation MWAHAHAHHA. GOLDEN CARROTS SHALL DO YOU NO GOOD HERE...
            foodData.setSaturation(0.0F);
        }

        currentForm.performAction(player, player.getLevel());

        if (currentForm.getInputType() == IForm.InputType.PRESS) {
            int cooldownTicks = currentForm.getCooldownTicks(player);
            cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

            if (cooldownTicks > 0) {
                cooldowns.setOnCooldownAtLeastOneTick(player.level, currentForm, cooldownTicks);
            }
        }
    }

    private void handleBreathingForm(ServerPlayer player, PlayerCooldowns cooldowns) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {

                    BreathingForms currentForm = style.getCurrentForms();

                    // Prevent HOLD forms from activating via simple press
                    if (currentForm.getInputType() == BreathingForms.InputType.HOLD) {
                        return;
                    }

                    // Cooldown guard
                    if (cooldowns.isOnCooldown(player.level, currentForm)) {
                        int ticksLeft = cooldowns.getCooldownRemainingTicks(player.level, currentForm);
                        player.sendSystemMessage(Component.literal(currentForm.getDisplayName() + " is on cooldown for " + String.format("%.1f", ticksLeft / 20.0) + "s"), true);
                        return;
                    }

                    // Check 1: Must hold a Nichirin Sword
                    boolean isHoldingNichirin = player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                            player.getOffhandItem().getItem() instanceof NichirinSwordItem;
                    if (!isHoldingNichirin) {
                        player.sendSystemMessage(Component.literal("You must hold a Nichirin Sword to use Breathing Styles.").withStyle(ChatFormatting.RED));
                        player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                        return;
                    }

                    // Check 2: Underwater usage
                    if (player.isUnderWater() && progression.getRank().ordinal() < Rank.TSUCHINOTO.ordinal()) {
                        player.sendSystemMessage(Component.literal("You struggle to maintain your breath underwater.").withStyle(ChatFormatting.DARK_AQUA));
                        player.level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        return;
                    }

                    // Check 3: Form unlocked
                    boolean isUnlocked = false;
                    Object styleObject = currentForm.getStyle();

                    if (styleObject instanceof com.wassuples.demonslayer.styles.BreathingStyles breathingStyle) {
                        isUnlocked = progression.getStyleMastery(breathingStyle) >= currentForm.getStyleMasteryToUnlock();
                    }

                    if (!isUnlocked) {
                        player.sendSystemMessage(Component.literal("This form hasn't been unlocked yet.").withStyle(ChatFormatting.RED));
                        return;
                    }

                    // Check 4: Enough breath
                    if (breath.getBreath() < currentForm.getBreathCost()) {
                        player.sendSystemMessage(Component.literal("You do not have enough breath. Requires: " + currentForm.getBreathCost())
                                .withStyle(ChatFormatting.DARK_AQUA));
                        player.level.playSound(null, player.getOnPos(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                        return;
                    }

                    // --- All checks passed, execute action ---
                    breath.subBreath(currentForm.getBreathCost());
                    currentForm.performAction(player, player.getLevel());

                    int cooldownTicks = currentForm.getCooldownTicks(player);

                    // Hashira sword synergy: only if sword matches the CURRENT breathing style.
                    if (HashiraSwordHelper.doesSwordMatchStyle(player.getMainHandItem(), style.getCurrentStyles())
                            || HashiraSwordHelper.doesSwordMatchStyle(player.getOffhandItem(), style.getCurrentStyles())) {
                        // -20% cooldown
                        cooldownTicks = Math.max(1, Math.round(cooldownTicks * 0.80f));
                    }

                    cooldownTicks = PlayerCooldowns.applyFormsCooldownMultiplier(player.level, cooldownTicks);

                    if (cooldownTicks > 0) {
                        cooldowns.setOnCooldownAtLeastOneTick(player.level, currentForm, cooldownTicks);
                    }

                    PlayerProgressionProvider.grantMasteryOnUse(player, currentForm);
                    ModUtils.syncBreath(player);
                    ModUtils.syncProgression(player);
                });
            });
        });
    }
}

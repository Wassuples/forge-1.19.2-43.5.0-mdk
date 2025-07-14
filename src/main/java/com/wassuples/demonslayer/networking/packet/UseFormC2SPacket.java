package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.DemonForms;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseFormC2SPacket {
    public UseFormC2SPacket() { }
    public UseFormC2SPacket(FriendlyByteBuf buf) { }
    public void toBytes(FriendlyByteBuf buf) { }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (demon.isBdaModeActive()) {
                    handleBdaForm(player, demon);
                } else {
                    handleBreathingForm(player);
                }
            });
        });
        return true;
    }

    private void handleBdaForm(ServerPlayer player, PlayerDemon demon) {
        DemonForms currentForm = demon.getCurrentDemonForm();
        if (currentForm == null) return;
        currentForm.performAction(player, player.getLevel());
    }

    private void handleBreathingForm(ServerPlayer player) {
        player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {

                    BreathingForms currentForm = style.getCurrentForms();

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
                    boolean isUnlocked = progression.getStyleMastery(currentForm.getStyle()) >= currentForm.getStyleMasteryToUnlock();
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

                    PlayerProgressionProvider.grantMasteryOnUse(player, currentForm);
                    ModUtils.syncBreath(player);
                    ModUtils.syncProgression(player);
                });
            });
        });
    }
}
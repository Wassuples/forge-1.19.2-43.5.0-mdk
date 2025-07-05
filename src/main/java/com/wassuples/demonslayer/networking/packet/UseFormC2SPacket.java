package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider; // <-- Make sure this is imported
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BreathingForms;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
            ServerLevel level = player.getLevel();

            BreathingForms currentForm = player.getCapability(PlayerStyleProvider.PLAYER_STYLE)
                    .map(style -> style.getCurrentForms())
                    .orElse(null);

            boolean isDemon = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                    .map(PlayerDemon::isDemon)
                    .orElse(false);

            if (currentForm == null) return;

            boolean isHoldingNichirin = player.getMainHandItem().getItem() instanceof NichirinSwordItem ||
                    player.getOffhandItem().getItem() instanceof NichirinSwordItem;

            // Is the player trying to  breath underwater?
            if (!currentForm.IsBDA() && player.isUnderWater()) {
                player.sendSystemMessage(Component.literal("You cannot use Breathing Styles underwater!").withStyle(ChatFormatting.DARK_AQUA));
                player.level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.0f, 1.0f);
                return;
            }

            // Is the user allowed to perform this action?
            if (currentForm.IsBDA() && !isDemon) {
                player.sendSystemMessage(Component.literal("You must be a demon to use Blood Demon Arts.").withStyle(ChatFormatting.RED));
                player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                return;
            }
            if (!currentForm.IsBDA() && !isHoldingNichirin) {
                player.sendSystemMessage(Component.literal("You must hold a Nichirin Sword to use Breathing Styles.").withStyle(ChatFormatting.RED));
                player.level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
                return;
            }

            // Has the user unlocked this form?
            player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                boolean isUnlocked = progression.getStyleMastery(currentForm.getStyle()) >= currentForm.getStyleMasteryToUnlock();
                if (!isUnlocked) {
                    player.sendSystemMessage(Component.literal("This form hasn't been unlocked yet.").withStyle(ChatFormatting.RED));
                    return;
                }

                // Handle Action & Resource Cost
                if (currentForm.IsBDA()) {
                    // DEMON LOGIC (No Breath Cost)
                    currentForm.performAction(player, level);
                    PlayerProgressionProvider.grantMasteryOnUse(player, currentForm);
                    ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(player.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), player);
                } else {
                    // SLAYER LOGIC (Has Breath Cost)
                    player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                        if (breath.getBreath() >= currentForm.getBreathCost()) {
                            breath.subBreath(currentForm.getBreathCost());
                            currentForm.performAction(player, level);
                            PlayerProgressionProvider.grantMasteryOnUse(player, currentForm);
                            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), player);
                            ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(player.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), player);
                        } else {
                            player.sendSystemMessage(Component.literal("You do not have enough breath. Requires: " + currentForm.getBreathCost())
                                    .withStyle(ChatFormatting.DARK_AQUA));
                            level.playSound(null, player.getOnPos(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                    });
                }
            });
        });
        return true;
    }
}
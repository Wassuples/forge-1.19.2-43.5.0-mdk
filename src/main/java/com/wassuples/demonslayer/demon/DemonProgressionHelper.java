package com.wassuples.demonslayer.demon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayerDemonSyncS2CPacket;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DemonProgressionHelper {
    public static final Map<BloodDemonArts, Integer> BDA_MASTERY_GOALS = new ImmutableMap.Builder<BloodDemonArts, Integer>()
            .put(BloodDemonArts.THREADS, 5000)
            .put(BloodDemonArts.CRYO, 5000)
            .put(BloodDemonArts.CRESCENT, 5000)
            .put(BloodDemonArts.DESTRUCTIVE_DEATH, 5000)
            .put(BloodDemonArts.EMOTION, 5000)
            .put(BloodDemonArts.FISH, 5000)
            .put(BloodDemonArts.HAEMO, 5000)
            .put(BloodDemonArts.SASH, 5000)
            .put(BloodDemonArts.SLEEP, 5000)
            .build();

    public static final Map<BloodDemonArts, Integer> BDA_PURCHASE_COSTS  = new ImmutableMap.Builder<BloodDemonArts, Integer>()
            .put(BloodDemonArts.THREADS, 5000)
            .put(BloodDemonArts.CRYO, 5000)
            .put(BloodDemonArts.CRESCENT, 5000)
            .put(BloodDemonArts.DESTRUCTIVE_DEATH, 5000)
            .put(BloodDemonArts.EMOTION, 5000)
            .put(BloodDemonArts.FISH, 5000)
            .put(BloodDemonArts.HAEMO, 5000)
            .put(BloodDemonArts.SASH, 5000)
            .put(BloodDemonArts.SLEEP, 5000)
            .build();

    public static final Map<BloodDemonArts, List<Integer>> BDA_UPGRADE_COSTS = new ImmutableMap.Builder<BloodDemonArts, List<Integer>>()
            .put(BloodDemonArts.THREADS, ImmutableList.of(1000, 2500, 5000, 10000)) // Costs for Tiers 1-4
            .put(BloodDemonArts.CRYO, ImmutableList.of(1000, 2500, 5000, 10000))
            .put(BloodDemonArts.CRESCENT, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.DESTRUCTIVE_DEATH, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.EMOTION, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.FISH, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.HAEMO, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.SASH, ImmutableList.of(1200, 2800, 5500, 11000))
            .put(BloodDemonArts.SLEEP, ImmutableList.of(1200, 2800, 5500, 11000))
            .build();

    public static final Map<DemonRank, Integer> RANK_UP_COSTS = new ImmutableMap.Builder<DemonRank, Integer>()
            .put(DemonRank.FODDER, 500)
            .put(DemonRank.LOWLY_DEMON, 1200)
            .put(DemonRank.DEMON, 3000)
            .put(DemonRank.GREATER_DEMON, 7500)
            .put(DemonRank.ELITE_DEMON, 15000)
            .put(DemonRank.LOWER_MOON_6, 25000)
            .put(DemonRank.LOWER_MOON_5, 35000)
            .put(DemonRank.LOWER_MOON_4, 50000)
            .put(DemonRank.LOWER_MOON_3, 70000)
            .put(DemonRank.LOWER_MOON_2, 90000)
            .put(DemonRank.LOWER_MOON_1, 120000)
            .put(DemonRank.UPPER_MOON_6, 200000)
            .put(DemonRank.UPPER_MOON_5, 300000)
            .put(DemonRank.UPPER_MOON_4, 450000)
            .put(DemonRank.UPPER_MOON_3, 600000)
            .put(DemonRank.UPPER_MOON_2, 800000)
            .build();

    public static final Map<DemonPassive, List<Integer>> PASSIVE_SKILL_COSTS = new ImmutableMap.Builder<DemonPassive, List<Integer>>()
            .put(DemonPassive.HYPER_REGENERATION, ImmutableList.of(500, 1500, 3000, 6000, 12000))
            .put(DemonPassive.TOUGHENED_HIDE, ImmutableList.of(400, 1200, 2500, 5000, 10000))
            .put(DemonPassive.DEMONIC_STRENGTH, ImmutableList.of(750, 2000, 4500, 8000, 15000))
            .put(DemonPassive.ENHANCED_VIGOR, ImmutableList.of(300, 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500, 7000))
            .put(DemonPassive.SUNLIGHT_RESISTANCE, ImmutableList.of(50000, 150000, 500000))
            .put(DemonPassive.STALKERS_GAIT, ImmutableList.of(2000)) // Single tier unlock
            .put(DemonPassive.BLOOD_ART_POTENCY, ImmutableList.of(1500, 3000, 6000, 10000, 18000)) // -2% cost, +1% damage per tier
            .put(DemonPassive.OVERWHELMING_AURA, ImmutableList.of(4000, 8000, 15000)) // Reduces stare time and increases duration
            .build();

    public static float getBdaCostModifier(PlayerDemon demon) {
        if (demon.isPassiveActive(DemonPassive.BLOOD_ART_POTENCY)) {
            // 5% cost reduction per tier
            float discount = demon.getPassiveTier(DemonPassive.BLOOD_ART_POTENCY) * 0.05f;
            return 1.0f - discount;
        }
        return 1.0f;
    }
    public static float getBdaDamageModifier(PlayerDemon demon) {
        if (demon.isPassiveActive(DemonPassive.BLOOD_ART_POTENCY)) {
            // 1% damage bonus per tier
            float bonus = demon.getPassiveTier(DemonPassive.BLOOD_ART_POTENCY) * 0.01f;
            return 1.0f + bonus;
        }
        return 1.0f;
    }
    public static int getModifiedBdaPurchaseCost(PlayerDemon demon, BloodDemonArts bda) {
        int baseCost = BDA_PURCHASE_COSTS.getOrDefault(bda, -1);
        if (baseCost == -1) return -1;
        return (int) (baseCost * getBdaCostModifier(demon));
    }
    public static int getModifiedBdaUpgradeCost(PlayerDemon demon, BloodDemonArts bda) {
        List<Integer> costs = BDA_UPGRADE_COSTS.get(bda);
        int currentTier = demon.getBdaTier(bda);
        if (costs == null || currentTier >= costs.size()) return -1;
        int baseCost = costs.get(currentTier);
        return (int) (baseCost * getBdaCostModifier(demon));
    }

    public static void tryRankUp(ServerPlayer player, PlayerDemon demon) {
        DemonRank currentRank = demon.getRank();
        Optional<DemonRank> nextRankOpt = getNextRank(currentRank);

        // Check if there is a next rank
        if (nextRankOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("You are already at the maximum rank.").withStyle(ChatFormatting.GOLD));
            return;
        }
        DemonRank nextRank = nextRankOpt.get();

        // Check if the cost is defined for the current rank
        Integer cost = RANK_UP_COSTS.get(currentRank);
        if (cost == null) {
            player.sendSystemMessage(Component.literal("Cannot rank up further from this point.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Check if the player has enough blood points
        if (demon.getBloodPoints() >= cost) {
            // Deduct cost and apply new rank
            demon.setBloodPoints(demon.getBloodPoints() - cost);
            demon.setRank(nextRank);

            // Give feedback to the player
            player.sendSystemMessage(Component.literal("You have ascended to the rank of ").append(nextRank.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.0f);

            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(demon), player);
        } else {
            // Not enough points
            player.sendSystemMessage(Component.literal("You do not have enough Blood Points to ascend.").withStyle(ChatFormatting.RED));
        }
    }

    public static void tryUnlockPassive(ServerPlayer player, PlayerDemon demon, DemonPassive passive) {
        int currentTier = demon.getPassiveTier(passive);
        int maxTier = passive.getMaxTier();

        // Check if the passive is already maxed out
        if (currentTier >= maxTier) {
            player.sendSystemMessage(Component.literal("This passive is already at the maximum tier.").withStyle(ChatFormatting.GOLD));
            return;
        }

        // Get the list of costs for this passive
        List<Integer> costs = PASSIVE_SKILL_COSTS.get(passive);
        if (costs == null || currentTier >= costs.size()) {
            // This is a data error, cost is not defined for the next tier
            return;
        }
        int cost = costs.get(currentTier);

        // Check if the player has enough blood points
        if (demon.getBloodPoints() >= cost) {
            // Deduct cost and apply the next tier
            demon.setBloodPoints(demon.getBloodPoints() - cost);
            demon.setPassiveTier(passive, currentTier + 1);

            // Give feedback to the player
            player.sendSystemMessage(Component.literal("Upgraded " + passive.getDisplayName() + " to Tier " + (currentTier + 1)).withStyle(ChatFormatting.GREEN));
            player.level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75f, 1.2f);

            // NOTE: You must sync the player's capability data to the client after changing it.
            // Example: ModMessages.sendToPlayer(new SyncDemonDataS2CPacket(demon), player);
        } else {
            // Not enough points
            player.sendSystemMessage(Component.literal("Not enough Blood Points to upgrade this passive.").withStyle(ChatFormatting.RED));
        }
    }

    public static void tryPurchaseBda(ServerPlayer player, PlayerDemon demon, BloodDemonArts bda) {
        if (demon.isBdaUnlocked(bda)) return;
        int finalCost = getModifiedBdaPurchaseCost(demon, bda); // Use modified cost
        if (finalCost == -1) return;

        if (demon.getBloodPoints() >= finalCost) {
            demon.setBloodPoints(demon.getBloodPoints() - finalCost);
            demon.unlockBda(bda);
            player.sendSystemMessage(Component.literal("Unlocked Blood Demon Art: ").append(Component.literal(bda.getDisplayName()).withStyle(ChatFormatting.GOLD)));
            player.level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(demon), player);
        } else {
            player.sendSystemMessage(Component.literal("Not enough Blood Points.").withStyle(ChatFormatting.RED));
        }
    }

    public static Optional<DemonRank> getNextRank(DemonRank currentRank) {
        if (currentRank.ordinal() < DemonRank.values().length - 1) {
            return Optional.of(DemonRank.values()[currentRank.ordinal() + 1]);
        }
        return Optional.empty();
    }

    public static List<Component> getRankUpStatBonus(DemonRank nextRank) {
        // TODO: We need to customize these bonuses per rank
        return switch (nextRank) {
            case LOWER_MOON_6 -> List.of(Component.literal("+15% Health"), Component.literal("+10% Damage"));
            case UPPER_MOON_6 -> List.of(Component.literal("+25% Health"), Component.literal("+15% Damage"), Component.literal("+5% Speed"));
            default -> List.of(Component.literal("+10% Health"), Component.literal("+5% Damage"));
        };
    }

    public static void tryUpgradeBda(ServerPlayer player, PlayerDemon demon, BloodDemonArts bda) {
        if (bda == BloodDemonArts.NONE) return;
        int finalCost = getModifiedBdaUpgradeCost(demon, bda);
        if (finalCost == -1) {
            player.sendSystemMessage(Component.literal("This Blood Demon Art is at its maximum tier.").withStyle(ChatFormatting.GOLD));
            return;
        }

        if (demon.getBloodPoints() >= finalCost) {
            demon.setBloodPoints(demon.getBloodPoints() - finalCost);
            demon.setBdaTier(bda, demon.getBdaTier(bda) + 1);
            player.sendSystemMessage(Component.literal("Upgraded " + bda.getDisplayName() + " to Tier " + demon.getBdaTier(bda)).withStyle(ChatFormatting.GREEN));
            player.level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75f, 1.2f);
            ModMessages.sendToPlayer(new PlayerDemonSyncS2CPacket(demon), player);
        } else {
            player.sendSystemMessage(Component.literal("Not enough Blood Points to upgrade.").withStyle(ChatFormatting.RED));
        }
    }

    public static float getBonusHealth(PlayerDemon demon) {
        float health = 0;
        if (demon.isPassiveActive(DemonPassive.ENHANCED_VIGOR)) {
            health += demon.getPassiveTier(DemonPassive.ENHANCED_VIGOR) * 2.0f; // +2 HP per tier
        }
        // You could also add a base health bonus per rank here
        return health;
    }

    public static float getBonusDamage(PlayerDemon demon) {
        float damageMultiplier = 0;
        if (demon.isPassiveActive(DemonPassive.DEMONIC_STRENGTH)) {
            damageMultiplier += demon.getPassiveTier(DemonPassive.DEMONIC_STRENGTH) * 0.05f; // +5% damage per tier
        }
        return damageMultiplier;
    }

    public static float getDamageResistance(PlayerDemon demon) {
        float resistance = 0;
        if (demon.isPassiveActive(DemonPassive.TOUGHENED_HIDE)) {
            resistance += demon.getPassiveTier(DemonPassive.TOUGHENED_HIDE) * 0.04f; // +4% resistance per tier
        }
        return resistance;
    }

    public static float getBonusRegen(PlayerDemon demon) {
        float regen = 0;
        if (demon.isPassiveActive(DemonPassive.HYPER_REGENERATION)) {
            regen += demon.getPassiveTier(DemonPassive.HYPER_REGENERATION) * 0.1f; // +0.1 HP/sec per tier
        }
        return regen;
    }

}
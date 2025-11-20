package com.wassuples.demonslayer.nemesis;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID)
public class NemesisSystemEvents {
    private static final Map<UUID, Float> lastPlayerDamageFromNemesis = new ConcurrentHashMap<>();
    private static int dailyTickCounter = 0;

    @SubscribeEvent public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level;
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        // --- Blood Battle Initiation & Interference Tracking ---
        Entity attacker = event.getSource().getEntity();
        Entity target = event.getEntity();

        if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof NemesisDemonEntity) {
            lastPlayerDamageFromNemesis.put(player.getUUID(), event.getAmount());
        }

        if (attacker instanceof Player && target instanceof NemesisDemonEntity) {
            attacker.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demonPlayer -> {
                if (demonPlayer.isDemon()) {
                    target.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(nemesisTrait -> {
                        // Check if a Demon Player is challenging a higher-rank demon
                        if (demonPlayer.getBloodPoints() < nemesisTrait.getBloodPoints()) {
                            manager.startBloodBattle(attacker.getUUID(), target.getUUID());
                        }
                    });
                }
            });
        }

        // Track interference
        BloodBattle activeBattle = manager.getActiveBattle();
        if (activeBattle != null && target.getUUID().equals(activeBattle.defender)) {
            if (!attacker.getUUID().equals(activeBattle.challenger)) {
                manager.recordInterference(target.getUUID(), attacker.getUUID());
            }
        }
    }

    @SubscribeEvent public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level;
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);
        Entity victim = event.getEntity();
        Entity killer = event.getSource().getEntity();

        // DEMON KILLS PLAYER
        if (killer instanceof NemesisDemonEntity nemesisDemon && victim instanceof Player player) {
            float damageAmount = lastPlayerDamageFromNemesis.getOrDefault(player.getUUID(), 0.0f);
            lastPlayerDamageFromNemesis.remove(player.getUUID());
            manager.demonKillsPlayer(nemesisDemon, player, event.getSource(), damageAmount);
        }

        // PLAYER KILLS DEMON
        if (victim instanceof NemesisDemonEntity nemesisDemon) {
            Optional<DemonRank> kizukiRank = manager.getKizukiRank(nemesisDemon.getUUID());
            BloodBattle battle = manager.getActiveBattle();
            if (battle != null && battle.defender.equals(nemesisDemon.getUUID())) {
                handleBloodBattleConclusion(battle, manager, level);
            } else {
                manager.killDemon(nemesisDemon.getUUID());
            }

            kizukiRank.ifPresent(manager::handlePowerVacuum);
        }
    }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            dailyTickCounter++;
            if (dailyTickCounter >= 24000) { // 24000 ticks = 20 minutes = 1 day
                dailyTickCounter = 0;
                ServerLevel overworld = event.getServer().overworld();
                DemonHierarchyManager.get(overworld).runDailySimulation(overworld);
            }
        }
    }

    @SubscribeEvent public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 40 == 0) {
            if (player.level instanceof ServerLevel level) {
                DemonHierarchyManager.get(level).trySpawnHunterForPlayer(player);
            }
        }
    }

    public static void handleBloodBattleConclusion(BloodBattle battle, DemonHierarchyManager manager, ServerLevel level) {
        ServerPlayer challenger = level.getServer().getPlayerList().getPlayer(battle.challenger);
        if (challenger == null) {
            manager.endBloodBattle();
            return;
        }

        challenger.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demonPlayer -> {
            DemonTrait defenderTrait = manager.getDemonTrait(battle.defender);
            if (defenderTrait == null) return;

            Optional<DemonRank> defenderRankOpt = manager.getKizukiRank(battle.defender);

            if (battle.interferers.isEmpty()) {
                // PURE VICTORY
                challenger.sendSystemMessage(Component.literal("A pure victory! You have devoured their essence and claimed their power.").withStyle(ChatFormatting.LIGHT_PURPLE));
                demonPlayer.addBloodPoints(defenderTrait.getBloodPoints());
                demonPlayer.setDishonored(false); // A pure victory cleanses dishonor

                if (defenderRankOpt.isPresent()) {
                    DemonRank rankToClaim = defenderRankOpt.get();
                    demonPlayer.setRank(rankToClaim);
                    manager.setKizukiRank(rankToClaim, challenger.getUUID());
                    challenger.sendSystemMessage(Component.literal("You have ascended to the rank of ").append(rankToClaim.getDisplayName()));
                }
            } else {
                // TAINTED VICTORY
                challenger.sendSystemMessage(Component.literal("Your victory was tainted by outside help. You have the rank, but not the respect.").withStyle(ChatFormatting.RED));
                demonPlayer.addBloodPoints(defenderTrait.getBloodPoints() / 2);
                demonPlayer.setDishonored(true);

                if (defenderRankOpt.isPresent()) {
                    DemonRank rankToClaim = defenderRankOpt.get();
                    demonPlayer.setRank(rankToClaim);
                    manager.setKizukiRank(rankToClaim, challenger.getUUID());
                    challenger.sendSystemMessage(Component.literal("You have stolen the rank of ").append(rankToClaim.getDisplayName()));
                }
            }
        });

        manager.killDemon(battle.defender);
        manager.endBloodBattle();
    }

}
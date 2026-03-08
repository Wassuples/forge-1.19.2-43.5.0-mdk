package com.wassuples.demonslayer.nemesis;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.battle.BattleSessionTracker;
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


/**
 * Forge event handler for the Nemesis system lifecycle.
 *
 * <p>Listens for combat and world-tick events to drive the dynamic demon hierarchy:
 * <ul>
 *   <li><b>Nemesis death</b> — triggers demon promotion/replacement in the hierarchy</li>
 *   <li><b>Player escape</b> — nemesis demons grow stronger on failed encounters</li>
 *   <li><b>Daily tick</b> — periodic hierarchy maintenance (fill vacant slots, apply growth)</li>
 *   <li><b>Battle tracking</b> — records damage dealt/taken for nemesis relationship building</li>
 * </ul>
 *
 * @see DemonHierarchyManager
 * @see com.wassuples.demonslayer.nemesis.battle.BattleSessionTracker
 */
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

        // --- Battle Session Tracking (D2) ---
        BattleSessionTracker tracker = manager.getBattleSessionTracker();
        long currentTime = level.getGameTime();

        // Demon attacks player
        if (attacker instanceof NemesisDemonEntity demon && target instanceof Player) {
            float demonHpPct = demon.getHealth() / Math.max(1f, demon.getMaxHealth());
            float opponentHpPct = target.isAlive()
                    ? ((Player) target).getHealth() / Math.max(1f, ((Player) target).getMaxHealth())
                    : 0f;
            tracker.recordDamage(demon.getUUID(), target.getUUID(), event.getAmount(),
                    true, demonHpPct, opponentHpPct, true, currentTime);
        }

        // Player attacks demon
        if (attacker instanceof Player && target instanceof NemesisDemonEntity demon) {
            float demonHpPct = demon.isAlive()
                    ? demon.getHealth() / Math.max(1f, demon.getMaxHealth())
                    : 0f;
            float opponentHpPct = ((Player) attacker).getHealth() / Math.max(1f, ((Player) attacker).getMaxHealth());
            tracker.recordDamage(demon.getUUID(), attacker.getUUID(), event.getAmount(),
                    false, demonHpPct, opponentHpPct, true, currentTime);
        }
    }

    @SubscribeEvent public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level;
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);
        Entity victim = event.getEntity();
        Entity killer = event.getSource().getEntity();
        BattleSessionTracker tracker = manager.getBattleSessionTracker();
        long currentTime = level.getGameTime();

        // DEMON KILLS PLAYER
        if (killer instanceof NemesisDemonEntity nemesisDemon && victim instanceof Player player) {
            tracker.recordDeath(nemesisDemon.getUUID(), player.getUUID(), false, currentTime);

            float damageAmount = lastPlayerDamageFromNemesis.getOrDefault(player.getUUID(), 0.0f);
            lastPlayerDamageFromNemesis.remove(player.getUUID());
            manager.demonKillsPlayer(nemesisDemon, player, event.getSource(), damageAmount);
        }

        // PLAYER KILLS DEMON
        if (victim instanceof NemesisDemonEntity nemesisDemon) {
            if (killer != null) {
                tracker.recordDeath(nemesisDemon.getUUID(), killer.getUUID(), true, currentTime);
            }

            // Check if this is part of a demon-vs-demon world battle (J1)
            if (manager.isInWorldBattle(nemesisDemon.getUUID()) && killer instanceof NemesisDemonEntity killerDemon) {
                manager.handleWorldBattleConclusion(nemesisDemon.getUUID(), killerDemon.getUUID(), level);
                return; // world battle handles kill + power vacuum internally
            }

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

            // Tick battle session tracker every 20 ticks (~1 second)
            if (dailyTickCounter % 20 == 0) {
                ServerLevel overworld = event.getServer().overworld();
                long currentTime = overworld.getGameTime();
                DemonHierarchyManager.get(overworld).getBattleSessionTracker().tick(currentTime);
            }

            // Try to spawn a demon-vs-demon world battle every ~5 minutes (J1)
            if (dailyTickCounter % 6000 == 0 && dailyTickCounter > 0) {
                ServerLevel overworld = event.getServer().overworld();
                DemonHierarchyManager manager = DemonHierarchyManager.get(overworld);
                if (new java.util.Random().nextFloat() < 0.15f) { // 15% chance
                    manager.trySpawnWorldBattle(overworld);
                }
            }

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

        long time = level.getGameTime();

        challenger.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demonPlayer -> {
            DemonTrait defenderTrait = manager.getDemonTrait(battle.defender);
            if (defenderTrait == null) return;

            Optional<DemonRank> defenderRankOpt = manager.getKizukiRank(battle.defender);
            boolean isTainted = !battle.interferers.isEmpty();

            // Write fact memories on the defeated demon (F1) — before it gets killed
            defenderTrait.addMemory(com.wassuples.demonslayer.nemesis.trait.DemonMemory.of(
                    com.wassuples.demonslayer.nemesis.trait.MemoryType.LOST_BADLY, time,
                    challenger.getUUID(), battle.defender));
            if (isTainted) {
                defenderTrait.addMemory(com.wassuples.demonslayer.nemesis.trait.DemonMemory.of(
                        com.wassuples.demonslayer.nemesis.trait.MemoryType.HUMILIATED, time,
                        challenger.getUUID(), battle.defender));
            }

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

        // Log story event (H1)
        DemonTrait defTrait = manager.getDemonTrait(battle.defender);
        if (defTrait != null) {
            manager.logStoryEvent(com.wassuples.demonslayer.nemesis.story.StoryEvent.demonKilled(
                    time, battle.defender, defTrait.getName(),
                    challenger.getUUID(), challenger.getGameProfile().getName()));
        }

        manager.killDemon(battle.defender);
        manager.endBloodBattle();
    }

}

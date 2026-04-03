package com.wassuples.demonslayer.nemesis;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.battle.BattleSessionTracker;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.IDemonTrait;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
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
    private static final String DEATH_DEFIANT_FLAG = "nemesis_death_defiant_used";
    private static final Map<UUID, Float> lastPlayerDamageFromNemesis = new ConcurrentHashMap<>();
    private static int dailyTickCounter = 0;

    @SubscribeEvent public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level;
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        // --- Blood Battle Initiation & Interference Tracking ---
        Entity attacker = event.getSource().getEntity();
        Entity target = event.getEntity();

        // Apply quirk effects before battle/session bookkeeping.
        if (attacker instanceof NemesisDemonEntity attackingDemon) {
            IDemonTrait trait = getTrait(attackingDemon);
            if (trait != null) {
                event.setAmount(applyOutgoingQuirkEffects(event.getAmount(), attackingDemon, trait, target, manager, level));
            }
        }
        if (target instanceof NemesisDemonEntity defendingDemon) {
            IDemonTrait trait = getTrait(defendingDemon);
            if (trait != null) {
                event.setAmount(applyIncomingQuirkEffects(event.getAmount(), defendingDemon, trait, attacker, event, manager, level));

                if (attacker instanceof Player player && (trait.hasQuirk(Quirk.OBSESSIVE_HUNTER) || trait.hasQuirk(Quirk.EASILY_TAUNTED))) {
                    defendingDemon.setTarget(player);
                }
            }
        }

        if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof NemesisDemonEntity demon) {
            lastPlayerDamageFromNemesis.put(player.getUUID(), event.getAmount());
            System.out.println("[NEMESIS DEBUG] Player " + player.getGameProfile().getName() +
                " damaged by " + demon.getDisplayName().getString() + " for " + event.getAmount() + " damage");
        }

        if (attacker instanceof Player && target instanceof NemesisDemonEntity) {
            attacker.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demonPlayer -> {
                if (demonPlayer.isDemon()) {
                    target.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(nemesisTrait -> {
                        // Check if a Demon Player is challenging a higher-rank demon
                        if (demonPlayer.getBloodPoints() < nemesisTrait.getBloodPoints()) {
                            System.out.println("[NEMESIS DEBUG] Blood Battle auto-initiated: " +
                                ((Player) attacker).getGameProfile().getName() + " (BP:" + demonPlayer.getBloodPoints() +
                                ") vs " + nemesisTrait.getName() + " (BP:" + nemesisTrait.getBloodPoints() + ")");
                            manager.startBloodBattle(attacker.getUUID(), target.getUUID());
                        } else {
                            System.out.println("[NEMESIS DEBUG] Attack by demon player " +
                                ((Player) attacker).getGameProfile().getName() + " (BP:" + demonPlayer.getBloodPoints() +
                                ") on " + nemesisTrait.getName() + " (BP:" + nemesisTrait.getBloodPoints() + ") - No Blood Battle (insufficient BP difference)");
                        }
                    });
                }
            });
        }

        // Track interference
        BloodBattle activeBattle = manager.getActiveBattle();
        if (activeBattle != null && attacker != null && target.getUUID().equals(activeBattle.defender)) {
            if (!attacker.getUUID().equals(activeBattle.challenger)) {
                System.out.println("[NEMESIS DEBUG] Blood Battle interference detected: " +
                    (attacker instanceof Player ? ((Player) attacker).getGameProfile().getName() : attacker.getDisplayName().getString()) +
                    " interfered in the battle");
                manager.recordInterference(target.getUUID(), attacker.getUUID());
            }
        }

        // Reactive alliances: demons can call compatible allies when engaged.
        if (target instanceof NemesisDemonEntity defender
                && (attacker instanceof Player || attacker instanceof NemesisDemonEntity)) {
            manager.tryMobilizeAlliesForFight(
                    defender.getUUID(),
                    attacker.getUUID(),
                    attacker instanceof Player,
                    level,
                    defender.blockPosition(),
                    2
            );
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
            System.out.println("[NEMESIS DEBUG] Battle session: " + demon.getDisplayName().getString() +
                " dealt " + event.getAmount() + " damage to " + ((Player) target).getGameProfile().getName() +
                " (Demon HP: " + String.format("%.1f%%", demonHpPct * 100) +
                ", Player HP: " + String.format("%.1f%%", opponentHpPct * 100) + ")");
            tracker.recordDamage(demon.getUUID(), target.getUUID(), event.getAmount(),
                    true, demonHpPct, opponentHpPct, true, currentTime);
        }

        // Player attacks demon
        if (attacker instanceof Player && target instanceof NemesisDemonEntity demon) {
            float demonHpPct = demon.isAlive()
                    ? demon.getHealth() / Math.max(1f, demon.getMaxHealth())
                    : 0f;
            float opponentHpPct = ((Player) attacker).getHealth() / Math.max(1f, ((Player) attacker).getMaxHealth());
            System.out.println("[NEMESIS DEBUG] Battle session: " + ((Player) attacker).getGameProfile().getName() +
                " dealt " + event.getAmount() + " damage to " + demon.getDisplayName().getString() +
                " (Player HP: " + String.format("%.1f%%", opponentHpPct * 100) +
                ", Demon HP: " + String.format("%.1f%%", demonHpPct * 100) + ")");
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
            System.out.println("[NEMESIS DEBUG] DEMON KILLED PLAYER: " +
                nemesisDemon.getDisplayName().getString() + " killed " + player.getGameProfile().getName());

            tracker.recordDeath(nemesisDemon.getUUID(), player.getUUID(), false, currentTime);

            float damageAmount = lastPlayerDamageFromNemesis.getOrDefault(player.getUUID(), 0.0f);
            lastPlayerDamageFromNemesis.remove(player.getUUID());

            System.out.println("[NEMESIS DEBUG] Processing demon kill rewards for " + nemesisDemon.getDisplayName().getString() +
                " (damage dealt: " + damageAmount + ")");
            manager.demonKillsPlayer(nemesisDemon, player, event.getSource(), damageAmount);
        }

        // PLAYER KILLS DEMON
        if (victim instanceof NemesisDemonEntity nemesisDemon) {
            String killerName = killer != null ?
                (killer instanceof Player ? ((Player) killer).getGameProfile().getName() : killer.getDisplayName().getString())
                : "Unknown";

            System.out.println("[NEMESIS DEBUG] NEMESIS DEMON KILLED: " + nemesisDemon.getDisplayName().getString() +
                " killed by " + killerName);

            if (killer != null) {
                tracker.recordDeath(nemesisDemon.getUUID(), killer.getUUID(), true, currentTime);
            }

            // Check if this is part of a demon-vs-demon world battle (J1)
            if (manager.isInWorldBattle(nemesisDemon.getUUID()) && killer instanceof NemesisDemonEntity killerDemon) {
                System.out.println("[NEMESIS DEBUG] World Battle concluded: " +
                    killerDemon.getDisplayName().getString() + " defeated " + nemesisDemon.getDisplayName().getString());
                manager.handleWorldBattleConclusion(nemesisDemon.getUUID(), killerDemon.getUUID(), level);
                return; // world battle handles kill + power vacuum internally
            }

            Optional<DemonRank> kizukiRank = manager.getKizukiRank(nemesisDemon.getUUID());
            BloodBattle battle = manager.getActiveBattle();
            if (battle != null && battle.defender.equals(nemesisDemon.getUUID())) {
                System.out.println("[NEMESIS DEBUG] Blood Battle concluded: Defender " +
                    nemesisDemon.getDisplayName().getString() + " killed" +
                    (battle.interferers.isEmpty() ? " (Pure Victory)" : " (Tainted Victory - " + battle.interferers.size() + " interferers)"));
                // Blood battle: player takes the rank, no power vacuum needed
                handleBloodBattleConclusion(battle, manager, level);
            } else {
                System.out.println("[NEMESIS DEBUG] Regular demon death: " + nemesisDemon.getDisplayName().getString() +
                    (kizukiRank.isPresent() ? " (Rank: " + kizukiRank.get().getDisplayName().getString() + ")" : " (Fodder)"));
                manager.killDemon(nemesisDemon.getUUID());
                // Only trigger power vacuum if the demon was Kizuki and no one took their rank
                if (kizukiRank.isPresent()) {
                    System.out.println("[NEMESIS DEBUG] Triggering power vacuum for fallen " + kizukiRank.get().getDisplayName().getString());
                    manager.handlePowerVacuum(kizukiRank.get());
                }
            }
        }
    }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            dailyTickCounter++;

            // Tick battle session tracker every 20 ticks (~1 second)
            if (dailyTickCounter % 20 == 0) {
                ServerLevel overworld = event.getServer().overworld();
                long currentTime = overworld.getGameTime();
                DemonHierarchyManager manager = DemonHierarchyManager.get(overworld);
                manager.getBattleSessionTracker().tick(currentTime);
                manager.tickSocialAlliances(currentTime);
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
            System.out.println("[NEMESIS DEBUG] Blood Battle conclusion aborted - challenger player not found");
            manager.endBloodBattle();
            return;
        }

        long time = level.getGameTime();

        challenger.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demonPlayer -> {
            DemonTrait defenderTrait = manager.getDemonTrait(battle.defender);
            if (defenderTrait == null) {
                System.out.println("[NEMESIS DEBUG] Blood Battle conclusion aborted - defender trait not found");
                return;
            }

            Optional<DemonRank> defenderRankOpt = manager.getKizukiRank(battle.defender);
            boolean isTainted = !battle.interferers.isEmpty();

            System.out.println("[NEMESIS DEBUG] ═══ BLOOD BATTLE CONCLUDED ═══");
            System.out.println("[NEMESIS DEBUG] Challenger: " + challenger.getGameProfile().getName() + " (BP: " + demonPlayer.getBloodPoints() + ")");
            System.out.println("[NEMESIS DEBUG] Defender: " + defenderTrait.getName() + " (BP: " + defenderTrait.getBloodPoints() +
                (defenderRankOpt.isPresent() ? ", Rank: " + defenderRankOpt.get().getDisplayName().getString() : ", Rank: Fodder") + ")");
            System.out.println("[NEMESIS DEBUG] Interferers: " + battle.interferers.size() +
                (isTainted ? " (TAINTED VICTORY)" : " (PURE VICTORY)"));

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
                System.out.println("[NEMESIS DEBUG] Processing pure victory rewards: " + defenderTrait.getBloodPoints() + " BP, dishonor cleansed");
                challenger.sendSystemMessage(Component.literal("A pure victory! You have devoured their essence and claimed their power.").withStyle(ChatFormatting.LIGHT_PURPLE));
                demonPlayer.addBloodPoints(defenderTrait.getBloodPoints());
                demonPlayer.setDishonored(false); // A pure victory cleanses dishonor

                if (defenderRankOpt.isPresent()) {
                    DemonRank rankToClaim = defenderRankOpt.get();
                    System.out.println("[NEMESIS DEBUG] Rank claimed: " + challenger.getGameProfile().getName() + " becomes " + rankToClaim.getDisplayName().getString());
                    demonPlayer.setRank(rankToClaim);
                    manager.setKizukiRank(rankToClaim, challenger.getUUID());
                    challenger.sendSystemMessage(Component.literal("You have ascended to the rank of ").append(rankToClaim.getDisplayName()));
                } else {
                    System.out.println("[NEMESIS DEBUG] Defender was fodder - no rank to claim");
                }
            } else {
                // TAINTED VICTORY
                System.out.println("[NEMESIS DEBUG] Processing tainted victory rewards: " + (defenderTrait.getBloodPoints() / 2) + " BP, marked as dishonored");
                challenger.sendSystemMessage(Component.literal("Your victory was tainted by outside help. You have the rank, but not the respect.").withStyle(ChatFormatting.RED));
                demonPlayer.addBloodPoints(defenderTrait.getBloodPoints() / 2);
                demonPlayer.setDishonored(true);

                if (defenderRankOpt.isPresent()) {
                    DemonRank rankToClaim = defenderRankOpt.get();
                    System.out.println("[NEMESIS DEBUG] Tainted rank claimed: " + challenger.getGameProfile().getName() + " steals " + rankToClaim.getDisplayName().getString());
                    demonPlayer.setRank(rankToClaim);
                    manager.setKizukiRank(rankToClaim, challenger.getUUID());
                    challenger.sendSystemMessage(Component.literal("You have stolen the rank of ").append(rankToClaim.getDisplayName()));
                } else {
                    System.out.println("[NEMESIS DEBUG] Defender was fodder - no rank to steal");
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

    private static IDemonTrait getTrait(NemesisDemonEntity demon) {
        return demon.getCapability(DemonTraitProvider.DEMON_TRAIT).resolve().orElse(null);
    }

    private static float applyOutgoingQuirkEffects(float baseDamage, NemesisDemonEntity demon, IDemonTrait trait,
                                                   Entity target, DemonHierarchyManager manager, ServerLevel level) {
        float amount = baseDamage;
        boolean isNight = level.getDayTime() % 24000L >= 13000L;

        if (trait.hasQuirk(Quirk.HEAVY_HITTER)) amount *= 1.12f;
        if (trait.hasQuirk(Quirk.SHOCKWAVE_STOMP)) amount *= 1.08f;
        if (trait.hasQuirk(Quirk.EXECUTIONER) && target instanceof LivingEntity living
                && living.getHealth() / Math.max(1.0f, living.getMaxHealth()) <= 0.25f) {
            amount *= 1.25f;
        }
        if (trait.hasQuirk(Quirk.NIGHT_STALKER) && isNight) amount *= 1.08f;
        if (trait.hasQuirk(Quirk.GLASS_CANNON)) amount *= 1.15f;
        if (trait.hasQuirk(Quirk.WIND_UP)) amount *= 1.06f;

        if (trait.hasQuirk(Quirk.BLIND_RAGE)) {
            if (demon.getHealth() / Math.max(1.0f, demon.getMaxHealth()) < 0.35f) {
                amount *= 1.12f;
            }
            if (demon.getRandom().nextFloat() < 0.20f) {
                amount *= 0.70f;
            }
        }

        if (trait.hasQuirk(Quirk.PURIFIED_WEAKNESS) && target instanceof Player) {
            amount *= 0.92f;
        }

        if (trait.hasQuirk(Quirk.VENOMOUS) && target instanceof LivingEntity living && demon.getRandom().nextFloat() < 0.30f) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        if (trait.hasQuirk(Quirk.BLOOD_SIPHON) && amount > 0.0f) {
            demon.heal(Math.max(0.6f, amount * 0.12f));
        }
        if (trait.hasQuirk(Quirk.SHOCKWAVE_STOMP) && target instanceof LivingEntity && demon.getRandom().nextFloat() < 0.18f) {
            final float splash = amount * 0.35f;
            level.getEntitiesOfClass(LivingEntity.class, demon.getBoundingBox().inflate(2.5D), e -> e != demon && e.isAlive())
                    .forEach(entity -> entity.hurt(demon.damageSources().mobAttack(demon), splash));
        }
        if (trait.hasQuirk(Quirk.PACK_CALLER) && demon.getRandom().nextFloat() < 0.12f) {
            manager.tryMobilizeAlliesForFight(demon.getUUID(), target.getUUID(), target instanceof Player, level, demon.blockPosition(), 3);
        }

        return Math.max(0.1f, amount);
    }

    private static float applyIncomingQuirkEffects(float baseDamage, NemesisDemonEntity demon, IDemonTrait trait,
                                                   Entity attacker, LivingHurtEvent event,
                                                   DemonHierarchyManager manager, ServerLevel level) {
        float amount = baseDamage;
        boolean isNight = level.getDayTime() % 24000L >= 13000L;

        if (trait.hasQuirk(Quirk.FIRE_RESISTANT) && event.getSource().isFire()) amount *= 0.65f;
        if (trait.hasQuirk(Quirk.FLAMMABLE_FLESH) && event.getSource().isFire()) {
            amount *= 1.35f;
            demon.setRemainingFireTicks(demon.getRemainingFireTicks() + 40);
        }
        if (trait.hasQuirk(Quirk.ARROW_PROOF) && event.getSource().isProjectile()) amount *= 0.58f;
        if (trait.hasQuirk(Quirk.BRITTLE) && attacker instanceof Player player && player.getMainHandItem().getItem() instanceof AxeItem) {
            amount *= 1.25f;
        }
        if (trait.hasQuirk(Quirk.SOUND_SENSITIVE) && event.getSource().isExplosion()) {
            amount *= 1.20f;
            demon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            demon.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
        }
        if (trait.hasQuirk(Quirk.SUNLIGHT_SICKNESS) && !isNight && level.canSeeSky(demon.blockPosition())) {
            amount *= 1.20f;
        }
        if (trait.hasQuirk(Quirk.FEAR_OF_WATER) && demon.isInWaterRainOrBubble()) amount *= 1.12f;
        if (trait.hasQuirk(Quirk.PURIFIED_WEAKNESS) && attacker instanceof Player) amount *= 1.15f;
        if (trait.hasQuirk(Quirk.GLASS_CANNON)) amount *= 1.12f;
        if (trait.hasQuirk(Quirk.EASILY_TAUNTED) && attacker instanceof Player) amount *= 1.05f;
        if (trait.hasQuirk(Quirk.DISHONORED)) amount *= 1.05f;

        if (trait.hasQuirk(Quirk.FROST_BLOODED) && demon.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            demon.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (trait.hasQuirk(Quirk.UNSHAKABLE)) {
            demon.setDeltaMovement(demon.getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));
        }
        if (trait.hasQuirk(Quirk.RAPID_RECOVERY) && demon.getRandom().nextFloat() < 0.30f) {
            demon.removeEffect(MobEffects.WEAKNESS);
            demon.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        if (trait.hasQuirk(Quirk.DEATH_DEFIANT)
                && amount >= demon.getHealth()
                && !demon.getPersistentData().getBoolean(DEATH_DEFIANT_FLAG)) {
            demon.getPersistentData().putBoolean(DEATH_DEFIANT_FLAG, true);
            amount = Math.max(0.0f, demon.getHealth() - 1.0f);
            demon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            demon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        }

        if (trait.hasQuirk(Quirk.PACK_CALLER) && attacker != null && demon.getRandom().nextFloat() < 0.20f) {
            manager.tryMobilizeAlliesForFight(demon.getUUID(), attacker.getUUID(), attacker instanceof Player, level, demon.blockPosition(), 3);
        }

        return Math.max(0.1f, amount);
    }

}

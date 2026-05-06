package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.entity.custom.DemonEntity;
import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.reputation.rank.RankAdvancementChecker;
import com.wassuples.demonslayer.reputation.witness.WitnessScanner;
import com.wassuples.demonslayer.reputation.witness.WitnessType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

public class ReputationEvents {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) {
            return;
        }
        if (!(event.getEntity().level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        ReputationManager manager = ReputationManager.get(serverLevel);
        Set<WitnessType> witnesses = WitnessScanner.scan(serverLevel, victim.blockPosition(), killer);

        if (victim instanceof Villager) {
            ReputationService.queueWitnessedEvent(serverLevel, killer.getUUID(), RepEventType.KILL_VILLAGER_WITNESSED, witnesses);
            return;
        }

        if (victim instanceof HashiraEntity) {
            manager.setHardline(killer.getUUID(), HardlineFlag.DSHQ_HASHIRA_KILL);
            ReputationService.queueWitnessedEvent(serverLevel, killer.getUUID(), RepEventType.KILL_DEMON_SLAYER_WITNESSED, witnesses);
            return;
        }

        if (isDemon(victim)) {
            boolean hasDshqWitness = witnesses.stream().anyMatch(WitnessType::reportsToDshq);
            boolean isLowerMoon = isLowerMoon(victim);
            boolean isUpperMoon = isUpperMoon(victim);

            PlayerRepRecord record = manager.getOrCreateRecord(killer.getUUID());
            if (isUpperMoon || isLowerMoon) {
                // If this is the only opponent the demon currently remembers fighting, it's a solo path.
                boolean isSolo = DemonHierarchyManager.get(serverLevel).getBattleSessionTracker().getOpponentsInBattle(victim.getUUID()).size() <= 1;

                if (isSolo) {
                    record.getKillRecord().recordKizukiSoloKill();
                } else {
                    record.getKillRecord().recordDemonKill();
                }
            } else {
                record.getKillRecord().recordDemonKill();
            }
            RankAdvancementChecker.checkAdvancement(killer, record);

            RepEventType baseEvent;
            if (isUpperMoon) {
                baseEvent = hasDshqWitness ? RepEventType.KILL_UPPER_MOON_WITNESSED : RepEventType.KILL_UPPER_MOON_UNWITNESSED;
            } else if (isLowerMoon) {
                baseEvent = hasDshqWitness ? RepEventType.KILL_LOWER_MOON_WITNESSED : RepEventType.KILL_LOWER_MOON_UNWITNESSED;
            } else {
                baseEvent = hasDshqWitness ? RepEventType.KILL_REGULAR_DEMON_WITNESSED : RepEventType.KILL_REGULAR_DEMON_UNWITNESSED;
            }

            ReputationService.queueWitnessedEvent(serverLevel, killer.getUUID(), baseEvent, witnesses);

            // Dying demon vision always reports to Muzan.
            manager.queueWitnessEvent(killer.getUUID(), baseEvent, WitnessType.DEMON_DYING, serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ServerLevel overworld = event.getServer().overworld();
        ReputationManager.get(overworld).processDailyTick(overworld.getGameTime());
    }

    private static boolean isDemon(LivingEntity victim) {
        return victim instanceof DemonEntity || victim instanceof NemesisDemonEntity || isLowerMoon(victim) || isUpperMoon(victim);
    }

    private static boolean isLowerMoon(LivingEntity victim) {
        String simpleName = victim.getClass().getSimpleName();
        return simpleName.contains("LowerMoon");
    }

    private static boolean isUpperMoon(LivingEntity victim) {
        String simpleName = victim.getClass().getSimpleName();
        if (simpleName.contains("UpperMoon")) {
            return true;
        }
        if (victim instanceof NemesisDemonEntity && victim.level instanceof ServerLevel serverLevel) {
            return DemonHierarchyManager.get(serverLevel).getKizukiRank(victim.getUUID())
                    .map(rank -> rank.name().startsWith("UPPER"))
                    .orElse(false);
        }
        return false;
    }
}

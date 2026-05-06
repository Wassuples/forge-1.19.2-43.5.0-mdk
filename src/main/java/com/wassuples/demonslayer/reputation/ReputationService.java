package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.entity.custom.CrowEntity;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.nemesis.battle.BattleOutcome;
import com.wassuples.demonslayer.reputation.witness.WitnessScanner;
import com.wassuples.demonslayer.reputation.witness.WitnessType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.*;

public final class ReputationService {
    private static final Map<ResourceLocation, List<RepEventType>> MISSION_COMPLETION_EVENTS = buildMissionCompletionMap();
    private static final Map<ResourceLocation, List<RepEventType>> MISSION_ABANDON_EVENTS = buildMissionAbandonMap();

    private ReputationService() {
    }

    public static PlayerRepRecord getRecord(ServerLevel level, UUID playerId) {
        return ReputationManager.get(level).getOrCreateRecord(playerId);
    }

    public static void applyAutoEvent(ServerLevel level, UUID playerId, RepEventType eventType) {
        ReputationManager manager = ReputationManager.get(level);
        manager.applyAutoEvent(playerId, eventType, level.getGameTime() / 24000L);
    }

    public static void queueWitnessedEvent(ServerLevel level, UUID playerId, RepEventType eventType, Set<WitnessType> witnessTypes) {
        ReputationManager manager = ReputationManager.get(level);
        long gameTime = level.getGameTime();
        if (witnessTypes == null || witnessTypes.isEmpty()) {
            manager.applyAutoEvent(playerId, eventType, gameTime / 24000L);
            return;
        }

        for (WitnessType witnessType : witnessTypes) {
            manager.queueWitnessEvent(playerId, eventType, witnessType, gameTime);
        }
    }

    public static void onMissionCompleted(ServerPlayer player, Mission mission) {
        ServerLevel level = player.getLevel();
        applyAutoEvent(level, player.getUUID(), RepEventType.COMPLETE_STANDARD_CORPS_MISSION);

        for (RepEventType eventType : MISSION_COMPLETION_EVENTS.getOrDefault(mission.id(), List.of())) {
            applyAutoEvent(level, player.getUUID(), eventType);
        }

        // These integrations depend on systems not yet implemented in the workspace:
        // - Mission-side ally attribution (Tanjiro present / shared mission participation)
        // - Mission branch outcomes beyond existing mission IDs
    }

    public static void onMissionAbandoned(ServerPlayer player, ResourceLocation missionId) {
        ServerLevel level = player.getLevel();
        applyAutoEvent(level, player.getUUID(), RepEventType.ABANDON_CORPS_MISSION);

        for (RepEventType eventType : MISSION_ABANDON_EVENTS.getOrDefault(missionId, List.of())) {
            applyAutoEvent(level, player.getUUID(), eventType);
        }
    }

    public static void onDemonPathAccepted(ServerPlayer player) {
        applyAutoEvent(player.getLevel(), player.getUUID(), RepEventType.DRINK_MUZAN_BLOOD);
    }

    public static void onCrowDeath(CrowEntity crow, DamageSource source) {
        if (!(crow.level instanceof ServerLevel level)) {
            return;
        }

        UUID ownerId = crow.getOwnerUUID();
        if (ownerId == null) {
            return;
        }

        Entity attacker = source.getEntity();
        Set<WitnessType> witnesses = WitnessScanner.scan(level, crow.blockPosition());
        boolean hasDshqWitness = witnesses.stream().anyMatch(WitnessType::reportsToDshq);

        if (attacker != null && ownerId.equals(attacker.getUUID())) {
            RepEventType eventType = hasDshqWitness
                    ? RepEventType.KILL_CROW_DIRECT_WITNESSED
                    : RepEventType.KILL_CROW_DIRECT_UNWITNESSED;
            queueWitnessedEvent(level, ownerId, eventType, witnesses);
            return;
        }

        // Non-owner deaths are treated as accidental for suspicion purposes.
        applyAutoEvent(level, ownerId, RepEventType.CROW_ACCIDENTAL_DEATH);
    }

    public static void onBloodBattleConclusion(ServerLevel level, UUID challengerId, boolean tainted, boolean defenderWasUpperMoon) {
        applyAutoEvent(level, challengerId, tainted ? RepEventType.CLAIM_KIZUKI_RANK_TAINTED : RepEventType.CLAIM_KIZUKI_RANK);

        if (defenderWasUpperMoon) {
            applyAutoEvent(level, challengerId, tainted
                    ? RepEventType.KILL_UPPER_MOON_TAINTED_BLOOD_BATTLE
                    : RepEventType.KILL_UPPER_MOON_PURE_BLOOD_BATTLE);
        }
    }

    public static void onBattleOutcome(ServerLevel level, BattleOutcome outcome) {
        if (!outcome.opponentIsPlayer()) {
            return;
        }

        UUID playerId = outcome.opponentId();
        if (outcome.isDemonEscape()) {
            applyAutoEvent(level, playerId, RepEventType.DEMON_SURVIVED_ENCOUNTER);
        }

        // Player-flee signals are not yet emitted by the battle tracker in current gameplay flows.
    }

    public static float getDemonNotorietyHunterRelationshipBonus(ServerPlayer player) {
        int demonRep = getRecord(player.getLevel(), player.getUUID()).getDemonRep();

        if (demonRep <= -301) return -0.40f;
        if (demonRep <= -101) return -0.20f;
        if (demonRep <= 99) return 0.00f;
        if (demonRep <= 199) return 0.10f;
        if (demonRep <= 349) return 0.25f;
        if (demonRep <= 499) return 0.50f;
        if (demonRep <= 699) return 0.80f;
        if (demonRep <= 849) return 1.20f;
        return 1.50f;
    }

    private static Map<ResourceLocation, List<RepEventType>> buildMissionCompletionMap() {
        Map<ResourceLocation, List<RepEventType>> map = new HashMap<>();

        // Current mission assets present in workspace.
        map.put(new ResourceLocation("demonslayer", "final_selection_mission0"), List.of());
        map.put(new ResourceLocation("demonslayer", "story_final_selection"),
                List.of());

        // Deferred IDs from REPUTATION_SYSTEM.md will be added as mission content lands.
        return map;
    }

    private static Map<ResourceLocation, List<RepEventType>> buildMissionAbandonMap() {
        return new HashMap<>();
    }
}

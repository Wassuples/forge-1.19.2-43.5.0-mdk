package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.reputation.witness.WitnessType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class RepDeltaCalculator {
    private static final Map<RepEventType, RepDelta> BASE_DELTAS = buildBaseDeltas();

    private RepDeltaCalculator() {
    }

    public static RepDelta forEvent(RepEventType eventType) {
        return BASE_DELTAS.getOrDefault(eventType, RepDelta.ZERO);
    }

    public static RepDelta forWitnessedEvent(RepEventType eventType, Set<WitnessType> witnessTypes) {
        RepDelta base = forEvent(eventType);
        if (witnessTypes == null || witnessTypes.isEmpty()) {
            return base.withFactionOnly(Faction.MUZAN).add(base.withFactionOnly(Faction.DEMON_WORLD));
        }

        RepDelta out = RepDelta.ZERO;
        if (witnessTypes.stream().anyMatch(WitnessType::reportsToDshq)) {
            out = out.add(base.withFactionOnly(Faction.DSHQ));
        }
        if (witnessTypes.stream().anyMatch(WitnessType::reportsToTanjiro)) {
            out = out.add(base.withFactionOnly(Faction.TANJIRO_CREW));
        }
        if (witnessTypes.stream().anyMatch(WitnessType::reportsToMuzan)) {
            out = out.add(base.withFactionOnly(Faction.MUZAN));
        }
        if (witnessTypes.stream().anyMatch(WitnessType::reportsToDemonWorld)) {
            out = out.add(base.withFactionOnly(Faction.DEMON_WORLD));
        }
        return out;
    }

    public static Map<RepEventType, RepDelta> immutableTable() {
        return BASE_DELTAS;
    }

    private static Map<RepEventType, RepDelta> buildBaseDeltas() {
        EnumMap<RepEventType, RepDelta> map = new EnumMap<>(RepEventType.class);

        map.put(RepEventType.KILL_REGULAR_DEMON_WITNESSED, new RepDelta(12, 0, -4, 6, 0));
        map.put(RepEventType.KILL_REGULAR_DEMON_UNWITNESSED, new RepDelta(0, 0, -4, 6, 0));
        map.put(RepEventType.KILL_LOWER_MOON_WITNESSED, new RepDelta(30, 0, -15, 25, 0));
        map.put(RepEventType.KILL_LOWER_MOON_UNWITNESSED, new RepDelta(0, 0, -15, 25, 0));
        map.put(RepEventType.KILL_UPPER_MOON_WITNESSED, new RepDelta(60, 0, -40, 50, 0));
        map.put(RepEventType.KILL_UPPER_MOON_UNWITNESSED, new RepDelta(0, 0, -40, 50, 0));
        map.put(RepEventType.KILL_UPPER_MOON_PURE_BLOOD_BATTLE, new RepDelta(60, 0, -40, 65, 0));
        map.put(RepEventType.KILL_UPPER_MOON_TAINTED_BLOOD_BATTLE, new RepDelta(60, 0, -40, 35, 30));
        map.put(RepEventType.CLAIM_KIZUKI_RANK, new RepDelta(-40, -10, -30, 50, 0));
        map.put(RepEventType.CLAIM_KIZUKI_RANK_TAINTED, new RepDelta(-70, -20, -30, 30, 0));
        map.put(RepEventType.PLAYER_FLED_FROM_DEMON, new RepDelta(-8, 0, 3, -8, 0));
        map.put(RepEventType.KILL_VILLAGER_WITNESSED, new RepDelta(-60, -30, 0, 0, 50));
        map.put(RepEventType.KILL_DEMON_SLAYER_WITNESSED, new RepDelta(-100, -50, 0, 0, 80));
        map.put(RepEventType.KILL_DEMON_SLAYER_UNWITNESSED, new RepDelta(-60, -30, 0, 0, 40));
        map.put(RepEventType.PROTECT_VILLAGER_WITNESSED, new RepDelta(15, 0, 0, 0, 0));
        map.put(RepEventType.SURVIVE_SUNRISE_AGAINST_DEMON, new RepDelta(8, 5, -8, 5, 0));
        map.put(RepEventType.DEMON_SURVIVED_ENCOUNTER, new RepDelta(0, 0, 5, -10, 0));

        map.put(RepEventType.COMPLETE_STANDARD_CORPS_MISSION, new RepDelta(18, 0, 0, 0, 0));
        map.put(RepEventType.ABANDON_CORPS_MISSION, new RepDelta(-20, 0, 0, 0, 0));
        map.put(RepEventType.COMPLETE_MISSION_WITH_TANJIRO, new RepDelta(12, 15, 0, 0, 0));
        map.put(RepEventType.REPORT_NEZUKO_TO_DSHQ, new RepDelta(25, -60, 0, 0, 0));
        map.put(RepEventType.PROTECT_NEZUKO_SECRETLY, new RepDelta(0, 35, 0, 0, 0));
        map.put(RepEventType.NEZUKO_PROVEN_INNOCENT, new RepDelta(30, 20, 0, 0, -50));
        map.put(RepEventType.ALLOW_SHINOBU_DAUGHTER_SPIDER_KILL, new RepDelta(0, 5, 0, -12, 0));
        map.put(RepEventType.ALLOW_GIYU_RUI_KILL, new RepDelta(10, 15, -12, -25, 0));
        map.put(RepEventType.KILL_ENMU, new RepDelta(35, 12, -25, 22, 0));
        map.put(RepEventType.SURVIVE_AKAZA_TO_SUNRISE, new RepDelta(25, 18, -15, 18, 0));
        map.put(RepEventType.DEPLETE_AKAZA_HP, new RepDelta(50, 25, -50, 55, 0));
        map.put(RepEventType.HIGH_CONTRIBUTION_SWORDSMITH_DEFENSE, new RepDelta(40, 18, 0, 30, 0));
        map.put(RepEventType.KILL_KOKUSHIBO, new RepDelta(80, 30, -80, 80, 0));
        map.put(RepEventType.KILL_MUZAN, new RepDelta(150, 50, -500, 50, 0));
        map.put(RepEventType.CURE_TANJIRO, new RepDelta(60, 80, -60, -20, -100));
        map.put(RepEventType.KILL_DEMON_TANJIRO, new RepDelta(40, -200, -40, 40, 0));
        map.put(RepEventType.ASSIST_TAMAYO, new RepDelta(8, 12, -8, 0, 0));
        map.put(RepEventType.COMPLETE_HASHIRA_TRAINING, new RepDelta(12, 0, 0, 0, 0));

        map.put(RepEventType.BRIBE_WITNESS_SUCCESS, RepDelta.ZERO);
        map.put(RepEventType.BRIBE_WITNESS_FAILURE, new RepDelta(0, 0, 0, 0, 15));
        map.put(RepEventType.INTIMIDATE_WITNESS_FAIL, new RepDelta(-8, 0, 0, 0, 25));
        map.put(RepEventType.KILL_WITNESS_SEEN_BY_DSHQ, new RepDelta(-50, -20, 0, 0, 50));
        map.put(RepEventType.KILL_CROW_DIRECT_WITNESSED, new RepDelta(-10, 0, 0, 0, 40));
        map.put(RepEventType.KILL_CROW_DIRECT_UNWITNESSED, new RepDelta(0, 0, 0, 0, 20));
        map.put(RepEventType.CROW_ACCIDENTAL_DEATH, RepDelta.ZERO);
        map.put(RepEventType.DRINK_MUZAN_BLOOD, new RepDelta(-150, -80, 0, 50, 0));

        return Collections.unmodifiableMap(map);
    }
}



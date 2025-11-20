package com.wassuples.demonslayer.nemesis;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.nemesis.blueprint.BlueprintLoader;
import com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class SimulationAnnouncer {
    private static final Random random = new Random();

    private static String getRandomElement(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private static String getAnnouncement(DemonTrait trait, String key) {
        // demon-specific
        String announcement = getRandomElement(trait.getBlueprintAnnouncements().get(key));
        if (announcement != null) return announcement;

        // personality-specific
        Optional<DemonBlueprint> personalityBp = BlueprintLoader.getPersonalityDefaults(trait.getPersonality());
        if (personalityBp.isPresent() && personalityBp.get().announcements() != null) {
            announcement = getRandomElement(personalityBp.get().announcements().get(key));
            if (announcement != null) return announcement;
        }

        // global defaults
        Optional<DemonBlueprint> globalBp = BlueprintLoader.getGlobalDefaults();
        if (globalBp.isPresent() && globalBp.get().announcements() != null) {
            announcement = getRandomElement(globalBp.get().announcements().get(key));
            if (announcement != null) return announcement;
        }

        // Ultimate fallback!!!
        return null;
    }

    public static String forFodderDuel(DemonTrait winner, DemonTrait loser, float winnerTTK, float loserTTK) {
        float margin = loserTTK / winnerTTK;
        String announcement;

        if (margin > 3.0f) { // High diff
            announcement = getAnnouncement(winner, "fodder_duel_win_high_diff");
            if (announcement != null) return announcement;
            return "In a swift and brutal confrontation, %s has annihilated %s.";
        } else if (margin < 1.5f) { // Low diff
            announcement = getAnnouncement(winner, "fodder_duel_win_low_diff");
            if (announcement != null) return announcement;
            return "In a grueling battle of wills, %s has narrowly overcome %s.";
        } else { // Mid diff
            announcement = getAnnouncement(winner, "fodder_duel_win_mid_diff");
            if (announcement != null) return announcement;
            return "%s has proven their strength by defeating %s in combat.";
        }
    }

    public static String forChallengerUpset(DemonTrait winner, DemonTrait loser) {
        String announcement = getAnnouncement(winner, "challenger_upset");
        if (announcement != null) return announcement;
        return "A new challenger rises! %s has overthrown %s and joined the Twelve Kizuki as the new Lower Moon Six!";
    }

    public static String forKizukiUpset(DemonTrait winner, DemonTrait loser, DemonRank newRank) {
        String announcement = getAnnouncement(winner, "kizuki_upset");
        if (announcement != null) return announcement;
        return "A shocking turn of events! In a Blood Battle, %s has slain %s and claimed the rank of %s!";
    }
}
package com.wassuples.demonslayer.ranking;

import com.wassuples.demonslayer.missions.PlayerMissions;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class RankRequirements {

    private RankRequirements() {}

    public record Context(PlayerProgression progression, PlayerMissions missions) {}

    public record Requirement(List<List<Predicate<Context>>> orGroups) {
        public boolean isMet(Context ctx) {
            for (List<Predicate<Context>> andGroup : orGroups) {
                if (andGroup.stream().allMatch(p -> p.test(ctx))) return true;
            }
            return false;
        }
    }

    private static final Map<Rank, Requirement> TABLE = new EnumMap<>(Rank.class);

    static {
        // CIVILIAN → MIZUNOTO: Completing the Final Selection mission.
        TABLE.put(Rank.CIVILIAN, or(
            and(missionComplete(new ResourceLocation("demonslayer", "final_selection")))
        ));

        // MIZUNOTO → MIZUNOE: 10 kills in this rank, or > 100 mastery in ANY style.
        TABLE.put(Rank.MIZUNOTO, or(
            and(demonKillsThisRank(10)),
            and(anyStyleMastery(100))
        ));

        // MIZUNOE → KANOTO: 10 kills in this rank, or > 250 mastery in ANY style.
        TABLE.put(Rank.MIZUNOE, or(
            and(demonKillsThisRank(10)),
            and(anyStyleMastery(250))
        ));

        // KANOTO → KANOE: 15 kills in this rank, or > 400 mastery in ANY style.
        TABLE.put(Rank.KANOTO, or(
            and(demonKillsThisRank(15)),
            and(anyStyleMastery(400))
        ));

        // KANOE → TSUCHINOTO: 20 kills in this rank, or > 500 mastery in ANY style.
        TABLE.put(Rank.KANOE, or(
            and(demonKillsThisRank(20)),
            and(anyStyleMastery(500))
        ));

        // TSUCHINOTO → TSUCHINOE: 25 kills in this rank, or > 600 mastery in ANY style.
        TABLE.put(Rank.TSUCHINOTO, or(
            and(demonKillsThisRank(25)),
            and(anyStyleMastery(600))
        ));

        // TSUCHINOE → HINOTO: 30 kills in this rank, or > 700 mastery in ANY style.
        TABLE.put(Rank.TSUCHINOE, or(
            and(demonKillsThisRank(30)),
            and(anyStyleMastery(700))
        ));

        // HINOTO → HINOE: 35 kills in this rank, or > 800 mastery in ANY style.
        TABLE.put(Rank.HINOTO, or(
            and(demonKillsThisRank(35)),
            and(anyStyleMastery(800))
        ));

        // HINOE → KINOTO: 40 kills in this rank, or > 900 mastery in ANY style.
        TABLE.put(Rank.HINOE, or(
            and(demonKillsThisRank(40)),
            and(anyStyleMastery(900))
        ));

        // KINOTO → KINOE: 45 kills in this rank, or > 1000 mastery in ANY style.
        TABLE.put(Rank.KINOTO, or(
            and(demonKillsThisRank(45)),
            and(anyStyleMastery(1000))
        ));

        // KINOE → HASHIRA: Kill 50 demons total (any type) OR singlehandedly kill any Kizuki.
        TABLE.put(Rank.KINOE, or(
            and(totalDemonKills(50)),
            and(killedKizuki())
        ));
    }

    public static boolean isMet(Rank rank, Context ctx) {
        Requirement req = TABLE.get(rank);
        return req != null && req.isMet(ctx);
    }

    // -------------------------------------------------------------------------
    // DSL
    // -------------------------------------------------------------------------

    @SafeVarargs
    private static Requirement or(List<Predicate<Context>>... andGroups) {
        return new Requirement(List.of(andGroups));
    }

    @SafeVarargs
    private static List<Predicate<Context>> and(Predicate<Context>... predicates) {
        return List.of(predicates);
    }

    private static Predicate<Context> totalDemonKills(int count) {
        return ctx -> ctx.progression().getDemonKills() >= count;
    }

    private static Predicate<Context> demonKillsThisRank(int count) {
        return ctx -> ctx.progression().getDemonKillsThisRank() >= count;
    }

    private static Predicate<Context> anyStyleMastery(int threshold) {
        return ctx -> ctx.progression().getStyleMasteryMap().values().stream()
            .anyMatch(mastery -> mastery >= threshold);
    }

    private static Predicate<Context> killedKizuki() {
        return ctx -> ctx.progression().hasKilledKizuki();
    }

    private static Predicate<Context> missionComplete(ResourceLocation missionId) {
        return ctx -> ctx.missions().isCompleted(missionId);
    }

    private static Predicate<Context> styleMastery(BreathingStyles style, int threshold) {
        return ctx -> ctx.progression().getStyleMastery(style) >= threshold;
    }
}

package com.wassuples.demonslayer.nemesis.growth;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.nemesis.trait.*;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Stateless utility that applies growth to a demon based on its
 * {@link GrowthStats} and age. Called once per simulation day for
 * age-based passive growth, and can also be called after a battle
 * outcome for immediate combat-driven growth.
 * <p>
 * Design goals (E2–E4):
 * <ul>
 *   <li>Fighting and surviving is the <b>primary</b> growth source.</li>
 *   <li>Age provides small passive growth.</li>
 *   <li>Close fights yield more growth than easy wins.</li>
 *   <li>Diminishing returns + rank-based ceilings prevent runaway scaling.</li>
 * </ul>
 */
public final class GrowthCalculator {

    private GrowthCalculator() {} // utility class

    private static final Random RANDOM = new Random();

    // ── Constants ───────────────────────────────────────────────────────

    private static final long TICKS_PER_DAY = 24_000L;

    /** Stat points granted per age milestone (every N days). */
    private static final int AGE_GROWTH_INTERVAL_DAYS = 1;  // every day instead of every 3
    private static final int AGE_GROWTH_POINTS = 3;          // 3 instead of 2

    /** Maximum stat points that can be granted in a single simulation day. */
    private static final int MAX_GROWTH_PER_DAY = 20;

    /** Soft cap threshold for diminishing returns. At this stat value,
     *  growth rate is halved. */
    private static final float SOFT_CAP_THRESHOLD = 25.0f;

    /** Rank-based stat ceilings. */
    private static final int FODDER_STAT_CAP = 20;
    private static final int LOWER_MOON_STAT_CAP = 35;
    private static final int UPPER_MOON_STAT_CAP = DemonTrait.STAT_HARD_CAP; // 50

    /** Battle count threshold to be eligible for BDA acquisition. */
    private static final int BDA_UNLOCK_BATTLES = 10;
    private static final int BDA_UNLOCK_WINS = 5;

    /** Win-streak thresholds for quirk acquisition. */
    private static final int QUIRK_STREAK_TIER1 = 3;
    private static final int QUIRK_STREAK_TIER2 = 5;
    private static final int QUIRK_STREAK_TIER3 = 10;

    /** Win-streak bonus cap (stops contributing extra growth after this). */
    private static final int WIN_STREAK_GROWTH_CAP = 10;

    // ── Main entry point ────────────────────────────────────────────────

    /**
     * Applies all applicable growth to the given demon.
     *
     * @param trait       the demon's trait data (mutated in place)
     * @param currentTime current world time in ticks
     * @param rank        the demon's current rank (null = fodder/unranked)
     */
    public static void applyGrowth(DemonTrait trait, long currentTime,
                                    @Nullable DemonRank rank) {
        int rankCap = getStatCapForRank(rank);
        GrowthStats stats = trait.getGrowthStats();
        int oldV = trait.getVigor(), oldS = trait.getStrength(), oldA = trait.getAgility();

        // 1. Age-based passive growth
        applyAgeGrowth(trait, currentTime, rankCap, stats);

        // 2. Battle-driven growth (processes unprocessed battles)
        applyBattleGrowth(trait, rankCap, stats);

        // 3. BDA unlock/leveling
        applyBdaGrowth(trait, stats);

        // 4. Quirk acquisition from streaks
        applyQuirkGrowth(trait, stats);

        int dV = trait.getVigor() - oldV, dS = trait.getStrength() - oldS, dA = trait.getAgility() - oldA;
        if (dV != 0 || dS != 0 || dA != 0) {
            System.out.println("[NEMESIS DEBUG] Growth applied to " + trait.getName() +
                ": V " + oldV + "→" + trait.getVigor() +
                ", S " + oldS + "→" + trait.getStrength() +
                ", A " + oldA + "→" + trait.getAgility() +
                " (cap=" + rankCap + ", rank=" + (rank != null ? rank.name() : "FODDER") + ")");
        }
    }

    // ── Age growth (E2: small passive) ──────────────────────────────────

    private static void applyAgeGrowth(DemonTrait trait, long currentTime,
                                        int rankCap, GrowthStats stats) {
        long ageTicks = trait.getAge(currentTime);
        long ageDays = ageTicks / TICKS_PER_DAY;

        // Only grant age growth at milestones
        if (ageDays > 0 && ageDays % AGE_GROWTH_INTERVAL_DAYS == 0) {
            if (stats.getGrowthPointsToday() >= MAX_GROWTH_PER_DAY) return;

            // Boost the lowest stat
            int lowestStat = Math.min(trait.getVigor(),
                    Math.min(trait.getStrength(), trait.getAgility()));

            if (lowestStat == trait.getVigor() && trait.getVigor() < rankCap) {
                int effective = diminish(AGE_GROWTH_POINTS, trait.getVigor());
                trait.setVigor(trait.getVigor() + effective);
                stats.addGrowthPointsToday(effective);
            } else if (lowestStat == trait.getStrength() && trait.getStrength() < rankCap) {
                int effective = diminish(AGE_GROWTH_POINTS, trait.getStrength());
                trait.setStrength(trait.getStrength() + effective);
                stats.addGrowthPointsToday(effective);
            } else if (trait.getAgility() < rankCap) {
                int effective = diminish(AGE_GROWTH_POINTS, trait.getAgility());
                trait.setAgility(trait.getAgility() + effective);
                stats.addGrowthPointsToday(effective);
            }
        }
    }

    // ── Battle growth (E2: primary source) ──────────────────────────────

    private static void applyBattleGrowth(DemonTrait trait, int rankCap,
                                           GrowthStats stats) {
        int processed = stats.getLastGrowthBattleCount();
        int total = stats.getBattleCount();
        if (total <= processed) return; // nothing new

        // Process each new battle
        int newBattles = total - processed;
        for (int i = 0; i < newBattles; i++) {
            if (stats.getGrowthPointsToday() >= MAX_GROWTH_PER_DAY) break;

            // Determine base points:
            // - Won close call = 3 base points
            // - Won easily = 1 base point
            // - Lost = 1 base point (survivors learn)
            // - Escaped = 0 base points
            // We approximate by checking win ratio changes
            int basePoints;
            if (stats.getWins() > 0 && stats.getCurrentWinStreak() > 0) {
                // Recent win — check if it was close
                float dmgRatio = stats.getDamageTakenTotal() /
                        Math.max(1f, stats.getDamageDealtTotal());
                basePoints = dmgRatio > 0.7f ? 3 : 1; // high damage taken = close fight
            } else if (stats.getLosses() > 0) {
                basePoints = 1; // survived to learn
            } else {
                basePoints = 0;
            }

            // Win-streak bonus (capped)
            int streakBonus = Math.min(stats.getCurrentWinStreak(), WIN_STREAK_GROWTH_CAP) / 3;
            int totalPoints = basePoints + streakBonus;

            // Distribute points across stats (weighted random)
            distributeStatPoints(trait, totalPoints, rankCap, stats);
        }

        stats.setLastGrowthBattleCount(total);
    }

    /**
     * Distributes growth points across vigor/strength/agility with some
     * randomness, respecting rank caps and diminishing returns.
     */
    private static void distributeStatPoints(DemonTrait trait, int points,
                                              int rankCap, GrowthStats stats) {
        for (int i = 0; i < points; i++) {
            if (stats.getGrowthPointsToday() >= MAX_GROWTH_PER_DAY) break;

            // Weighted random: prefer the lowest stat
            int v = trait.getVigor(), s = trait.getStrength(), a = trait.getAgility();
            int total = (rankCap - v) + (rankCap - s) + (rankCap - a);
            if (total <= 0) break; // all stats at cap

            float roll = RANDOM.nextFloat() * total;
            if (roll < (rankCap - v) && v < rankCap) {
                int effective = diminish(1, v);
                trait.setVigor(v + effective);
                stats.addGrowthPointsToday(effective);
            } else if (roll < (rankCap - v) + (rankCap - s) && s < rankCap) {
                int effective = diminish(1, s);
                trait.setStrength(s + effective);
                stats.addGrowthPointsToday(effective);
            } else if (a < rankCap) {
                int effective = diminish(1, a);
                trait.setAgility(a + effective);
                stats.addGrowthPointsToday(effective);
            }
        }
    }

    // ── BDA growth (E3: unlock/level) ───────────────────────────────────

    private static void applyBdaGrowth(DemonTrait trait, GrowthStats stats) {
        if (trait.getAssignedBDA() != BloodDemonArts.NONE) return;
        if (stats.getBattleCount() < BDA_UNLOCK_BATTLES) return;
        if (stats.getWins() < BDA_UNLOCK_WINS) return;

        // Roll for BDA acquisition (20% chance per eligible check)
        if (RANDOM.nextFloat() < 0.20f) {
            List<BloodDemonArts> pool = List.of(
                    BloodDemonArts.THREADS,
                    BloodDemonArts.SASH,
                    BloodDemonArts.HAEMO,
                    BloodDemonArts.SLEEP
            );
            BloodDemonArts newBda = pool.get(RANDOM.nextInt(pool.size()));
            trait.setAssignedBDA(newBda);
            System.out.println("[NEMESIS DEBUG] BDA UNLOCKED: " + trait.getName() + " acquired " + newBda.name() +
                " (battles=" + stats.getBattleCount() + ", wins=" + stats.getWins() + ")");
        }
    }

    // ── Quirk growth (E3: streak-based) ─────────────────────────────────

    private static void applyQuirkGrowth(DemonTrait trait, GrowthStats stats) {
        int streak = stats.getBestWinStreak();

        // Win-streak → strength quirks
        if (streak >= QUIRK_STREAK_TIER3 && !trait.hasQuirk(Quirk.DEATH_DEFIANT)) {
            if (RANDOM.nextFloat() < 0.15f) {
                trait.addQuirk(Quirk.DEATH_DEFIANT);
                System.out.println("[NEMESIS DEBUG] QUIRK GAINED: " + trait.getName() + " acquired DEATH_DEFIANT (streak=" + streak + ")");
            }
        } else if (streak >= QUIRK_STREAK_TIER2 && !trait.hasQuirk(Quirk.BLOOD_SIPHON)) {
            if (RANDOM.nextFloat() < 0.20f) {
                trait.addQuirk(Quirk.BLOOD_SIPHON);
                System.out.println("[NEMESIS DEBUG] QUIRK GAINED: " + trait.getName() + " acquired BLOOD_SIPHON (streak=" + streak + ")");
            }
        } else if (streak >= QUIRK_STREAK_TIER1 && !trait.hasQuirk(Quirk.NIMBLE)) {
            if (RANDOM.nextFloat() < 0.25f) {
                trait.addQuirk(Quirk.NIMBLE);
                System.out.println("[NEMESIS DEBUG] QUIRK GAINED: " + trait.getName() + " acquired NIMBLE (streak=" + streak + ")");
            }
        }

        // Loss-streak → weakness quirks (if current streak is 0 and losses > wins)
        if (stats.getCurrentWinStreak() == 0 && stats.getLosses() > stats.getWins() + 2) {
            if (!trait.hasQuirk(Quirk.OVERCONFIDENT) && RANDOM.nextFloat() < 0.15f) {
                trait.addQuirk(Quirk.OVERCONFIDENT);
                System.out.println("[NEMESIS DEBUG] QUIRK GAINED (weakness): " + trait.getName() + " acquired OVERCONFIDENT (losses=" + stats.getLosses() + ")");
            }
        }
    }

    // ── Diminishing returns (E4) ────────────────────────────────────────

    /**
     * Applies diminishing returns to growth points based on the current
     * stat value. Returns the effective points after the modifier.
     * Always returns at least 0; returns 1 with decreasing probability
     * at higher stat values.
     */
    private static int diminish(int rawPoints, int currentStat) {
        float modifier = 1.0f / (1.0f + currentStat / SOFT_CAP_THRESHOLD);
        float effective = rawPoints * modifier;
        // Stochastic rounding: if effective < 1, roll for a chance at 1
        if (effective < 1.0f) {
            return RANDOM.nextFloat() < effective ? 1 : 0;
        }
        return Math.round(effective);
    }

    // ── Rank-based caps (E4) ────────────────────────────────────────────

    /**
     * Returns the maximum stat value a demon can reach based on its rank.
     */
    public static int getStatCapForRank(@Nullable DemonRank rank) {
        if (rank == null) return FODDER_STAT_CAP;
        if (rank.ordinal() >= DemonRank.UPPER_MOON_6.ordinal()) {
            return UPPER_MOON_STAT_CAP;
        }
        if (rank.ordinal() >= DemonRank.LOWER_MOON_6.ordinal()) {
            return LOWER_MOON_STAT_CAP;
        }
        return FODDER_STAT_CAP;
    }
}


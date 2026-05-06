package com.wassuples.demonslayer.reputation.persuasion;

import com.wassuples.demonslayer.ranking.Rank;

public final class PersuasionManager {
    private PersuasionManager() {
    }

    public static int getBaseChanceForRank(Rank rank) {
        if (rank == Rank.HASHIRA) return 90;
        if (rank == Rank.KINOTO || rank == Rank.KINOE) return 75;
        if (rank == Rank.TSUCHINOE || rank == Rank.HINOTO || rank == Rank.HINOE) return 65;
        if (rank == Rank.KANOE || rank == Rank.KANOTO) return 50;
        return 30;
    }

    public static int clampChance(int chance) {
        return Math.max(0, Math.min(100, chance));
    }
}


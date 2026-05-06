package com.wassuples.demonslayer.reputation.persuasion;

public final class PersuasionContextEvaluator {
    private PersuasionContextEvaluator() {
    }

    public static int evaluateModifier(boolean witnessedDemonKill,
                                       boolean witnessRecentlyProtected,
                                       boolean recentlyKilledCivilianOrSlayer,
                                       boolean corpsRepNegative,
                                       boolean previouslyPersuadedThisWitness,
                                       boolean witnessAlreadySuspicious) {
        int modifier = 0;
        if (witnessedDemonKill) modifier += 15;
        if (witnessRecentlyProtected) modifier += 25;
        if (recentlyKilledCivilianOrSlayer) modifier -= 40;
        if (corpsRepNegative) modifier -= 20;
        if (previouslyPersuadedThisWitness) modifier -= 10;
        if (witnessAlreadySuspicious) modifier -= 30;
        return modifier;
    }
}


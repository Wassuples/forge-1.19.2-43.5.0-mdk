package com.wassuples.demonslayer.nemesis.ai;

public enum Behavior {
    // Offensive
    AGGRESSIVE,     // Seeks fights proactively.
    BERSERKER,      // Fights recklessly with no self-preservation.
    AMBITIOUS,      // Prioritizes targets that will help it gain rank.
    DUELIST,        // Prefers honorable 1v1 combat, focusing on single a single challenged target.

    // Defensive
    STALWART,       // A defensive tank that outlasts opponents.
    BODYGUARD,      // A specialized loyalist that actively protects a specific, stronger demon.
    TERRITORIAL,    // Fights fiercely in its home territory, but may retreat if lured too far away.

    // Tactical
    CALM,           // Avoids fights unless provoked or has a clear advantage (e.g. ambush).
    COWARDLY,       // Actively flees from stronger opponents or when low on health.
    STRATEGIST,     // Uses the environment and lures players into traps.
    SKIRMISHER,     // Prefers ranged attacks and kiting; maintains distance.
    TRICKSTER,      // Relies on misdirection, clones, and teleportation to confuse enemies.
    SCAVENGER,      // Avoids healthy targets, preying on demons who are already wounded.

    // Other
    OVERLORD,       // Rarely fights alone, summons and commands weaker allies.
    LOYALIST,       // Forms alliances and fights alongside stronger demons.
    SADIST;         // Focuses on crippling and toying with opponents rather than quick kills.
}

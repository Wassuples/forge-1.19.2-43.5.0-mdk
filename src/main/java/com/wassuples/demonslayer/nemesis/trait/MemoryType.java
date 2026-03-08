package com.wassuples.demonslayer.nemesis.trait;

/**
 * A compact representation of "things the demon remembers".
 *
 * Notes:
 * - Keep this enum stable across versions when possible; renames break old saves.
 * - Prefer adding new entries over renaming/removing existing ones.
 */
public enum MemoryType {
    // Combat Encounters (wounds / style trauma)
    WOUNDED_BY_FLAME,
    WOUNDED_BY_WATER,
    WOUNDED_BY_THUNDER,
    WOUNDED_BY_WIND,
    WOUNDED_BY_BEAST,
    WOUNDED_BY_INSECT,
    WOUNDED_BY_SUN,
    WOUNDED_BY_EXPLOSION,
    WOUNDED_BY_POISON,

    // Battle Outcomes
    ESCAPED_BATTLE,
    WON_EASILY,
    WON_CLOSE_CALL,
    LOST_BADLY,
    HUMILIATED,
    KILLED_PLAYER,
    KILLED_KIZUKI,
    DEFEATED_KIZUKI,

    // Emotional / Status
    FEAR_FIRE,
    HATES_THUNDER,
    RIVALRY_CREATED,

    // Time/Growth / Meta
    SURVIVED_SUNRISE,
    PROMOTED,
    DEMOTED
}


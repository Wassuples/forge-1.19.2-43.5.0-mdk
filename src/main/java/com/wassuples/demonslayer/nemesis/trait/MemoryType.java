package com.wassuples.demonslayer.nemesis.trait;

/**
 * A compact representation of <b>factual</b> events the demon remembers.
 * <p>
 * Design rules (Nemesis System TODO §1):
 * <ul>
 *   <li>Every entry here is a <b>fact</b> ("what happened"), never a feeling.</li>
 *   <li>Feelings (fear, hate, respect…) are modeled separately via
 *       {@link EmotionType} and {@code EmotionState}.</li>
 *   <li>Prefer adding new entries over renaming/removing existing ones —
 *       renames break old saves.</li>
 *   <li>Use the {@link DemonMemory} context map for fine-grained details
 *       (biome, style tag, etc.) instead of exploding this enum.</li>
 * </ul>
 */

/**
 * Memory Type.
 */
public enum MemoryType {

    // ── Combat Encounters: wounds received ───────────────────────────────
    WOUNDED_BY_FLAME,
    WOUNDED_BY_WATER,
    WOUNDED_BY_THUNDER,
    WOUNDED_BY_WIND,
    WOUNDED_BY_BEAST,
    WOUNDED_BY_INSECT,
    WOUNDED_BY_SUN,
    WOUNDED_BY_EXPLOSION,
    WOUNDED_BY_POISON,

    // ── Combat Encounters: styles fought against (A5) ────────────────────
    FOUGHT_FLAME_STYLE,
    FOUGHT_WATER_STYLE,
    FOUGHT_THUNDER_STYLE,
    FOUGHT_WIND_STYLE,
    FOUGHT_BEAST_STYLE,
    FOUGHT_INSECT_STYLE,
    FOUGHT_STONE_STYLE,
    FOUGHT_SOUND_STYLE,
    FOUGHT_MIST_STYLE,
    FOUGHT_LOVE_STYLE,
    FOUGHT_SERPENT_STYLE,
    FOUGHT_FLOWER_STYLE,
    FOUGHT_MOON_STYLE,
    FOUGHT_SUN_STYLE,

    // ── Battle Outcomes ──────────────────────────────────────────────────
    ESCAPED_BATTLE,
    WON_EASILY,
    WON_CLOSE_CALL,
    LOST_BADLY,
    HUMILIATED,
    KILLED_PLAYER,
    KILLED_KIZUKI,
    DEFEATED_KIZUKI,

    // ── Hierarchy / Rank Changes ─────────────────────────────────────────
    PROMOTED,
    DEMOTED,

    // ── Social / Rivalry (facts only) ────────────────────────────────────
    RIVALRY_STARTED,
    BETRAYED_ALLY,
    WAS_BETRAYED,

    // ── Survival / Meta ──────────────────────────────────────────────────
    SURVIVED_SUNRISE,
    NEAR_DEATH_EXPERIENCE
}


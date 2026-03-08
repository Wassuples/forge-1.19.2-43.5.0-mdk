package com.wassuples.demonslayer.nemesis.taunt;

import java.util.Set;

/**
 * Single source of truth for all taunt key constants (I1).
 * Keys are used to look up taunt text from blueprint JSON files,
 * personality defaults, or global defaults.
 */
public final class TauntKeyRegistry {

    private TauntKeyRegistry() {}

    // ── Style encounter (demon sees a player using a style) ─────────────

    public static final String ENCOUNTER_FLAME    = "encounter_flame";
    public static final String ENCOUNTER_WATER    = "encounter_water";
    public static final String ENCOUNTER_THUNDER  = "encounter_thunder";
    public static final String ENCOUNTER_WIND     = "encounter_wind";
    public static final String ENCOUNTER_STONE    = "encounter_stone";
    public static final String ENCOUNTER_BEAST    = "encounter_beast";
    public static final String ENCOUNTER_INSECT   = "encounter_insect";
    public static final String ENCOUNTER_LOVE     = "encounter_love";
    public static final String ENCOUNTER_SERPENT   = "encounter_serpent";
    public static final String ENCOUNTER_FLOWER   = "encounter_flower";
    public static final String ENCOUNTER_MIST     = "encounter_mist";
    public static final String ENCOUNTER_SOUND    = "encounter_sound";
    public static final String ENCOUNTER_SUN      = "encounter_sun";
    public static final String ENCOUNTER_MOON     = "encounter_moon";

    // ── Fear/hate reactions (emotion-driven) ────────────────────────────

    public static final String FEAR_FIRE_PANIC     = "fear_fire_panic";
    public static final String FEAR_WATER_PANIC    = "fear_water_panic";
    public static final String FEAR_LIGHTNING_PANIC= "fear_lightning_panic";
    public static final String FEAR_POISON_PANIC   = "fear_poison_panic";
    public static final String HATE_FLAME_RAGE     = "hate_flame_rage";
    public static final String HATE_WATER_RAGE     = "hate_water_rage";
    public static final String HATE_THUNDER_RAGE   = "hate_thunder_rage";
    public static final String FEAR_PLAYER_PANIC   = "fear_player_panic";
    public static final String HATE_PLAYER_RAGE    = "hate_player_rage";
    public static final String RESPECT_PLAYER      = "respect_player";
    public static final String CONTEMPT_PLAYER     = "contempt_player";

    // ── Outcome reactions (kill context) ─────────────────────────────────

    public static final String KILL_LOW_HP_COMEBACK = "low_health_comeback";
    public static final String KILL_FIRE            = "fire_kill";
    public static final String KILL_POISON          = "poison_kill";
    public static final String KILL_OVERKILL        = "overkill";
    public static final String KILL_TACTICAL        = "tactical_kill";
    public static final String KILL_GENERIC         = "generic";

    // ── Rivalry mentions ────────────────────────────────────────────────

    public static final String RIVALRY_GREETING     = "rivalry_greeting";
    public static final String RIVALRY_REMATCH      = "rivalry_rematch";
    public static final String RIVALRY_REVENGE      = "rivalry_revenge";

    // ── Hunter arrival ──────────────────────────────────────────────────

    public static final String HUNTER_ARRIVAL        = "hunter_arrival";
    public static final String HUNTER_TERRITORIAL    = "hunter_territorial";
    public static final String HUNTER_BLOODTHIRSTY   = "hunter_bloodthirsty";
    public static final String HUNTER_AMBITIOUS      = "hunter_ambitious";

    // ── Demon-vs-demon world battle ─────────────────────────────────────

    public static final String WORLD_BATTLE_START    = "world_battle_start";
    public static final String WORLD_BATTLE_VICTORY  = "world_battle_victory";
    public static final String WORLD_BATTLE_DEFEAT   = "world_battle_defeat";

    // ── All keys (for validation) ───────────────────────────────────────

    public static final Set<String> ALL_KEYS = Set.of(
            ENCOUNTER_FLAME, ENCOUNTER_WATER, ENCOUNTER_THUNDER, ENCOUNTER_WIND,
            ENCOUNTER_STONE, ENCOUNTER_BEAST, ENCOUNTER_INSECT, ENCOUNTER_LOVE,
            ENCOUNTER_SERPENT, ENCOUNTER_FLOWER, ENCOUNTER_MIST, ENCOUNTER_SOUND,
            ENCOUNTER_SUN, ENCOUNTER_MOON,
            FEAR_FIRE_PANIC, FEAR_WATER_PANIC, FEAR_LIGHTNING_PANIC, FEAR_POISON_PANIC,
            HATE_FLAME_RAGE, HATE_WATER_RAGE, HATE_THUNDER_RAGE,
            FEAR_PLAYER_PANIC, HATE_PLAYER_RAGE, RESPECT_PLAYER, CONTEMPT_PLAYER,
            KILL_LOW_HP_COMEBACK, KILL_FIRE, KILL_POISON, KILL_OVERKILL,
            KILL_TACTICAL, KILL_GENERIC,
            RIVALRY_GREETING, RIVALRY_REMATCH, RIVALRY_REVENGE,
            HUNTER_ARRIVAL, HUNTER_TERRITORIAL, HUNTER_BLOODTHIRSTY, HUNTER_AMBITIOUS,
            WORLD_BATTLE_START, WORLD_BATTLE_VICTORY, WORLD_BATTLE_DEFEAT
    );
}


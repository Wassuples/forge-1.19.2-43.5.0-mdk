package com.wassuples.demonslayer.nemesis.taunt;

import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.nemesis.trait.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Condition-based taunt key selection (I2).
 * Given a demon's full state and a context, picks the most appropriate
 * taunt key from {@link TauntKeyRegistry}.
 */
public final class TauntSelector {

    private TauntSelector() {}

    /**
     * Context for taunt selection.
     */
    public record TauntContext(
            @Nullable UUID targetPlayerId,
            @Nullable String currentStyle,       // player's breathing style name
            @Nullable String biome,              // current biome path
            long timeOfDay,                      // 0-24000
            @Nullable String killContext,         // legacy: "low_health_comeback", "fire_kill", etc.
            boolean isHunterArrival              // is this a hunter spawning in
    ) {
        public static TauntContext forKill(String killContext) {
            return new TauntContext(null, null, null, 0, killContext, false);
        }

        public static TauntContext forHunterArrival(@Nullable UUID playerId, @Nullable String biome) {
            return new TauntContext(playerId, null, biome, 0, null, true);
        }

        public static TauntContext forEncounter(@Nullable UUID playerId, @Nullable String style,
                                                 @Nullable String biome, long timeOfDay) {
            return new TauntContext(playerId, style, biome, timeOfDay, null, false);
        }
    }

    /**
     * Selects the best taunt key for the given demon and context.
     * Uses a priority-ordered chain of checks (I2).
     */
    public static String selectTauntKey(DemonTrait trait, TauntContext ctx) {
        // Priority 1: Hunter arrival
        if (ctx.isHunterArrival()) {
            return selectHunterArrivalKey(trait);
        }

        // Priority 2: Strong emotion-driven reactions
        String emotionKey = selectEmotionKey(trait, ctx);
        if (emotionKey != null) return emotionKey;

        // Priority 3: Rivalry-based greeting
        if (ctx.targetPlayerId() != null) {
            String rivalryKey = selectRivalryKey(trait, ctx.targetPlayerId());
            if (rivalryKey != null) return rivalryKey;
        }

        // Priority 4: Style encounter
        if (ctx.currentStyle() != null) {
            String styleKey = selectStyleEncounterKey(ctx.currentStyle());
            if (styleKey != null) return styleKey;
        }

        // Priority 5: Kill context (legacy backward-compat)
        if (ctx.killContext() != null) {
            return ctx.killContext(); // already matches registry keys
        }

        // Fallback
        return TauntKeyRegistry.KILL_GENERIC;
    }

    private static String selectHunterArrivalKey(DemonTrait trait) {
        return switch (trait.getBehavior()) {
            case TERRITORIAL -> TauntKeyRegistry.HUNTER_TERRITORIAL;
            case AGGRESSIVE, BERSERKER -> TauntKeyRegistry.HUNTER_BLOODTHIRSTY;
            case AMBITIOUS, OVERLORD -> TauntKeyRegistry.HUNTER_AMBITIOUS;
            default -> TauntKeyRegistry.HUNTER_ARRIVAL;
        };
    }

    /**
     * Checks emotion state for strong reactions (I2).
     * Returns a fear/hate/respect key if intensity is high enough.
     */
    @Nullable
    private static String selectEmotionKey(DemonTrait trait, TauntContext ctx) {
        EmotionState emotions = trait.getEmotionState();
        float threshold = 0.40f; // minimum intensity to trigger emotion-driven taunt

        // Player-specific emotions
        if (emotions.getIntensity(EmotionType.FEAR_PLAYER) > threshold) {
            return TauntKeyRegistry.FEAR_PLAYER_PANIC;
        }
        if (emotions.getIntensity(EmotionType.HATE_PLAYER) > threshold) {
            return TauntKeyRegistry.HATE_PLAYER_RAGE;
        }
        if (emotions.getIntensity(EmotionType.RESPECT_PLAYER) > threshold) {
            return TauntKeyRegistry.RESPECT_PLAYER;
        }
        if (emotions.getIntensity(EmotionType.CONTEMPT_PLAYER) > threshold) {
            return TauntKeyRegistry.CONTEMPT_PLAYER;
        }

        // Elemental fear (if the player's current style matches)
        if (ctx.currentStyle() != null) {
            String styleLower = ctx.currentStyle().toLowerCase();
            if (styleLower.contains("flame") || styleLower.contains("fire")) {
                if (emotions.getIntensity(EmotionType.FEAR_FIRE) > threshold) {
                    return TauntKeyRegistry.FEAR_FIRE_PANIC;
                }
                if (emotions.getIntensity(EmotionType.HATE_FLAME_STYLE) > threshold) {
                    return TauntKeyRegistry.HATE_FLAME_RAGE;
                }
            }
            if (styleLower.contains("water")) {
                if (emotions.getIntensity(EmotionType.FEAR_WATER) > threshold) {
                    return TauntKeyRegistry.FEAR_WATER_PANIC;
                }
                if (emotions.getIntensity(EmotionType.HATE_WATER_STYLE) > threshold) {
                    return TauntKeyRegistry.HATE_WATER_RAGE;
                }
            }
            if (styleLower.contains("thunder") || styleLower.contains("lightning")) {
                if (emotions.getIntensity(EmotionType.FEAR_THUNDER) > threshold) {
                    return TauntKeyRegistry.FEAR_LIGHTNING_PANIC;
                }
                if (emotions.getIntensity(EmotionType.HATE_THUNDER_STYLE) > threshold) {
                    return TauntKeyRegistry.HATE_THUNDER_RAGE;
                }
            }
        }

        // Personality-skewed emotions for cowardly vs brave
        if (trait.getPersonality() == Personality.NERVOUS
                || trait.getPersonality() == Personality.WHINING
                || trait.getBehavior() == Behavior.COWARDLY) {
            if (emotions.getIntensity(EmotionType.FEAR_PLAYER) > 0.2f) {
                return TauntKeyRegistry.FEAR_PLAYER_PANIC;
            }
        }

        return null;
    }

    /**
     * Checks demon-to-player relationship for rivalry-based taunts.
     */
    @Nullable
    private static String selectRivalryKey(DemonTrait trait, UUID playerId) {
        DemonRelationship rel = trait.getRelationship(playerId);
        if (rel == null) return null;

        if (rel.getRivalryIntensity() > 0.6f && rel.getEncounterCount() > 3) {
            return TauntKeyRegistry.RIVALRY_REVENGE;
        }
        if (rel.getRivalryIntensity() > 0.3f && rel.getEncounterCount() > 1) {
            return TauntKeyRegistry.RIVALRY_REMATCH;
        }
        if (rel.getRivalryIntensity() > 0.1f) {
            return TauntKeyRegistry.RIVALRY_GREETING;
        }
        return null;
    }

    /**
     * Maps a breathing style name to the corresponding encounter taunt key.
     */
    @Nullable
    private static String selectStyleEncounterKey(String styleName) {
        if (styleName == null) return null;
        String lower = styleName.toLowerCase();
        if (lower.contains("flame") || lower.contains("fire")) return TauntKeyRegistry.ENCOUNTER_FLAME;
        if (lower.contains("water"))    return TauntKeyRegistry.ENCOUNTER_WATER;
        if (lower.contains("thunder"))  return TauntKeyRegistry.ENCOUNTER_THUNDER;
        if (lower.contains("wind"))     return TauntKeyRegistry.ENCOUNTER_WIND;
        if (lower.contains("stone"))    return TauntKeyRegistry.ENCOUNTER_STONE;
        if (lower.contains("beast"))    return TauntKeyRegistry.ENCOUNTER_BEAST;
        if (lower.contains("insect"))   return TauntKeyRegistry.ENCOUNTER_INSECT;
        if (lower.contains("love"))     return TauntKeyRegistry.ENCOUNTER_LOVE;
        if (lower.contains("serpent"))   return TauntKeyRegistry.ENCOUNTER_SERPENT;
        if (lower.contains("flower"))   return TauntKeyRegistry.ENCOUNTER_FLOWER;
        if (lower.contains("mist"))     return TauntKeyRegistry.ENCOUNTER_MIST;
        if (lower.contains("sound"))    return TauntKeyRegistry.ENCOUNTER_SOUND;
        if (lower.contains("sun"))      return TauntKeyRegistry.ENCOUNTER_SUN;
        if (lower.contains("moon"))     return TauntKeyRegistry.ENCOUNTER_MOON;
        return null;
    }
}


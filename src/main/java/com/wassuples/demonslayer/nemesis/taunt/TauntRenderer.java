package com.wassuples.demonslayer.nemesis.taunt;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expands taunt template strings by replacing placeholders (I3).
 * <p>
 * Supported placeholders:
 * <ul>
 *   <li>{player} — target player name</li>
 *   <li>{demon} — demon's short name</li>
 *   <li>{title} — demon's title</li>
 *   <li>{rank} — demon's rank display name, or "Unranked"</li>
 *   <li>{style} — player's current breathing style</li>
 *   <li>{biome} — current biome display name</li>
 *   <li>{time} — "night" or "day"</li>
 * </ul>
 */
public final class TauntRenderer {

    private TauntRenderer() {}

    /**
     * Renders a raw taunt string by replacing all known placeholders.
     *
     * @param template   the raw template string (e.g., "You dare face {demon}, {title}?")
     * @param demon      the demon's trait data
     * @param playerName target player's display name (may be null)
     * @param rank       the demon's current rank (null = unranked/fodder)
     * @param style      the player's current breathing style name (may be null)
     * @param biome      the current biome resource location path (may be null)
     * @param timeOfDay  the current world time (0-24000)
     */
    public static String render(String template, DemonTrait demon,
                                 @Nullable String playerName,
                                 @Nullable DemonRank rank,
                                 @Nullable String style,
                                 @Nullable String biome,
                                 long timeOfDay) {
        Map<String, String> replacements = buildReplacements(
                demon, playerName, rank, style, biome, timeOfDay);
        return applyReplacements(template, replacements);
    }

    /**
     * Builds the full replacement map from context.
     */
    public static Map<String, String> buildReplacements(DemonTrait demon,
                                                          @Nullable String playerName,
                                                          @Nullable DemonRank rank,
                                                          @Nullable String style,
                                                          @Nullable String biome,
                                                          long timeOfDay) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("player", playerName != null ? playerName : "warrior");
        map.put("demon", demon.getName() != null ? demon.getName() : "Unknown");
        map.put("title", demon.getTitle() != null ? demon.getTitle() : "");
        map.put("rank", rank != null ? rank.getDisplayName().getString() : "Unranked");
        map.put("style", style != null ? style : "an unknown style");
        map.put("biome", biome != null ? formatBiomeName(biome) : "these lands");
        map.put("time", timeOfDay >= 13000 ? "night" : "day");
        return map;
    }

    /**
     * Applies a map of placeholder→value replacements to a template.
     */
    public static String applyReplacements(String template, Map<String, String> replacements) {
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Converts a biome resource path like "minecraft:dark_forest" into "Dark Forest".
     */
    private static String formatBiomeName(String biomePath) {
        // Strip namespace
        int colonIdx = biomePath.indexOf(':');
        String path = colonIdx >= 0 ? biomePath.substring(colonIdx + 1) : biomePath;

        // Convert underscores to spaces and capitalize
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}


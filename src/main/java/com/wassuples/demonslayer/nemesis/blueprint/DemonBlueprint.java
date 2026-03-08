package com.wassuples.demonslayer.nemesis.blueprint;

import com.google.gson.annotations.SerializedName;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import com.wassuples.demonslayer.styles.BloodDemonArts;

import java.util.List;
import java.util.Map;

/**
 * JSON-deserialized blueprint defining a canonical Nemesis demon's initial state.
 *
 * <p>Blueprints are loaded from {@code config/demonslayer/} by {@link BlueprintLoader} and
 * used by the {@link com.wassuples.demonslayer.nemesis.DemonHierarchyManager} to populate
 * Kizuki slots with lore-accurate demons (e.g. Rui, Akaza).
 *
 * <p>Fields include name, title, behavior, personality, BDA, quirks, base stats, and taunt lines.
 *
 * @see BlueprintLoader
 */
public class DemonBlueprint {
    public String id;
    public String name;
    public String title;
    public Behavior behavior;
    public Personality personality;
    @SerializedName("bda") public BloodDemonArts bloodDemonArt;
    public List<Quirk> quirks;
    public Stats stats;
    @SerializedName("blood_points") public int bloodPoints;
    public Map<String, List<String>> taunts;
    public Map<String, List<String>> announcements;


    public static class Stats {
        public int vigor;
        public int strength;
        public int agility;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String title() { return title; }
    public Behavior behavior() { return behavior; }
    public Personality personality() { return personality; }
    public BloodDemonArts bloodDemonArt() { return bloodDemonArt; }
    public List<Quirk> quirks() { return quirks; }
    public Stats stats() { return stats; }
    public int bloodPoints() { return bloodPoints; }
    public Map<String, List<String>> taunts() { return taunts; }
    public Map<String, List<String>> announcements() { return announcements; }
}

package com.wassuples.demonslayer.styles;

/**
 * Enumerates all Breathing Styles available to slayer-path players.
 *
 * <p>Each style groups a set of {@link BreathingForms} and can be unlocked, selected, and mastered
 * via the {@link com.wassuples.demonslayer.ranking.PlayerProgression} capability. The first style
 * ({@link #WATER}) is granted by default on player creation.
 *
 * <p>{@link #NONE} represents the absence of a style and should not be displayed in selection UIs.
 *
 * @see BreathingForms
 * @see com.wassuples.demonslayer.breathing.PlayerStyle
 */
public enum BreathingStyles {
    NONE("NONE"),
    WIND("Wind Breathing"),
    INSECT("Insect Breathing"),
    STONE("Stone Breathing"),
    WATER("Water Breathing"),
    LOVE("Love Breathing"),
    SOUND("Sound Breathing"),
    BEAST("Beast Breathing"),
    SERPENT("Serpent Breathing"),
    FLOWER("Flower Breathing"),
    THUNDER("Thunder Breathing"),
    FLAME("Flame Breathing"),
    MIST("Mist Breathing"),
    MOON("Moon Breathing"),
    SUN("Sun Breathing"),
    WOOD("Wood Breathing"),
    FROST("Frost Breathing"),
    SAND("Sand Breathing"),
    ;

    private final String displayName;

    BreathingStyles(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return this.displayName;
    }
}

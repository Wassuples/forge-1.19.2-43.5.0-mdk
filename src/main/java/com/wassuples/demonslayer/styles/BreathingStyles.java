package com.wassuples.demonslayer.styles;

import net.minecraft.ChatFormatting;

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

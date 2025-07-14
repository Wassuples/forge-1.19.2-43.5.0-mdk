package com.wassuples.demonslayer.styles;

public enum BloodDemonArts {
    NONE("None"),
    CRESCENT("Crescent Moon Blades"),
    THREADS("Thread Manipulation"),
    SLEEP("Sleep Manipulation"),
    SASH("Obi Sash Manipulation"),
    HAEMO("Haemokinesis"),
    FISH("Fish Scale Attacks"),
    EMOTION("Emotion Manifestation"),
    DESTRUCTIVE_DEATH("Destructive Death"),
    CRYO("Cryokinesis");

    private final String displayName;

    BloodDemonArts(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return this.displayName;
    }
}
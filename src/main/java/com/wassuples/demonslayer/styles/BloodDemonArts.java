package com.wassuples.demonslayer.styles;

import net.minecraft.network.chat.Component;

/**
 * Enumerates all Blood Demon Arts (BDAs) available to demon-path players.
 *
 * <p>Each BDA groups a set of {@link DemonForms}. BDAs are unlocked by spending Blood Points
 * through {@link com.wassuples.demonslayer.demon.DemonProgressionHelper#tryPurchaseBda} and can
 * be upgraded in tiers for stronger forms.
 *
 * <p>{@link #NONE} represents no equipped BDA and is always considered "unlocked."
 *
 * @see DemonForms
 * @see com.wassuples.demonslayer.demon.PlayerDemon
 */
public enum BloodDemonArts {
    NONE("None", "Unequip your Blood Demon Art."),
    CRESCENT("Crescent Moon Blades", "Originally Kokushibo's Blood Demon Art, the Upper Moon One."),
    CRYO("Cryokinesis", "Originally Doma's Blood Demon Art, the Upper Moon Two."),
    DESTRUCTIVE_DEATH("Destructive Death", "Originally Akaza's Blood Demon Art, the Upper Moon Three."),
    EMOTION("Emotion Manifestation", "Originally Hantengu's Blood Demon Art, the Upper Moon Four."),
    FISH("Fish Scale Attacks", "Originally Gyokko's Blood Demon Art, the Upper Moon Five."),
    SASH("Obi Sash Manipulation", "Originally  Daki's Blood Demon Art, the Upper Moon Six."),
    HAEMO("Haemokinesis", "Originally Gyutaro's Blood Demon Art, the Upper Moon Six."),
    SLEEP("Sleep Manipulation", "Originally Enmu's Blood Demon Art, the Lower Moon One."),
    THREADS("Thread Manipulation", "Originally Rui's Blood Demon Art, the Lower Moon Five."),
    TOON_FORCE("Toon Force", "Cartoon physics haha go BRRRRRRRRRRR.");

    private final String displayName;
    private final String description;

    BloodDemonArts(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    public String getDisplayName() {
        return this.displayName;
    }
    /**
     * True for arts from the franchise itself (Upper/Lower Moons), false for the mod's own creations such as Toon
     * Force. The Arts page lists the two groups under separate headings.
     */
    public boolean isCanon() {
        return this != TOON_FORCE && this != NONE;
    }

    public Component getDescription() {
        return Component.literal(this.description);
    }
}

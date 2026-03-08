package com.wassuples.demonslayer.demon;

/**
 * Enumerates purchasable demon passive abilities.
 *
 * <p>Each passive has a maximum tier and provides scaling bonuses per tier. Passives are
 * purchased with Blood Points through {@link DemonProgressionHelper#tryUnlockPassive} and
 * can be toggled on/off via {@link PlayerDemon#setPassiveActive}.
 *
 * @see DemonProgressionHelper#PASSIVE_SKILL_COSTS
 * @see PlayerDemon#getPassiveTier(DemonPassive)
 */
public enum DemonPassive {
    HYPER_REGENERATION("Hyper Regeneration", "Increases passive health regeneration.", 5),
    TOUGHENED_HIDE("Toughened Hide", "Grants passive damage resistance.", 5),
    DEMONIC_STRENGTH("Demonic Strength", "Increases base melee damage.", 5),
    ENHANCED_VIGOR("Enhanced Vigor", "Increases maximum health.", 10),
    SUNLIGHT_RESISTANCE("Sunlight Resistance", "Allows temporary survival in sunlight.", 3),
    STALKERS_GAIT("Stalker's Gait", "Your footsteps are silenced, making you harder to detect.", 1),
    BLOOD_ART_POTENCY("Blood Art Potency", "Reduces the cost and increases the damage of Blood Demon Arts.", 5),
    OVERWHELMING_AURA("Overwhelming Aura", "Focus on a target to inflict them with debilitating weakness and slowness.", 3);

    private final String displayName;
    private final String description;
    private final int maxTier;

    DemonPassive(String displayName, String description, int maxTier) {
        this.displayName = displayName;
        this.description = description;
        this.maxTier = maxTier;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMaxTier() { return maxTier; }
}

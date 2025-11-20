package com.wassuples.demonslayer.nemesis.trait;

public enum Quirk {
    // Offensive Strengths
    HEAVY_HITTER("Heavy Hitter", "Melee attacks are stronger and have increased knockback."),
    BLOOD_SIPHON("Blood Siphon", "Heals slightly upon dealing damage to players."),
    SHOCKWAVE_STOMP("Shockwave Stomp", "Occasionally stomps, creating a damaging area-of-effect shockwave."),
    VENOMOUS("Venomous", "Attacks have a chance to inflict poison."),
    EXECUTIONER("Executioner", "Deals significantly more damage to targets below 25% health."),

    // Defensive Strengths
    FIRE_RESISTANT("Fire Resistant", "Takes reduced damage from fire."),
    FROST_BLOODED("Frost-Blooded", "Takes reduced damage from freezing and slowing effects."),
    ARROW_PROOF("Arrow-Proof", "Takes significantly reduced damage from projectiles."),
    TENACIOUS("Tenacious", "Gains temporary resistance when at low health."),
    UNSHAKABLE("Unshakable", "Has a high resistance to being knocked back."),
    RAPID_RECOVERY("Rapid Recovery", "Recovers from negative status effects like slowness and weakness more quickly."),
    DEATH_DEFIANT("Death Defiant", "Upon taking fatal damage, survives with 1 HP and gains a short burst of strength (once per fight)."),

    // Mobility and Other
    NIMBLE("Nimble", "Moves faster than others of its kind."),
    NIGHT_STALKER("Night Stalker", "Moves even faster and is harder to see at night."),
    KEEN_Scent("Keen Scent", "Can detect and track players from a greater distance."),
    PACK_CALLER("Pack Caller", "Has a chance to summon weaker demons when it takes damage."),

    // Elemental Weaknesses
    FEAR_OF_WATER("Fear of Water", "Becomes slowed and weakened near or in water."),
    BRITTLE("Brittle", "Takes extra damage from axe-based and blunt attacks."),
    SUNLIGHT_SICKNESS("Sunlight Sickness", "Takes damage in the sun much faster than normal demons."),
    FLAMMABLE_FLESH("Flammable Flesh", "Takes extra damage from fire and burns for longer."),
    PURIFIED_WEAKNESS("Purified Weakness", "Takes extra damage from unique 'Slayer' abilities or items."),

    //  Combat Weaknesses
    OVERCONFIDENT("Overconfident", "Has a longer cooldown after missing a powerful attack."),
    GLASS_CANNON("Glass Cannon", "Deals high damage but has significantly less health or armor."),
    WIND_UP("Wind-Up", "Its most powerful attacks have a very obvious and slow telegraph."),
    BLIND_RAGE("Blind Rage", "When heavily damaged, it attacks faster but its accuracy decreases."),
    OBSESSIVE_HUNTER("Obsessive Hunter", "Becomes fixated on the first player who damages it, ignoring all others."),

    // Sensory Weaknesses
    SOUND_SENSITIVE("Sound Sensitive", "Becomes stunned or disoriented by loud noises like explosions."),
    MAGPIE("Magpie", "Can be momentarily distracted by shiny items dropped on the ground."),
    EASILY_TAUNTED("Easily Taunted", "Will aggressively switch targets to a player who uses a taunt emote."),

    // Statuses
    DISHONORED("Dishonored", "Seen as an illegitimate rank-holder, making them a prime target for challengers.")
    ;

    private final String displayName;
    private final String description;

    Quirk(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return this.displayName; }
    public String getDescription() { return this.description; }
}
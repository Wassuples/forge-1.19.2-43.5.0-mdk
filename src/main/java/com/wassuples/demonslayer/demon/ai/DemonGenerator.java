package com.wassuples.demonslayer.demon.ai;

public class DemonGenerator {
    generateName()
    generateTitle()
    generateBehavior()
    generatePersonality()

    public static void initializeDemon(NemesisDemonEntity demon) {
        get(DemonTrait);
        populate(allFields);
        registerInDemonHierarchy(this.demon);
        // Call this when the demonEntity is first spawned in.
    }
}

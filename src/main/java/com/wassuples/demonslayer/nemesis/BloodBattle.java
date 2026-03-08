package com.wassuples.demonslayer.nemesis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an active Blood Battle — a formal 1v1 duel between a demon-path player
 * (challenger) and a Nemesis demon (defender) for rank usurpation.
 *
 * <p>Tracks any third-party {@link #interferers} whose involvement taints the victory,
 * resulting in the "Dishonored" flag on the winner.
 *
 * @see DemonHierarchyManager#startBloodBattle
 * @see NemesisSystemEvents#handleBloodBattleConclusion
 */
public class BloodBattle {
    public final UUID challenger;
    public final UUID defender;
    public final Set<UUID> interferers = new HashSet<>();

    public BloodBattle(UUID challenger, UUID defender) {
        this.challenger = challenger;
        this.defender = defender;
    }
}

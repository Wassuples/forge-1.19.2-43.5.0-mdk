package com.wassuples.demonslayer.nemesis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BloodBattle {
    public final UUID challenger;
    public final UUID defender;
    public final Set<UUID> interferers = new HashSet<>();

    public BloodBattle(UUID challenger, UUID defender) {
        this.challenger = challenger;
        this.defender = defender;
    }
}
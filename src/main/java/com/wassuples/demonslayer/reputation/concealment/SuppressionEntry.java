package com.wassuples.demonslayer.reputation.concealment;

import com.wassuples.demonslayer.reputation.witness.WitnessType;

public record SuppressionEntry(WitnessType targetType, float strength, long expiryTick) {
    public boolean isActive(long gameTime) {
        return gameTime < expiryTick;
    }
}


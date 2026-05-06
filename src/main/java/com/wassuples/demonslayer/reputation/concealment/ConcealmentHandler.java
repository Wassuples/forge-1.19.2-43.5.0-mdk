package com.wassuples.demonslayer.reputation.concealment;

import com.wassuples.demonslayer.reputation.witness.WitnessType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ConcealmentHandler {
    private ConcealmentHandler() {
    }

    public static void pruneExpired(List<SuppressionEntry> entries, long gameTime) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Iterator<SuppressionEntry> it = entries.iterator();
        while (it.hasNext()) {
            if (!it.next().isActive(gameTime)) {
                it.remove();
            }
        }
    }

    public static boolean isSuppressed(List<SuppressionEntry> entries, WitnessType witnessType, long gameTime) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (SuppressionEntry entry : entries) {
            if (entry.targetType() == witnessType && entry.isActive(gameTime) && entry.strength() >= 1.0f) {
                return true;
            }
        }
        return false;
    }

    public static List<SuppressionEntry> addSuppression(List<SuppressionEntry> existing, SuppressionEntry entry) {
        List<SuppressionEntry> out = existing == null ? new ArrayList<>() : existing;
        out.add(entry);
        return out;
    }
}


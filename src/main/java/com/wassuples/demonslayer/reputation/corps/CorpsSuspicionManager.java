package com.wassuples.demonslayer.reputation.corps;

import com.wassuples.demonslayer.reputation.PlayerRepRecord;

public final class CorpsSuspicionManager {
    public static final int INVESTIGATION_THRESHOLD = 500;

    private CorpsSuspicionManager() {
    }

    public static boolean shouldTriggerInvestigation(PlayerRepRecord record) {
        return record.getSuspicion() >= INVESTIGATION_THRESHOLD;
    }
}


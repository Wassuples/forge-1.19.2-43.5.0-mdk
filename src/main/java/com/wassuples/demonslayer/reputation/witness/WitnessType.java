package com.wassuples.demonslayer.reputation.witness;

public enum WitnessType {
    CROW(true, false, false, false),
    VILLAGER(true, false, false, false),
    DEMON_SLAYER(true, false, false, false),
    DEMON_ALIVE(false, false, false, true),
    DEMON_DYING(false, false, true, false);

    private final boolean reportsToDshq;
    private final boolean reportsToTanjiro;
    private final boolean reportsToMuzan;
    private final boolean reportsToDemonWorld;

    WitnessType(boolean reportsToDshq, boolean reportsToTanjiro, boolean reportsToMuzan, boolean reportsToDemonWorld) {
        this.reportsToDshq = reportsToDshq;
        this.reportsToTanjiro = reportsToTanjiro;
        this.reportsToMuzan = reportsToMuzan;
        this.reportsToDemonWorld = reportsToDemonWorld;
    }

    public boolean reportsToDshq() {
        return reportsToDshq;
    }

    public boolean reportsToTanjiro() {
        return reportsToTanjiro;
    }

    public boolean reportsToMuzan() {
        return reportsToMuzan;
    }

    public boolean reportsToDemonWorld() {
        return reportsToDemonWorld;
    }
}


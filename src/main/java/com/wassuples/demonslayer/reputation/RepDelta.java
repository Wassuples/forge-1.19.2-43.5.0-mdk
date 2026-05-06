package com.wassuples.demonslayer.reputation;

public record RepDelta(int corps, int tanjiro, int muzan, int demon, int suspicion) {
    public static final RepDelta ZERO = new RepDelta(0, 0, 0, 0, 0);

    public RepDelta add(RepDelta other) {
        return new RepDelta(
                corps + other.corps,
                tanjiro + other.tanjiro,
                muzan + other.muzan,
                demon + other.demon,
                suspicion + other.suspicion
        );
    }

    public RepDelta withFactionOnly(Faction faction) {
        return switch (faction) {
            case DSHQ -> new RepDelta(corps, 0, 0, 0, suspicion);
            case TANJIRO_CREW -> new RepDelta(0, tanjiro, 0, 0, 0);
            case MUZAN -> new RepDelta(0, 0, muzan, 0, 0);
            case DEMON_WORLD -> new RepDelta(0, 0, 0, demon, 0);
        };
    }
}


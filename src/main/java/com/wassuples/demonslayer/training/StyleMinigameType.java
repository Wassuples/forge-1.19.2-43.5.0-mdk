package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.styles.BreathingStyles;

/**
 * Maps each {@link BreathingStyles} to its canonical minigame mechanic.
 *
 * <p>The minigame is style-specific, not master-specific — every master for a given style
 * runs the same game. It is used both for the initial style unlock and for subsequent
 * training (mastery gain on individual forms).
 *
 * <p>Failure consequences:
 * <ul>
 *   <li>{@link #FLAME} and {@link #STONE} apply a one in-game day cooldown on all lives lost.</li>
 *   <li>{@link #FLAME} and {@link #SERPENT} grant three lives per attempt.</li>
 *   <li>All other styles retry immediately with no penalty.</li>
 * </ul>
 */
public enum StyleMinigameType {

    /** Water – Flow &amp; Continuity: chain-click shrinking targets along a flowing path. */
    WATER(BreathingStyles.WATER,   "Flow & Continuity",       FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Flame – Burst Timing: release the attack key at charge peak, 3 lives. */
    FLAME(BreathingStyles.FLAME,   "Burst Timing",             FailureConsequence.COOLDOWN_ONE_DAY,  3),

    /** Thunder – Reaction Speed: single flash target, strike within a shrinking window. */
    THUNDER(BreathingStyles.THUNDER, "Reaction Speed",         FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Mist – Pattern Recognition: memorise and reproduce a directional movement sequence. */
    MIST(BreathingStyles.MIST,     "Pattern Recognition",      FailureConsequence.RETRY_AFTER_200T,  0),

    /** Wind – Sustained Pressure: track an erratic moving target; too many misses fails. */
    WIND(BreathingStyles.WIND,     "Sustained Pressure",       FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Stone – Endurance &amp; Resolve: maintain minimum CPS for an escalating duration. */
    STONE(BreathingStyles.STONE,   "Endurance & Resolve",      FailureConsequence.COOLDOWN_ONE_DAY,  0),

    /** Insect – Precision Timing: strike multiple small targets in briefly revealed order. */
    INSECT(BreathingStyles.INSECT, "Precision Timing",         FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Sound – Rhythm: hit inputs on beat; full-miss streaks drop score below threshold. */
    SOUND(BreathingStyles.SOUND,   "Rhythm",                   FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Love – Momentum &amp; Escalation: ever-growing combo chain; break resets the attempt. */
    LOVE(BreathingStyles.LOVE,     "Momentum & Escalation",    FailureConsequence.RETRY_IMMEDIATELY, 0),

    /** Serpent – Misdirection: identify the real target among identical decoys, 3 lives. */
    SERPENT(BreathingStyles.SERPENT, "Misdirection",           FailureConsequence.RETRY_IMMEDIATELY, 3);

    // ─── per-type metadata ────────────────────────────────────────────────────

    private final BreathingStyles style;
    private final String mechanicName;
    private final FailureConsequence failureConsequence;
    /** Starting lives (0 = unlimited / retry-on-fail). */
    private final int startingLives;

    StyleMinigameType(BreathingStyles style, String mechanicName,
                      FailureConsequence failureConsequence, int startingLives) {
        this.style = style;
        this.mechanicName = mechanicName;
        this.failureConsequence = failureConsequence;
        this.startingLives = startingLives;
    }

    public BreathingStyles getStyle()                        { return style; }
    public String getMechanicName()                          { return mechanicName; }
    public FailureConsequence getFailureConsequence()        { return failureConsequence; }
    /** Starting lives, or 0 if the style uses infinite retries. */
    public int getStartingLives()                            { return startingLives; }
    public boolean hasLives()                                { return startingLives > 0; }

    /** Resolves the minigame type for the given style. */
    public static StyleMinigameType forStyle(BreathingStyles style) {
        for (StyleMinigameType t : values()) {
            if (t.style == style) return t;
        }
        throw new IllegalArgumentException("No minigame registered for style: " + style);
    }

    // ─── failure consequence ──────────────────────────────────────────────────

    public enum FailureConsequence {
        /** Player can retry immediately with no penalty. */
        RETRY_IMMEDIATELY,
        /** Retry only after 200 server ticks (~10 s). */
        RETRY_AFTER_200T,
        /** Must wait one in-game day (24 000 ticks) before retrying. */
        COOLDOWN_ONE_DAY
    }
}

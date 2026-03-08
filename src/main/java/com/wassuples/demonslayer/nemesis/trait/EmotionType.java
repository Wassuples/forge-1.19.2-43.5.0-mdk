package com.wassuples.demonslayer.nemesis.trait;

/**
 * Represents a <b>feeling</b> a demon can develop over time, derived from
 * factual {@link MemoryType} events but stored independently.
 * <p>
 * Each emotion has:
 * <ul>
 *   <li>{@link #defaultCap} — maximum intensity (0–1 scale).</li>
 *   <li>{@link #baseGainRate} — intensity gained per triggering event before
 *       personality modifiers.</li>
 *   <li>{@link #decayPerDay} — passive intensity lost each in-game day
 *       without reinforcement (0 = permanent).</li>
 * </ul>
 *
 * @see EmotionState
 */
public enum EmotionType {

    // ── Fears (style-linked) ────────────────────────────────────────────
    FEAR_FIRE       (1.0f, 0.15f, 0.02f),
    FEAR_WATER      (1.0f, 0.15f, 0.02f),
    FEAR_THUNDER    (1.0f, 0.15f, 0.02f),
    FEAR_WIND       (1.0f, 0.15f, 0.02f),
    FEAR_BEAST      (1.0f, 0.12f, 0.02f),
    FEAR_INSECT     (1.0f, 0.12f, 0.02f),
    FEAR_SUN        (1.0f, 0.20f, 0.01f),
    FEAR_EXPLOSION  (1.0f, 0.15f, 0.02f),
    FEAR_POISON     (1.0f, 0.12f, 0.02f),

    // ── Hates (style-linked) ────────────────────────────────────────────
    HATE_FLAME_STYLE   (1.0f, 0.12f, 0.01f),
    HATE_WATER_STYLE   (1.0f, 0.12f, 0.01f),
    HATE_THUNDER_STYLE (1.0f, 0.12f, 0.01f),
    HATE_WIND_STYLE    (1.0f, 0.12f, 0.01f),
    HATE_BEAST_STYLE   (1.0f, 0.10f, 0.01f),
    HATE_INSECT_STYLE  (1.0f, 0.10f, 0.01f),
    HATE_SUN_STYLE     (1.0f, 0.15f, 0.01f),

    // ── Player-directed emotions ────────────────────────────────────────
    RESPECT_PLAYER  (1.0f, 0.08f, 0.005f),
    CONTEMPT_PLAYER (1.0f, 0.10f, 0.01f),
    FEAR_PLAYER     (1.0f, 0.12f, 0.02f),
    HATE_PLAYER     (1.0f, 0.10f, 0.01f),

    // ── Demon-directed emotions ─────────────────────────────────────────
    RIVALRY_GRUDGE  (1.0f, 0.10f, 0.005f),

    // ── General emotional states ────────────────────────────────────────
    BLOODLUST       (1.0f, 0.08f, 0.03f),
    DESPERATION     (1.0f, 0.20f, 0.05f);

    // ── Enum fields ─────────────────────────────────────────────────────

    /** Maximum intensity this emotion can reach (0–1). */
    public final float defaultCap;

    /** Base intensity gained per triggering memory before personality modifiers. */
    public final float baseGainRate;

    /** Intensity lost passively each in-game day (24 000 ticks). 0 = permanent. */
    public final float decayPerDay;

    EmotionType(float defaultCap, float baseGainRate, float decayPerDay) {
        this.defaultCap = defaultCap;
        this.baseGainRate = baseGainRate;
        this.decayPerDay = decayPerDay;
    }
}


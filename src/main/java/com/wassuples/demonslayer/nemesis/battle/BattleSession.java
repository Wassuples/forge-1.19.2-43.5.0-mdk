package com.wassuples.demonslayer.nemesis.battle;

import java.util.UUID;

/**
 * Transient, in-memory tracker for a single ongoing battle between a
 * demon and an opponent (player or another demon).
 * <p>
 * <b>Not persisted.</b> Only the derived outcome is written
 * as a {@link com.wassuples.demonslayer.nemesis.trait.DemonMemory} fact.
 */
public class BattleSession {

    /** If no damage is exchanged for this many ticks, the session expires. */
    public static final int SESSION_TIMEOUT_TICKS = 600; // 30 seconds

    private final UUID demonId;
    private final UUID opponentId;
    private final long startTime;
    /** True if the opponent is a player rather than another demon. */
    private final boolean opponentIsPlayer;

    private long lastHitTime;
    private float damageDealt;       // damage the DEMON dealt to the opponent
    private float damageTaken;       // damage the DEMON received from the opponent
    private float demonMinHealthPct = 1.0f; // lowest health % the demon reached
    private float opponentMinHealthPct = 1.0f;
    private boolean demonDied;
    private boolean opponentDied;
    private boolean demonFled;

    public BattleSession(UUID demonId, UUID opponentId, long startTime, boolean opponentIsPlayer) {
        this.demonId = demonId;
        this.opponentId = opponentId;
        this.startTime = startTime;
        this.lastHitTime = startTime;
        this.opponentIsPlayer = opponentIsPlayer;
    }

    // ── Recording events ────────────────────────────────────────────────

    /**
     * Records damage dealt or taken by the demon.
     *
     * @param amount          positive damage amount
     * @param demonDealtIt    true if the demon dealt the damage, false if the demon received it
     * @param demonHealthPct  demon's health percentage after this event (0–1)
     * @param opponentHealthPct opponent's health percentage after this event (0–1)
     * @param currentTime     world tick
     */
    public void recordDamage(float amount, boolean demonDealtIt,
                             float demonHealthPct, float opponentHealthPct,
                             long currentTime) {
        this.lastHitTime = currentTime;
        if (demonDealtIt) {
            this.damageDealt += amount;
        } else {
            this.damageTaken += amount;
        }
        if (demonHealthPct < this.demonMinHealthPct) {
            this.demonMinHealthPct = demonHealthPct;
        }
        if (opponentHealthPct < this.opponentMinHealthPct) {
            this.opponentMinHealthPct = opponentHealthPct;
        }
    }

    public void markDemonDied()   { this.demonDied = true; }
    public void markOpponentDied(){ this.opponentDied = true; }
    public void markDemonFled()   { this.demonFled = true; }

    // ── Queries ─────────────────────────────────────────────────────────

    public boolean isExpired(long currentTime) {
        return currentTime - lastHitTime > SESSION_TIMEOUT_TICKS;
    }

    public long getDuration(long currentTime) {
        return currentTime - startTime;
    }

    public boolean isResolved() {
        return demonDied || opponentDied || demonFled;
    }

    // ── Getters ─────────────────────────────────────────────────────────

    public UUID getDemonId()             { return demonId; }
    public UUID getOpponentId()          { return opponentId; }
    public long getStartTime()           { return startTime; }
    public long getLastHitTime()         { return lastHitTime; }
    public boolean isOpponentIsPlayer()  { return opponentIsPlayer; }
    public float getDamageDealt()        { return damageDealt; }
    public float getDamageTaken()        { return damageTaken; }
    public float getDemonMinHealthPct()  { return demonMinHealthPct; }
    public float getOpponentMinHealthPct() { return opponentMinHealthPct; }
    public boolean isDemonDied()         { return demonDied; }
    public boolean isOpponentDied()      { return opponentDied; }
    public boolean isDemonFled()         { return demonFled; }
}


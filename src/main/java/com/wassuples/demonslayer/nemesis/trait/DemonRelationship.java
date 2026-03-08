package com.wassuples.demonslayer.nemesis.trait;

import net.minecraft.nbt.CompoundTag;

/**
 * Tracks a directional relationship from one demon toward another.
 * Stored per-demon in a {@code Map<UUID, DemonRelationship>}.
 * <p>
 * Covers both rivalries (G2) and persistent intel knowledge (G4).
 */
public class DemonRelationship {

    /** How intense the rivalry is. 0 = neutral, 1.0 = blood feud. */
    private float rivalryIntensity;

    /** Positive = allied, negative = hostile beyond rivalry. 0 = neutral. */
    private float allianceStrength;

    /** How many times these two demons have met in battle. */
    private int encounterCount;

    /** World time of the last conflict between these two. */
    private long lastConflictTime;

    /**
     * Per-matchup intel level (G4). Range [0, 1].
     * 0 = knows nothing, 1 = full knowledge of quirks/stats/BDA.
     * Grows after encounters, decays daily.
     */
    private float intelOnTarget;

    // ── Getters ─────────────────────────────────────────────────────────

    public float getRivalryIntensity()  { return rivalryIntensity; }
    public float getAllianceStrength()   { return allianceStrength; }
    public int getEncounterCount()      { return encounterCount; }
    public long getLastConflictTime()   { return lastConflictTime; }
    public float getIntelOnTarget()     { return intelOnTarget; }

    // ── Mutators ────────────────────────────────────────────────────────

    public void addRivalry(float amount) {
        this.rivalryIntensity = Math.min(1.0f, Math.max(0.0f, this.rivalryIntensity + amount));
    }

    public void addAlliance(float amount) {
        this.allianceStrength = Math.min(1.0f, Math.max(-1.0f, this.allianceStrength + amount));
    }

    public void recordEncounter(long time) {
        this.encounterCount++;
        this.lastConflictTime = time;
    }

    public void addIntel(float amount) {
        this.intelOnTarget = Math.min(1.0f, Math.max(0.0f, this.intelOnTarget + amount));
    }

    /**
     * Applies daily decay to intel knowledge. Called once per simulation day.
     */
    public void decayIntel(float decayAmount) {
        this.intelOnTarget = Math.max(0.0f, this.intelOnTarget - decayAmount);
    }

    /**
     * Applies daily decay to rivalry if no recent encounters.
     */
    public void decayRivalry(float decayAmount) {
        this.rivalryIntensity = Math.max(0.0f, this.rivalryIntensity - decayAmount);
    }

    /**
     * Returns true if this relationship has any meaningful data worth persisting.
     */
    public boolean isSignificant() {
        return rivalryIntensity > 0.01f
                || Math.abs(allianceStrength) > 0.01f
                || encounterCount > 0
                || intelOnTarget > 0.01f;
    }

    // ── NBT Persistence ─────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("rivalryIntensity", rivalryIntensity);
        nbt.putFloat("allianceStrength", allianceStrength);
        nbt.putInt("encounterCount", encounterCount);
        nbt.putLong("lastConflictTime", lastConflictTime);
        nbt.putFloat("intelOnTarget", intelOnTarget);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.rivalryIntensity = nbt.getFloat("rivalryIntensity");
        this.allianceStrength = nbt.getFloat("allianceStrength");
        this.encounterCount   = nbt.getInt("encounterCount");
        this.lastConflictTime = nbt.getLong("lastConflictTime");
        this.intelOnTarget    = nbt.getFloat("intelOnTarget");
    }

    public void copyFrom(DemonRelationship other) {
        this.rivalryIntensity = other.rivalryIntensity;
        this.allianceStrength = other.allianceStrength;
        this.encounterCount   = other.encounterCount;
        this.lastConflictTime = other.lastConflictTime;
        this.intelOnTarget    = other.intelOnTarget;
    }
}


package com.wassuples.demonslayer.nemesis.trait;

import net.minecraft.nbt.CompoundTag;

/**
 * Persistent combat statistics for a single demon, used as inputs to the
 * growth system. Updated after every battle session (both live and offscreen).
 * <p>
 * All counters are monotonically increasing (except {@link #currentWinStreak}
 * which resets on loss) and default to zero for backward compatibility.
 *
 * @see com.wassuples.demonslayer.nemesis.growth.GrowthCalculator
 */
public class GrowthStats {

    // ── Counters ────────────────────────────────────────────────────────

    private int battleCount;
    private int wins;
    private int losses;
    private int escapes;

    /** Longest consecutive win streak ever achieved. */
    private int bestWinStreak;
    /** Current consecutive win streak (resets to 0 on loss/escape). */
    private int currentWinStreak;

    private float damageDealtTotal;
    private float damageTakenTotal;

    private int playersKilled;
    private int demonsKilled;
    private int bloodConsumedTotal;
    private int specialEventCount;

    /**
     * Tracks how many battles have already been processed by the growth
     * calculator. Prevents double-applying growth on the same fight.
     */
    private int lastGrowthBattleCount;

    /**
     * Stat points granted today (reset each simulation day to enforce a
     * per-day growth budget).
     */
    private int growthPointsToday;

    // ── Getters ─────────────────────────────────────────────────────────

    public int getBattleCount()       { return battleCount; }
    public int getWins()              { return wins; }
    public int getLosses()            { return losses; }
    public int getEscapes()           { return escapes; }
    public int getBestWinStreak()     { return bestWinStreak; }
    public int getCurrentWinStreak()  { return currentWinStreak; }
    public float getDamageDealtTotal(){ return damageDealtTotal; }
    public float getDamageTakenTotal(){ return damageTakenTotal; }
    public int getPlayersKilled()     { return playersKilled; }
    public int getDemonsKilled()      { return demonsKilled; }
    public int getBloodConsumedTotal(){ return bloodConsumedTotal; }
    public int getSpecialEventCount() { return specialEventCount; }
    public int getLastGrowthBattleCount() { return lastGrowthBattleCount; }
    public int getGrowthPointsToday() { return growthPointsToday; }

    // ── Mutators ────────────────────────────────────────────────────────

    public void recordBattle() { this.battleCount++; }

    public void recordWin() {
        this.wins++;
        this.currentWinStreak++;
        if (this.currentWinStreak > this.bestWinStreak) {
            this.bestWinStreak = this.currentWinStreak;
        }
    }

    public void recordLoss() {
        this.losses++;
        this.currentWinStreak = 0;
    }

    public void recordEscape() {
        this.escapes++;
        this.currentWinStreak = 0;
    }

    public void addDamageDealt(float amount) { this.damageDealtTotal += amount; }
    public void addDamageTaken(float amount) { this.damageTakenTotal += amount; }
    public void incrementPlayersKilled()     { this.playersKilled++; }
    public void incrementDemonsKilled()      { this.demonsKilled++; }
    public void addBloodConsumed(int amount) { this.bloodConsumedTotal += amount; }
    public void incrementSpecialEvent()      { this.specialEventCount++; }

    public void setLastGrowthBattleCount(int count) { this.lastGrowthBattleCount = count; }
    public void addGrowthPointsToday(int points)    { this.growthPointsToday += points; }
    public void resetDailyGrowthBudget()             { this.growthPointsToday = 0; }

    // ── NBT Persistence ─────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("battleCount", battleCount);
        nbt.putInt("wins", wins);
        nbt.putInt("losses", losses);
        nbt.putInt("escapes", escapes);
        nbt.putInt("bestWinStreak", bestWinStreak);
        nbt.putInt("currentWinStreak", currentWinStreak);
        nbt.putFloat("damageDealtTotal", damageDealtTotal);
        nbt.putFloat("damageTakenTotal", damageTakenTotal);
        nbt.putInt("playersKilled", playersKilled);
        nbt.putInt("demonsKilled", demonsKilled);
        nbt.putInt("bloodConsumedTotal", bloodConsumedTotal);
        nbt.putInt("specialEventCount", specialEventCount);
        nbt.putInt("lastGrowthBattleCount", lastGrowthBattleCount);
        nbt.putInt("growthPointsToday", growthPointsToday);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.battleCount        = nbt.getInt("battleCount");
        this.wins               = nbt.getInt("wins");
        this.losses             = nbt.getInt("losses");
        this.escapes            = nbt.getInt("escapes");
        this.bestWinStreak      = nbt.getInt("bestWinStreak");
        this.currentWinStreak   = nbt.getInt("currentWinStreak");
        this.damageDealtTotal   = nbt.getFloat("damageDealtTotal");
        this.damageTakenTotal   = nbt.getFloat("damageTakenTotal");
        this.playersKilled      = nbt.getInt("playersKilled");
        this.demonsKilled       = nbt.getInt("demonsKilled");
        this.bloodConsumedTotal = nbt.getInt("bloodConsumedTotal");
        this.specialEventCount  = nbt.getInt("specialEventCount");
        this.lastGrowthBattleCount = nbt.getInt("lastGrowthBattleCount");
        this.growthPointsToday  = nbt.getInt("growthPointsToday");
    }

    /**
     * Deep-copy all stats from another instance.
     */
    public void copyFrom(GrowthStats other) {
        this.battleCount        = other.battleCount;
        this.wins               = other.wins;
        this.losses             = other.losses;
        this.escapes            = other.escapes;
        this.bestWinStreak      = other.bestWinStreak;
        this.currentWinStreak   = other.currentWinStreak;
        this.damageDealtTotal   = other.damageDealtTotal;
        this.damageTakenTotal   = other.damageTakenTotal;
        this.playersKilled      = other.playersKilled;
        this.demonsKilled       = other.demonsKilled;
        this.bloodConsumedTotal = other.bloodConsumedTotal;
        this.specialEventCount  = other.specialEventCount;
        this.lastGrowthBattleCount = other.lastGrowthBattleCount;
        this.growthPointsToday  = other.growthPointsToday;
    }

    @Override
    public String toString() {
        return "GrowthStats{battles=" + battleCount +
                ", W/L/E=" + wins + "/" + losses + "/" + escapes +
                ", streak=" + currentWinStreak + "(best:" + bestWinStreak + ")" +
                ", dmgDealt=" + String.format("%.1f", damageDealtTotal) +
                ", dmgTaken=" + String.format("%.1f", damageTakenTotal) +
                ", pKills=" + playersKilled + ", dKills=" + demonsKilled +
                "}";
    }
}


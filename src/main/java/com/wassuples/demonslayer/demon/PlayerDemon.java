package com.wassuples.demonslayer.demon;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public class PlayerDemon {
    private boolean isDemon = false;
    private boolean isOvercomer = false;
    private DemonRank rank = DemonRank.FODDER;
    private int bloodPoints = 0;

    private static final Map<DemonRank, Integer> RANK_UP_COSTS = new ImmutableMap.Builder<DemonRank, Integer>()
            .put(DemonRank.FODDER, 50)
            .put(DemonRank.LOWLY_DEMON, 100)
            .put(DemonRank.DEMON, 250)
            .put(DemonRank.GREATER_DEMON, 500)
            .put(DemonRank.ELITE_DEMON, 1000)
            .build();

    public boolean isDemon() {
        return isDemon;
    }
    public boolean isOvercomer() { return isOvercomer; }
    public void setDemon(boolean demon) {
        isDemon = demon;
    }
    public void setBloodPoints(int amount) { this.bloodPoints = amount; }
    public DemonRank getRank() {
        return rank;
    }
    public void setRank(DemonRank rank) {
        this.rank = rank;
    }
    public int getBloodPoints() {
        return bloodPoints;
    }

    public void addBloodPoints(int amount) {
        if(!isDemon) return;
        this.bloodPoints += amount;
        checkRankUp();
    }
    public void checkRankUp() {
        if (rank.ordinal() >= DemonRank.ELITE_DEMON.ordinal()) {
            return;
        }

        Integer cost = RANK_UP_COSTS.get(this.rank);
        if(cost != null && this.bloodPoints >= cost) {
            this.bloodPoints -= cost;
            this.rank = DemonRank.values()[this.rank.ordinal() + 1];
            checkRankUp();
        }
    }

    public void copyFrom(PlayerDemon source) {
        this.isDemon = source.isDemon;
        this.rank = source.rank;
        this.bloodPoints = source.bloodPoints;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("isDemon", this.isDemon);
        nbt.putString("demonRank", this.rank.name());
        nbt.putInt("bloodPoints", this.bloodPoints);
    }

    public void loadNBTData(CompoundTag nbt) {
        this.isDemon = nbt.getBoolean("isDemon");
        if (nbt.contains("demonRank")) {
            try {
                this.rank = DemonRank.valueOf(nbt.getString("demonRank"));
            } catch (IllegalArgumentException e) {
                this.rank = DemonRank.FODDER;
            }
        }
        this.bloodPoints = nbt.getInt("bloodPoints");
    }
}
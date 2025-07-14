package com.wassuples.demonslayer.demon;

import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerDemon {
    private boolean isDemon = false;
    private boolean isOvercomer = false;
    private DemonRank rank = DemonRank.FODDER;
    private int bloodPoints = 0;
    private BloodDemonArts equippedBda = BloodDemonArts.NONE;
    private boolean bdaModeActive = false;

    private DemonForms currentDemonForm = DemonForms.THREADS_1;

    private static final Map<DemonRank, Integer> RANK_UP_COSTS = new ImmutableMap.Builder<DemonRank, Integer>()
            .put(DemonRank.FODDER, 50)
            .put(DemonRank.LOWLY_DEMON, 100)
            .put(DemonRank.DEMON, 250)
            .put(DemonRank.GREATER_DEMON, 500)
            .put(DemonRank.ELITE_DEMON, 1000)
            .build();

    public void cycleDemonForm() {
        if (this.equippedBda == BloodDemonArts.NONE) return;

        List<DemonForms> availableForms = Arrays.stream(DemonForms.values())
                .filter(form -> form.getStyle() == this.equippedBda)
                .collect(Collectors.toList());

        if (availableForms.isEmpty()) return;

        int currentIndex = availableForms.indexOf(this.currentDemonForm);
        int nextIndex = (currentIndex + 1) % availableForms.size();
        this.currentDemonForm = availableForms.get(nextIndex);
    }

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
    public BloodDemonArts getEquippedBda() {
        return this.equippedBda;
    }
    public void setEquippedBda(BloodDemonArts bda) {
        this.equippedBda = bda;
    }
    public DemonForms getCurrentDemonForm() {
        return this.currentDemonForm;
    }
    public void setCurrentDemonForm(DemonForms form) {
        this.currentDemonForm = form;
    }

    public boolean isBdaModeActive() {
        return this.isDemon && this.bdaModeActive;
    }
    public void setBdaModeActive(boolean active) {
        if (this.isDemon) {
            this.bdaModeActive = active;
        } else {
            this.bdaModeActive = false;
        }
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
        this.equippedBda = source.equippedBda;
        this.bdaModeActive = source.bdaModeActive;
        this.currentDemonForm = source.currentDemonForm;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("isDemon", this.isDemon);
        nbt.putString("demonRank", this.rank.name());
        nbt.putInt("bloodPoints", this.bloodPoints);
        nbt.putString("equippedBda", this.equippedBda.name());
        nbt.putBoolean("bdaModeActive", this.bdaModeActive);
        nbt.putString("currentDemonForm", this.currentDemonForm.name());
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
        if (nbt.contains("equippedBda")) {
            try {
                this.equippedBda = BloodDemonArts.valueOf(nbt.getString("equippedBda"));
            } catch (IllegalArgumentException e) {
                this.equippedBda = BloodDemonArts.NONE;
            }
        }
        this.bdaModeActive = nbt.getBoolean("bdaModeActive");
        if (nbt.contains("currentDemonForm")) {
            try { this.currentDemonForm = DemonForms.valueOf(nbt.getString("currentDemonForm")); }
            catch (IllegalArgumentException e) { this.currentDemonForm = DemonForms.THREADS_1; }
        }
    }
}
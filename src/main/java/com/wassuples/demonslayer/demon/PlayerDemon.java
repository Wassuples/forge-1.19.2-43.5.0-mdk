package com.wassuples.demonslayer.demon;

import com.google.common.collect.ImmutableMap;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-authoritative capability data for a player on the <b>demon path</b>.
 *
 * <p>Tracks whether the player is a demon, their {@link DemonRank}, Blood Points currency,
 * equipped {@link BloodDemonArts}, unlocked BDAs and their tiers, passive abilities, and
 * the currently selected {@link DemonForms}.
 *
 * <h2>Persistence</h2>
 * <p>Serialized/deserialized via {@link #saveNBTData(CompoundTag)} / {@link #loadNBTData(CompoundTag)}.
 * State is copied on player death/respawn via {@link #copyFrom(PlayerDemon)}.
 *
 * <h2>Overcomer / Dishonored flags</h2>
 * <ul>
 *   <li><b>Overcomer</b> — a demon who has conquered sunlight weakness (narrative progression).</li>
 *   <li><b>Dishonored</b> — a demon stripped of rank privileges (penalty mechanic).</li>
 * </ul>
 *
 * @see DemonRank
 * @see DemonPassive
 * @see BloodDemonArts
 * @see PlayerDemonProvider
 */

/**
 * Player Demon.
 */
public class PlayerDemon {
    private boolean isDemon = false;
    private boolean isOvercomer = false;
    private boolean isDishonored = false;

    private DemonRank rank = DemonRank.FODDER;
    private int bloodPoints = 0;

    private BloodDemonArts equippedBda = BloodDemonArts.NONE;
    private boolean bdaModeActive = false;
    private DemonForms currentDemonForm = DemonForms.THREADS_1;

    private final Set<DemonPassive> activePassives = new HashSet<>();
    private final Map<DemonPassive, Integer> unlockedPassives = new HashMap<>();

    private final Map<BloodDemonArts, Integer> bdaTiers = new HashMap<>();
    private final Set<BloodDemonArts> unlockedBdas = new HashSet<>();

    @Nullable
    private UUID targetedEntityId = null;
    private int ticksStared = 0;
    private int sunGraceTicks = 0;

    public PlayerDemon() {
        Arrays.stream(DemonPassive.values()).forEach(activePassives::add);
    }

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

    public void previousDemonForm() {
        if (this.equippedBda == BloodDemonArts.NONE) return;

        List<DemonForms> availableForms = Arrays.stream(DemonForms.values())
                .filter(form -> form.getStyle() == this.equippedBda)
                .collect(Collectors.toList());

        if (availableForms.isEmpty()) return;

        int currentIndex = availableForms.indexOf(this.currentDemonForm);
        int previousIndex = (currentIndex - 1 + availableForms.size()) % availableForms.size();
        this.currentDemonForm = availableForms.get(previousIndex);
    }

    public boolean isDemon() {
        return isDemon;
    }
    public boolean isOvercomer() { return isOvercomer; }
    public void setOvercomer(boolean overcomer) { isOvercomer = overcomer; }
    public boolean isDishonored() { return isDishonored; }
    public void setDishonored(boolean dishonored) { isDishonored = dishonored; }
    public void setDemon(boolean demon) {
        isDemon = demon;
    }
    public void setBloodPoints(int amount) {
        this.bloodPoints = Math.max(0, amount);
        if (this.isDemon && !this.rank.isMoonRank()) {
            this.rank = DemonProgressionHelper.getFodderRankForBloodPoints(this.bloodPoints);
        }
    }
    public DemonRank getRank() {
        return rank;
    }
    public void setRank(DemonRank rank) {
        if (this.isDemon && !rank.isMoonRank()) {
            this.rank = DemonProgressionHelper.getFodderRankForBloodPoints(this.bloodPoints);
        } else {
            this.rank = rank;
        }
    }
    public int getBloodPoints() {
        return bloodPoints;
    }
    public BloodDemonArts getEquippedBda() {
        return this.equippedBda;
    }
    public void setEquippedBda(BloodDemonArts bda) {
        this.equippedBda = bda;
        this.ensureCurrentDemonFormMatchesEquippedBda();
    }
    public DemonForms getCurrentDemonForm() {
        return this.currentDemonForm;
    }
    public void setCurrentDemonForm(DemonForms form) {
        if (form.getStyle() == this.equippedBda) {
            this.currentDemonForm = form;
            return;
        }
        this.ensureCurrentDemonFormMatchesEquippedBda();
    }

    private void ensureCurrentDemonFormMatchesEquippedBda() {
        if (this.equippedBda == BloodDemonArts.NONE) {
            return;
        }

        if (this.currentDemonForm != null && this.currentDemonForm.getStyle() == this.equippedBda) {
            return;
        }

        Arrays.stream(DemonForms.values())
                .filter(form -> form.getStyle() == this.equippedBda)
                .findFirst()
                .ifPresent(form -> this.currentDemonForm = form);
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

    public boolean isBdaUnlocked(BloodDemonArts bda) {
        if (bda == BloodDemonArts.NONE) {
            return true; // "NONE" is always considered unlocked.
        }
        return this.unlockedBdas.contains(bda);
    }
    public void unlockBda(BloodDemonArts bda) {
        if (bda != BloodDemonArts.NONE) {
            this.unlockedBdas.add(bda);
        }
    }

    public int getPassiveTier(DemonPassive passive) {
        return this.unlockedPassives.getOrDefault(passive, 0);
    }
    public void setPassiveTier(DemonPassive passive, int tier) {
        if (tier > 0) {
            this.unlockedPassives.put(passive, tier);
        } else {
            this.unlockedPassives.remove(passive);
            this.activePassives.remove(passive);
        }
    }

    public boolean isPassiveActive(DemonPassive passive) {
        return this.activePassives.contains(passive);
    }
    public void setPassiveActive(DemonPassive passive, boolean isActive) {
        if (isActive && this.getPassiveTier(passive) > 0) {
            this.activePassives.add(passive);
        } else {
            this.activePassives.remove(passive);
        }
    }

    public int getBdaTier(BloodDemonArts bda) {
        return this.bdaTiers.getOrDefault(bda, 0);
    }
    public void setBdaTier(BloodDemonArts bda, int tier) {
        if (tier > 0) {
            this.bdaTiers.put(bda, tier);
        } else {
            this.bdaTiers.remove(bda);
        }
    }

    public void addBloodPoints(int amount) {
        if(!isDemon) return;
        this.setBloodPoints(this.bloodPoints + amount);
    }

    public void setGazeTarget(@Nullable UUID targetId) {
        if (!Objects.equals(this.targetedEntityId, targetId)) {
            this.targetedEntityId = targetId;
            this.ticksStared = 0; // Reset ticks when target changes
        }
    }
    public void incrementGazeTicks() {
        this.ticksStared++;
    }
    public int getGazeTicks() {
        return this.ticksStared;
    }
    @Nullable
    public UUID getGazeTargetId() {
        return this.targetedEntityId;
    }

    public void setSunGraceTicks(int ticks) {
        this.sunGraceTicks = ticks;
    }
    public int getSunGraceTicks() {
        return this.sunGraceTicks;
    }
    public void incrementSunGraceTicks() {
        this.sunGraceTicks++;
    }
    public void resetSunGraceTicks() {
        this.sunGraceTicks = 0;
    }

    public void copyFrom(PlayerDemon source) {
        this.isDemon = source.isDemon;
        this.rank = source.rank;
        this.isDishonored = source.isDishonored;
        this.bloodPoints = source.bloodPoints;
        this.equippedBda = source.equippedBda;
        this.bdaModeActive = source.bdaModeActive;
        this.currentDemonForm = source.currentDemonForm;
        this.unlockedPassives.clear();
        this.unlockedPassives.putAll(source.unlockedPassives);
        this.bdaTiers.clear();
        this.bdaTiers.putAll(source.bdaTiers);
        this.unlockedBdas.clear();
        this.unlockedBdas.addAll(source.unlockedBdas);
        this.activePassives.clear();
        this.activePassives.addAll(source.activePassives);
        this.ensureCurrentDemonFormMatchesEquippedBda();
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("isDemon", this.isDemon);
        nbt.putString("demonRank", this.rank.name());
        nbt.putInt("bloodPoints", this.bloodPoints);
        nbt.putString("equippedBda", this.equippedBda.name());
        nbt.putBoolean("bdaModeActive", this.bdaModeActive);
        nbt.putString("currentDemonForm", this.currentDemonForm.name());
        nbt.putBoolean("isDishonored", this.isDishonored);

        CompoundTag passivesTag = new CompoundTag();
        this.unlockedPassives.forEach((passive, tier) -> passivesTag.putInt(passive.name(), tier));
        nbt.put("unlockedPassives", passivesTag);

        CompoundTag bdaTiersTag = new CompoundTag();
        this.bdaTiers.forEach((bda, tier) -> bdaTiersTag.putInt(bda.name(), tier));
        nbt.put("bdaTiers", bdaTiersTag);

        ListTag unlockedBdasTag = new ListTag();
        this.unlockedBdas.forEach(bda -> unlockedBdasTag.add(StringTag.valueOf(bda.name())));
        nbt.put("unlockedBdas", unlockedBdasTag);

        ListTag activePassivesTag = new ListTag();
        this.activePassives.forEach(passive -> activePassivesTag.add(StringTag.valueOf(passive.name())));
        nbt.put("activePassives", activePassivesTag);
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
        this.setBloodPoints(nbt.getInt("bloodPoints"));
        if (nbt.contains("equippedBda")) {
            try {
                this.equippedBda = BloodDemonArts.valueOf(nbt.getString("equippedBda"));
            } catch (IllegalArgumentException e) {
                this.equippedBda = BloodDemonArts.NONE;
            }
        }
        this.bdaModeActive = nbt.getBoolean("bdaModeActive");
        this.isDishonored = nbt.getBoolean("isDishonored");
        if (nbt.contains("currentDemonForm")) {
            try { this.currentDemonForm = DemonForms.valueOf(nbt.getString("currentDemonForm")); }
            catch (IllegalArgumentException e) { this.currentDemonForm = DemonForms.THREADS_1; }
        }
        if (nbt.contains("unlockedPassives")) {
            this.unlockedPassives.clear();
            CompoundTag passivesTag = nbt.getCompound("unlockedPassives");
            for (String key : passivesTag.getAllKeys()) {
                try {
                    DemonPassive passive = DemonPassive.valueOf(key);
                    this.unlockedPassives.put(passive, passivesTag.getInt(key));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid passive
                }
            }
        }
        if (nbt.contains("bdaTiers")) {
            this.bdaTiers.clear();
            CompoundTag bdaTiersTag = nbt.getCompound("bdaTiers");
            for (String key : bdaTiersTag.getAllKeys()) {
                try {
                    BloodDemonArts bda = BloodDemonArts.valueOf(key);
                    this.bdaTiers.put(bda, bdaTiersTag.getInt(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (nbt.contains("unlockedBdas", 9)) {
            this.unlockedBdas.clear();
            ListTag unlockedBdasTag = nbt.getList("unlockedBdas", 8);
            for (int i = 0; i < unlockedBdasTag.size(); i++) {
                try {
                    BloodDemonArts bda = BloodDemonArts.valueOf(unlockedBdasTag.getString(i));
                    this.unlockedBdas.add(bda);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (nbt.contains("activePassives", 9)) {
            this.activePassives.clear();
            ListTag activePassivesTag = nbt.getList("activePassives", 8);
            for (int i = 0; i < activePassivesTag.size(); i++) {
                try {
                    DemonPassive passive = DemonPassive.valueOf(activePassivesTag.getString(i));
                    this.activePassives.add(passive);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        this.ensureCurrentDemonFormMatchesEquippedBda();
    }
}

package com.wassuples.demonslayer.ranking;

import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerProgression {
    private Rank rank = Rank.CIVILIAN;
    private final Set<BreathingStyles> unlockedStyles = new HashSet<>();
    private final Map<BreathingStyles, Integer> styleMastery = new HashMap<>();
    private final Map<BreathingForms, Integer> formMastery = new HashMap<>();
    private boolean hasDodgeCharge = false;

    public boolean hasDodgeCharge() {
        return this.hasDodgeCharge;
    }

    public void setDodgeCharge(boolean hasCharge) {
        this.hasDodgeCharge = hasCharge;
    }

    public PlayerProgression() {
        this.unlockStyle(BreathingStyles.WATER);
    }

    // --- Getters and Setters ---
    public Rank getRank() { return this.rank; }
    public void setRank(Rank rank) { this.rank = rank; }

    public boolean isStyleUnlocked(BreathingStyles style) { return unlockedStyles.contains(style); }
    public Set<BreathingStyles> getUnlockedStyles() { return this.unlockedStyles; }

    public int getStyleMastery(BreathingStyles style) { return styleMastery.getOrDefault(style, 0); }
    public Map<BreathingStyles, Integer> getStyleMasteryMap() { return this.styleMastery; }

    public int getFormMastery(BreathingForms form) { return formMastery.getOrDefault(form, 0); }
    public Map<BreathingForms, Integer> getFormMasteryMap() { return this.formMastery; }

    // --- Core Logic ---
    public void unlockStyle(BreathingStyles style) {
        unlockedStyles.add(style);
        styleMastery.putIfAbsent(style, 0);
    }

    public void addStyleMastery(BreathingStyles style, int amount) {
        styleMastery.put(style, getStyleMastery(style) + amount);
    }

    public void addFormMastery(BreathingForms form, int amount) {
        formMastery.put(form, getFormMastery(form) + amount);
    }

    public void setStyleMastery(BreathingStyles style, int amount) {
        styleMastery.put(style, Math.max(0, amount));
    }

    public void setFormMastery(BreathingForms form, int amount) {
        formMastery.put(form, Math.max(0, amount));
    }

    public void setUnlockedStyles(Set<BreathingStyles> newStyles) {
        this.unlockedStyles.clear();
        this.unlockedStyles.addAll(newStyles);
    }

    public void setAllStyleMastery(Map<BreathingStyles, Integer> newMastery) {
        this.styleMastery.clear();
        this.styleMastery.putAll(newMastery);
    }

    public void setAllFormMastery(Map<BreathingForms, Integer> newMastery) {
        this.formMastery.clear();
        this.formMastery.putAll(newMastery);
    }

    public void copyFrom(PlayerProgression source) {
        this.rank = source.rank;
        this.unlockedStyles.clear();
        this.unlockedStyles.addAll(source.unlockedStyles);
        this.styleMastery.clear();
        this.styleMastery.putAll(source.styleMastery);
        this.formMastery.clear();
        this.formMastery.putAll(source.formMastery);
    }

    // --- Persistence (NBT) ---
    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("rank", rank.name());

        ListTag unlockedStylesTag = new ListTag();
        unlockedStyles.forEach(style -> unlockedStylesTag.add(StringTag.valueOf(style.name())));
        nbt.put("unlockedStyles", unlockedStylesTag);

        CompoundTag styleMasteryTag = new CompoundTag();
        styleMastery.forEach((style, mastery) -> styleMasteryTag.putInt(style.name(), mastery));
        nbt.put("styleMastery", styleMasteryTag);

        CompoundTag formMasteryTag = new CompoundTag();
        formMastery.forEach((form, mastery) -> formMasteryTag.putInt(form.name(), mastery));
        nbt.put("formMastery", formMasteryTag);
    }

    public void loadNBTData(CompoundTag nbt) {
        if (nbt.contains("rank", 8)) {
            try { this.rank = Rank.valueOf(nbt.getString("rank")); }
            catch (IllegalArgumentException e) { this.rank = Rank.MIZUNOTO; }
        }

        if (nbt.contains("unlockedStyles", 9)) {
            unlockedStyles.clear();
            ListTag unlockedStylesTag = nbt.getList("unlockedStyles", 8);
            unlockedStylesTag.forEach(tag -> {
                try { unlockedStyles.add(BreathingStyles.valueOf(tag.getAsString())); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        if (nbt.contains("styleMastery", 10)) {
            styleMastery.clear();
            CompoundTag styleMasteryTag = nbt.getCompound("styleMastery");
            styleMasteryTag.getAllKeys().forEach(key -> {
                try { styleMastery.put(BreathingStyles.valueOf(key), styleMasteryTag.getInt(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        if (nbt.contains("formMastery", 10)) {
            formMastery.clear();
            CompoundTag formMasteryTag = nbt.getCompound("formMastery");
            formMasteryTag.getAllKeys().forEach(key -> {
                try { formMastery.put(BreathingForms.valueOf(key), formMasteryTag.getInt(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }
    }
}
package com.wassuples.demonslayer.ranking;

import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.styles.DemonForms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Capability data tracking a player's slayer/demon progression, style mastery, and form unlocks.
 *
 * <p>This is the central progression system for both slayer and demon paths:
 *
 * <ul>
 *   <li><b>Slayer rank</b> — {@link Rank} from Civilian to Hashira</li>
 *   <li><b>Style unlocks &amp; mastery</b> — which {@link BreathingStyles} are available, with mastery 0–1000</li>
 *   <li><b>Form unlocks &amp; mastery</b> — which {@link BreathingForms} are available, with mastery 0–100</li>
 *   <li><b>BDA mastery</b> — demon art mastery 0–1000 per {@link BloodDemonArts}</li>
 *   <li><b>Demon form mastery</b> — per-{@link DemonForms} mastery 0–100</li>
 *   <li><b>Dodge charge</b> — whether the player has an available dodge action</li>
 * </ul>
 *
 * <h2>Defaults</h2>
 * <p>New players start as {@link Rank#CIVILIAN} with {@link BreathingStyles#WATER} unlocked.
 *
 * @see PlayerProgressionProvider
 * @see Rank
 */

/**
 * Player Progression.
 */
public class PlayerProgression {
    private Rank rank = Rank.CIVILIAN;
    private final Set<BreathingStyles> unlockedStyles = new HashSet<>();
    private final Map<BreathingStyles, Integer> styleMastery = new HashMap<>();

    private final Map<BloodDemonArts, Integer> bdaMastery = new HashMap<>();
    private final Map<DemonForms, Integer> demonFormMastery = new HashMap<>();

    private final Set<BreathingForms> unlockedForms = new HashSet<>();
    private final Map<BreathingForms, Integer> formMastery = new HashMap<>();

    private int demonKills = 0;
    private int demonKillsThisRank = 0;
    private boolean killedKizuki = false;

    public int getDemonKills() { return demonKills; }
    public int getDemonKillsThisRank() { return demonKillsThisRank; }
    public void incrementDemonKills() { demonKills++; demonKillsThisRank++; }
    public boolean hasKilledKizuki() { return killedKizuki; }
    public void recordKizukiKill() { killedKizuki = true; }

    private boolean hasDodgeCharge = false;

    public boolean hasDodgeCharge() {
        return this.hasDodgeCharge;
    }
    public void setDodgeCharge(boolean hasCharge) {
        this.hasDodgeCharge = hasCharge;
    }

    public PlayerProgression() {
        this.unlockStyle(BreathingStyles.NONE);
        this.setRank(Rank.CIVILIAN);
    }

    public Rank getRank() { return this.rank; }
    public void setRank(Rank rank) {
        if (this.rank != rank) demonKillsThisRank = 0;
        this.rank = rank;
    }

    public boolean isStyleUnlocked(BreathingStyles style) { return unlockedStyles.contains(style); }
    public Set<BreathingStyles> getUnlockedStyles() { return this.unlockedStyles; }

    public int getStyleMastery(BreathingStyles style) { return styleMastery.getOrDefault(style, 0); }
    public Map<BreathingStyles, Integer> getStyleMasteryMap() { return this.styleMastery; }

    public int getDemonArtMastery(BloodDemonArts bda) {
        return bdaMastery.getOrDefault(bda, 0);
    }
    public int getDemonFormMastery(DemonForms form) { return demonFormMastery.getOrDefault(form, 0); }

    public int getFormMastery(BreathingForms form) { return formMastery.getOrDefault(form, 0); }
    public Map<BreathingForms, Integer> getFormMasteryMap() { return this.formMastery; }

    public boolean isFormUnlocked(BreathingForms form) { return unlockedForms.contains(form); }
    public Set<BreathingForms> getUnlockedForms() { return this.unlockedForms; }

    public void unlockStyle(BreathingStyles style) {
        unlockedStyles.add(style);
        styleMastery.putIfAbsent(style, 0);
    }
    public void unlockForm(BreathingForms form) {
        unlockedForms.add(form);
        formMastery.putIfAbsent(form, 0);
    }

    public void addStyleMastery(BreathingStyles style, int amount) {
        int currentMastery = getStyleMastery(style);
        int newMastery = Math.min(currentMastery + amount, 1000);
        styleMastery.put(style, newMastery);
    }

    public void addDemonArtMastery(BloodDemonArts bda, int amount) {
        int currentMastery = getDemonArtMastery(bda);
        int newMastery = Math.min(currentMastery + amount, 1000);
        bdaMastery.put(bda, newMastery);
    }

    public void addDemonFormMastery(DemonForms form, int amount) {
        int currentMastery = getDemonFormMastery(form);
        int newMastery = Math.min(currentMastery + amount, 100);
        demonFormMastery.put(form, newMastery);
    }

    public void addFormMastery(BreathingForms form, int amount) {
        int currentMastery = getFormMastery(form);
        int newMastery = Math.min(currentMastery + amount, 100);
        formMastery.put(form, newMastery);
    }

    public void setStyleMastery(BreathingStyles style, int amount) {
        int cappedAmount = Math.min(amount, 1000);
        styleMastery.put(style, Math.max(0, cappedAmount));
    }

    public void setFormMastery(BreathingForms form, int amount) {
        int cappedAmount = Math.min(amount, 100);
        formMastery.put(form, Math.max(0, cappedAmount));
    }

    public void setUnlockedStyles(Set<BreathingStyles> newStyles) {
        this.unlockedStyles.clear();
        this.unlockedStyles.addAll(newStyles);
    }
    public void setUnlockedForms(Set<BreathingForms> newForms) {
        this.unlockedForms.clear();
        this.unlockedForms.addAll(newForms);
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
        this.unlockedForms.clear();
        this.unlockedForms.addAll(source.unlockedForms);
        this.styleMastery.clear();
        this.styleMastery.putAll(source.styleMastery);
        this.formMastery.clear();
        this.formMastery.putAll(source.formMastery);

        this.bdaMastery.clear();
        this.bdaMastery.putAll(source.bdaMastery);
        this.demonFormMastery.clear();
        this.demonFormMastery.putAll(source.demonFormMastery);

        this.demonKills = source.demonKills;
        this.demonKillsThisRank = source.demonKillsThisRank;
        this.killedKizuki = source.killedKizuki;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("rank", rank.name());

        ListTag unlockedStylesTag = new ListTag();
        unlockedStyles.forEach(style -> unlockedStylesTag.add(StringTag.valueOf(style.name())));
        nbt.put("unlockedStyles", unlockedStylesTag);

        ListTag unlockedFormsTag = new ListTag();
        unlockedForms.forEach(form -> unlockedFormsTag.add(StringTag.valueOf(form.name())));
        nbt.put("unlockedForms", unlockedFormsTag);

        CompoundTag styleMasteryTag = new CompoundTag();
        styleMastery.forEach((style, mastery) -> styleMasteryTag.putInt(style.name(), mastery));
        nbt.put("styleMastery", styleMasteryTag);

        CompoundTag formMasteryTag = new CompoundTag();
        formMastery.forEach((form, mastery) -> formMasteryTag.putInt(form.name(), mastery));
        nbt.put("formMastery", formMasteryTag);

        CompoundTag bdaMasteryTag = new CompoundTag();
        bdaMastery.forEach((bda, mastery) -> bdaMasteryTag.putInt(bda.name(), mastery));
        nbt.put("bdaMastery", bdaMasteryTag);

        CompoundTag demonFormMasteryTag = new CompoundTag();
        demonFormMastery.forEach((form, mastery) -> demonFormMasteryTag.putInt(form.name(), mastery));
        nbt.put("demonFormMastery", demonFormMasteryTag);

        nbt.putInt("demonKills", demonKills);
        nbt.putInt("demonKillsThisRank", demonKillsThisRank);
        nbt.putBoolean("killedKizuki", killedKizuki);
    }

    public void loadNBTData(CompoundTag nbt) {
        if (nbt.contains("rank", 8)) {
            try { this.rank = Rank.valueOf(nbt.getString("rank")); }
            catch (IllegalArgumentException e) { this.rank = Rank.CIVILIAN; }
        }

        if (nbt.contains("unlockedStyles", 9)) {
            unlockedStyles.clear();
            ListTag unlockedStylesTag = nbt.getList("unlockedStyles", 8);
            unlockedStylesTag.forEach(tag -> {
                try { unlockedStyles.add(BreathingStyles.valueOf(tag.getAsString())); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        if (nbt.contains("unlockedForms", 9)) {
            unlockedForms.clear();
            ListTag unlockedFormsTag = nbt.getList("unlockedForms", 8);
            unlockedFormsTag.forEach(tag -> {
                try { unlockedForms.add(BreathingForms.valueOf(tag.getAsString())); }
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

        if (nbt.contains("bdaMastery", 10)) {
            bdaMastery.clear();
            CompoundTag bdaMasteryTag = nbt.getCompound("bdaMastery");
            bdaMasteryTag.getAllKeys().forEach(key -> {
                try { bdaMastery.put(BloodDemonArts.valueOf(key), bdaMasteryTag.getInt(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        if (nbt.contains("demonFormMastery", 10)) {
            demonFormMastery.clear();
            CompoundTag demonFormMasteryTag = nbt.getCompound("demonFormMastery");
            demonFormMasteryTag.getAllKeys().forEach(key -> {
                try { demonFormMastery.put(DemonForms.valueOf(key), demonFormMasteryTag.getInt(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        demonKills = nbt.getInt("demonKills");
        demonKillsThisRank = nbt.getInt("demonKillsThisRank");
        killedKizuki = nbt.getBoolean("killedKizuki");
    }
}

package com.wassuples.demonslayer.missions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-player story progress (capability).
 *
 * <ul>
 *   <li>At most one story mission is active at a time. Its run state is an opaque compound owned by the mission script
 *       ({@link StoryMission}), so each mission can keep exactly the data it needs.</li>
 *   <li>Completed missions remember the branch they ended on ("survived", "hand_demon_slain", ...); later branches
 *       read these.</li>
 *   <li>Story flags record world/relationship facts ("final_selection.passed"); perks are mission-granted passives
 *       ("unyielding").</li>
 * </ul>
 */
public class PlayerMissions {
    @Nullable
    private ResourceLocation activeId;
    private CompoundTag activeState = new CompoundTag();
    private final Map<ResourceLocation, String> completed = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> failures = new HashMap<>();
    private final Set<String> flags = new HashSet<>();
    private final Set<String> perks = new HashSet<>();
    /** Day number on which the Unyielding passive was last spent. */
    private long unyieldingDay = Long.MIN_VALUE;
    /** Named story timers/values (e.g. when a swordsmith is due), kept after a mission ends. */
    private CompoundTag timers = new CompoundTag();

    // ------------------------------------------------------------------ active run

    @Nullable
    public ResourceLocation getActiveId() {
        return activeId;
    }

    public boolean hasActive() {
        return activeId != null;
    }

    public boolean isActive(ResourceLocation id) {
        return id.equals(activeId);
    }

    public CompoundTag getActiveState() {
        return activeState;
    }

    void setActive(ResourceLocation id, CompoundTag state) {
        this.activeId = id;
        this.activeState = state;
    }

    void clearActive() {
        this.activeId = null;
        this.activeState = new CompoundTag();
    }

    // ------------------------------------------------------------------ history

    public boolean isCompleted(ResourceLocation id) {
        return completed.containsKey(id);
    }

    /** Branch the mission ended on, or null if not completed. */
    @Nullable
    public String getOutcome(ResourceLocation id) {
        return completed.get(id);
    }

    void markCompleted(ResourceLocation id, String outcome) {
        completed.put(id, outcome);
    }

    public Map<ResourceLocation, String> getCompleted() {
        return Collections.unmodifiableMap(completed);
    }

    public int getFailures(ResourceLocation id) {
        return failures.getOrDefault(id, 0);
    }

    void addFailure(ResourceLocation id) {
        failures.merge(id, 1, Integer::sum);
    }

    /** Admin reset of one mission (or all, when id is null). */
    void reset(@Nullable ResourceLocation id) {
        if (id == null) {
            completed.clear();
            failures.clear();
            flags.clear();
            perks.clear();
            timers = new CompoundTag();
            clearActive();
        } else {
            completed.remove(id);
            failures.remove(id);
            if (id.equals(activeId)) clearActive();
        }
    }

    // ------------------------------------------------------------------ flags & perks

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void setFlag(String flag) {
        flags.add(flag);
    }

    public boolean hasPerk(String perk) {
        return perks.contains(perk);
    }

    public void addPerk(String perk) {
        perks.add(perk);
    }

    public Set<String> getPerks() {
        return Collections.unmodifiableSet(perks);
    }

    public long getUnyieldingDay() {
        return unyieldingDay;
    }

    public void setUnyieldingDay(long day) {
        this.unyieldingDay = day;
    }

    // ------------------------------------------------------------------ story timers

    public boolean hasTimer(String key) {
        return timers.contains(key);
    }

    public long getTimer(String key) {
        return timers.getLong(key);
    }

    public void setTimer(String key, long value) {
        timers.putLong(key, value);
    }

    public void removeTimer(String key) {
        timers.remove(key);
    }

    // ------------------------------------------------------------------ persistence

    public void copyFrom(PlayerMissions source) {
        CompoundTag tag = new CompoundTag();
        source.saveNBTData(tag);
        loadNBTData(tag);
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("version", 2);
        if (activeId != null) {
            nbt.putString("active", activeId.toString());
            nbt.put("activeState", activeState.copy());
        }
        CompoundTag done = new CompoundTag();
        completed.forEach((id, outcome) -> done.putString(id.toString(), outcome));
        nbt.put("completed", done);
        CompoundTag fails = new CompoundTag();
        failures.forEach((id, n) -> fails.putInt(id.toString(), n));
        nbt.put("failures", fails);
        ListTag flagList = new ListTag();
        flags.forEach(f -> flagList.add(StringTag.valueOf(f)));
        nbt.put("flags", flagList);
        ListTag perkList = new ListTag();
        perks.forEach(p -> perkList.add(StringTag.valueOf(p)));
        nbt.put("perks", perkList);
        nbt.putLong("unyieldingDay", unyieldingDay);
        nbt.put("timers", timers.copy());
    }

    public void loadNBTData(CompoundTag nbt) {
        clearActive();
        completed.clear();
        failures.clear();
        flags.clear();
        perks.clear();
        unyieldingDay = Long.MIN_VALUE;
        timers = new CompoundTag();

        if (nbt.getInt("version") < 2) {
            // Pre-rework saves: keep the completed ids; old active missions are dropped (their scripts no longer exist).
            ListTag old = nbt.getList("completedMissions", Tag.TAG_COMPOUND);
            for (int i = 0; i < old.size(); i++) {
                String id = old.getCompound(i).getString("id");
                if (ResourceLocation.isValidResourceLocation(id)) completed.put(new ResourceLocation(id), "legacy");
            }
            return;
        }

        if (nbt.contains("active", Tag.TAG_STRING) && ResourceLocation.isValidResourceLocation(nbt.getString("active"))) {
            activeId = new ResourceLocation(nbt.getString("active"));
            activeState = nbt.getCompound("activeState").copy();
        }
        CompoundTag done = nbt.getCompound("completed");
        for (String key : done.getAllKeys()) {
            if (ResourceLocation.isValidResourceLocation(key)) completed.put(new ResourceLocation(key), done.getString(key));
        }
        CompoundTag fails = nbt.getCompound("failures");
        for (String key : fails.getAllKeys()) {
            if (ResourceLocation.isValidResourceLocation(key)) failures.put(new ResourceLocation(key), fails.getInt(key));
        }
        ListTag flagList = nbt.getList("flags", Tag.TAG_STRING);
        for (int i = 0; i < flagList.size(); i++) flags.add(flagList.getString(i));
        ListTag perkList = nbt.getList("perks", Tag.TAG_STRING);
        for (int i = 0; i < perkList.size(); i++) perks.add(perkList.getString(i));
        if (nbt.contains("unyieldingDay")) unyieldingDay = nbt.getLong("unyieldingDay");
        timers = nbt.getCompound("timers").copy();
    }
}

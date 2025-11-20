package com.wassuples.demonslayer.nemesis.trait;

import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class DemonTrait implements IDemonTrait {
    private String sourceBlueprintId = "procedural";

    private String name = "Nameless";
    private String title = "the Weak";

    private int vigor = 5;
    private int strength = 5;
    private int agility = 5;

    private Behavior behavior = Behavior.AGGRESSIVE;
    private Personality personality = Personality.COCKY;

    private int bloodPoints = 0;
    private BloodDemonArts assignedBDA = BloodDemonArts.NONE;

    private final Map<UUID, List<String>> tauntMap = new HashMap<>();
    private final Map<String, List<String>> blueprintTaunts = new HashMap<>();
    private final Map<String, List<String>> blueprintAnnouncements = new HashMap<>();
    private final List<Quirk> quirks = new ArrayList<>();

    private ResourceLocation territory = new ResourceLocation("plains");
    private BlockPos homePosition = BlockPos.ZERO;

    @Override public String getSourceBlueprintId() { return this.sourceBlueprintId; }
    @Override public void setSourceBlueprintId(String id) { this.sourceBlueprintId = id; }

    @Override public ResourceLocation getTerritory() { return this.territory; }
    @Override public void setTerritory(ResourceLocation territory) { this.territory = territory; }

    @Override public BlockPos getHomePosition() { return this.homePosition; }
    @Override public void setHomePosition(BlockPos pos) { this.homePosition = pos; }


    @Override public String getName() { return this.name; }
    @Override public void setName(String name) { this.name = name; }

    @Override public String getTitle() { return this.title; }
    @Override public void setTitle(String title) { this.title = title; }

    @Override public int getVigor() { return this.vigor; }
    @Override public void setVigor(int vigor) { this.vigor = vigor; }

    @Override public int getStrength() { return this.strength; }
    @Override public void setStrength(int strength) { this.strength = strength; }

    @Override public int getAgility() { return this.agility; }
    @Override public void setAgility(int agility) { this.agility = agility; }

    @Override public Behavior getBehavior() { return this.behavior; }
    @Override public void setBehavior(Behavior behavior) { this.behavior = behavior; }

    @Override public Personality getPersonality() { return this.personality; }
    @Override public void setPersonality(Personality personality) { this.personality = personality; }

    @Override public int getBloodPoints() { return this.bloodPoints; }
    @Override public void setBloodPoints(int points) { this.bloodPoints = points; }
    @Override public void addBloodPoints(int points) { this.bloodPoints += points; }

    @Override public BloodDemonArts getAssignedBDA() { return this.assignedBDA; }
    @Override public void setAssignedBDA(BloodDemonArts bda) { this.assignedBDA = bda; }

    @Override public List<Quirk> getQuirks() { return Collections.unmodifiableList(this.quirks); }
    @Override public void addQuirk(Quirk quirk) {
        if (quirk != null && !this.quirks.contains(quirk)) {
            this.quirks.add(quirk);
        }
    }
    @Override public void removeQuirk(Quirk quirk) { this.quirks.remove(quirk); }
    @Override public boolean hasQuirk(Quirk quirk) { return this.quirks.contains(quirk); }

    @Override public List<String> getPlayerTaunts(UUID playerUUID) {
        return this.tauntMap.getOrDefault(playerUUID, new ArrayList<>());
    }
    @Override public void addPlayerTaunt(UUID playerUUID, String taunt) {
        this.tauntMap.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(taunt);
    }

    @Override public Map<String, List<String>> getBlueprintTaunts() { return this.blueprintTaunts; }
    @Override public void setBlueprintTaunts(Map<String, List<String>> taunts) {
        if (taunts != null) {
            this.blueprintTaunts.clear();
            this.blueprintTaunts.putAll(taunts);
        }
    }

    @Override public Map<String, List<String>> getBlueprintAnnouncements() { return this.blueprintAnnouncements; }
    @Override public void setBlueprintAnnouncements(Map<String, List<String>> announcements) {
        if (announcements != null) {
            this.blueprintAnnouncements.clear();
            this.blueprintAnnouncements.putAll(announcements);
        }
    }

    @Override public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", this.name);
        nbt.putString("title", this.title);
        nbt.putInt("vigor", this.vigor);
        nbt.putInt("strength", this.strength);
        nbt.putInt("agility", this.agility);
        nbt.putString("behavior", this.behavior.name());
        nbt.putString("personality", this.personality.name());
        nbt.putInt("bloodPoints", this.bloodPoints);
        nbt.putString("assignedBDA", this.assignedBDA.name());
        nbt.putString("territory", this.territory.toString());
        nbt.putString("sourceBlueprintId", this.sourceBlueprintId);

        CompoundTag tauntsTag = new CompoundTag();
        this.tauntMap.forEach((uuid, list) -> {
            ListTag listTag = new ListTag();
            list.forEach(taunt -> listTag.add(StringTag.valueOf(taunt)));
            tauntsTag.put(uuid.toString(), listTag);
        });
        nbt.put("tauntMap", tauntsTag);

        ListTag quirksTag = new ListTag();
        this.quirks.forEach(quirk -> quirksTag.add(StringTag.valueOf(quirk.name())));
        nbt.put("quirks", quirksTag);

        if (this.homePosition != null) {
            nbt.put("homePosition", NbtUtils.writeBlockPos(this.homePosition));
        }

        CompoundTag bpTauntsTag = new CompoundTag();
        this.blueprintTaunts.forEach((key, list) -> {
            ListTag listTag = new ListTag();
            list.forEach(taunt -> listTag.add(StringTag.valueOf(taunt)));
            bpTauntsTag.put(key, listTag);
        });
        nbt.put("blueprintTaunts", bpTauntsTag);

        CompoundTag bpAnnouncementsTag = new CompoundTag();
        this.blueprintAnnouncements.forEach((key, list) -> {
            ListTag listTag = new ListTag();
            list.forEach(announcement -> listTag.add(StringTag.valueOf(announcement)));
            bpAnnouncementsTag.put(key, listTag);
        });
        nbt.put("blueprintAnnouncements", bpAnnouncementsTag);

        return nbt;
    }
    @Override public void deserializeNBT(CompoundTag nbt) {
        this.sourceBlueprintId = nbt.getString("sourceBlueprintId");
        this.name = nbt.getString("name");
        this.title = nbt.getString("title");
        this.vigor = nbt.getInt("vigor");
        this.strength = nbt.getInt("strength");
        this.agility = nbt.getInt("agility");
        this.behavior = Behavior.valueOf(nbt.getString("behavior"));
        this.personality = Personality.valueOf(nbt.getString("personality"));
        this.bloodPoints = nbt.getInt("bloodPoints");
        this.assignedBDA = BloodDemonArts.valueOf(nbt.getString("assignedBDA"));

        this.tauntMap.clear();
        CompoundTag tauntsTag = nbt.getCompound("tauntMap");
        for (String key : tauntsTag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            List<String> taunts = new ArrayList<>();
            ListTag listTag = tauntsTag.getList(key, 8);
            listTag.forEach(tag -> taunts.add(tag.getAsString()));
            this.tauntMap.put(uuid, taunts);
        }

        if (nbt.contains("homePosition")) {
            this.homePosition = NbtUtils.readBlockPos(nbt.getCompound("homePosition"));
        }

        if (nbt.contains("territory")) {
            this.territory = new ResourceLocation(nbt.getString("territory"));
        }

        this.quirks.clear();
        ListTag quirksTag = nbt.getList("quirks", 8);
        for (int i = 0; i < quirksTag.size(); i++) {
            try {
                this.quirks.add(Quirk.valueOf(quirksTag.getString(i)));
            } catch (IllegalArgumentException ignored) {}
        }

        this.blueprintTaunts.clear();
        CompoundTag bpTauntsTag = nbt.getCompound("blueprintTaunts");
        for (String key : bpTauntsTag.getAllKeys()) {
            List<String> taunts = new ArrayList<>();
            ListTag listTag = bpTauntsTag.getList(key, 8);
            listTag.forEach(tag -> taunts.add(tag.getAsString()));
            this.blueprintTaunts.put(key, taunts);
        }

        this.blueprintAnnouncements.clear();
        CompoundTag bpAnnouncementsTag = nbt.getCompound("blueprintAnnouncements");
        for (String key : bpAnnouncementsTag.getAllKeys()) {
            List<String> announcements = new ArrayList<>();
            ListTag listTag = bpAnnouncementsTag.getList(key, 8);
            listTag.forEach(tag -> announcements.add(tag.getAsString()));
            this.blueprintAnnouncements.put(key, announcements);
        }
    }
    @Override public void copyFrom(IDemonTrait source) {
        if (source instanceof DemonTrait sourceTrait) {
            this.setSourceBlueprintId(sourceTrait.getSourceBlueprintId());
            this.setName(sourceTrait.getName());
            this.setTitle(sourceTrait.getTitle());
            this.setVigor(sourceTrait.getVigor());
            this.setStrength(sourceTrait.getStrength());
            this.setAgility(sourceTrait.getAgility());
            this.setBehavior(sourceTrait.getBehavior());
            this.setPersonality(sourceTrait.getPersonality());
            this.setBloodPoints(sourceTrait.getBloodPoints());
            this.setAssignedBDA(sourceTrait.getAssignedBDA());
            this.setTerritory(sourceTrait.getTerritory());
            this.setHomePosition(sourceTrait.getHomePosition());

            this.quirks.clear();
            this.quirks.addAll(sourceTrait.getQuirks());

            this.tauntMap.clear();
            this.tauntMap.putAll(sourceTrait.tauntMap);

            this.blueprintTaunts.clear();
            this.blueprintTaunts.putAll(sourceTrait.getBlueprintTaunts());

            this.blueprintAnnouncements.clear();
            this.blueprintAnnouncements.putAll(sourceTrait.getBlueprintAnnouncements());
        }
    }
}
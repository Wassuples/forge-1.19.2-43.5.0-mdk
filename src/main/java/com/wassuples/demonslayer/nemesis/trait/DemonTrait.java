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

/**
 * Default implementation of {@link IDemonTrait} — the full identity profile of a Nemesis demon.
 *
 * <p>Contains all persistent state for a named demon: name, title, base stats (vigor/strength/agility),
 * behavior, personality, Blood Demon Art, quirks, memories, relationships, emotions, growth stats,
 * taunt lines, and home location. Fully serializable to/from NBT.
 *
 * @see IDemonTrait
 * @see DemonTraitProvider
 * @see com.wassuples.demonslayer.nemesis.DemonHierarchyManager
 */
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

    private static final int MAX_MEMORIES = 50;

    /** Hard cap for vigor/strength/agility stats. */
    public static final int STAT_HARD_CAP = 50;

    /**
     * World time (ticks) when the demon was created (approximate).
     * Set exactly once at creation to {@code level.getGameTime()}.
     */
    private long birthTime = 0L;

    /**
     * World time (ticks) when the demon last concluded a battle session.
     * Updated only at battle-session end, never on individual hits.
     */
    private long lastBattleTime = 0L;

    /**
     * World time (ticks) when the demon was last alive in a loaded chunk
     * or processed by the daily simulation.
     */
    private long lastSeenTime = 0L;

    /**
     * Bounded memory log (most recent first).
     */
    private final List<DemonMemory> memories = new ArrayList<>();

    /**
     * Fast indexes so we don't have to scan {@link #memories} every time.
     */
    private final EnumMap<MemoryType, Integer> memoryCounts = new EnumMap<>(MemoryType.class);
    private final EnumMap<MemoryType, Long> lastMemoryTimes = new EnumMap<>(MemoryType.class);

    /**
     * Per-demon emotion state (feelings derived from factual memories).
     * Persisted independently so emotions are never re-derived on load.
     */
    private final EmotionState emotionState = new EmotionState();

    /**
     * Persistent combat statistics used as inputs to the growth system.
     */
    private final GrowthStats growthStats = new GrowthStats();

    /**
     * Directed relationships toward other demons (G2).
     * Key = target demon UUID. Only significant relationships are persisted.
     */
    private final Map<UUID, DemonRelationship> relationships = new HashMap<>();

    /**
     * Base intel-gathering ability, range [0, 1].
     * Derived from Behavior + Personality at creation; persisted.
     */
    private float baseIntelGathering = 0.2f;

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
    @Override public void setVigor(int vigor) { this.vigor = Math.max(0, Math.min(STAT_HARD_CAP, vigor)); }

    @Override public int getStrength() { return this.strength; }
    @Override public void setStrength(int strength) { this.strength = Math.max(0, Math.min(STAT_HARD_CAP, strength)); }

    @Override public int getAgility() { return this.agility; }
    @Override public void setAgility(int agility) { this.agility = Math.max(0, Math.min(STAT_HARD_CAP, agility)); }

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

    @Override
    public long getBirthTime() {
        return this.birthTime;
    }

    @Override
    public void setBirthTime(long birthTime) {
        this.birthTime = Math.max(0L, birthTime);
    }

    @Override
    public long getLastBattleTime() {
        return this.lastBattleTime;
    }

    @Override
    public void setLastBattleTime(long time) {
        this.lastBattleTime = Math.max(0L, time);
    }

    @Override
    public long getLastSeenTime() {
        return this.lastSeenTime;
    }

    @Override
    public void setLastSeenTime(long time) {
        this.lastSeenTime = Math.max(0L, time);
    }

    @Override
    public GrowthStats getGrowthStats() {
        return this.growthStats;
    }

    // ── Relationships (G2) ──────────────────────────────────────────────

    @Override
    public @org.jetbrains.annotations.Nullable DemonRelationship getRelationship(UUID targetId) {
        return this.relationships.get(targetId);
    }

    @Override
    public DemonRelationship getOrCreateRelationship(UUID targetId) {
        return this.relationships.computeIfAbsent(targetId, k -> new DemonRelationship());
    }

    @Override
    public Map<UUID, DemonRelationship> getRelationships() {
        return Collections.unmodifiableMap(this.relationships);
    }

    // ── Intel (G4) ──────────────────────────────────────────────────────

    @Override
    public float getBaseIntelGathering() {
        return this.baseIntelGathering;
    }

    @Override
    public void setBaseIntelGathering(float value) {
        this.baseIntelGathering = Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public List<DemonMemory> getMemories() {
        return Collections.unmodifiableList(this.memories);
    }

    @Override
    public void addMemory(DemonMemory memory) {
        if (memory == null) return;

        // Insert newest first (recent-first order)
        this.memories.add(0, memory);

        // Keep the log bounded (drop oldest = last element)
        while (this.memories.size() > MAX_MEMORIES) {
            this.memories.remove(this.memories.size() - 1);
        }

        // Update fast indexes
        MemoryType type = memory.getType();
        if (type != null) {
            this.memoryCounts.put(type, this.memoryCounts.getOrDefault(type, 0) + 1);
            long ts = memory.getTimestamp();
            long prev = this.lastMemoryTimes.getOrDefault(type, 0L);
            if (ts >= prev) {
                this.lastMemoryTimes.put(type, ts);
            }
        }

        // Derive emotions from this new fact (called once at insert time only)
        this.emotionState.deriveFromMemory(memory, this.personality);
    }

    @Override
    public void clearMemories() {
        this.memories.clear();
        this.memoryCounts.clear();
        this.lastMemoryTimes.clear();
    }

    @Override
    public int getMemoryCount(MemoryType type) {
        if (type == null) return 0;
        return this.memoryCounts.getOrDefault(type, 0);
    }

    @Override
    public long getLastMemoryTime(MemoryType type) {
        if (type == null) return 0L;
        return this.lastMemoryTimes.getOrDefault(type, 0L);
    }

    // ── Emotion state ───────────────────────────────────────────────────

    @Override
    public EmotionState getEmotionState() {
        return this.emotionState;
    }

    @Override
    public float getEmotionIntensity(EmotionType type) {
        return this.emotionState.getIntensity(type);
    }

    @Override
    public void applyEmotionDecay(long currentTime) {
        this.emotionState.applyDecay(currentTime);
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
        nbt.putLong("birthTime", this.birthTime);
        nbt.putLong("lastBattleTime", this.lastBattleTime);
        nbt.putLong("lastSeenTime", this.lastSeenTime);

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

        // Memories
        ListTag memoriesTag = new ListTag();
        for (DemonMemory memory : this.memories) {
            memoriesTag.add(memory.serializeNBT());
        }
        nbt.put("memories", memoriesTag);

        // Fast indexes (redundant, but speeds up load and keeps behavior stable if log is truncated)
        CompoundTag memoryCountsTag = new CompoundTag();
        for (Map.Entry<MemoryType, Integer> e : this.memoryCounts.entrySet()) {
            memoryCountsTag.putInt(e.getKey().name(), e.getValue());
        }
        nbt.put("memoryCounts", memoryCountsTag);

        CompoundTag lastMemoryTimesTag = new CompoundTag();
        for (Map.Entry<MemoryType, Long> e : this.lastMemoryTimes.entrySet()) {
            lastMemoryTimesTag.putLong(e.getKey().name(), e.getValue());
        }
        nbt.put("lastMemoryTimes", lastMemoryTimesTag);

        // Emotions (persisted independently — never re-derived on load)
        nbt.put("emotions", this.emotionState.serializeNBT());

        // Growth stats
        nbt.put("growthStats", this.growthStats.serializeNBT());

        // Relationships (G2) — only persist significant ones
        CompoundTag relsTag = new CompoundTag();
        for (Map.Entry<UUID, DemonRelationship> e : this.relationships.entrySet()) {
            if (e.getValue().isSignificant()) {
                relsTag.put(e.getKey().toString(), e.getValue().serializeNBT());
            }
        }
        nbt.put("relationships", relsTag);

        // Intel base stat (G4)
        nbt.putFloat("baseIntelGathering", this.baseIntelGathering);

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

        // Times (optional for backward compatibility)
        if (nbt.contains("birthTime")) {
            this.birthTime = Math.max(0L, nbt.getLong("birthTime"));
        }
        if (nbt.contains("lastBattleTime")) {
            this.lastBattleTime = Math.max(0L, nbt.getLong("lastBattleTime"));
        }
        if (nbt.contains("lastSeenTime")) {
            this.lastSeenTime = Math.max(0L, nbt.getLong("lastSeenTime"));
        }

        // Memories
        this.memories.clear();
        this.memoryCounts.clear();
        this.lastMemoryTimes.clear();

        if (nbt.contains("memories")) {
            ListTag memoriesTag = nbt.getList("memories", 10); // 10 = CompoundTag
            for (int i = 0; i < memoriesTag.size(); i++) {
                if (this.memories.size() >= MAX_MEMORIES) break;
                CompoundTag memTag = memoriesTag.getCompound(i);
                try {
                    DemonMemory memory = DemonMemory.deserializeNBT(memTag);
                    this.memories.add(memory);
                } catch (Exception ignored) {
                }
            }
        }

        // Prefer saved indexes if present; otherwise rebuild from log
        if (nbt.contains("memoryCounts")) {
            CompoundTag countsTag = nbt.getCompound("memoryCounts");
            for (String key : countsTag.getAllKeys()) {
                try {
                    MemoryType type = MemoryType.valueOf(key);
                    this.memoryCounts.put(type, countsTag.getInt(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (nbt.contains("lastMemoryTimes")) {
            CompoundTag timesTag = nbt.getCompound("lastMemoryTimes");
            for (String key : timesTag.getAllKeys()) {
                try {
                    MemoryType type = MemoryType.valueOf(key);
                    this.lastMemoryTimes.put(type, timesTag.getLong(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (this.memoryCounts.isEmpty() || this.lastMemoryTimes.isEmpty()) {
            // Rebuild indexes from log as a fallback
            for (DemonMemory memory : this.memories) {
                if (memory == null || memory.getType() == null) continue;
                MemoryType type = memory.getType();
                this.memoryCounts.put(type, this.memoryCounts.getOrDefault(type, 0) + 1);
                long ts = memory.getTimestamp();
                long prev = this.lastMemoryTimes.getOrDefault(type, 0L);
                if (ts >= prev) {
                    this.lastMemoryTimes.put(type, ts);
                }
            }
        }

        // Emotions: load persisted state (never re-derive from facts on load)
        if (nbt.contains("emotions")) {
            this.emotionState.deserializeNBT(nbt.getCompound("emotions"));
        }
        // If "emotions" key is absent (old save), emotionState stays at default zeros.

        // Growth stats
        if (nbt.contains("growthStats")) {
            this.growthStats.deserializeNBT(nbt.getCompound("growthStats"));
        }

        // Relationships (G2)
        this.relationships.clear();
        if (nbt.contains("relationships")) {
            CompoundTag relsTag = nbt.getCompound("relationships");
            for (String key : relsTag.getAllKeys()) {
                try {
                    UUID targetId = UUID.fromString(key);
                    DemonRelationship rel = new DemonRelationship();
                    rel.deserializeNBT(relsTag.getCompound(key));
                    this.relationships.put(targetId, rel);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Intel base stat (G4)
        if (nbt.contains("baseIntelGathering")) {
            this.baseIntelGathering = nbt.getFloat("baseIntelGathering");
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
            this.setBirthTime(sourceTrait.getBirthTime());
            this.setLastBattleTime(sourceTrait.getLastBattleTime());
            this.setLastSeenTime(sourceTrait.getLastSeenTime());

            this.quirks.clear();
            this.quirks.addAll(sourceTrait.getQuirks());

            this.tauntMap.clear();
            this.tauntMap.putAll(sourceTrait.tauntMap);

            this.blueprintTaunts.clear();
            this.blueprintTaunts.putAll(sourceTrait.getBlueprintTaunts());

            this.blueprintAnnouncements.clear();
            this.blueprintAnnouncements.putAll(sourceTrait.getBlueprintAnnouncements());

            this.memories.clear();
            this.memories.addAll(sourceTrait.memories);

            this.memoryCounts.clear();
            this.memoryCounts.putAll(sourceTrait.memoryCounts);

            this.lastMemoryTimes.clear();
            this.lastMemoryTimes.putAll(sourceTrait.lastMemoryTimes);

            this.emotionState.copyFrom(sourceTrait.emotionState);
            this.growthStats.copyFrom(sourceTrait.growthStats);

            this.relationships.clear();
            for (Map.Entry<UUID, DemonRelationship> e : sourceTrait.relationships.entrySet()) {
                DemonRelationship copy = new DemonRelationship();
                copy.copyFrom(e.getValue());
                this.relationships.put(e.getKey(), copy);
            }
            this.setBaseIntelGathering(sourceTrait.getBaseIntelGathering());
        }
    }
}

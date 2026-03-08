package com.wassuples.demonslayer.nemesis.trait;

import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Capability interface defining the identity and behavioral profile of a Nemesis demon.
 *
 * <p>Each Nemesis demon entity carries a trait that encodes its unique personality,
 * combat behavior, Blood Demon Art, quirks, memories, relationships, emotional state,
 * and growth statistics. This is the primary data source for AI decision-making,
 * taunt generation, and progression tracking.
 *
 * @see DemonTrait
 * @see DemonTraitProvider
 */
public interface IDemonTrait {
    String getSourceBlueprintId();
    void setSourceBlueprintId(String id);

    String getName();
    void setName(String name);

    String getTitle();
    void setTitle(String title);

    int getVigor();
    void setVigor(int vigor);

    int getStrength();
    void setStrength(int strength);

    int getAgility();
    void setAgility(int agility);

    Behavior getBehavior();
    void setBehavior(Behavior behavior);

    Personality getPersonality();
    void setPersonality(Personality personality);

    int getBloodPoints();
    void setBloodPoints(int points);
    void addBloodPoints(int points);

    BloodDemonArts getAssignedBDA();
    void setAssignedBDA(BloodDemonArts bda);

    List<Quirk> getQuirks();
    void addQuirk(Quirk quirk);
    void removeQuirk(Quirk quirk);
    boolean hasQuirk(Quirk quirk);

    List<String> getPlayerTaunts(UUID playerUUID);
    void addPlayerTaunt(UUID playerUUID, String taunt);

    Map<String, List<String>> getBlueprintTaunts();
    void setBlueprintTaunts(Map<String, List<String>> taunts);
    Map<String, List<String>> getBlueprintAnnouncements();
    void setBlueprintAnnouncements(Map<String, List<String>> announcements);

    ResourceLocation getTerritory();
    void setTerritory(ResourceLocation territory);

    BlockPos getHomePosition();
    void setHomePosition(BlockPos pos);

    void copyFrom(IDemonTrait source);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    /**
     * World time (ticks) when this demon was created (approximate birth).
     * Must be set exactly once at creation to {@code level.getGameTime()}.
     */
    long getBirthTime();

    void setBirthTime(long birthTime);

    /**
     * World time (ticks) when the demon last concluded a notable battle session.
     * Updated only at battle-session end, never on individual hits.
     */
    long getLastBattleTime();

    void setLastBattleTime(long time);

    /**
     * World time (ticks) when this demon was last "seen" — either alive in a
     * loaded chunk or processed by the daily simulation.
     */
    long getLastSeenTime();

    void setLastSeenTime(long time);

    // ── Time helpers (pure computation, not persisted) ──────────────────

    /**
     * Returns the demon's age in ticks: {@code currentTime - birthTime}.
     */
    default long getAge(long currentTime) {
        return Math.max(0L, currentTime - getBirthTime());
    }

    /**
     * Returns ticks since the last battle ended, or {@link Long#MAX_VALUE}
     * if the demon has never fought.
     */
    default long getTimeSinceLastBattle(long currentTime) {
        long last = getLastBattleTime();
        return last <= 0L ? Long.MAX_VALUE : Math.max(0L, currentTime - last);
    }

    // ── Growth stats ────────────────────────────────────────────────────

    /**
     * Returns the demon's persistent combat statistics used for growth
     * calculations.
     */
    GrowthStats getGrowthStats();

    /**
     * Full memory log (bounded). Do not mutate directly.
     */
    List<DemonMemory> getMemories();

    void addMemory(DemonMemory memory);

    void clearMemories();

    /**
     * Fast stats to drive behavior/taunts without scanning the whole log.
     */
    int getMemoryCount(MemoryType type);

    long getLastMemoryTime(MemoryType type);

    // ── Emotions (derived feelings) ─────────────────────────────────────

    /**
     * Returns the full mutable emotion state for this demon.
     */
    EmotionState getEmotionState();

    /**
     * Convenience: returns the current intensity of a single emotion.
     */
    float getEmotionIntensity(EmotionType type);

    /**
     * Applies passive decay to all emotions based on elapsed time.
     * Should be called once per simulation day.
     */
    void applyEmotionDecay(long currentTime);

    // ── Relationships (G2) ──────────────────────────────────────────────

    /**
     * Returns the relationship toward the given demon, or null if none exists.
     */
    @Nullable DemonRelationship getRelationship(UUID targetId);

    /**
     * Returns the relationship toward the given demon, creating one if absent.
     */
    DemonRelationship getOrCreateRelationship(UUID targetId);

    /**
     * Returns the full relationships map (unmodifiable view).
     */
    Map<UUID, DemonRelationship> getRelationships();

    // ── Intel (G4) ──────────────────────────────────────────────────────

    /**
     * Base intel-gathering ability derived from Behavior + Personality.
     * Range [0, 1]. Set once at creation, persisted.
     */
    float getBaseIntelGathering();

    void setBaseIntelGathering(float value);
}

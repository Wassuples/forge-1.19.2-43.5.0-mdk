package com.wassuples.demonslayer.nemesis.trait;

import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     */
    long getBirthTime();

    void setBirthTime(long birthTime);

    /**
     * World time (ticks) when the demon last participated in a notable battle.
     */
    long getLastBattleTime();

    void setLastBattleTime(long time);

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
}

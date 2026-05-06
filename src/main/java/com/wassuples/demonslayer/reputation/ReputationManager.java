package com.wassuples.demonslayer.reputation;

import com.wassuples.demonslayer.reputation.concealment.ConcealmentHandler;
import com.wassuples.demonslayer.reputation.concealment.SuppressionEntry;
import com.wassuples.demonslayer.reputation.concealment.SinRecord;
import com.wassuples.demonslayer.reputation.corps.CorpsSuspicionManager;
import com.wassuples.demonslayer.reputation.corps.InvestigationEvent;
import com.wassuples.demonslayer.reputation.witness.WitnessEvent;
import com.wassuples.demonslayer.reputation.witness.WitnessType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ReputationManager extends SavedData {
    private static final String DATA_NAME = "reputation";

    private final Map<UUID, PlayerRepRecord> playerRecords = new HashMap<>();
    private final Deque<WitnessEvent> witnessEventQueue = new ArrayDeque<>();
    private final Map<UUID, Set<HardlineFlag>> hardlineFlags = new HashMap<>();

    private final Map<UUID, List<SuppressionEntry>> suppressionLog = new HashMap<>();
    private final Map<UUID, InvestigationEvent> activeInvestigations = new HashMap<>();

    private long lastProcessedDay = -1L;

    public static ReputationManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(ReputationManager::load, ReputationManager::new, DATA_NAME);
    }

    public PlayerRepRecord getOrCreateRecord(UUID playerId) {
        return playerRecords.computeIfAbsent(playerId, id -> new PlayerRepRecord());
    }

    public Map<UUID, PlayerRepRecord> getPlayerRecords() {
        return playerRecords;
    }

    public Deque<WitnessEvent> getWitnessEventQueue() {
        return witnessEventQueue;
    }

    public Map<UUID, Set<HardlineFlag>> getHardlineFlags() {
        return hardlineFlags;
    }

    public Map<UUID, InvestigationEvent> getActiveInvestigations() {
        return activeInvestigations;
    }

    public void queueWitnessEvent(UUID playerId, RepEventType eventType, WitnessType witnessType, long gameTime) {
        witnessEventQueue.add(new WitnessEvent(playerId, eventType, witnessType, gameTime));
        setDirty();
    }

    public void applyAutoEvent(UUID playerId, RepEventType eventType, long worldDay) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        RepDelta delta = RepDeltaCalculator.forEvent(eventType);
        record.applyDelta(delta);

        if (delta.suspicion() > 0) {
            record.addSin(new SinRecord(eventType, Faction.DSHQ, worldDay));
        }

        syncHardlineCache(playerId, record);
        maybeTriggerInvestigation(playerId, record, worldDay * 24000L);
        setDirty();
    }

    public void processDailyTick(long gameTime) {
        long worldDay = gameTime / 24000L;
        if (worldDay <= lastProcessedDay) {
            return;
        }
        lastProcessedDay = worldDay;

        while (!witnessEventQueue.isEmpty()) {
            WitnessEvent witnessEvent = witnessEventQueue.poll();
            PlayerRepRecord record = getOrCreateRecord(witnessEvent.playerId());

            List<SuppressionEntry> suppressionEntries = suppressionLog.get(witnessEvent.playerId());
            ConcealmentHandler.pruneExpired(suppressionEntries, gameTime);
            if (ConcealmentHandler.isSuppressed(suppressionEntries, witnessEvent.witnessType(), gameTime)) {
                continue;
            }

            RepDelta delta = RepDeltaCalculator.forWitnessedEvent(
                    witnessEvent.eventType(),
                    EnumSet.of(witnessEvent.witnessType())
            );
            record.applyDelta(delta);

            if (delta.suspicion() > 0) {
                record.addSin(new SinRecord(witnessEvent.eventType(), Faction.DSHQ, worldDay));
            }

            syncHardlineCache(witnessEvent.playerId(), record);
            maybeTriggerInvestigation(witnessEvent.playerId(), record, gameTime);
        }

        long oldestAllowedDay = Math.max(0, worldDay - 14);
        for (PlayerRepRecord record : playerRecords.values()) {
            record.pruneSinsOlderThan(oldestAllowedDay);
        }

        setDirty();
    }

    public void addSuppression(UUID playerId, SuppressionEntry entry) {
        suppressionLog.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(entry);
    }

    public void clearSuppression(UUID playerId) {
        suppressionLog.remove(playerId);
    }

    public void setDimension(UUID playerId, ReputationDimension dimension, int value) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        record.setDimension(dimension, value);
        syncHardlineCache(playerId, record);
        setDirty();
    }

    public void addDimension(UUID playerId, ReputationDimension dimension, int delta) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        record.addDimension(dimension, delta);
        syncHardlineCache(playerId, record);
        setDirty();
    }

    public void resetPlayer(UUID playerId) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        record.clearForReset();
        hardlineFlags.remove(playerId);
        activeInvestigations.remove(playerId);
        suppressionLog.remove(playerId);
        setDirty();
    }

    public void setHardline(UUID playerId, HardlineFlag flag) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        record.addHardline(flag);
        syncHardlineCache(playerId, record);
        setDirty();
    }

    public void clearHardline(UUID playerId, HardlineFlag flag) {
        PlayerRepRecord record = getOrCreateRecord(playerId);
        record.removeHardline(flag);
        syncHardlineCache(playerId, record);
        setDirty();
    }

    public void triggerInvestigation(UUID playerId, long gameTime) {
        activeInvestigations.put(playerId, new InvestigationEvent(playerId, gameTime));
        setDirty();
    }

    private void maybeTriggerInvestigation(UUID playerId, PlayerRepRecord record, long gameTime) {
        if (CorpsSuspicionManager.shouldTriggerInvestigation(record)) {
            activeInvestigations.put(playerId, new InvestigationEvent(playerId, gameTime));
        }
    }

    private void syncHardlineCache(UUID playerId, PlayerRepRecord record) {
        if (record.getHardlineFlags().isEmpty()) {
            hardlineFlags.remove(playerId);
            return;
        }
        hardlineFlags.put(playerId, EnumSet.copyOf(record.getHardlineFlags()));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playersTag = new ListTag();
        for (Map.Entry<UUID, PlayerRepRecord> entry : playerRecords.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("playerId", entry.getKey());
            row.put("record", entry.getValue().save());
            playersTag.add(row);
        }
        tag.put("playerRecords", playersTag);

        ListTag witnessTag = new ListTag();
        for (WitnessEvent witnessEvent : witnessEventQueue) {
            witnessTag.add(witnessEvent.save());
        }
        tag.put("witnessEventQueue", witnessTag);

        ListTag hardlineTag = new ListTag();
        for (Map.Entry<UUID, Set<HardlineFlag>> entry : hardlineFlags.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("playerId", entry.getKey());

            ListTag flags = new ListTag();
            for (HardlineFlag flag : entry.getValue()) {
                CompoundTag flagTag = new CompoundTag();
                flagTag.putString("name", flag.name());
                flags.add(flagTag);
            }
            row.put("flags", flags);
            hardlineTag.add(row);
        }
        tag.put("hardlineFlags", hardlineTag);
        tag.putLong("lastProcessedDay", lastProcessedDay);
        return tag;
    }

    public static ReputationManager load(CompoundTag tag) {
        ReputationManager manager = new ReputationManager();

        ListTag playersTag = tag.getList("playerRecords", Tag.TAG_COMPOUND);
        for (Tag entry : playersTag) {
            CompoundTag row = (CompoundTag) entry;
            UUID playerId = row.getUUID("playerId");
            PlayerRepRecord record = PlayerRepRecord.load(row.getCompound("record"));
            manager.playerRecords.put(playerId, record);
            manager.syncHardlineCache(playerId, record);
        }

        ListTag witnessTag = tag.getList("witnessEventQueue", Tag.TAG_COMPOUND);
        for (Tag entry : witnessTag) {
            manager.witnessEventQueue.add(WitnessEvent.load((CompoundTag) entry));
        }

        ListTag hardlineTag = tag.getList("hardlineFlags", Tag.TAG_COMPOUND);
        for (Tag entry : hardlineTag) {
            CompoundTag row = (CompoundTag) entry;
            UUID playerId = row.getUUID("playerId");
            ListTag flags = row.getList("flags", Tag.TAG_COMPOUND);
            EnumSet<HardlineFlag> loadedFlags = EnumSet.noneOf(HardlineFlag.class);
            for (Tag flagTag : flags) {
                String flagName = ((CompoundTag) flagTag).getString("name");
                try {
                    loadedFlags.add(HardlineFlag.valueOf(flagName));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from older/newer saves.
                }
            }
            if (!loadedFlags.isEmpty()) {
                manager.hardlineFlags.put(playerId, loadedFlags);
                manager.getOrCreateRecord(playerId).getHardlineFlags().addAll(loadedFlags);
            }
        }

        manager.lastProcessedDay = tag.getLong("lastProcessedDay");
        return manager;
    }
}


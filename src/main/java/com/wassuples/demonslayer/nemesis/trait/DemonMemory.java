package com.wassuples.demonslayer.nemesis.trait;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An immutable record of a single factual event that a demon "remembers".
 * <p>
 * Design rules (from Nemesis System TODO):
 * <ul>
 *   <li>Memories are <b>facts</b> ("what happened"), never feelings.</li>
 *   <li>Each memory carries a mandatory {@link MemoryType} + timestamp,
 *       plus optional source/target entity UUIDs and a free-form context map.</li>
 *   <li>Memories are stored in a bounded list on {@link DemonTrait} (recent-first).</li>
 * </ul>
 */
public final class DemonMemory {

    // ── Well-known context-map keys (use these constants to avoid typos) ────
    public static final String KEY_STYLE    = "style";
    public static final String KEY_BIOME    = "biome";
    public static final String KEY_DAMAGE   = "damageTag";
    public static final String KEY_BATTLE   = "battleId";
    public static final String KEY_RANK     = "rank";

    // ── Fields ──────────────────────────────────────────────────────────────
    private final MemoryType type;
    private final long timestamp;
    @Nullable private final UUID sourceEntity;
    @Nullable private final UUID targetEntity;
    private final Map<String, String> context;

    // ── Construction ────────────────────────────────────────────────────────

    private DemonMemory(MemoryType type, long timestamp,
                        @Nullable UUID sourceEntity, @Nullable UUID targetEntity,
                        Map<String, String> context) {
        this.type = type;
        this.timestamp = timestamp;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.context = context == null ? Collections.emptyMap()
                                       : Collections.unmodifiableMap(new HashMap<>(context));
    }

    /**
     * Creates a simple memory with only type and timestamp.
     */
    public static DemonMemory of(MemoryType type, long timestamp) {
        return new DemonMemory(type, timestamp, null, null, null);
    }

    /**
     * Creates a memory with source and target entities.
     */
    public static DemonMemory of(MemoryType type, long timestamp,
                                 @Nullable UUID source, @Nullable UUID target) {
        return new DemonMemory(type, timestamp, source, target, null);
    }

    /**
     * Creates a fully-specified memory with context.
     */
    public static DemonMemory of(MemoryType type, long timestamp,
                                 @Nullable UUID source, @Nullable UUID target,
                                 @Nullable Map<String, String> context) {
        return new DemonMemory(type, timestamp, source, target, context);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public MemoryType getType() { return this.type; }

    public long getTimestamp() { return this.timestamp; }

    @Nullable
    public UUID getSourceEntity() { return this.sourceEntity; }

    @Nullable
    public UUID getTargetEntity() { return this.targetEntity; }

    /**
     * Returns an unmodifiable view of the context map.
     */
    public Map<String, String> getContext() { return this.context; }

    /**
     * Convenience: look up a single context value.
     */
    @Nullable
    public String getContextValue(String key) { return this.context.get(key); }

    // ── NBT Serialization ───────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", this.type.name());
        nbt.putLong("timestamp", this.timestamp);

        if (this.sourceEntity != null) {
            nbt.putUUID("source", this.sourceEntity);
        }
        if (this.targetEntity != null) {
            nbt.putUUID("target", this.targetEntity);
        }

        if (!this.context.isEmpty()) {
            CompoundTag ctxTag = new CompoundTag();
            for (Map.Entry<String, String> entry : this.context.entrySet()) {
                ctxTag.putString(entry.getKey(), entry.getValue());
            }
            nbt.put("context", ctxTag);
        }

        return nbt;
    }

    /**
     * Deserializes a DemonMemory from NBT.
     *
     * @throws IllegalArgumentException if the MemoryType is unknown or missing
     */
    public static DemonMemory deserializeNBT(CompoundTag nbt) {
        MemoryType type = MemoryType.valueOf(nbt.getString("type"));
        long timestamp = nbt.getLong("timestamp");

        UUID source = nbt.hasUUID("source") ? nbt.getUUID("source") : null;
        UUID target = nbt.hasUUID("target") ? nbt.getUUID("target") : null;

        Map<String, String> context = null;
        if (nbt.contains("context")) {
            CompoundTag ctxTag = nbt.getCompound("context");
            context = new HashMap<>();
            for (String key : ctxTag.getAllKeys()) {
                context.put(key, ctxTag.getString(key));
            }
        }

        return new DemonMemory(type, timestamp, source, target, context);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DemonMemory{");
        sb.append("type=").append(type);
        sb.append(", ts=").append(timestamp);
        if (sourceEntity != null) sb.append(", src=").append(sourceEntity);
        if (targetEntity != null) sb.append(", tgt=").append(targetEntity);
        if (!context.isEmpty()) sb.append(", ctx=").append(context);
        sb.append('}');
        return sb.toString();
    }
}


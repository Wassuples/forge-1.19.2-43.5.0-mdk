package com.wassuples.demonslayer.nemesis.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A single event in the world story log (H1).
 * Immutable after construction. Persisted via NBT.
 */
public class StoryEvent {

    private final StoryEventType type;
    private final long timestamp;
    private final List<UUID> involvedDemons;
    @Nullable private final UUID involvedPlayer;
    private final Map<String, String> context;
    private final String descriptionTemplate;

    public StoryEvent(StoryEventType type, long timestamp,
                      List<UUID> involvedDemons, @Nullable UUID involvedPlayer,
                      Map<String, String> context, String descriptionTemplate) {
        this.type = type;
        this.timestamp = timestamp;
        this.involvedDemons = List.copyOf(involvedDemons);
        this.involvedPlayer = involvedPlayer;
        this.context = Map.copyOf(context);
        this.descriptionTemplate = descriptionTemplate;
    }

    // ── Getters ─────────────────────────────────────────────────────────

    public StoryEventType getType()          { return type; }
    public long getTimestamp()               { return timestamp; }
    public List<UUID> getInvolvedDemons()    { return involvedDemons; }
    @Nullable public UUID getInvolvedPlayer(){ return involvedPlayer; }
    public Map<String, String> getContext()  { return context; }
    public String getDescriptionTemplate()   { return descriptionTemplate; }

    // ── Rendering ───────────────────────────────────────────────────────

    /**
     * Renders the full description, replacing all placeholders with context values.
     */
    public String renderFull() {
        String result = descriptionTemplate;
        for (Map.Entry<String, String> e : context.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    /**
     * Renders a vague rumor version — hides names and specifics.
     */
    public String renderRumor() {
        String result = descriptionTemplate;
        result = result.replace("{demon1}", "a mysterious demon");
        result = result.replace("{demon2}", "another demon");
        result = result.replace("{rank}", "a Kizuki rank");
        result = result.replace("{player}", "a warrior");
        // Leave other placeholders replaced from context
        for (Map.Entry<String, String> e : context.entrySet()) {
            if (!e.getKey().equals("demon1") && !e.getKey().equals("demon2")
                    && !e.getKey().equals("rank") && !e.getKey().equals("player")) {
                result = result.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return result;
    }

    /**
     * Renders a confirmed version — shows names but hides reasons/details.
     */
    public String renderConfirmed() {
        String result = descriptionTemplate;
        for (Map.Entry<String, String> e : context.entrySet()) {
            // Hide detail fields like "reason", "outcome_detail"
            if (e.getKey().equals("reason") || e.getKey().equals("outcome_detail")) {
                result = result.replace("{" + e.getKey() + "}", "unknown reasons");
            } else {
                result = result.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return result;
    }

    // ── Factory methods ─────────────────────────────────────────────────

    public static StoryEvent battle(long time, UUID demon1, UUID demon2,
                                     String name1, String name2, String outcome) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name1);
        ctx.put("demon2", name2);
        ctx.put("outcome", outcome);
        return new StoryEvent(StoryEventType.BATTLE_OCCURRED, time,
                List.of(demon1, demon2), null, ctx,
                "{demon1} fought {demon2}. Outcome: {outcome}.");
    }

    public static StoryEvent promotion(long time, UUID demonId, String name, String rank) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name);
        ctx.put("rank", rank);
        return new StoryEvent(StoryEventType.PROMOTION, time,
                List.of(demonId), null, ctx,
                "{demon1} has been promoted to {rank}.");
    }

    public static StoryEvent demotion(long time, UUID demonId, String name, String reason) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name);
        ctx.put("reason", reason);
        return new StoryEvent(StoryEventType.DEMOTION, time,
                List.of(demonId), null, ctx,
                "{demon1} has been demoted. Reason: {reason}.");
    }

    public static StoryEvent hunterSent(long time, UUID hunterId, String hunterName,
                                         UUID playerId, String playerName) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", hunterName);
        ctx.put("player", playerName);
        return new StoryEvent(StoryEventType.HUNTER_SENT, time,
                List.of(hunterId), playerId, ctx,
                "{demon1} has been dispatched to hunt {player}.");
    }

    public static StoryEvent demonKilled(long time, UUID demonId, String demonName,
                                          @Nullable UUID killerId, String killerName) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", demonName);
        ctx.put("killer", killerName);
        return new StoryEvent(StoryEventType.DEMON_KILLED, time,
                List.of(demonId), killerId, ctx,
                "{demon1} has been slain by {killer}.");
    }

    public static StoryEvent playerKilled(long time, UUID demonId, String demonName,
                                           UUID playerId, String playerName) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", demonName);
        ctx.put("player", playerName);
        return new StoryEvent(StoryEventType.PLAYER_KILLED, time,
                List.of(demonId), playerId, ctx,
                "{demon1} has killed {player}.");
    }

    public static StoryEvent evolution(long time, UUID demonId, String name, String newTitle) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name);
        ctx.put("title", newTitle);
        return new StoryEvent(StoryEventType.EVOLUTION, time,
                List.of(demonId), null, ctx,
                "{demon1} has evolved, now known as {demon1}, {title}!");
    }

    public static StoryEvent retreat(long time, UUID demon1, UUID demon2,
                                      String name1, String name2) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name1);
        ctx.put("demon2", name2);
        return new StoryEvent(StoryEventType.RETREAT, time,
                List.of(demon1, demon2), null, ctx,
                "{demon2} retreated from battle with {demon1}.");
    }

    public static StoryEvent worldBattle(long time, UUID demon1, UUID demon2,
                                          String name1, String name2) {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("demon1", name1);
        ctx.put("demon2", name2);
        return new StoryEvent(StoryEventType.DEMON_VS_DEMON_WORLD_BATTLE, time,
                List.of(demon1, demon2), null, ctx,
                "{demon1} and {demon2} clashed in a blood battle that could be witnessed by all.");
    }

    // ── NBT Persistence ─────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", type.name());
        nbt.putLong("timestamp", timestamp);

        ListTag demonsTag = new ListTag();
        involvedDemons.forEach(id -> demonsTag.add(StringTag.valueOf(id.toString())));
        nbt.put("involvedDemons", demonsTag);

        if (involvedPlayer != null) {
            nbt.putUUID("involvedPlayer", involvedPlayer);
        }

        CompoundTag ctxTag = new CompoundTag();
        context.forEach(ctxTag::putString);
        nbt.put("context", ctxTag);

        nbt.putString("descriptionTemplate", descriptionTemplate);
        return nbt;
    }

    public static StoryEvent fromNBT(CompoundTag nbt) {
        StoryEventType type = StoryEventType.valueOf(nbt.getString("type"));
        long timestamp = nbt.getLong("timestamp");

        List<UUID> demons = new ArrayList<>();
        ListTag demonsTag = nbt.getList("involvedDemons", 8);
        demonsTag.forEach(tag -> demons.add(UUID.fromString(tag.getAsString())));

        UUID player = nbt.contains("involvedPlayer") ? nbt.getUUID("involvedPlayer") : null;

        Map<String, String> ctx = new LinkedHashMap<>();
        CompoundTag ctxTag = nbt.getCompound("context");
        for (String key : ctxTag.getAllKeys()) {
            ctx.put(key, ctxTag.getString(key));
        }

        String template = nbt.getString("descriptionTemplate");
        return new StoryEvent(type, timestamp, demons, player, ctx, template);
    }
}


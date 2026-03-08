package com.wassuples.demonslayer.nemesis.story;

import com.wassuples.demonslayer.crow.PlayerCrowProvider;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages player-specific intel tiers on story events (H2),
 * rumor spreading rules (H4), and weekly digest summaries (H3).
 * <p>
 * Stored inside {@code DemonHierarchyManager} and serialized alongside it.
 */
public class PlayerIntelManager {

    /** Visibility tiers for story events (H2). */
    public enum IntelTier {
        /** Vague hints — no names, no specifics. */
        RUMOR,
        /** Names visible, but motives/details hidden. */
        CONFIRMED,
        /** Full information including rank, reason, style. */
        FULL
    }

    /**
     * Per-player intel state. Key = player UUID.
     * Inner map: event index (position in story log) → tier.
     * We use event timestamp as key since log is bounded/evicting.
     */
    private final Map<UUID, Map<Long, IntelTier>> playerIntel = new HashMap<>();

    private static final long TICKS_PER_DAY = 24_000L;
    private static final long DIGEST_WINDOW_TICKS = 7 * TICKS_PER_DAY; // 7 in-game days

    // ── Intel access ────────────────────────────────────────────────────

    /**
     * Returns the intel tier a player has for a specific event timestamp.
     * Defaults to {@code null} (no intel) if not known.
     */
    @Nullable
    public IntelTier getIntel(UUID playerId, long eventTimestamp) {
        Map<Long, IntelTier> map = playerIntel.get(playerId);
        return map != null ? map.get(eventTimestamp) : null;
    }

    /**
     * Sets (or upgrades) intel for a player on a specific event.
     * Never downgrades — if the player already has FULL, it stays FULL.
     */
    public void setIntel(UUID playerId, long eventTimestamp, IntelTier tier) {
        Map<Long, IntelTier> map = playerIntel.computeIfAbsent(playerId, k -> new HashMap<>());
        IntelTier existing = map.get(eventTimestamp);
        if (existing == null || tier.ordinal() > existing.ordinal()) {
            map.put(eventTimestamp, tier);
        }
    }

    /**
     * Renders a story event for a specific player, respecting their intel tier.
     */
    public String renderForPlayer(StoryEvent event, UUID playerId) {
        IntelTier tier = getIntel(playerId, event.getTimestamp());
        if (tier == null) return null; // player doesn't know about this event
        return switch (tier) {
            case RUMOR -> event.renderRumor();
            case CONFIRMED -> event.renderConfirmed();
            case FULL -> event.renderFull();
        };
    }

    // ── Rumor spread (H4) ───────────────────────────────────────────────

    /**
     * Spreads rumors about recent story events to online players.
     * Called once per daily simulation.
     *
     * @param storyLog       the world story log (newest first)
     * @param players        online players
     * @param demonTraitLookup function to resolve demon UUID → trait
     * @param kizukiRankLookup function to check if a demon is Kizuki-ranked
     */
    public void spreadRumors(List<StoryEvent> storyLog,
                              List<ServerPlayer> players,
                              java.util.function.Function<UUID, DemonTrait> demonTraitLookup,
                              java.util.function.Predicate<UUID> kizukiRankLookup) {
        if (storyLog.isEmpty() || players.isEmpty()) return;

        // Only process events from the last 3 days
        long cutoff = storyLog.get(0).getTimestamp() - (3 * TICKS_PER_DAY);

        for (StoryEvent event : storyLog) {
            if (event.getTimestamp() < cutoff) break;

            boolean isKizukiEvent = event.getInvolvedDemons().stream().anyMatch(kizukiRankLookup::test);

            for (ServerPlayer player : players) {
                UUID playerId = player.getUUID();

                // Rule 1: Kizuki events → everyone gets RUMOR
                if (isKizukiEvent) {
                    setIntel(playerId, event.getTimestamp(), IntelTier.RUMOR);
                }

                // Rule 2: Proximity → CONFIRMED if in demon's territory
                ResourceLocation playerBiome = player.level.getBiome(player.blockPosition())
                        .unwrapKey().map(k -> k.location()).orElse(null);
                if (playerBiome != null) {
                    for (UUID demonId : event.getInvolvedDemons()) {
                        DemonTrait trait = demonTraitLookup.apply(demonId);
                        if (trait != null && trait.getTerritory().equals(playerBiome)) {
                            setIntel(playerId, event.getTimestamp(), IntelTier.CONFIRMED);
                        }
                    }
                }

                // Rule 3: Crow system → upgrade one RUMOR to CONFIRMED per day
                player.getCapability(PlayerCrowProvider.PLAYER_CROW).ifPresent(crowData -> {
                    if (crowData.hasCrow()) {
                        Map<Long, IntelTier> myIntel = playerIntel.get(playerId);
                        if (myIntel != null) {
                            Optional<Long> rumorToUpgrade = myIntel.entrySet().stream()
                                    .filter(e -> e.getValue() == IntelTier.RUMOR)
                                    .map(Map.Entry::getKey)
                                    .findFirst();
                            rumorToUpgrade.ifPresent(ts -> myIntel.put(ts, IntelTier.CONFIRMED));
                        }
                    }
                });

                // Rule 4: Loud demons broadcast as RUMOR
                for (UUID demonId : event.getInvolvedDemons()) {
                    DemonTrait trait = demonTraitLookup.apply(demonId);
                    if (trait != null) {
                        var p = trait.getPersonality();
                        if (p == com.wassuples.demonslayer.nemesis.ai.Personality.MOCKING
                                || p == com.wassuples.demonslayer.nemesis.ai.Personality.MELODRAMATIC
                                || p == com.wassuples.demonslayer.nemesis.ai.Personality.COCKY) {
                            setIntel(playerId, event.getTimestamp(), IntelTier.RUMOR);
                        }
                    }
                }

                // Rule 5: Player directly involved → FULL
                if (playerId.equals(event.getInvolvedPlayer())) {
                    setIntel(playerId, event.getTimestamp(), IntelTier.FULL);
                }
            }
        }
    }

    // ── Weekly digest (H3) ──────────────────────────────────────────────

    /**
     * Generates a weekly digest for a player, summarizing the last 7
     * in-game days of story events they have intel on.
     */
    public Component generateWeeklyDigest(UUID playerId, List<StoryEvent> storyLog, long currentTime) {
        long windowStart = currentTime - DIGEST_WINDOW_TICKS;

        Map<StoryEventType, Integer> counts = new EnumMap<>(StoryEventType.class);
        List<String> highlights = new ArrayList<>();
        int maxHighlights = 5;

        for (StoryEvent event : storyLog) {
            if (event.getTimestamp() < windowStart) break;

            String rendered = renderForPlayer(event, playerId);
            if (rendered == null) continue;

            counts.merge(event.getType(), 1, Integer::sum);
            if (highlights.size() < maxHighlights) {
                highlights.add(rendered);
            }
        }

        if (counts.isEmpty()) {
            return Component.literal("§7[Weekly Report] Nothing of note happened this week.")
                    .withStyle(ChatFormatting.GRAY);
        }

        MutableComponent digest = Component.literal("§6═══ Weekly Nemesis Report ═══\n")
                .withStyle(ChatFormatting.GOLD);

        // Summary counts
        for (Map.Entry<StoryEventType, Integer> e : counts.entrySet()) {
            String label = formatEventTypeName(e.getKey());
            digest.append(Component.literal("  §7" + label + ": §f" + e.getValue() + "\n"));
        }

        // Highlights
        if (!highlights.isEmpty()) {
            digest.append(Component.literal("§6── Highlights ──\n"));
            for (String h : highlights) {
                digest.append(Component.literal("  §7• " + h + "\n"));
            }
        }

        digest.append(Component.literal("§6══════════════════════════"));
        return digest;
    }

    private static String formatEventTypeName(StoryEventType type) {
        return switch (type) {
            case BATTLE_OCCURRED -> "Battles";
            case PROMOTION -> "Promotions";
            case DEMOTION -> "Demotions";
            case BETRAYAL -> "Betrayals";
            case HUNTER_SENT -> "Hunters dispatched";
            case DEMON_KILLED -> "Demons slain";
            case PLAYER_KILLED -> "Players killed";
            case POWER_VACUUM -> "Power vacuums";
            case DEMON_VS_DEMON_WORLD_BATTLE -> "World battles";
            case EVOLUTION -> "Evolutions";
            case RIVALRY_FORMED -> "Rivalries formed";
            case RETREAT -> "Retreats";
            case BLOOD_BATTLE_CONCLUDED -> "Blood battles";
        };
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    /**
     * Removes intel entries for events older than the given cutoff.
     * Called during daily sim to prevent unbounded growth.
     */
    public void pruneOldIntel(long oldestEventTimestamp) {
        for (Map<Long, IntelTier> map : playerIntel.values()) {
            map.entrySet().removeIf(e -> e.getKey() < oldestEventTimestamp);
        }
        playerIntel.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    // ── NBT Persistence ─────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<UUID, Map<Long, IntelTier>> entry : playerIntel.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            for (Map.Entry<Long, IntelTier> ie : entry.getValue().entrySet()) {
                playerTag.putString(ie.getKey().toString(), ie.getValue().name());
            }
            nbt.put(entry.getKey().toString(), playerTag);
        }
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        playerIntel.clear();
        for (String playerKey : nbt.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(playerKey);
                CompoundTag playerTag = nbt.getCompound(playerKey);
                Map<Long, IntelTier> map = new HashMap<>();
                for (String tsKey : playerTag.getAllKeys()) {
                    try {
                        long ts = Long.parseLong(tsKey);
                        IntelTier tier = IntelTier.valueOf(playerTag.getString(tsKey));
                        map.put(ts, tier);
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!map.isEmpty()) {
                    playerIntel.put(playerId, map);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }
}


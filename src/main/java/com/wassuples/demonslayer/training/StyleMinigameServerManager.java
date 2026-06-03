package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * World-persistent server-side manager for style-minigame state.
 *
 * <p>Tracks:
 * <ul>
 *   <li><b>Cooldowns</b> — per-player, per-style game-tick timestamps after which retrying is allowed
 *       (used by {@link StyleMinigameType.FailureConsequence#COOLDOWN_ONE_DAY} and {@link
 *       StyleMinigameType.FailureConsequence#RETRY_AFTER_200T} consequences).</li>
 *   <li><b>Lives</b> — remaining lives for styles that grant a limited pool
 *       ({@link StyleMinigameType#FLAME}, {@link StyleMinigameType#SERPENT}).</li>
 *   <li><b>Active sessions</b> — volatile (not saved to disk) map of players currently inside a
 *       minigame, used to associate an incoming result packet with the correct style and intent.</li>
 * </ul>
 *
 * <p>Usage on the server side:
 * <pre>{@code
 * StyleMinigameServerManager mgr = StyleMinigameServerManager.get(serverLevel);
 * mgr.startSession(player.getUUID(), style, isTraining, teacherUUID);
 * // … player finishes or fails …
 * StyleMinigameSession session = mgr.popSession(player.getUUID());
 * }</pre>
 */
public class StyleMinigameServerManager extends SavedData {

    private static final String DATA_NAME = "style_minigame_manager";

    // ── persistent state ─────────────────────────────────────────────────────
    /** cooldownEndTick: playerId → (style → game-tick at which retry is allowed). */
    private final Map<UUID, Map<BreathingStyles, Long>> cooldowns = new HashMap<>();
    /** livesRemaining: playerId → (style → lives left in the current pool). */
    private final Map<UUID, Map<BreathingStyles, Integer>> livesRemaining = new HashMap<>();

    // ── volatile state (not saved) ────────────────────────────────────────────
    private final Map<UUID, StyleMinigameSession> activeSessions = new HashMap<>();

    // ── singleton access ─────────────────────────────────────────────────────

    public static StyleMinigameServerManager get(ServerLevel level) {
        return level.getServer().overworld()
                .getDataStorage()
                .computeIfAbsent(StyleMinigameServerManager::load,
                        StyleMinigameServerManager::new,
                        DATA_NAME);
    }

    // ── session management ────────────────────────────────────────────────────

    /**
     * Opens an active session for the given player. Resets lives to the type's starting value if
     * this is a fresh attempt (i.e. no lives currently tracked or lives hit 0 and were cleared).
     */
    public void startSession(UUID playerId, BreathingStyles style, boolean isTraining, UUID teacherUUID) {
        StyleMinigameType type = StyleMinigameType.forStyle(style);
        if (type.hasLives()) {
            Map<BreathingStyles, Integer> playerLives = livesRemaining.computeIfAbsent(playerId, k -> new HashMap<>());
            playerLives.putIfAbsent(style, type.getStartingLives());
        }
        activeSessions.put(playerId, new StyleMinigameSession(playerId, style, isTraining, teacherUUID));
    }

    /**
     * Removes and returns the active session (or {@code null} if none exists).
     * Called by the result-packet handler when processing a result from the client.
     */
    public StyleMinigameSession popSession(UUID playerId) {
        return activeSessions.remove(playerId);
    }

    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    // ── cooldown helpers ──────────────────────────────────────────────────────

    /**
     * Records a failure and applies the appropriate cooldown / life-deduction for the given style.
     *
     * @param currentTick the current server game tick ({@code level.getGameTime()})
     * @return {@code true} if the player is now blocked (cooldown set or out of lives)
     */
    public boolean recordFailure(UUID playerId, BreathingStyles style, long currentTick) {
        StyleMinigameType type = StyleMinigameType.forStyle(style);

        switch (type.getFailureConsequence()) {
            case COOLDOWN_ONE_DAY -> {
                if (type.hasLives()) {
                    // Deduct a life; cooldown triggers only when all lives are gone.
                    int remaining = deductLife(playerId, style);
                    if (remaining <= 0) {
                        applyCooldown(playerId, style, currentTick + 24_000L);
                        return true; // blocked by cooldown
                    }
                    return false; // still has lives
                } else {
                    applyCooldown(playerId, style, currentTick + 24_000L);
                    return true;
                }
            }
            case RETRY_AFTER_200T -> {
                applyCooldown(playerId, style, currentTick + 200L);
                return true;
            }
            default -> { return false; } // RETRY_IMMEDIATELY
        }
    }

    public boolean isOnCooldown(UUID playerId, BreathingStyles style, long currentTick) {
        Map<BreathingStyles, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Long endTick = playerCooldowns.get(style);
        return endTick != null && currentTick < endTick;
    }

    /** Returns the game tick at which the cooldown ends, or -1 if not on cooldown. */
    public long getCooldownEndTick(UUID playerId, BreathingStyles style) {
        Map<BreathingStyles, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return -1L;
        return playerCooldowns.getOrDefault(style, -1L);
    }

    public int getLivesRemaining(UUID playerId, BreathingStyles style) {
        Map<BreathingStyles, Integer> playerLives = livesRemaining.get(playerId);
        if (playerLives == null) return StyleMinigameType.forStyle(style).getStartingLives();
        return playerLives.getOrDefault(style, StyleMinigameType.forStyle(style).getStartingLives());
    }

    /** Called after a successful pass to reset the player's lives pool for that style. */
    public void resetLives(UUID playerId, BreathingStyles style) {
        Map<BreathingStyles, Integer> playerLives = livesRemaining.get(playerId);
        if (playerLives != null) playerLives.remove(style);
        setDirty();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void applyCooldown(UUID playerId, BreathingStyles style, long endTick) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(style, endTick);
        setDirty();
    }

    /** Returns remaining lives after deduction (minimum 0). */
    private int deductLife(UUID playerId, BreathingStyles style) {
        Map<BreathingStyles, Integer> playerLives =
                livesRemaining.computeIfAbsent(playerId, k -> new HashMap<>());
        int current = playerLives.getOrDefault(style,
                StyleMinigameType.forStyle(style).getStartingLives());
        int updated = Math.max(0, current - 1);
        playerLives.put(style, updated);
        setDirty();
        return updated;
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        // cooldowns
        ListTag cooldownsTag = new ListTag();
        for (Map.Entry<UUID, Map<BreathingStyles, Long>> entry : cooldowns.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("playerId", entry.getKey());
            CompoundTag styleMap = new CompoundTag();
            entry.getValue().forEach((style, tick) -> styleMap.putLong(style.name(), tick));
            row.put("styleMap", styleMap);
            cooldownsTag.add(row);
        }
        tag.put("cooldowns", cooldownsTag);

        // lives
        ListTag livesTag = new ListTag();
        for (Map.Entry<UUID, Map<BreathingStyles, Integer>> entry : livesRemaining.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("playerId", entry.getKey());
            CompoundTag styleMap = new CompoundTag();
            entry.getValue().forEach((style, lives) -> styleMap.putInt(style.name(), lives));
            row.put("styleMap", styleMap);
            livesTag.add(row);
        }
        tag.put("lives", livesTag);

        return tag;
    }

    public static StyleMinigameServerManager load(CompoundTag tag) {
        StyleMinigameServerManager mgr = new StyleMinigameServerManager();

        ListTag cooldownsTag = tag.getList("cooldowns", Tag.TAG_COMPOUND);
        for (Tag entry : cooldownsTag) {
            CompoundTag row = (CompoundTag) entry;
            UUID id = row.getUUID("playerId");
            CompoundTag styleMap = row.getCompound("styleMap");
            Map<BreathingStyles, Long> map = mgr.cooldowns.computeIfAbsent(id, k -> new HashMap<>());
            styleMap.getAllKeys().forEach(key -> {
                try { map.put(BreathingStyles.valueOf(key), styleMap.getLong(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        ListTag livesTag = tag.getList("lives", Tag.TAG_COMPOUND);
        for (Tag entry : livesTag) {
            CompoundTag row = (CompoundTag) entry;
            UUID id = row.getUUID("playerId");
            CompoundTag styleMap = row.getCompound("styleMap");
            Map<BreathingStyles, Integer> map = mgr.livesRemaining.computeIfAbsent(id, k -> new HashMap<>());
            styleMap.getAllKeys().forEach(key -> {
                try { map.put(BreathingStyles.valueOf(key), styleMap.getInt(key)); }
                catch (IllegalArgumentException ignored) {}
            });
        }

        return mgr;
    }
}

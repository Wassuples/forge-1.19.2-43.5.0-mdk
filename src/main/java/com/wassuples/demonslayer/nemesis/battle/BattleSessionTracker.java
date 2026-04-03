package com.wassuples.demonslayer.nemesis.battle;

import com.wassuples.demonslayer.nemesis.trait.DemonMemory;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.nemesis.trait.GrowthStats;
import com.wassuples.demonslayer.nemesis.trait.MemoryType;

import java.util.*;
import java.util.function.Function;

/**
 * Singleton-per-server battle session manager. Tracks all active
 * demon-vs-opponent engagements and finalizes them into
 * {@link BattleOutcome} + {@link DemonMemory} facts when the session
 * concludes (via death, flee, or timeout).
 * <p>
 * <b>Transient</b> — not serialized. Only the resulting memories and
 * growth stat increments are persisted on the {@link DemonTrait}.
 * <p>
 * Thread safety: all Forge events fire on the server thread in 1.19.2,
 * so no synchronization is needed.
 */
public class BattleSessionTracker {

    /** Composite key for the session map. */
    public record SessionKey(UUID demonId, UUID opponentId) {}

    private final Map<SessionKey, BattleSession> activeSessions = new HashMap<>();

    /**
     * Lookup function to resolve a demon UUID to its trait.
     * Injected at construction time so the tracker doesn't directly
     * depend on DemonHierarchyManager.
     */
    private final Function<UUID, DemonTrait> traitLookup;

    public BattleSessionTracker(Function<UUID, DemonTrait> traitLookup) {
        this.traitLookup = traitLookup;
    }

    // ── Recording events ────────────────────────────────────────────────

    /**
     * Records a damage event between a demon and its opponent. Creates a
     * new session if one doesn't already exist.
     *
     * @param demonId           UUID of the nemesis demon
     * @param opponentId        UUID of the opponent (player or demon)
     * @param amount            positive damage amount
     * @param demonDealtDamage  true if the demon dealt the damage
     * @param demonHealthPct    demon's current health ratio (0–1)
     * @param opponentHealthPct opponent's current health ratio (0–1)
     * @param opponentIsPlayer  whether the opponent is a player
     * @param currentTime       world tick time
     */
    public void recordDamage(UUID demonId, UUID opponentId, float amount,
                             boolean demonDealtDamage,
                             float demonHealthPct, float opponentHealthPct,
                             boolean opponentIsPlayer, long currentTime) {
        SessionKey key = new SessionKey(demonId, opponentId);
        boolean isNew = !activeSessions.containsKey(key);
        BattleSession session = activeSessions.computeIfAbsent(key,
                k -> new BattleSession(demonId, opponentId, currentTime, opponentIsPlayer));
        session.recordDamage(amount, demonDealtDamage, demonHealthPct, opponentHealthPct, currentTime);
        if (isNew) {
            System.out.println("[NEMESIS DEBUG] Battle session STARTED: demon=" + demonId.toString().substring(0, 8) +
                " vs opponent=" + opponentId.toString().substring(0, 8) + " (player=" + opponentIsPlayer + ")");
        }
    }

    /**
     * Records that either the demon or its opponent died.
     */
    public void recordDeath(UUID demonId, UUID opponentId, boolean demonDied, long currentTime) {
        SessionKey key = new SessionKey(demonId, opponentId);
        BattleSession session = activeSessions.get(key);
        if (session == null) return;

        System.out.println("[NEMESIS DEBUG] Battle session DEATH: " + (demonDied ? "demon" : "opponent") +
            " died (demon=" + demonId.toString().substring(0, 8) + ", opponent=" + opponentId.toString().substring(0, 8) + ")");

        if (demonDied) {
            session.markDemonDied();
        } else {
            session.markOpponentDied();
        }
        finalizeSession(key, currentTime);
    }

    /**
     * Records that the demon fled the battle.
     */
    public void recordFlee(UUID demonId, UUID opponentId, long currentTime) {
        SessionKey key = new SessionKey(demonId, opponentId);
        BattleSession session = activeSessions.get(key);
        if (session == null) return;

        System.out.println("[NEMESIS DEBUG] Battle session FLEE: demon=" + demonId.toString().substring(0, 8) +
            " fled from opponent=" + opponentId.toString().substring(0, 8));

        session.markDemonFled();
        finalizeSession(key, currentTime);
    }

    // ── Periodic tick ───────────────────────────────────────────────────

    /**
     * Checks all active sessions for expiry. Call every ~20 ticks from
     * the server tick event.
     */
    public void tick(long currentTime) {
        List<SessionKey> expired = new ArrayList<>();
        for (Map.Entry<SessionKey, BattleSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().isExpired(currentTime)) {
                expired.add(entry.getKey());
            }
        }
        for (SessionKey key : expired) {
            finalizeSession(key, currentTime);
        }
    }

    // ── Session finalization ────────────────────────────────────────────

    /**
     * Finalizes a session: derives the outcome, writes a {@link DemonMemory}
     * to the demon's trait, updates growth stats, and removes the session.
     */
    private void finalizeSession(SessionKey key, long currentTime) {
        BattleSession session = activeSessions.remove(key);
        if (session == null) return;

        BattleOutcome outcome = BattleOutcome.derive(session, currentTime);
        DemonTrait trait = traitLookup.apply(key.demonId());
        if (trait == null) return;

        System.out.println("[NEMESIS DEBUG] Battle session FINALIZED: demon=" + trait.getName() +
            " outcome=" + outcome.memoryType() +
            " (dmgDealt=" + String.format("%.1f", session.getDamageDealt()) +
            ", dmgTaken=" + String.format("%.1f", session.getDamageTaken()) +
            ", nearDeath=" + outcome.wasNearDeath() + ")");

        // 1. Write the outcome as a fact memory
        DemonMemory memory = outcome.toMemory(currentTime);
        trait.addMemory(memory);

        // 2. If near-death, also write a NEAR_DEATH_EXPERIENCE fact
        if (outcome.wasNearDeath() && !outcome.isDemonLoss()) {
            trait.addMemory(DemonMemory.of(MemoryType.NEAR_DEATH_EXPERIENCE, currentTime,
                    session.getOpponentId(), session.getDemonId()));
        }

        // 3. If demon killed a player, record KILLED_PLAYER
        if (outcome.isDemonWin() && session.isOpponentIsPlayer()) {
            trait.addMemory(DemonMemory.of(MemoryType.KILLED_PLAYER, currentTime,
                    session.getOpponentId(), session.getDemonId()));
        }

        // 4. Update lastBattleTime (C2: only at session end)
        trait.setLastBattleTime(currentTime);

        // 5. Update growth stats (E1)
        GrowthStats stats = trait.getGrowthStats();
        stats.recordBattle();
        stats.addDamageDealt(session.getDamageDealt());
        stats.addDamageTaken(session.getDamageTaken());

        if (outcome.isDemonWin()) {
            stats.recordWin();
            if (session.isOpponentIsPlayer()) {
                stats.incrementPlayersKilled();
            } else {
                stats.incrementDemonsKilled();
            }
        } else if (outcome.isDemonLoss()) {
            stats.recordLoss();
        } else if (outcome.isDemonEscape()) {
            stats.recordEscape();
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────

    /**
     * Returns true if the given demon currently has an active battle session
     * with any opponent.
     */
    public boolean isInBattle(UUID demonId) {
        return activeSessions.keySet().stream()
                .anyMatch(k -> k.demonId().equals(demonId));
    }

    /**
     * Returns the number of currently active sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}


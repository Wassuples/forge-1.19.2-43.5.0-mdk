package com.wassuples.demonslayer.nemesis.battle;

import com.wassuples.demonslayer.nemesis.trait.DemonMemory;
import com.wassuples.demonslayer.nemesis.trait.MemoryType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Derives a factual outcome from raw {@link BattleSession} metrics.
 * <p>
 * Derivation rules (D3):
 * <ul>
 *   <li>Opponent died + demon was never below 50% HP → {@code WON_EASILY}</li>
 *   <li>Opponent died + demon dropped below 25% HP → {@code WON_CLOSE_CALL}</li>
 *   <li>Demon died + opponent never below 50% HP → {@code LOST_BADLY}</li>
 *   <li>Demon died + damage ratio &lt; 10% → {@code HUMILIATED}</li>
 *   <li>Demon fled → {@code ESCAPED_BATTLE}</li>
 *   <li>Session timed out with no death → {@code ESCAPED_BATTLE}</li>
 * </ul>
 */
public record BattleOutcome(
        MemoryType memoryType,
        UUID demonId,
        UUID opponentId,
        boolean opponentIsPlayer,
        Map<String, String> context
) {

    /**
     * Derives the battle outcome from a finalized session.
     */
    public static BattleOutcome derive(BattleSession session, long currentTime) {
        MemoryType type;

        if (session.isDemonFled()) {
            type = MemoryType.ESCAPED_BATTLE;
        } else if (session.isOpponentDied()) {
            // Demon won
            if (session.getDemonMinHealthPct() > 0.50f) {
                type = MemoryType.WON_EASILY;
            } else if (session.getDemonMinHealthPct() <= 0.25f) {
                type = MemoryType.WON_CLOSE_CALL;
            } else {
                type = MemoryType.WON_EASILY; // moderate fight still counts as easy
            }
        } else if (session.isDemonDied()) {
            // Demon lost
            float totalDamage = session.getDamageDealt() + session.getDamageTaken();
            float demonDmgRatio = totalDamage > 0 ? session.getDamageDealt() / totalDamage : 0;

            if (demonDmgRatio < 0.10f) {
                type = MemoryType.HUMILIATED; // barely scratched the opponent
            } else if (session.getOpponentMinHealthPct() > 0.50f) {
                type = MemoryType.LOST_BADLY;
            } else {
                type = MemoryType.LOST_BADLY; // lost but at least fought decently
            }
        } else {
            // No death, session expired → escaped
            type = MemoryType.ESCAPED_BATTLE;
        }

        // Build context map
        Map<String, String> ctx = new HashMap<>();
        ctx.put("damageDealt", String.format("%.1f", session.getDamageDealt()));
        ctx.put("damageTaken", String.format("%.1f", session.getDamageTaken()));
        ctx.put("durationTicks", String.valueOf(session.getDuration(currentTime)));
        ctx.put("demonMinHpPct", String.format("%.2f", session.getDemonMinHealthPct()));
        ctx.put("opponentMinHpPct", String.format("%.2f", session.getOpponentMinHealthPct()));
        if (session.isOpponentIsPlayer()) {
            ctx.put("opponentType", "player");
        } else {
            ctx.put("opponentType", "demon");
        }

        return new BattleOutcome(type, session.getDemonId(), session.getOpponentId(),
                session.isOpponentIsPlayer(), ctx);
    }

    /**
     * Creates a {@link DemonMemory} fact from this outcome.
     */
    public DemonMemory toMemory(long timestamp) {
        return DemonMemory.of(memoryType, timestamp, opponentId, demonId, context);
    }

    /**
     * Returns true if the demon won this battle (any kind of win).
     */
    public boolean isDemonWin() {
        return memoryType == MemoryType.WON_EASILY || memoryType == MemoryType.WON_CLOSE_CALL;
    }

    /**
     * Returns true if the demon lost (died) in this battle.
     */
    public boolean isDemonLoss() {
        return memoryType == MemoryType.LOST_BADLY || memoryType == MemoryType.HUMILIATED;
    }

    /**
     * Returns true if the demon escaped without a decisive outcome.
     */
    public boolean isDemonEscape() {
        return memoryType == MemoryType.ESCAPED_BATTLE;
    }

    /**
     * Returns true if the demon was near death during the fight (below 25% HP).
     */
    public boolean wasNearDeath() {
        String val = context.get("demonMinHpPct");
        if (val == null) return false;
        try {
            return Float.parseFloat(val) <= 0.25f;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}


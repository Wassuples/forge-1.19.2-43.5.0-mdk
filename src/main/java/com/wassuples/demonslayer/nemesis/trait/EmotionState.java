package com.wassuples.demonslayer.nemesis.trait;

import com.wassuples.demonslayer.nemesis.ai.Personality;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-demon emotion store.
 * <p>
 * Holds current intensity and last-update time for every {@link EmotionType}.
 * Emotions are <b>derived</b> from factual {@link DemonMemory} events (via
 * {@link #deriveFromMemory}) and <b>persisted</b> independently so they are
 * never re-rolled on load.
 * <p>
 * Personality modifies how quickly a demon develops fear vs. hate.
 *
 * @see EmotionType
 * @see DemonTrait
 */
public class EmotionState {

    private static final long TICKS_PER_DAY = 24_000L;

    private final EnumMap<EmotionType, Float> intensities = new EnumMap<>(EmotionType.class);
    private final EnumMap<EmotionType, Long>  lastUpdated = new EnumMap<>(EmotionType.class);

    // ── Accessors ───────────────────────────────────────────────────────

    /**
     * Returns the current intensity of the given emotion (0–cap, default 0).
     */
    public float getIntensity(EmotionType type) {
        return intensities.getOrDefault(type, 0.0f);
    }

    /**
     * Returns the world-tick time when this emotion was last updated.
     */
    public long getLastUpdated(EmotionType type) {
        return lastUpdated.getOrDefault(type, 0L);
    }

    /**
     * Directly set an emotion intensity. Clamps to [0, cap].
     */
    public void setIntensity(EmotionType type, float value, long currentTime) {
        intensities.put(type, Math.max(0.0f, Math.min(type.defaultCap, value)));
        lastUpdated.put(type, currentTime);
    }

    /**
     * Returns true if the demon has any non-zero emotion of this type.
     */
    public boolean hasEmotion(EmotionType type) {
        return getIntensity(type) > 0.0f;
    }

    /**
     * Returns the strongest emotion the demon currently feels, or null if all
     * intensities are zero.
     */
    public EmotionType getStrongestEmotion() {
        EmotionType best = null;
        float bestVal = 0.0f;
        for (Map.Entry<EmotionType, Float> entry : intensities.entrySet()) {
            if (entry.getValue() > bestVal) {
                bestVal = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    // ── Decay ───────────────────────────────────────────────────────────

    /**
     * Applies passive decay to all emotions based on elapsed time since each
     * emotion was last updated. Should be called once per simulation day.
     */
    public void applyDecay(long currentTime) {
        for (EmotionType type : EmotionType.values()) {
            float intensity = getIntensity(type);
            if (intensity <= 0.0f) continue;
            if (type.decayPerDay <= 0.0f) continue;

            long lastTime = getLastUpdated(type);
            if (lastTime <= 0L) continue;

            long elapsed = currentTime - lastTime;
            if (elapsed <= 0L) continue;

            float daysElapsed = (float) elapsed / TICKS_PER_DAY;
            float decay = type.decayPerDay * daysElapsed;
            float newIntensity = Math.max(0.0f, intensity - decay);

            intensities.put(type, newIntensity);
            // Don't update lastUpdated here — that only changes when the
            // emotion is actively reinforced by a new event.
        }
    }

    // ── Derivation from facts ───────────────────────────────────────────

    /**
     * Given a newly recorded fact memory, updates emotion intensities according
     * to personality-based derivation rules.
     * <p>
     * This must be called <b>exactly once</b> per memory event (at insert time)
     * and <b>never replayed</b> on load (emotions are persisted).
     *
     * @param memory      the factual memory just recorded
     * @param personality the demon's current personality (affects fear vs. hate bias)
     */
    public void deriveFromMemory(DemonMemory memory, Personality personality) {
        if (memory == null || memory.getType() == null) return;

        float fearMod = getFearModifier(personality);
        float hateMod = getHateModifier(personality);

        MemoryType fact = memory.getType();
        long time = memory.getTimestamp();

        switch (fact) {
            // ── Wounds → fear of the element ────────────────────────────
            case WOUNDED_BY_FLAME ->     applyGain(EmotionType.FEAR_FIRE,      fearMod, time);
            case WOUNDED_BY_WATER ->     applyGain(EmotionType.FEAR_WATER,     fearMod, time);
            case WOUNDED_BY_THUNDER ->   applyGain(EmotionType.FEAR_THUNDER,   fearMod, time);
            case WOUNDED_BY_WIND ->      applyGain(EmotionType.FEAR_WIND,      fearMod, time);
            case WOUNDED_BY_BEAST ->     applyGain(EmotionType.FEAR_BEAST,     fearMod, time);
            case WOUNDED_BY_INSECT ->    applyGain(EmotionType.FEAR_INSECT,    fearMod, time);
            case WOUNDED_BY_SUN ->       applyGain(EmotionType.FEAR_SUN,       fearMod, time);
            case WOUNDED_BY_EXPLOSION -> applyGain(EmotionType.FEAR_EXPLOSION, fearMod, time);
            case WOUNDED_BY_POISON ->    applyGain(EmotionType.FEAR_POISON,    fearMod, time);

            // ── Fought styles → hate of that style ──────────────────────
            case FOUGHT_FLAME_STYLE ->   applyGain(EmotionType.HATE_FLAME_STYLE,   hateMod, time);
            case FOUGHT_WATER_STYLE ->   applyGain(EmotionType.HATE_WATER_STYLE,   hateMod, time);
            case FOUGHT_THUNDER_STYLE -> applyGain(EmotionType.HATE_THUNDER_STYLE, hateMod, time);
            case FOUGHT_WIND_STYLE ->    applyGain(EmotionType.HATE_WIND_STYLE,    hateMod, time);
            case FOUGHT_BEAST_STYLE ->   applyGain(EmotionType.HATE_BEAST_STYLE,   hateMod, time);
            case FOUGHT_INSECT_STYLE ->  applyGain(EmotionType.HATE_INSECT_STYLE,  hateMod, time);
            case FOUGHT_SUN_STYLE ->     applyGain(EmotionType.HATE_SUN_STYLE,     hateMod, time);

            // ── Battle outcomes → player-directed emotions ──────────────
            case LOST_BADLY ->      { applyGain(EmotionType.FEAR_PLAYER,     fearMod, time);
                                      applyGain(EmotionType.HATE_PLAYER,     hateMod * 0.5f, time); }
            case HUMILIATED ->      { applyGain(EmotionType.FEAR_PLAYER,     fearMod * 0.5f, time);
                                      applyGain(EmotionType.HATE_PLAYER,     hateMod, time);
                                      applyGain(EmotionType.DESPERATION,     fearMod * 0.7f, time); }
            case KILLED_PLAYER ->   { applyGain(EmotionType.CONTEMPT_PLAYER, hateMod * 0.5f, time);
                                      applyGain(EmotionType.BLOODLUST,       hateMod * 0.3f, time); }
            case WON_EASILY ->        applyGain(EmotionType.CONTEMPT_PLAYER, hateMod * 0.3f, time);
            case WON_CLOSE_CALL ->    applyGain(EmotionType.RESPECT_PLAYER,  1.0f, time);
            case ESCAPED_BATTLE ->    applyGain(EmotionType.FEAR_PLAYER,     fearMod * 0.7f, time);
            case NEAR_DEATH_EXPERIENCE -> applyGain(EmotionType.DESPERATION, fearMod, time);

            // ── Rivalry facts ───────────────────────────────────────────
            case RIVALRY_STARTED -> applyGain(EmotionType.RIVALRY_GRUDGE, hateMod, time);
            case BETRAYED_ALLY ->   applyGain(EmotionType.RIVALRY_GRUDGE, hateMod * 1.5f, time);
            case WAS_BETRAYED ->  { applyGain(EmotionType.RIVALRY_GRUDGE, hateMod * 1.5f, time);
                                    applyGain(EmotionType.HATE_PLAYER,    hateMod * 0.5f, time); }

            // ── Hierarchy changes ───────────────────────────────────────
            case PROMOTED ->  applyGain(EmotionType.CONTEMPT_PLAYER, hateMod * 0.1f, time); // minor confidence
            case DEMOTED ->   applyGain(EmotionType.DESPERATION,     fearMod * 0.5f, time);

            default -> { /* no emotional derivation for unmatched facts */ }
        }
    }

    // ── Personality → modifier tables ───────────────────────────────────

    /**
     * Returns a multiplier [0.5 – 2.0] for fear-based emotion gains based on
     * the demon's personality.
     */
    private static float getFearModifier(Personality p) {
        if (p == null) return 1.0f;
        return switch (p) {
            // High fear gain
            case NERVOUS, PARANOID, INSECURE, WHINING -> 2.0f;
            case CAUTIOUS, GRIEVING                    -> 1.5f;

            // Low fear gain (brave / aggressive)
            case RUTHLESS, BLOODTHIRSTY, FRENZIED     -> 0.5f;
            case HONOR_BOUND, STOIC                    -> 0.6f;
            case COCKY, ARROGANT                       -> 0.7f;

            // Neutral
            default -> 1.0f;
        };
    }

    /**
     * Returns a multiplier [0.5 – 2.0] for hate-based emotion gains based on
     * the demon's personality.
     */
    private static float getHateModifier(Personality p) {
        if (p == null) return 1.0f;
        return switch (p) {
            // High hate gain
            case RUTHLESS, BLOODTHIRSTY, FRENZIED     -> 2.0f;
            case CRUEL, FANATICAL                      -> 1.8f;
            case COCKY, ARROGANT, MOCKING              -> 1.5f;

            // Low hate gain (passive / reserved)
            case NERVOUS, WHINING, INSECURE            -> 0.6f;
            case STOIC, PHILOSOPHICAL, PATIENT         -> 0.7f;
            case WEARY, LAZY                           -> 0.5f;

            // Neutral
            default -> 1.0f;
        };
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private void applyGain(EmotionType type, float modifier, long time) {
        float gain = type.baseGainRate * modifier;
        float current = getIntensity(type);
        float newVal = Math.min(type.defaultCap, current + gain);
        intensities.put(type, newVal);
        lastUpdated.put(type, time);
    }

    // ── NBT Persistence ─────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        CompoundTag intensitiesTag = new CompoundTag();
        for (Map.Entry<EmotionType, Float> entry : intensities.entrySet()) {
            if (entry.getValue() > 0.0f) {
                intensitiesTag.putFloat(entry.getKey().name(), entry.getValue());
            }
        }
        nbt.put("intensities", intensitiesTag);

        CompoundTag timesTag = new CompoundTag();
        for (Map.Entry<EmotionType, Long> entry : lastUpdated.entrySet()) {
            if (entry.getValue() > 0L) {
                timesTag.putLong(entry.getKey().name(), entry.getValue());
            }
        }
        nbt.put("lastUpdated", timesTag);

        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        intensities.clear();
        lastUpdated.clear();

        if (nbt.contains("intensities")) {
            CompoundTag intensitiesTag = nbt.getCompound("intensities");
            for (String key : intensitiesTag.getAllKeys()) {
                try {
                    EmotionType type = EmotionType.valueOf(key);
                    intensities.put(type, intensitiesTag.getFloat(key));
                } catch (IllegalArgumentException ignored) {
                    // Unknown emotion type (removed in a newer version) — skip
                }
            }
        }

        if (nbt.contains("lastUpdated")) {
            CompoundTag timesTag = nbt.getCompound("lastUpdated");
            for (String key : timesTag.getAllKeys()) {
                try {
                    EmotionType type = EmotionType.valueOf(key);
                    lastUpdated.put(type, timesTag.getLong(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /**
     * Deep-copy all emotion data from another {@link EmotionState}.
     */
    public void copyFrom(EmotionState other) {
        this.intensities.clear();
        this.intensities.putAll(other.intensities);
        this.lastUpdated.clear();
        this.lastUpdated.putAll(other.lastUpdated);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EmotionState{");
        boolean first = true;
        for (Map.Entry<EmotionType, Float> entry : intensities.entrySet()) {
            if (entry.getValue() <= 0.0f) continue;
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(String.format("%.2f", entry.getValue()));
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }
}


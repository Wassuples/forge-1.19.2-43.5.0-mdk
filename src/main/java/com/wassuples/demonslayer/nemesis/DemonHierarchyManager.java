package com.wassuples.demonslayer.nemesis;

import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.demon.ai.DemonGenerator;
import com.wassuples.demonslayer.effects.ModEffects;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.gamerule.ModGameRules;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.nemesis.battle.BattleSessionTracker;
import com.wassuples.demonslayer.nemesis.blueprint.BlueprintLoader;
import com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint;
import com.wassuples.demonslayer.nemesis.growth.GrowthCalculator;
import com.wassuples.demonslayer.nemesis.story.PlayerIntelManager;
import com.wassuples.demonslayer.nemesis.story.StoryEvent;
import com.wassuples.demonslayer.nemesis.taunt.TauntRenderer;
import com.wassuples.demonslayer.nemesis.trait.*;
import com.wassuples.demonslayer.reputation.ReputationService;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * World-saved data managing the global demon power hierarchy (Twelve Kizuki system).
 *
 * <p>This is the core of the Nemesis system. It tracks all named demons in the world,
 * their ranks, traits, Blood Demon Arts, personalities, and relationships with players.
 *
 * <h2>Key responsibilities</h2>
 * <ul>
 *   <li>Maintaining a roster of demon slots (Lower/Upper Moons)</li>
 *   <li>Spawning nemesis demons when players enter areas of influence</li>
 *   <li>Handling demon promotion/demotion on kill/escape events</li>
 *   <li>Persisting demon data via {@link net.minecraft.world.level.saveddata.SavedData}</li>
 *   <li>Loading canon demon blueprints from JSON datapacks</li>
 * </ul>
 *
 * @see NemesisSystemEvents
 * @see com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity
 * @see com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint
 */

/**
 * Demon Hierarchy Manager.
 */
public class DemonHierarchyManager extends SavedData {
    private final Map<DemonRank, UUID> kizukiSlots = new EnumMap<>(DemonRank.class);
    private final List<UUID> rankedFodder = new ArrayList<>();
    private final Map<UUID, DemonTrait> demonDataCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> demonPacks = new HashMap<>();
    @Nullable private BloodBattle activeBattle = null;
    private boolean initialized = false;

    public record BattleContext(ResourceLocation biome, long timeOfDay) {}
    private enum IntelLevel { NONE, PARTIAL, FULL }

    private static final Map<BloodDemonArts, List<String>> BDA_TAGS = Map.of(
            BloodDemonArts.CRYO, List.of("COLD"),
            BloodDemonArts.DESTRUCTIVE_DEATH, List.of("PHYSICAL_BLUNT"),
            BloodDemonArts.HAEMO, List.of("POISON", "PHYSICAL_SLASHING"),
            BloodDemonArts.CRESCENT, List.of("PHYSICAL_SLASHING"),
            BloodDemonArts.SASH, List.of("PHYSICAL_BLUNT"),
            BloodDemonArts.THREADS, List.of("PHYSICAL_SLASHING"),
            BloodDemonArts.SLEEP, List.of("MIND_AFFECTING")
    );

    private int worldDayCounter = 0;
    private final Random random = new Random();

    /**
     * Transient (not serialized) battle session tracker for live entity fights.
     * Lazily initialized on first access.
     */
    private transient BattleSessionTracker battleSessionTracker;

    // ── Story engine (H1) ───────────────────────────────────────────────

    /** Bounded world story log, newest-first. Max 200 entries. */
    private static final int MAX_STORY_LOG = 200;
    private final List<StoryEvent> worldStoryLog = new ArrayList<>();

    /** Per-player intel tiers on story events (H2). */
    private final PlayerIntelManager playerIntelManager = new PlayerIntelManager();

    // ── World blood battles (J1) ────────────────────────────────────────

    /**
     * Tracks demon-vs-demon world battles spawned for players to witness.
     * Key = first demon UUID, Value = second demon UUID.
     * Transient (not persisted); if server restarts mid-battle, it just ends.
     */
    private final transient Map<UUID, UUID> activeWorldBattles = new HashMap<>();
    private final transient Map<UUID, ActiveAlliance> activeAlliances = new HashMap<>();

    private static final int MAX_PACK_SIZE_HARD = 10;
    private static final int MAX_ALLIES_PER_CALL_HARD = 5;
    private static final int MAX_PACKS_PER_DAY = 2;

    private record ActiveAlliance(Set<UUID> members, @Nullable UUID targetId, boolean targetIsPlayer, long expiresAt) {}

    public record PackSnapshot(UUID leaderId, String leaderName, List<UUID> memberIds, List<String> memberNames, int averageStrengthScore) {}
    public record AllianceSnapshot(UUID leaderId, String leaderName, List<UUID> memberIds, List<String> memberNames,
                                   @Nullable UUID targetId, String targetName, boolean targetIsPlayer, long expiresAt) {}

    public DemonHierarchyManager() {}

    /**
     * IMPORTANT: Do not do worldgen/population work in the constructor.
     * SavedData can be constructed in scenarios where the backing NBT isn't yet loaded,
     * or the instance is created temporarily. Initialization is handled lazily in get().
     */
    public DemonHierarchyManager(ServerLevel level) {
        // Intentionally empty.
    }

    private void removeDemonFromSocialStructures(UUID demonId) {
        demonPacks.entrySet().removeIf(entry -> {
            Set<UUID> members = entry.getValue();
            members.remove(demonId);
            return members.size() < 2 || !members.contains(entry.getKey());
        });

        activeAlliances.entrySet().removeIf(entry ->
                entry.getKey().equals(demonId) || entry.getValue().members().contains(demonId));
    }

    private void expireActiveAlliances(long currentTime) {
        activeAlliances.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= currentTime);
    }

    private void updateDemonPacks(long currentTime) {
        demonPacks.entrySet().removeIf(entry -> {
            Set<UUID> members = entry.getValue();
            members.removeIf(id -> getDemonTrait(id) == null);
            return members.size() < 2 || !members.contains(entry.getKey());
        });

        Set<UUID> assigned = new HashSet<>();
        demonPacks.values().forEach(assigned::addAll);

        Set<UUID> attemptedLeaders = new HashSet<>();
        for (int i = 0; i < MAX_PACKS_PER_DAY; i++) {
            UUID leaderId = chooseStrongestEligibleLeader(assigned, attemptedLeaders);
            if (leaderId == null) {
                break;
            }
            attemptedLeaders.add(leaderId);

            DemonTrait leader = getDemonTrait(leaderId);
            if (leader == null) continue;

            int targetPackCap = getSoftLimitedPackCap(leader);
            if (targetPackCap < 2) {
                continue;
            }

            Set<UUID> members = new HashSet<>();
            members.add(leaderId);

            List<UUID> candidates = demonDataCache.keySet().stream()
                    .filter(id -> !id.equals(leaderId) && !assigned.contains(id))
                .sorted((a, b) -> Float.compare(
                        compatibilityScore(leaderId, leader, b, getDemonTrait(b)),
                        compatibilityScore(leaderId, leader, a, getDemonTrait(a))
                ))
                    .toList();

            for (UUID candidateId : candidates) {
                if (members.size() >= targetPackCap) {
                    break;
                }

                DemonTrait candidate = getDemonTrait(candidateId);
                if (candidate == null) continue;

                float score = compatibilityScore(leaderId, leader, candidateId, candidate);
                if (score < 0.45f) {
                    continue;
                }

                if (!passesSoftMaintenanceRoll(leader, members.size() + 1, true)) {
                    continue;
                }

                members.add(candidateId);

                DemonRelationship forward = leader.getOrCreateRelationship(candidateId);
                DemonRelationship reverse = candidate.getOrCreateRelationship(leaderId);
                forward.addAlliance(0.03f);
                reverse.addAlliance(0.03f);
                forward.recordEncounter(currentTime);
                reverse.recordEncounter(currentTime);
            }

            if (members.size() >= 2) {
                assigned.addAll(members);
                demonPacks.put(leaderId, members);
            }
        }
    }

    private List<UUID> findCompatibleAllies(UUID leaderId, UUID targetId, boolean targetIsPlayer, ServerLevel level, int maxAllies) {
        DemonTrait leader = getDemonTrait(leaderId);
        if (leader == null) return List.of();

        int allyCap = getSoftLimitedAllyCallCap(leader, maxAllies);
        if (allyCap <= 0) return List.of();

        List<UUID> ordered = new ArrayList<>();
        Set<UUID> used = new HashSet<>();

        for (Map.Entry<UUID, Set<UUID>> pack : demonPacks.entrySet()) {
            if (!pack.getValue().contains(leaderId)) continue;
            for (UUID memberId : pack.getValue()) {
                if (!memberId.equals(leaderId) && level.getEntity(memberId) == null && used.add(memberId)) {
                    ordered.add(memberId);
                }
            }
            break;
        }

        List<UUID> socialCandidates = demonDataCache.keySet().stream()
                .filter(id -> !id.equals(leaderId) && !used.contains(id) && level.getEntity(id) == null)
                .filter(id -> {
                    DemonTrait candidate = getDemonTrait(id);
                    if (candidate == null) return false;
                    float score = compatibilityScore(leaderId, leader, id, candidate)
                            + mutualEnemyBonus(leaderId, id, targetId, targetIsPlayer);
                    return score >= 0.55f;
                })
                .sorted((a, b) -> Float.compare(
                        compatibilityScore(leaderId, leader, b, getDemonTrait(b)) + mutualEnemyBonus(leaderId, b, targetId, targetIsPlayer),
                        compatibilityScore(leaderId, leader, a, getDemonTrait(a)) + mutualEnemyBonus(leaderId, a, targetId, targetIsPlayer)
                ))
                .toList();

        for (UUID id : socialCandidates) {
            if (used.add(id)) {
                ordered.add(id);
            }
        }

        List<UUID> selected = new ArrayList<>();
        for (UUID id : ordered) {
            if (selected.size() >= allyCap) break;
            if (!targetIsPlayer && id.equals(targetId)) continue;
            if (!passesSoftMaintenanceRoll(leader, selected.size() + 1, false)) continue;
            selected.add(id);
        }

        return selected;
    }

    private @Nullable UUID chooseStrongestEligibleLeader(Set<UUID> assigned, Set<UUID> attemptedLeaders) {
        return demonDataCache.entrySet().stream()
                .filter(e -> !assigned.contains(e.getKey()) && !attemptedLeaders.contains(e.getKey()))
                .filter(e -> isPackLeaderCandidate(e.getValue()))
                .max(Comparator.comparingInt(e -> getStrengthScore(e.getValue())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private int getStrengthScore(@Nullable DemonTrait trait) {
        if (trait == null) return 0;
        int statScore = (trait.getVigor() + trait.getStrength() + trait.getAgility()) * 12;
        return Math.max(0, trait.getBloodPoints()) + statScore;
    }

    private int getSoftLimitedPackCap(DemonTrait leader) {
        int base = behaviorPackCapBase(leader.getBehavior());
        base += personalityPackCapMod(leader.getPersonality());

        int strengthBonus = Math.min(2, getStrengthScore(leader) / 900);
        return Math.max(2, Math.min(MAX_PACK_SIZE_HARD, base + strengthBonus));
    }

    private int getSoftLimitedAllyCallCap(DemonTrait leader, int requested) {
        int base = behaviorAllyCapBase(leader.getBehavior());
        base += personalityAllyCapMod(leader.getPersonality());

        int strengthBonus = Math.min(1, getStrengthScore(leader) / 1200);
        int cap = Math.max(1, Math.min(MAX_ALLIES_PER_CALL_HARD, base + strengthBonus));
        return Math.min(cap, Math.max(0, requested));
    }

    private boolean passesSoftMaintenanceRoll(DemonTrait leader, int projectedSize, boolean packMode) {
        if (projectedSize <= 2) {
            return true;
        }

        float pressure = packMode ? 0.16f : 0.22f;
        float strengthFactor = Math.min(0.35f, getStrengthScore(leader) / 2600.0f);
        float leaderFactor = behaviorSoftLimitLeaderFactor(leader.getBehavior())
                + personalitySoftLimitLeaderFactor(leader.getPersonality());

        float chance = 0.86f - ((projectedSize - 2) * pressure) + strengthFactor + leaderFactor;
        chance = Math.max(0.20f, Math.min(0.95f, chance));
        return random.nextFloat() <= chance;
    }

    private float mutualEnemyBonus(UUID leaderId, UUID allyId, UUID targetId, boolean targetIsPlayer) {
        DemonTrait leader = getDemonTrait(leaderId);
        DemonTrait ally = getDemonTrait(allyId);
        if (leader == null || ally == null || targetId == null) {
            return 0.0f;
        }

        float bonus = 0.0f;

        DemonRelationship leaderVsTarget = leader.getRelationship(targetId);
        DemonRelationship allyVsTarget = ally.getRelationship(targetId);

        if (leaderVsTarget != null && allyVsTarget != null) {
            if (leaderVsTarget.getRivalryIntensity() > 0.22f && allyVsTarget.getRivalryIntensity() > 0.22f) {
                bonus += 0.16f;
            }
            if (leaderVsTarget.getAllianceStrength() < -0.10f && allyVsTarget.getAllianceStrength() < -0.10f) {
                bonus += 0.08f;
            }
        }

        if (targetIsPlayer) {
            if (leader.getEmotionState().getIntensity(EmotionType.HATE_PLAYER) > 0.35f
                    && ally.getEmotionState().getIntensity(EmotionType.HATE_PLAYER) > 0.35f) {
                bonus += 0.10f;
            }
        }

        return Math.max(0.0f, Math.min(0.35f, bonus));
    }

    private float compatibilityScore(UUID aId, DemonTrait a, UUID bId, @Nullable DemonTrait b) {
        if (a == null || b == null) return -1.0f;

        float score = 0.25f;
        score += behaviorAffinity(a.getBehavior(), b.getBehavior());
        score += personalityAffinity(a.getPersonality(), b.getPersonality());
        score += packJoinAffinityBonus(b);
        score += (behaviorJoinBias(a.getBehavior()) + personalityJoinBias(a.getPersonality())) * 0.35f;

        DemonRelationship rel = a.getRelationship(bId);
        if (rel != null) {
            score += rel.getAllianceStrength() * 0.35f;
            score -= rel.getRivalryIntensity() * 0.45f;
            score += rel.getIntelOnTarget() * 0.05f;
        }

        DemonRelationship reverse = b.getRelationship(aId);
        if (reverse != null) {
            score += reverse.getAllianceStrength() * 0.20f;
            score -= reverse.getRivalryIntensity() * 0.30f;
        }

        return Math.max(-1.0f, Math.min(1.0f, score));
    }

    private float behaviorAffinity(Behavior a, Behavior b) {
        float base = (behaviorCooperationBias(a) + behaviorCooperationBias(b)) * 0.5f;
        if (a == b) return base + 0.35f;

        EnumSet<Behavior> tactical = EnumSet.of(Behavior.STRATEGIST, Behavior.TRICKSTER, Behavior.CALM, Behavior.TERRITORIAL);
        EnumSet<Behavior> command = EnumSet.of(Behavior.OVERLORD, Behavior.LOYALIST, Behavior.BODYGUARD);
        EnumSet<Behavior> volatileSet = EnumSet.of(Behavior.BERSERKER, Behavior.COWARDLY, Behavior.SCAVENGER);

        if (tactical.contains(a) && tactical.contains(b)) return base + 0.18f;
        if (command.contains(a) && command.contains(b)) return base + 0.22f;
        if ((a == Behavior.BERSERKER && b == Behavior.COWARDLY) || (a == Behavior.COWARDLY && b == Behavior.BERSERKER)) return base - 0.30f;
        if (volatileSet.contains(a) && volatileSet.contains(b)) return base - 0.12f;

        if ((a == Behavior.OVERLORD && b == Behavior.LOYALIST) || (a == Behavior.LOYALIST && b == Behavior.OVERLORD)) {
            return base + 0.20f;
        }

        if ((a == Behavior.DUELIST && (b == Behavior.OVERLORD || b == Behavior.BODYGUARD || b == Behavior.LOYALIST))
                || (b == Behavior.DUELIST && (a == Behavior.OVERLORD || a == Behavior.BODYGUARD || a == Behavior.LOYALIST))) {
            return base - 0.18f;
        }

        return base;
    }

    private float personalityAffinity(Personality a, Personality b) {
        float base = (personalityCohesionBias(a) + personalityCohesionBias(b)) * 0.5f;
        if (a == b) return base + 0.20f;
        if (arePersonalitiesClashing(a, b)) return base - 0.25f;

        if ((a == Personality.DEVOTED && b == Personality.COMMANDING)
                || (b == Personality.DEVOTED && a == Personality.COMMANDING)) {
            return base + 0.16f;
        }

        if ((a == Personality.PROTECTIVE && b == Personality.CAUTIOUS)
                || (b == Personality.PROTECTIVE && a == Personality.CAUTIOUS)) {
            return base + 0.10f;
        }

        if ((a == Personality.BLOODTHIRSTY && b == Personality.CALCULATING)
                || (b == Personality.BLOODTHIRSTY && a == Personality.CALCULATING)) {
            return base - 0.12f;
        }

        if ((a == Personality.JESTING && b == Personality.MELODRAMATIC)
                || (b == Personality.JESTING && a == Personality.MELODRAMATIC)) {
            return base + 0.08f;
        }

        if ((a == Personality.STOIC && b == Personality.WHINING)
                || (b == Personality.STOIC && a == Personality.WHINING)) {
            return base - 0.10f;
        }

        return base;
    }

    private boolean isPackLeaderCandidate(DemonTrait trait) {
        if (trait == null) return false;
        return switch (trait.getBehavior()) {
            case OVERLORD, LOYALIST, STRATEGIST, TERRITORIAL -> true;
            default -> trait.getPersonality() == Personality.COMMANDING
                    || trait.getPersonality() == Personality.CALCULATING
                    || trait.getPersonality() == Personality.DECEPTIVE
                    || trait.getPersonality() == Personality.MANIPULATIVE;
        };
    }

    private float packJoinAffinityBonus(DemonTrait candidate) {
        return behaviorJoinBias(candidate.getBehavior()) + personalityJoinBias(candidate.getPersonality());
    }

    private float behaviorCooperationBias(Behavior behavior) {
        return switch (behavior) {
            case OVERLORD -> 0.10f;
            case LOYALIST -> 0.14f;
            case BODYGUARD -> 0.12f;
            case STRATEGIST -> 0.08f;
            case TERRITORIAL -> 0.04f;
            case CALM -> 0.06f;
            case STALWART -> 0.05f;
            case SKIRMISHER -> 0.02f;
            case TRICKSTER -> 0.03f;
            case AMBITIOUS -> -0.03f;
            case AGGRESSIVE -> -0.01f;
            case SADIST -> -0.04f;
            case DUELIST -> -0.08f;
            case BERSERKER -> -0.12f;
            case COWARDLY -> -0.10f;
            case SCAVENGER -> -0.06f;
        };
    }

    private float behaviorJoinBias(Behavior behavior) {
        return switch (behavior) {
            case LOYALIST -> 0.22f;
            case BODYGUARD -> 0.20f;
            case OVERLORD, STRATEGIST, STALWART, CALM -> 0.08f;
            case TERRITORIAL, SKIRMISHER, TRICKSTER, AGGRESSIVE -> 0.02f;
            case AMBITIOUS, SCAVENGER -> -0.02f;
            case DUELIST -> -0.05f;
            case COWARDLY, BERSERKER -> -0.06f;
            case SADIST -> -0.04f;
        };
    }

    private int behaviorPackCapBase(Behavior behavior) {
        return switch (behavior) {
            case OVERLORD -> 5;
            case LOYALIST -> 4;
            case STRATEGIST, TERRITORIAL, STALWART -> 3;
            case BODYGUARD, CALM, SKIRMISHER, TRICKSTER, AGGRESSIVE, AMBITIOUS -> 2;
            case BERSERKER, DUELIST, COWARDLY, SCAVENGER, SADIST -> 1;
        };
    }

    private int behaviorAllyCapBase(Behavior behavior) {
        return switch (behavior) {
            case OVERLORD -> 3;
            case LOYALIST, STRATEGIST, TERRITORIAL, BODYGUARD -> 2;
            case STALWART, CALM, SKIRMISHER, TRICKSTER, AGGRESSIVE, AMBITIOUS -> 1;
            case BERSERKER, DUELIST, COWARDLY, SCAVENGER, SADIST -> 0;
        };
    }

    private float behaviorSoftLimitLeaderFactor(Behavior behavior) {
        return switch (behavior) {
            case OVERLORD, LOYALIST -> 0.16f;
            case BODYGUARD, STRATEGIST, TERRITORIAL, STALWART -> 0.08f;
            case CALM, SKIRMISHER, TRICKSTER -> 0.03f;
            case AGGRESSIVE, AMBITIOUS, SADIST -> -0.02f;
            case COWARDLY, SCAVENGER, BERSERKER, DUELIST -> -0.08f;
        };
    }

    private float personalityCohesionBias(Personality personality) {
        return switch (personality) {
            case COMMANDING, DEVOTED, PROTECTIVE, CALCULATING, PATIENT, STOIC, PHILOSOPHICAL -> 0.10f;
            case OBSERVANT, CAUTIOUS, DECEPTIVE, MANIPULATIVE, OPPORTUNISTIC, FANATICAL -> 0.06f;
            case COCKY, RUTHLESS, ARROGANT, MOCKING, CRUEL, JESTING, MELODRAMATIC -> -0.02f;
            case BLOODTHIRSTY, FRENZIED, GLUTTONOUS, PARANOID, NERVOUS, WHINING, INSECURE, GRIEVING, WEARY, LAZY, HONOR_BOUND, YANDERE -> -0.05f;
        };
    }

    private float personalityJoinBias(Personality personality) {
        return switch (personality) {
            case DEVOTED, PROTECTIVE, FANATICAL -> 0.12f;
            case CALCULATING, DECEPTIVE, MANIPULATIVE, CAUTIOUS, PATIENT, STOIC, OBSERVANT -> 0.06f;
            case COMMANDING, PHILOSOPHICAL, OPPORTUNISTIC, WEARY -> 0.02f;
            case COCKY, ARROGANT, RUTHLESS, JESTING, MELODRAMATIC, CRUEL, GLUTTONOUS, GRIEVING, LAZY, YANDERE -> -0.02f;
            case HONOR_BOUND, INSECURE, WHINING, NERVOUS, PARANOID, BLOODTHIRSTY, FRENZIED, MOCKING -> -0.05f;
        };
    }

    private int personalityPackCapMod(Personality personality) {
        return switch (personality) {
            case COMMANDING, FANATICAL, PROTECTIVE, CALCULATING -> 1;
            case LAZY, WHINING, INSECURE, PARANOID, NERVOUS -> -1;
            default -> 0;
        };
    }

    private int personalityAllyCapMod(Personality personality) {
        return switch (personality) {
            case COMMANDING, FANATICAL, PROTECTIVE, DECEPTIVE, MANIPULATIVE -> 1;
            case HONOR_BOUND, LAZY, WHINING, INSECURE -> -1;
            default -> 0;
        };
    }

    private float personalitySoftLimitLeaderFactor(Personality personality) {
        return switch (personality) {
            case COMMANDING, FANATICAL, PROTECTIVE, CALCULATING -> 0.07f;
            case DEVOTED, STOIC, PATIENT, OBSERVANT -> 0.04f;
            case DECEPTIVE, MANIPULATIVE, OPPORTUNISTIC -> 0.02f;
            case WHINING, INSECURE, LAZY, NERVOUS, PARANOID -> -0.05f;
            case BLOODTHIRSTY, FRENZIED, ARROGANT, COCKY, CRUEL, YANDERE -> -0.03f;
            default -> 0.0f;
        };
    }

    public Collection<DemonTrait> getAllDemonTraits() {
        // Return an unmodifiable view to prevent external mutation of the internal cache
        return Collections.unmodifiableCollection(demonDataCache.values());
    }

    /**
     * Returns the transient battle session tracker, creating it lazily.
     * The tracker is NOT serialized; only the resulting memories are persisted.
     */
    public BattleSessionTracker getBattleSessionTracker() {
        if (this.battleSessionTracker == null) {
            this.battleSessionTracker = new BattleSessionTracker(this::getDemonTrait);
        }
        return this.battleSessionTracker;
    }

    /**
     * Logs a story event to the world story log (H1).
     * Newest events are at the front. Evicts oldest if over capacity.
     */
    public void logStoryEvent(StoryEvent event) {
        System.out.println("[NEMESIS DEBUG] STORY EVENT: " + event.getType() + " - " + event.renderFull());
        this.worldStoryLog.add(0, event);
        while (this.worldStoryLog.size() > MAX_STORY_LOG) {
            this.worldStoryLog.remove(this.worldStoryLog.size() - 1);
        }
        setDirty();
    }

    /** Returns the world story log (newest-first, unmodifiable). */
    public List<StoryEvent> getWorldStoryLog() {
        return Collections.unmodifiableList(this.worldStoryLog);
    }

    /** Returns the player intel manager (H2). */
    public PlayerIntelManager getPlayerIntelManager() {
        return this.playerIntelManager;
    }

    /** Returns active world battles (J1). */
    public Map<UUID, UUID> getActiveWorldBattles() {
        return this.activeWorldBattles;
    }

    public List<PackSnapshot> getPackSnapshots() {
        List<PackSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : demonPacks.entrySet()) {
            UUID leaderId = entry.getKey();
            Set<UUID> members = entry.getValue();
            DemonTrait leaderTrait = getDemonTrait(leaderId);
            if (leaderTrait == null || members == null || members.isEmpty()) continue;

            List<UUID> memberIds = members.stream().sorted().toList();
            List<String> memberNames = memberIds.stream()
                    .map(this::getDemonTrait)
                    .filter(Objects::nonNull)
                    .map(DemonTrait::getName)
                    .toList();

            int avgStrength = memberIds.isEmpty() ? 0 :
                    (int) memberIds.stream()
                            .map(this::getDemonTrait)
                            .filter(Objects::nonNull)
                            .mapToInt(this::getStrengthScore)
                            .average()
                            .orElse(0.0);

            snapshots.add(new PackSnapshot(
                    leaderId,
                    leaderTrait.getName(),
                    memberIds,
                    memberNames,
                    avgStrength
            ));
        }

        snapshots.sort(Comparator.comparingInt(PackSnapshot::averageStrengthScore).reversed());
        return snapshots;
    }

    public List<AllianceSnapshot> getActiveAllianceSnapshots(ServerLevel level) {
        List<AllianceSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<UUID, ActiveAlliance> entry : activeAlliances.entrySet()) {
            UUID leaderId = entry.getKey();
            ActiveAlliance alliance = entry.getValue();
            DemonTrait leaderTrait = getDemonTrait(leaderId);
            if (leaderTrait == null || alliance == null) continue;

            List<UUID> memberIds = alliance.members().stream().sorted().toList();
            List<String> memberNames = memberIds.stream()
                    .map(this::getDemonTrait)
                    .filter(Objects::nonNull)
                    .map(DemonTrait::getName)
                    .toList();

            String targetName = "Unknown";
            if (alliance.targetId() != null) {
                if (alliance.targetIsPlayer()) {
                    ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayer(alliance.targetId());
                    targetName = targetPlayer != null ? targetPlayer.getGameProfile().getName() : "Offline Player";
                } else {
                    DemonTrait targetTrait = getDemonTrait(alliance.targetId());
                    targetName = targetTrait != null ? targetTrait.getName() : "Unknown Demon";
                }
            }

            snapshots.add(new AllianceSnapshot(
                    leaderId,
                    leaderTrait.getName(),
                    memberIds,
                    memberNames,
                    alliance.targetId(),
                    targetName,
                    alliance.targetIsPlayer(),
                    alliance.expiresAt()
            ));
        }

        snapshots.sort(Comparator.comparingLong(AllianceSnapshot::expiresAt));
        return snapshots;
    }

    public Optional<UUID> getPackLeaderFor(UUID demonId) {
        return demonPacks.entrySet().stream()
                .filter(e -> e.getValue().contains(demonId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean forceRebuildPacks(ServerLevel level) {
        updateDemonPacks(level.getGameTime());
        setDirty();
        return !demonPacks.isEmpty();
    }

    public boolean disbandPack(UUID leaderId) {
        Set<UUID> removed = demonPacks.remove(leaderId);
        if (removed == null) return false;

        activeAlliances.remove(leaderId);
        setDirty();
        return true;
    }

    public int clearActiveAlliances() {
        int count = activeAlliances.size();
        activeAlliances.clear();
        return count;
    }

    public float previewCompatibility(UUID demonAId, UUID demonBId) {
        DemonTrait a = getDemonTrait(demonAId);
        DemonTrait b = getDemonTrait(demonBId);
        if (a == null || b == null) return -1.0f;
        return compatibilityScore(demonAId, a, demonBId, b);
    }

    public float previewMutualEnemyBonus(UUID leaderId, UUID allyId, @Nullable UUID targetId, boolean targetIsPlayer) {
        return mutualEnemyBonus(leaderId, allyId, targetId, targetIsPlayer);
    }

    /**
     * Finds a demon UUID by its displayed name.
     *
     * @param name Name to search for (case-insensitive). Matches either the short name ("Akaza")
     *             or full name ("Akaza Upper Moon Three") depending on how the trait is stored.
     */
    public Optional<UUID> getDemonUUIDByName(String name) {
        return Optional.ofNullable(findDemonUUIDByName(name));
    }

    /**
     * Nullable variant used by older code paths.
     */
    @Nullable
    public UUID findDemonUUIDByName(@Nullable String name) {
        if (name == null) return null;
        String lookup = name.trim().toLowerCase(Locale.ROOT);
        if (lookup.isEmpty()) return null;

        for (Map.Entry<UUID, DemonTrait> entry : demonDataCache.entrySet()) {
            DemonTrait trait = entry.getValue();
            if (trait == null) continue;

            String traitName = trait.getName() != null ? trait.getName().trim().toLowerCase(Locale.ROOT) : "";
            if (!traitName.isEmpty() && traitName.equals(lookup)) return entry.getKey();

            // Some names are presented as "Name Title" in UI/commands
            String fullName = (String.valueOf(trait.getName()) + " " + String.valueOf(trait.getTitle())).trim().toLowerCase(Locale.ROOT);
            if (!fullName.isEmpty() && fullName.equals(lookup)) return entry.getKey();
        }

        return null;
    }

    public record DemonDisplayDataLists(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder) {}
    public DemonDisplayDataLists getDemonDisplayData() {
        List<DemonDisplayData> kizukiData = new ArrayList<>();

        List<DemonRank> moonRanks = Arrays.stream(DemonRank.values())
                .filter(DemonRank::isMoonRank).toList();

        for (DemonRank rank : moonRanks) {
            UUID demonId = kizukiSlots.get(rank);
            if (demonId != null) {
                DemonTrait trait = getDemonTrait(demonId);
                if (trait != null) {
                    kizukiData.add(new DemonDisplayData(rank.getDisplayName().getString(), trait.getName(), trait.getTitle(), trait.getBloodPoints()));
                }
            } else {
                kizukiData.add(new DemonDisplayData(rank.getDisplayName().getString(), "---", "Slot Empty", 0));
            }
        }

        List<DemonDisplayData> fodderData = rankedFodder.stream()
                .map(this::getDemonTrait)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(DemonTrait::getBloodPoints).reversed())
                .limit(10)
                .map(trait -> new DemonDisplayData("Fodder", trait.getName(), trait.getTitle(), trait.getBloodPoints()))
                .toList();

        return new DemonDisplayDataLists(kizukiData, fodderData);
    }

    public void runDailySimulation(ServerLevel level) {
        worldDayCounter++;
        System.out.println("[NEMESIS DEBUG] ═══ Starting Daily Simulation for Day " + worldDayCounter + " ═══");

        // Apply passive emotion decay for all demons
        long currentTime = level.getGameTime();
        System.out.println("[NEMESIS DEBUG] Applying emotion decay and resetting growth budgets for " + demonDataCache.size() + " demons");
        for (DemonTrait trait : demonDataCache.values()) {
            trait.applyEmotionDecay(currentTime);
            trait.setLastSeenTime(currentTime);
            trait.getGrowthStats().resetDailyGrowthBudget();
        }

        // Decay intel + rivalry across all relationships (G4)
        System.out.println("[NEMESIS DEBUG] Decaying relationships (intel -3%/day, rivalry -1%/day)");
        decayAllRelationships();

        // Build passive rivalries from personality clashes + adjacency (G2)
        System.out.println("[NEMESIS DEBUG] Building passive rivalries from personality clashes and rank adjacency");
        buildPassiveRivalries();

        // Maintain social structures used by coordinated hunts/battles.
        updateDemonPacks(currentTime);
        expireActiveAlliances(currentTime);

        // Fodder vs Fodder — scheme-driven (G3)
        System.out.println("[NEMESIS DEBUG] Simulating Fodder vs Fodder battles");
        simulateFodderVsFodder(level);

        // Challenger vs Kizuki.
        if (worldDayCounter > 0 && worldDayCounter % 3 == 0) { // every 3 days
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " is divisible by 3 - Simulating Challenger vs Kizuki battles");
            simulateChallengerVsKizuki(level);
        } else {
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " - Skipping Challenger vs Kizuki battles (not divisible by 3)");
        }

        // Kizuki vs Kizuki. Every 9-10 days, and less likely
        if (worldDayCounter > 0 && worldDayCounter % 9 == 0 && random.nextFloat() < 0.5f) { // every 9 days, 50% chance
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " is divisible by 9 and 50% roll passed - Simulating Kizuki vs Kizuki battles");
            simulateKizukiVsKizuki(level);
        } else if (worldDayCounter > 0 && worldDayCounter % 9 == 0) {
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " is divisible by 9 but 50% roll failed - Skipping Kizuki vs Kizuki battles");
        } else {
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " - Skipping Kizuki vs Kizuki battles (not divisible by 9)");
        }

        System.out.println("[NEMESIS DEBUG] Refilling demon roster (target: 20 fodder, current: " + rankedFodder.size() + ")");
        refillDemonRoster(level);

        System.out.println("[NEMESIS DEBUG] Filling ALL vacant Kizuki slots immediately");
        fillVacantKizukiSlots(level);

        System.out.println("[NEMESIS DEBUG] Applying passive growth to all " + demonDataCache.size() + " demons");
        long growthTime = level.getGameTime();
        for (Map.Entry<UUID, DemonTrait> entry : demonDataCache.entrySet()) {
            Optional<DemonRank> rank = getKizukiRank(entry.getKey());
            GrowthCalculator.applyGrowth(entry.getValue(), growthTime,
                    rank.orElse(null));
        }

        List<ServerPlayer> onlinePlayers = level.getServer().getPlayerList().getPlayers();
        System.out.println("[NEMESIS DEBUG] Spreading rumors about " + worldStoryLog.size() + " story events to " + onlinePlayers.size() + " online players");
        playerIntelManager.spreadRumors(worldStoryLog, onlinePlayers,
                this::getDemonTrait,
                id -> getKizukiRank(id).isPresent());

        if (!worldStoryLog.isEmpty()) {
            long oldestTimestamp = worldStoryLog.get(worldStoryLog.size() - 1).getTimestamp();
            System.out.println("[NEMESIS DEBUG] Pruning old intel entries (cutoff: " + oldestTimestamp + ")");
            playerIntelManager.pruneOldIntel(oldestTimestamp);
        }

        if (worldDayCounter % 7 == 0) {
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " is divisible by 7 - Sending weekly digest to " + onlinePlayers.size() + " players");
            for (ServerPlayer player : onlinePlayers) {
                Component digest = playerIntelManager.generateWeeklyDigest(
                        player.getUUID(), worldStoryLog, level.getGameTime());
                player.sendSystemMessage(digest);
            }
        } else {
            System.out.println("[NEMESIS DEBUG] Day " + worldDayCounter + " - Skipping weekly digest (not divisible by 7)");
        }

        System.out.println("[NEMESIS DEBUG] ═══ Daily Simulation Complete for Day " + worldDayCounter + " ═══");
        setDirty();
    }

    private BattleResult simulateBattle(UUID aggressorId, UUID defenderId, ServerLevel level) {
        DemonTrait aggressor = getDemonTrait(aggressorId);
        DemonTrait defender = getDemonTrait(defenderId);

        // Intel-gaining (G4: uses persistent stats now)
        IntelLevel intel = getIntelLevel(aggressor, defenderId);

        // Time of day
        long simulatedTime = getSimulatedTime(aggressor, defender, intel);

        // Home-Advantage & Biome
        ResourceLocation arena = defender.getTerritory(); // may change this, but for now, always defender's territory
        BattleContext context = new BattleContext(arena, simulatedTime);
        boolean isAggressorHomeField = aggressor.getTerritory().equals(arena);
        boolean isDefenderHomeField = true; // Always true for now

        // Calculate Effective Stats for each fighter
        EffectiveStats stats1 = getEffectiveStats(aggressor, defender, context, isAggressorHomeField);
        EffectiveStats stats2 = getEffectiveStats(defender, aggressor, context, isDefenderHomeField);

        // TimeToKill
        float ttk1 = stats2.health / (stats1.damage * stats1.agility); // Don't really like this equation, would like to change it later.
        float ttk2 = stats1.health / (stats2.damage * stats2.agility);

        UUID winnerId = ttk1 <= ttk2 ? aggressorId : defenderId;
        UUID loserId = winnerId.equals(aggressorId) ? defenderId : aggressorId;

        return new BattleResult(winnerId, loserId, context, ttk1, ttk2);
    }

    /**
     * Possible outcomes of a simulated offscreen battle (G5).
     */
    private enum SimOutcome { DECISIVE_WIN, RETREAT, HUMILIATION }

    /**
     * Writes fact memories, updates growth stats, updates relationships,
     * and returns the outcome type for an offscreen simulated battle.
     * Supports extended outcomes: DECISIVE_WIN, RETREAT, HUMILIATION (G5).
     */
    private SimOutcome writeSimulatedBattleOutcome(DemonTrait winner, DemonTrait loser,
                                                    BattleResult result, long time) {
        // Determine closeness: ratio of TTKs
        float ratio = Math.min(result.winnerTTK(), result.loserTTK())
                     / Math.max(result.winnerTTK(), result.loserTTK());
        boolean closeFight = ratio > 0.70f;

        // Determine extended outcome
        SimOutcome outcome;
        float loserDmgRatio = result.loserTTK() / (result.winnerTTK() + result.loserTTK());

        if (loserDmgRatio < 0.10f) {
            outcome = SimOutcome.HUMILIATION; // loser barely fought
        } else if (closeFight && shouldRetreat(loser)) {
            outcome = SimOutcome.RETREAT; // close fight + personality allows retreat
        } else {
            outcome = SimOutcome.DECISIVE_WIN;
        }

        // Winner memories + growth
        MemoryType winType = closeFight ? MemoryType.WON_CLOSE_CALL : MemoryType.WON_EASILY;
        winner.addMemory(DemonMemory.of(winType, time, result.loserId(), result.winnerId()));
        winner.setLastBattleTime(time);

        GrowthStats winStats = winner.getGrowthStats();
        winStats.recordBattle();
        winStats.recordWin();
        winStats.incrementDemonsKilled();

        // Loser memories + growth (written before potential kill)
        MemoryType loseType = switch (outcome) {
            case HUMILIATION -> MemoryType.HUMILIATED;
            case RETREAT -> MemoryType.ESCAPED_BATTLE;
            case DECISIVE_WIN -> MemoryType.LOST_BADLY;
        };
        loser.addMemory(DemonMemory.of(loseType, time, result.winnerId(), result.loserId()));
        loser.setLastBattleTime(time);

        GrowthStats loseStats = loser.getGrowthStats();
        loseStats.recordBattle();
        if (outcome == SimOutcome.RETREAT) {
            loseStats.recordEscape();
        } else {
            loseStats.recordLoss();
        }

        // Update relationships for both fighters (G2)
        updateRelationshipAfterBattle(winner, result.winnerId(), result.loserId(), closeFight, time);
        updateRelationshipAfterBattle(loser, result.loserId(), result.winnerId(), closeFight, time);

        // Apply humiliation quirk if applicable
        if (outcome == SimOutcome.HUMILIATION && !loser.hasQuirk(Quirk.DISHONORED)) {
            loser.addQuirk(Quirk.DISHONORED);
        }

        // Retreat: loser loses BP but survives
        if (outcome == SimOutcome.RETREAT) {
            int bpLoss = Math.max(5, loser.getBloodPoints() / 5);
            loser.setBloodPoints(Math.max(0, loser.getBloodPoints() - bpLoss));
        }

        // Log story event (H1)
        String outcomeName = outcome == SimOutcome.RETREAT ? "retreat"
                : outcome == SimOutcome.HUMILIATION ? "humiliation" : "decisive victory";
        if (outcome == SimOutcome.RETREAT) {
            logStoryEvent(StoryEvent.retreat(time, result.winnerId(), result.loserId(),
                    winner.getName(), loser.getName()));
        } else {
            logStoryEvent(StoryEvent.battle(time, result.winnerId(), result.loserId(),
                    winner.getName(), loser.getName(), outcomeName));
        }

        return outcome;
    }

    /**
     * Determines if a losing demon would retreat rather than fight to the death.
     */
    private boolean shouldRetreat(DemonTrait loser) {
        return switch (loser.getBehavior()) {
            case COWARDLY, SCAVENGER, SKIRMISHER -> random.nextFloat() < 0.70f;
            case TERRITORIAL, CALM -> random.nextFloat() < 0.40f;
            case STRATEGIST -> random.nextFloat() < 0.50f;
            case AGGRESSIVE, BERSERKER, DUELIST -> random.nextFloat() < 0.05f;
            default -> random.nextFloat() < 0.20f;
        };
    }

    /**
     * Updates relationship data between two demons after a battle (G2, G4).
     */
    private void updateRelationshipAfterBattle(DemonTrait self, UUID selfId, UUID otherId,
                                                boolean closeFight, long time) {
        DemonRelationship rel = self.getOrCreateRelationship(otherId);
        rel.recordEncounter(time);

        // Rivalry grows from conflict; more for close fights
        float rivalryGain = closeFight ? 0.15f : 0.08f;
        rel.addRivalry(rivalryGain);

        // Intel grows from direct encounters
        float intelGain = 0.15f + self.getBaseIntelGathering() * 0.10f;
        rel.addIntel(intelGain);
    }

    /**
     * Derives intel level from persistent stats (G4).
     * Uses the aggressor's base intel ability + per-matchup knowledge
     * of the defender, with a small random element.
     */
    private IntelLevel getIntelLevel(DemonTrait aggressor, UUID defenderId) {
        float baseIntel = aggressor.getBaseIntelGathering();
        DemonRelationship rel = aggressor.getRelationship(defenderId);
        float matchupIntel = (rel != null) ? rel.getIntelOnTarget() : 0f;

        // Combined score: base + matchup + small random factor
        float score = baseIntel * 0.5f + matchupIntel * 0.4f + random.nextFloat() * 0.1f;

        if (score >= 0.60f) return IntelLevel.FULL;
        if (score >= 0.30f) return IntelLevel.PARTIAL;
        return IntelLevel.NONE;
    }

    private long getSimulatedTime(DemonTrait aggressor, DemonTrait defender, IntelLevel intel) {
        if (aggressor.getBehavior() == Behavior.STRATEGIST && intel != IntelLevel.NONE) {
            List<Quirk> knownQuirks = (intel == IntelLevel.FULL) ? defender.getQuirks() : List.of(getRandomElement(defender.getQuirks()));

            if (knownQuirks.contains(Quirk.SUNLIGHT_SICKNESS)) return 6000L; // Use weakness against them
            if (knownQuirks.contains(Quirk.NIGHT_STALKER)) return 6000L;     // Force daytime to nullify strength
        }

        if (aggressor.getBehavior() == Behavior.BERSERKER ||
                aggressor.getPersonality() == Personality.ARROGANT ||
                aggressor.getPersonality() == Personality.BLOODTHIRSTY) {
            return random.nextInt(24000); // OOGA BOOGA CAVEMAN STUPID
        }

        return random.nextFloat() < 0.8f ? 18000L : 6000L; // 80% chance for night
    }

    private record EffectiveStats(float health, float damage, float agility) {}
    private EffectiveStats getEffectiveStats(DemonTrait demon, DemonTrait opponent, BattleContext context, boolean isHomeField) {
        float health = (demon.getVigor() * 10) + (demon.getBloodPoints() / 1.5f);
        float damage = (demon.getStrength() * 5f) + (demon.getBloodPoints() / 15f);
        float agility = demon.getAgility() + (demon.getBloodPoints() / 40f);

        if (isHomeField) {
            float bonus = 1.0f;
            switch (demon.getBehavior()) {
                case STRATEGIST, TERRITORIAL -> bonus = 1.30f;  // Prepared
                case OVERLORD, BODYGUARD -> bonus = 1.20f;      // Prepared, but not too well
                case BERSERKER, COWARDLY, SCAVENGER -> bonus = 0.9f; // Unprepared
                default -> bonus = 1.05f;                       // Slightly prepared
            }
            switch (demon.getPersonality()) {
                case CALCULATING, PATIENT, PARANOID, CAUTIOUS -> bonus *= 1.1f; // Careful planning
                case COCKY, ARROGANT, FRENZIED, BLOODTHIRSTY -> bonus *= 0.9f;  // ooga booga
                default -> bonus *= 1.0f;                                       // Neutral
            }
            health *= bonus;
            damage *= bonus;
            agility *= bonus;
        }

        List<String> opponentTags = BDA_TAGS.getOrDefault(opponent.getAssignedBDA(), Collections.emptyList());
        boolean isNight = context.timeOfDay() >= 13000;

        // Offensive strengths
        if (demon.hasQuirk(Quirk.HEAVY_HITTER)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.SHOCKWAVE_STOMP)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.VENOMOUS)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.EXECUTIONER)) damage *= 1.05f;

        // Defensive strengths
        if (demon.hasQuirk(Quirk.BLOOD_SIPHON)) health *= 1.05f;
        if (demon.hasQuirk(Quirk.ARROW_PROOF)) health *= 1.05f;
        if (demon.hasQuirk(Quirk.UNSHAKABLE)) {
            health *= 1.05f;
            agility *= 0.97f;
        }
        if (demon.hasQuirk(Quirk.RAPID_RECOVERY)) {
            health *= 1.08f;
            agility *= 1.03f;
        }
        if (demon.hasQuirk(Quirk.DEATH_DEFIANT)) health *= 1.2f;
        if (demon.hasQuirk(Quirk.TENACIOUS) && demon.getBloodPoints() < opponent.getBloodPoints()) {
            health *= 1.2f; // Underdog bonus
        }

        // Mobility strengths
        if (demon.hasQuirk(Quirk.NIMBLE)) agility *= 1.1f;
        if (demon.hasQuirk(Quirk.KEEN_Scent)) {
            agility *= 1.05f;
            damage *= 1.03f;
        }

        // Miscellaneous strengths
        if (demon.hasQuirk(Quirk.PACK_CALLER)) { // Having allies is a major buff, but also gives Dishonored for blood battles.
            health *= 1.1f;
            damage *= 1.15f;
        }
        if (demon.hasQuirk(Quirk.NIGHT_STALKER) && isNight) {
            agility *= 1.25f;
            damage *= 1.1f;
        }

        // Combat weaknesses
        if (demon.hasQuirk(Quirk.BRITTLE) && opponentTags.contains("PHYSICAL_BLUNT")) {
            health *= 0.85f;
        }
        if (demon.hasQuirk(Quirk.OVERCONFIDENT)) damage *= 0.95f;
        if (demon.hasQuirk(Quirk.GLASS_CANNON)) {
            damage *= 1.25f;
            health *= 0.7f;
        }
        if (demon.hasQuirk(Quirk.WIND_UP)) {
            damage *= 1.08f;
            agility *= 0.88f;
        }
        if (demon.hasQuirk(Quirk.BLIND_RAGE) && demon.getBloodPoints() < opponent.getBloodPoints()) {
            damage *= 1.1f;
            health *= 0.9f;
        }
        if (demon.hasQuirk(Quirk.OBSESSIVE_HUNTER)) {
            damage *= 1.05f;
            agility *= 0.95f;
        }

        // Environmental weaknesses
        if (demon.hasQuirk(Quirk.SUNLIGHT_SICKNESS) && !isNight) {
            health *= 0.7f;
            agility *= 0.8f;
        }
        if (demon.hasQuirk(Quirk.FEAR_OF_WATER) && isWetBiome(context.biome)) {
            agility *= 0.8f;
            damage *= 0.8f;
        }
        if (demon.hasQuirk(Quirk.FLAMMABLE_FLESH) && (isHotBiome(context.biome) || opponentTags.contains("FIRE"))) {
            health *= 0.8f;
        }
        if (demon.hasQuirk(Quirk.PURIFIED_WEAKNESS)) {
            health *= 0.92f;
            damage *= 0.95f;
        }
        if (demon.hasQuirk(Quirk.SOUND_SENSITIVE) && opponentTags.contains("MIND_AFFECTING")) {
            agility *= 0.85f;
            health *= 0.95f;
        }
        if (demon.hasQuirk(Quirk.MAGPIE)) agility *= 0.92f;
        if (demon.hasQuirk(Quirk.EASILY_TAUNTED)) {
            agility *= 0.9f;
            health *= 0.95f;
        }

        // Elemental Interactions
        if (demon.hasQuirk(Quirk.FIRE_RESISTANT) && (isHotBiome(context.biome) || opponentTags.contains("FIRE"))) {
            health *= 1.2f; // Takes less damage, effectively increasing health
        }
        if (demon.hasQuirk(Quirk.FROST_BLOODED)) {
            if (isColdBiome(context.biome) || opponentTags.contains("COLD")) health *= 1.2f;
            if (isHotBiome(context.biome) || opponentTags.contains("FIRE")) health *= 0.85f;
        }

        return new EffectiveStats(Math.max(1, health), Math.max(1, damage), Math.max(1, agility));
    }

    private record BattleResult(UUID winnerId, UUID loserId, BattleContext context, float winnerTTK, float loserTTK) {}

    private boolean isHotBiome(ResourceLocation biome) { return biome.getPath().contains("nether") || biome.getPath().contains("desert"); }
    private boolean isWetBiome(ResourceLocation biome) { return biome.getPath().contains("swamp") || biome.getPath().contains("river") || biome.getPath().contains("ocean"); }
    private boolean isColdBiome(ResourceLocation biome) { return biome.getPath().contains("snow") || biome.getPath().contains("ice") || biome.getPath().contains("tundra"); }

    private <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    // ── Scheme & relationship helpers (G1–G4) ──────────────────────────

    /** Daily intel + rivalry decay on all demon relationships (G4). */
    private void decayAllRelationships() {
        for (DemonTrait trait : demonDataCache.values()) {
            for (DemonRelationship rel : trait.getRelationships().values()) {
                rel.decayIntel(0.03f);    // ~3% per day
                rel.decayRivalry(0.01f);  // slow rivalry decay
            }
        }
    }

    /**
     * Passive rivalry from personality clashes and rank adjacency (G2).
     * Runs once per day. Only creates small increments — real rivalries
     * come from actual combat encounters.
     */
    private void buildPassiveRivalries() {
        // Adjacent Kizuki ranks build rivalry: lower-ranked demon rivals the one above
        DemonRank[] ranks = DemonRank.values();
        for (int i = 0; i < ranks.length - 1; i++) {
            if (!ranks[i].isMoonRank()) continue;
            UUID lowerId = kizukiSlots.get(ranks[i]);
            // ranks[i+1] is the next higher rank in power
            UUID higherId = (i + 1 < ranks.length && ranks[i + 1].isMoonRank()) ? kizukiSlots.get(ranks[i + 1]) : null;
            if (lowerId == null || higherId == null) continue;

            DemonTrait lowerTrait = getDemonTrait(lowerId);
            if (lowerTrait == null) continue;

            // Ambitious behaviors build rivalry with the demon above them
            if (lowerTrait.getBehavior() == Behavior.AMBITIOUS
                    || lowerTrait.getPersonality() == Personality.ARROGANT) {
                lowerTrait.getOrCreateRelationship(higherId).addRivalry(0.03f);
            }
        }

        // Personality clashes between co-existing fodder (small sample)
        if (rankedFodder.size() >= 2) {
            int checks = Math.min(5, rankedFodder.size());
            for (int i = 0; i < checks; i++) {
                UUID a = rankedFodder.get(random.nextInt(rankedFodder.size()));
                UUID b = rankedFodder.get(random.nextInt(rankedFodder.size()));
                if (a.equals(b)) continue;

                DemonTrait traitA = getDemonTrait(a);
                DemonTrait traitB = getDemonTrait(b);
                if (traitA == null || traitB == null) continue;

                if (arePersonalitiesClashing(traitA.getPersonality(), traitB.getPersonality())) {
                    traitA.getOrCreateRelationship(b).addRivalry(0.02f);
                    traitB.getOrCreateRelationship(a).addRivalry(0.02f);
                }
            }
        }
    }

    /**
     * Returns true if two personalities naturally clash.
     */
    private boolean arePersonalitiesClashing(Personality a, Personality b) {
        return (a == Personality.HONOR_BOUND && (b == Personality.DECEPTIVE || b == Personality.MANIPULATIVE))
            || (b == Personality.HONOR_BOUND && (a == Personality.DECEPTIVE || a == Personality.MANIPULATIVE))
            || (a == Personality.COCKY && b == Personality.RUTHLESS)
            || (b == Personality.COCKY && a == Personality.RUTHLESS)
            || (a == Personality.ARROGANT && b == Personality.ARROGANT)
            || (a == Personality.COMMANDING && b == Personality.COMMANDING)
            || (a == Personality.DEVOTED && b == Personality.MOCKING)
            || (b == Personality.DEVOTED && a == Personality.MOCKING)
            || ((a == Personality.STOIC && b == Personality.WHINING) || (b == Personality.STOIC && a == Personality.WHINING))
            || ((a == Personality.CALCULATING && b == Personality.FRENZIED) || (b == Personality.CALCULATING && a == Personality.FRENZIED))
            || ((a == Personality.PROTECTIVE && b == Personality.CRUEL) || (b == Personality.PROTECTIVE && a == Personality.CRUEL))
            || ((a == Personality.PATIENT && b == Personality.BLOODTHIRSTY) || (b == Personality.PATIENT && a == Personality.BLOODTHIRSTY))
            || ((a == Personality.CAUTIOUS && b == Personality.BLOODTHIRSTY) || (b == Personality.CAUTIOUS && a == Personality.BLOODTHIRSTY));
    }

    /**
     * Scores a potential target for a given aggressor demon (G3).
     * Higher score = more likely to be chosen as the day's scheme target.
     */
    private float scoreSchemeTarget(UUID aggressorId, DemonTrait aggressor, UUID targetId) {
        DemonTrait target = getDemonTrait(targetId);
        if (target == null) return -1f;

        float score = 0f;

        // Rivalry bonus (major driver)
        DemonRelationship rel = aggressor.getRelationship(targetId);
        if (rel != null) {
            score += rel.getRivalryIntensity() * 5.0f;
            score += rel.getIntelOnTarget() * 1.5f; // confidence from knowledge
        }

        // Ambition: want to fight stronger targets to climb
        switch (aggressor.getBehavior()) {
            case AMBITIOUS, OVERLORD -> {
                float powerRatio = (float) target.getBloodPoints() / Math.max(1, aggressor.getBloodPoints());
                if (powerRatio > 1.0f && powerRatio < 2.0f) score += 2.0f; // reachable but stronger
            }
            default -> {}
        }

        // Fear penalty: avoid much stronger targets
        if (aggressor.getBehavior() == Behavior.COWARDLY || aggressor.getPersonality() == Personality.CAUTIOUS
                || aggressor.getPersonality() == Personality.PARANOID) {
            float powerRatio = (float) target.getBloodPoints() / Math.max(1, aggressor.getBloodPoints());
            if (powerRatio > 1.5f) score -= 3.0f;
        }

        // Opportunism: wounded/recently-lost targets
        if (aggressor.getBehavior() == Behavior.SCAVENGER || aggressor.getPersonality() == Personality.OPPORTUNISTIC) {
            if (target.getGrowthStats().getCurrentWinStreak() == 0 && target.getGrowthStats().getLosses() > 0) {
                score += 2.0f; // smells blood
            }
        }

        // Small random element
        score += random.nextFloat() * 1.0f;

        return score;
    }

    /**
     * Selects the best scheme target for an aggressor from a list of candidates (G3).
     * Returns null if no valid target found.
     */
    @Nullable
    private UUID selectSchemeTarget(UUID aggressorId, DemonTrait aggressor, List<UUID> candidates) {
        UUID bestTarget = null;
        float bestScore = -Float.MAX_VALUE;

        for (UUID candidateId : candidates) {
            if (candidateId.equals(aggressorId)) continue;
            float s = scoreSchemeTarget(aggressorId, aggressor, candidateId);
            if (s > bestScore) {
                bestScore = s;
                bestTarget = candidateId;
            }
        }
        return bestTarget;
    }

    // ── Simulation methods ──────────────────────────────────────────────

    private void simulateFodderVsFodder(ServerLevel level) {
        if (rankedFodder.size() < 2) {
            System.out.println("[NEMESIS DEBUG] Skipping Fodder vs Fodder - insufficient demons (" + rankedFodder.size() + " < 2)");
            return;
        }

        // Select aggressor: pick a random fodder demon, weighted by aggressiveness
        UUID aggressorId = null;
        DemonTrait aggressorTrait = null;
        List<UUID> shuffled = new ArrayList<>(rankedFodder);
        Collections.shuffle(shuffled, random);
        for (UUID id : shuffled) {
            DemonTrait t = getDemonTrait(id);
            if (t != null) {
                aggressorId = id;
                aggressorTrait = t;
                break;
            }
        }
        if (aggressorId == null || aggressorTrait == null) {
            System.out.println("[NEMESIS DEBUG] Fodder vs Fodder failed - no valid aggressor found");
            return;
        }

        // Use scheme scoring to pick target (G3)
        UUID defenderId = selectSchemeTarget(aggressorId, aggressorTrait, rankedFodder);
        if (defenderId == null) {
            // Fallback: random pick
            do {
                defenderId = rankedFodder.get(random.nextInt(rankedFodder.size()));
            } while (defenderId.equals(aggressorId));
        }

        DemonTrait defenderTrait = getDemonTrait(defenderId);
        String defenderName = defenderTrait != null ? defenderTrait.getName() : "Unknown";
        System.out.println("[NEMESIS DEBUG] Fodder vs Fodder: " + aggressorTrait.getName() + " vs " + defenderName);

        BattleResult result = simulateBattle(aggressorId, defenderId, level);

        DemonTrait winner = getDemonTrait(result.winnerId());
        DemonTrait loser = getDemonTrait(result.loserId());
        if (winner == null || loser == null) return;

        System.out.println("[NEMESIS DEBUG] Battle result: " + winner.getName() + " defeated " + loser.getName() +
            " (Winner TTK: " + String.format("%.1f", result.winnerTTK()) + "s, Loser TTK: " + String.format("%.1f", result.loserTTK()) + "s)");

        // Write battle outcome memories + growth stats for both fighters
        long time = level.getGameTime();
        SimOutcome outcome = writeSimulatedBattleOutcome(winner, loser, result, time);

        System.out.println("[NEMESIS DEBUG] Battle outcome: " + outcome);

        if (outcome == SimOutcome.RETREAT) {
            // Loser survives — already lost BP in writeSimulatedBattleOutcome
            int pointsGained = Math.max(5, loser.getBloodPoints() / 20);
            winner.addBloodPoints(pointsGained);
            System.out.println("[NEMESIS DEBUG] " + loser.getName() + " retreated, " + winner.getName() + " gains " + pointsGained + " BP");

            String announcement = SimulationAnnouncer.forFodderDuel(winner, loser, result.winnerTTK(), result.loserTTK());
            broadcastToPlayers(level, Component.literal(String.format(announcement, winner.getName(), loser.getName()) + " §7(opponent retreated)").withStyle(ChatFormatting.GRAY));
        } else {
            // Decisive win or humiliation — loser dies
            int pointsGained = Math.max(10, loser.getBloodPoints() / 10);
            winner.addBloodPoints(pointsGained);
            System.out.println("[NEMESIS DEBUG] " + loser.getName() + " killed, " + winner.getName() + " gains " + pointsGained + " BP");
            killDemon(result.loserId());

            String announcement = SimulationAnnouncer.forFodderDuel(winner, loser, result.winnerTTK(), result.loserTTK());
            broadcastToPlayers(level, Component.literal(String.format(announcement, winner.getName(), loser.getName())).withStyle(ChatFormatting.GRAY));
        }
    }

    private void simulateChallengerVsKizuki(ServerLevel level) {
        if (rankedFodder.isEmpty() || !kizukiSlots.containsKey(DemonRank.LOWER_MOON_6)) {
            System.out.println("[NEMESIS DEBUG] Skipping Challenger vs Kizuki - " +
                (rankedFodder.isEmpty() ? "no fodder" : "no LM6"));
            return;
        }

        rankedFodder.sort(Comparator.comparingInt(uuid -> -getDemonTrait(uuid).getBloodPoints()));
        UUID challengerId = rankedFodder.get(0);
        UUID defenderId = kizukiSlots.get(DemonRank.LOWER_MOON_6);

        DemonTrait challengerTrait = getDemonTrait(challengerId);
        DemonTrait lm6Trait = getDemonTrait(defenderId);
        if (challengerTrait == null || lm6Trait == null) return;

        System.out.println("[NEMESIS DEBUG] Challenger vs Kizuki: " + challengerTrait.getName() +
            " (BP: " + challengerTrait.getBloodPoints() + ") challenges " + lm6Trait.getName() +
            " (LM6, BP: " + lm6Trait.getBloodPoints() + ")");

        BattleResult result = simulateBattle(challengerId, defenderId, level);

        // Write battle outcome memories + growth stats for both
        long time = level.getGameTime();
        DemonTrait winner = result.winnerId().equals(challengerId) ? challengerTrait : lm6Trait;
        DemonTrait loser = result.winnerId().equals(challengerId) ? lm6Trait : challengerTrait;

        System.out.println("[NEMESIS DEBUG] Battle result: " + winner.getName() + " defeated " + loser.getName() +
            " (Winner TTK: " + String.format("%.1f", result.winnerTTK()) + "s, Loser TTK: " + String.format("%.1f", result.loserTTK()) + "s)");

        writeSimulatedBattleOutcome(winner, loser, result, time);

        if (result.winnerId().equals(challengerId)) {
            // Huzzah! A victory — also record hierarchy changes
            System.out.println("[NEMESIS DEBUG] Challenger victory! " + challengerTrait.getName() +
                " promoted to Lower Moon Six, " + lm6Trait.getName() + " demoted to fodder");
            challengerTrait.addMemory(DemonMemory.of(MemoryType.PROMOTED, time, defenderId, challengerId));
            challengerTrait.addMemory(DemonMemory.of(MemoryType.DEFEATED_KIZUKI, time, defenderId, challengerId));
            challengerTrait.addMemory(DemonMemory.of(MemoryType.KILLED_KIZUKI, time, defenderId, challengerId));

            killDemon(defenderId);
            rankedFodder.remove(challengerId);
            kizukiSlots.put(DemonRank.LOWER_MOON_6, challengerId);

            // Log story events (H1)
            logStoryEvent(StoryEvent.promotion(time, challengerId,
                    challengerTrait.getName(), DemonRank.LOWER_MOON_6.getDisplayName().getString()));
            logStoryEvent(StoryEvent.demonKilled(time, defenderId, lm6Trait.getName(),
                    challengerId, challengerTrait.getName()));

            String announcement = SimulationAnnouncer.forChallengerUpset(challengerTrait, lm6Trait);
            broadcastToPlayers(level, Component.literal(String.format(announcement,
                    challengerTrait.getName() + " " + challengerTrait.getTitle(),
                    lm6Trait.getName())).withStyle(ChatFormatting.RED));
        } else {
            System.out.println("[NEMESIS DEBUG] Challenger defeat: " + lm6Trait.getName() + " successfully defended Lower Moon Six");
        }
    }

    private void simulateKizukiVsKizuki(ServerLevel level) {
        // All moon ranks EXCEPT the highest (UM1) can challenge the rank above
        List<DemonRank> challengerCandidates = Arrays.stream(DemonRank.values())
            .filter(r -> {
                if (!r.isMoonRank()) return false;
                if (r == DemonRank.UPPER_MOON_1) return false;

                return kizukiSlots.containsKey(r)
                    && kizukiSlots.containsKey(DemonRank.values()[r.ordinal() + 1]);
            })
            .toList();

        if (challengerCandidates.isEmpty()) {
            System.out.println("[NEMESIS DEBUG] Skipping Kizuki vs Kizuki - no challenger candidates available");
            return;
        }

        DemonRank challengerRank = challengerCandidates.get(random.nextInt(challengerCandidates.size()));
        // Defender is one rank HIGHER in power (ordinal + 1)
        DemonRank defenderRank = DemonRank.values()[challengerRank.ordinal() + 1];

        UUID challengerId = kizukiSlots.get(challengerRank);
        UUID defenderId = kizukiSlots.get(defenderRank);

        if (challengerId == null || defenderId == null) {
            System.out.println("[NEMESIS DEBUG] Skipping Kizuki vs Kizuki - missing demons for " +
                challengerRank.getDisplayName().getString() + " or " + defenderRank.getDisplayName().getString());
            return;
        }

        DemonTrait challengerTrait = getDemonTrait(challengerId);
        DemonTrait defenderTrait = getDemonTrait(defenderId);
        if (challengerTrait == null || defenderTrait == null) return;

        System.out.println("[NEMESIS DEBUG] Kizuki vs Kizuki: " + challengerTrait.getName() +
            " (" + challengerRank.getDisplayName().getString() + ", BP: " + challengerTrait.getBloodPoints() + ") challenges " +
            defenderTrait.getName() + " (" + defenderRank.getDisplayName().getString() + ", BP: " + defenderTrait.getBloodPoints() + ")");

        BattleResult result = simulateBattle(challengerId, defenderId, level);

        // Write battle outcome memories + growth stats for both
        long time = level.getGameTime();
        DemonTrait winner = result.winnerId().equals(challengerId) ? challengerTrait : defenderTrait;
        DemonTrait loser = result.winnerId().equals(challengerId) ? defenderTrait : challengerTrait;

        System.out.println("[NEMESIS DEBUG] Battle result: " + winner.getName() + " defeated " + loser.getName() +
            " (Winner TTK: " + String.format("%.1f", result.winnerTTK()) + "s, Loser TTK: " + String.format("%.1f", result.loserTTK()) + "s)");

        writeSimulatedBattleOutcome(winner, loser, result, time);

        if (result.winnerId().equals(challengerId)) {
            // SHOCKING UPSET — record hierarchy changes
            System.out.println("[NEMESIS DEBUG] SHOCKING UPSET! " + challengerTrait.getName() +
                " promoted to " + defenderRank.getDisplayName().getString() + ", " +
                defenderTrait.getName() + " killed, power vacuum triggered for " + challengerRank.getDisplayName().getString());

            challengerTrait.addMemory(DemonMemory.of(MemoryType.PROMOTED, time, defenderId, challengerId));
            challengerTrait.addMemory(DemonMemory.of(MemoryType.DEFEATED_KIZUKI, time, defenderId, challengerId));
            challengerTrait.addMemory(DemonMemory.of(MemoryType.KILLED_KIZUKI, time, defenderId, challengerId));
            defenderTrait.addMemory(DemonMemory.of(MemoryType.DEMOTED, time, challengerId, defenderId));

            kizukiSlots.put(defenderRank, challengerId);
            kizukiSlots.remove(challengerRank);
            killDemon(defenderId);
            handlePowerVacuum(challengerRank);

            // Log story events (H1)
            logStoryEvent(StoryEvent.promotion(time, challengerId,
                    challengerTrait.getName(), defenderRank.getDisplayName().getString()));
            logStoryEvent(StoryEvent.demonKilled(time, defenderId, defenderTrait.getName(),
                    challengerId, challengerTrait.getName()));

            String announcement = SimulationAnnouncer.forKizukiUpset(challengerTrait, defenderTrait, defenderRank);
            broadcastToPlayers(level, Component.literal(String.format(announcement,
                    challengerTrait.getName(),
                    defenderTrait.getName(),
                    defenderRank.getDisplayName().getString())).withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            System.out.println("[NEMESIS DEBUG] Kizuki defense: " + defenderTrait.getName() +
                " successfully defended " + defenderRank.getDisplayName().getString());
        }
    }

    /**
     * Ensures the nemesis roster stays filled up to a target size.
     *
     * This method must be cheap and bounded; it should never spam-generate demons indefinitely.
     */
    private void refillDemonRoster(ServerLevel level) {
        // "Fodder" excludes the moon-rank slots.
        final int targetFodderCount = 20;
        if (level == null) return;

        // Safety clamp: never generate more than this many in a single call.
        final int maxToGenerateThisCall = 5;

        int needed = targetFodderCount - rankedFodder.size();
        if (needed <= 0) {
            System.out.println("[NEMESIS DEBUG] Roster full: " + rankedFodder.size() + " fodder, no generation needed");
            return;
        }

        long gameTime = level.getGameTime();
        int toGenerate = Math.min(needed, maxToGenerateThisCall);
        System.out.println("[NEMESIS DEBUG] Roster refill: need " + needed + " fodder, generating " + toGenerate);
        for (int i = 0; i < toGenerate; i++) {
            UUID demonUUID = UUID.randomUUID();
            DemonTrait trait = new DemonTrait();
            DemonGenerator.initializeProceduralTrait(trait, gameTime);
            this.addDemon(demonUUID, trait);
        }
    }

    /**
     * Fills ALL vacant Kizuki slots immediately by promoting from below.
     * Loops until no more promotions can be made. For each vacant slot,
     * if the rank directly below is occupied and the demon meets the BP
     * threshold for the target rank, they are promoted upward and a power
     * vacuum is triggered for their old slot (which cascades fodder in).
     *
     * <p>Lower Moon ranks have similar, low thresholds (they're close in power).
     * Upper Moon ranks have dramatically higher thresholds, representing the
     * massive power gap between Lower and Upper Moons.
     */
    private void fillVacantKizukiSlots(ServerLevel level) {
        long time = level.getGameTime();
        DemonRank[] allRanks = DemonRank.values();

        // Keep looping until no more promotions happen in a full pass
        boolean promotedAny;
        do {
            promotedAny = false;
            // Walk from the lowest moon rank up to the highest
            for (int ord = DemonRank.LOWER_MOON_6.ordinal(); ord <= DemonRank.UPPER_MOON_1.ordinal(); ord++) {
                DemonRank targetRank = allRanks[ord];
                if (!targetRank.isMoonRank()) continue;

                // If this slot is already filled, skip
                if (kizukiSlots.containsKey(targetRank)) continue;

                int bpThreshold = getOrganicPromotionBpThreshold(targetRank);

                UUID bestCandidateId = null;
                DemonRank bestCandidateRank = null;
                int bestCandidateBP = -1;

                // Check Fodder first
                for (UUID fodderId : rankedFodder) {
                    DemonTrait t = getDemonTrait(fodderId);
                    if (t != null && t.getBloodPoints() > bestCandidateBP) {
                        bestCandidateBP = t.getBloodPoints();
                        bestCandidateId = fodderId;
                    }
                }

                // Check all Kizuki ranks BELOW current
                for (int lowerOrd = ord - 1; lowerOrd >= DemonRank.LOWER_MOON_6.ordinal(); lowerOrd--) {
                    DemonRank r = allRanks[lowerOrd];
                    if (kizukiSlots.containsKey(r)) {
                        UUID id = kizukiSlots.get(r);
                        DemonTrait t = getDemonTrait(id);
                        if (t != null && t.getBloodPoints() > bestCandidateBP) {
                            bestCandidateBP = t.getBloodPoints();
                            bestCandidateId = id;
                            bestCandidateRank = r;
                        }
                    }
                }

                if (bestCandidateId != null && bestCandidateBP >= bpThreshold) {
                    DemonTrait trait = getDemonTrait(bestCandidateId);
                    String candidateName = trait != null ? trait.getName() : "Unknown";

                    System.out.println("[NEMESIS DEBUG] VACANCY FILL: " + candidateName +
                        " (BP: " + bestCandidateBP + ", threshold: " + bpThreshold +
                        ") promoted to vacant " + targetRank.getDisplayName().getString());

                    kizukiSlots.put(targetRank, bestCandidateId);

                    if (bestCandidateRank != null) {
                        kizukiSlots.remove(bestCandidateRank);
                    } else {
                        rankedFodder.remove(bestCandidateId);
                    }

                    if (trait != null) {
                        trait.addMemory(DemonMemory.of(MemoryType.PROMOTED, time, null, bestCandidateId));
                        logStoryEvent(StoryEvent.promotion(time, bestCandidateId,
                                trait.getName(), targetRank.getDisplayName().getString()));
                    }

                    broadcastToPlayers(level, Component.literal(
                            "§5" + candidateName + " has risen to the rank of " + targetRank.getDisplayName().getString() + "!")
                            .withStyle(ChatFormatting.DARK_PURPLE));

                    if (bestCandidateRank != null) {
                        // The rank below is now vacant — handle it via cascade (pulls fodder in)
                        handlePowerVacuum(bestCandidateRank);
                    }

                    promotedAny = true;
                    setDirty();
                }
            }
        } while (promotedAny);
    }

    /**
     * Returns the BP threshold a demon needs to be promoted into the given rank.
     *
     * <p>Lower Moons are similar in power — low, uniform thresholds.
     * Upper Moons have a VERY large gap, representing the canonical power
     * difference between Lower and Upper Kizuki.
     */
    private int getOrganicPromotionBpThreshold(DemonRank rank) {
        return switch (rank) {
            case LOWER_MOON_6, LOWER_MOON_5,
                 LOWER_MOON_4, LOWER_MOON_3,
                 LOWER_MOON_2, LOWER_MOON_1 -> 200;
            case UPPER_MOON_6 -> 450;
            case UPPER_MOON_5 -> 650;
            case UPPER_MOON_4 -> 900;
            case UPPER_MOON_3 -> 1200;
            case UPPER_MOON_2 -> 1600;
            case UPPER_MOON_1 -> 2000;
            default -> Integer.MAX_VALUE; // non-moon ranks cannot be promoted into
        };
    }

    private void broadcastToPlayers(ServerLevel level, Component message) {
        if (level != null) {
            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        } else {
            System.out.println("[Nemesis Announcement] " + message.getString());
        }
    }

    public void startBloodBattle(UUID challenger, UUID defender) {
        if (this.activeBattle == null) {
            this.activeBattle = new BloodBattle(challenger, defender);
            System.out.println("[NEMESIS DEBUG] BLOOD BATTLE INITIATED: challenger=" + challenger.toString().substring(0, 8) +
                " vs defender=" + defender.toString().substring(0, 8));
            setDirty();
        }
    }

    public void recordInterference(UUID defenderUUID, UUID attackerUUID) {
        if (this.activeBattle != null && this.activeBattle.defender.equals(defenderUUID)) {
            // Only record interference if: the attacker != original challenger
            if (!this.activeBattle.challenger.equals(attackerUUID)) {
                this.activeBattle.interferers.add(attackerUUID);
                System.out.println("[NEMESIS DEBUG] Blood Battle interference recorded: attacker=" + attackerUUID.toString().substring(0, 8) +
                    " (total interferers: " + this.activeBattle.interferers.size() + ")");
                setDirty();
            }
        }
    }

    @Nullable public BloodBattle getActiveBattle() {
        return this.activeBattle;
    }

    public void endBloodBattle() {
        this.activeBattle = null;
        System.out.println("[NEMESIS DEBUG] Blood Battle has concluded and been cleared.");
        setDirty();
    }

    private String getTauntForSituation(DemonTrait trait, String key, String playerName) {
        // demon-specific blueprint taunts
        String taunt = getRandomElement(trait.getBlueprintTaunts().get(key));
        if (taunt != null) return taunt; // Caller handles template expansion via TauntRenderer

        // personality-specific blueprint taunts
        Optional<DemonBlueprint> personalityBp = BlueprintLoader.getPersonalityDefaults(trait.getPersonality());
        if (personalityBp.isPresent() && personalityBp.get().taunts() != null) {
            taunt = getRandomElement(personalityBp.get().taunts().get(key));
            if (taunt != null) return taunt;
        }

        // global defaults blueprint taunts
        Optional<DemonBlueprint> globalBp = BlueprintLoader.getGlobalDefaults();
        if (globalBp.isPresent() && globalBp.get().taunts() != null) {
            taunt = getRandomElement(globalBp.get().taunts().get(key));
            if (taunt != null) return taunt;
        }

        // Ultimate fallback — still uses {player} placeholder for TauntRenderer
        return "You have fallen, {player}.";
    }

    public void demonKillsPlayer(NemesisDemonEntity demon, Player player, DamageSource source, float amount) {
        UUID demonUUID = demon.getUUID();
        DemonTrait trait = demonDataCache.get(demonUUID);
        if (trait == null) return;

        System.out.println("[NEMESIS DEBUG] demonKillsPlayer: " + trait.getName() + " killed " + player.getGameProfile().getName());

        int playerBloodPoints = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(PlayerDemon::getBloodPoints)
                .orElse(0);
        int demonBloodPoints = trait.getBloodPoints();
        int bpGain;

        if (demonBloodPoints < playerBloodPoints * 0.5f) { // DemonBP < 50% PlayerBP
            bpGain = 100; // Underdog
            trait.addBloodPoints(bpGain);
        } else if (demonBloodPoints < playerBloodPoints * 0.8f) { // DemonBP < 80% PlayerBP
            bpGain = 85; // Underdog, but not as much
            trait.addBloodPoints(bpGain);
        } else { // DemonBP >= 80% PlayerBP
            bpGain = 60; // Not underdog
            trait.addBloodPoints(bpGain);
        }

        System.out.println("[NEMESIS DEBUG] BP reward: +" + bpGain + " (demon BP: " + demonBloodPoints + " → " + trait.getBloodPoints() +
            ", player BP: " + playerBloodPoints + ")");

        boolean hasEvolved = false;
        String tauntKey;

        if (demon.getHealth() / demon.getMaxHealth() < 0.25f) {
            tauntKey = "low_health_comeback";
            if (!trait.hasQuirk(Quirk.TENACIOUS) && random.nextFloat() < 0.25f) {
                trait.setTitle(getRandomElement(List.of("the Unbroken", "the Tenacious", "the Resilient")));
                trait.addQuirk(Quirk.TENACIOUS);
                hasEvolved = true;
            }
        } else if (source.isExplosion() || source.isFire()) {
            tauntKey = "fire_kill";
            if (!trait.hasQuirk(Quirk.HEAVY_HITTER) && random.nextFloat() < 0.25f) {
                trait.setTitle(getRandomElement(List.of("the Calamity", "the Cremator", "the Inferno")));
                trait.addQuirk(Quirk.HEAVY_HITTER);
                hasEvolved = true;
            }
        } else if (player.hasEffect(MobEffects.POISON) || player.hasEffect(ModEffects.WISTERIA_POISON.get()) || trait.getAssignedBDA() == BloodDemonArts.HAEMO) {
            tauntKey = "poison_kill";
            if (!trait.hasQuirk(Quirk.VENOMOUS) && random.nextFloat() < 0.3f) {
                trait.setTitle(getRandomElement(List.of("the Plague", "the Corruptor", "the Blighted")));
                trait.addQuirk(Quirk.VENOMOUS);
                hasEvolved = true;
            }
        } else if (amount > player.getMaxHealth() * 0.7f) {
            tauntKey = "overkill";
            if (!trait.hasQuirk(Quirk.EXECUTIONER) && random.nextFloat() < 0.2f) {
                trait.setTitle(getRandomElement(List.of("the Executioner", "the Decimator")));
                trait.addQuirk(Quirk.EXECUTIONER);
                hasEvolved = true;
            }
        } else if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || player.hasEffect(MobEffects.WEAKNESS) || player.hasEffect(ModEffects.PARALYZING_POISON.get()) || player.hasEffect(ModEffects.FORCED_SLEEP.get())) {
            tauntKey = "tactical_kill";
        } else {
            tauntKey = "generic";
        }

        String taunt = getTauntForSituation(trait, tauntKey, player.getGameProfile().getName());

        // Expand template with all placeholders (I3)
        Optional<DemonRank> demonRank = getKizukiRank(demonUUID);
        taunt = TauntRenderer.render(taunt, trait, player.getGameProfile().getName(),
                demonRank.orElse(null), null, null, demon.level.getDayTime() % 24000);

        trait.addPlayerTaunt(player.getUUID(), taunt);

        if (hasEvolved) {
            System.out.println("[NEMESIS DEBUG] EVOLUTION: " + trait.getName() + " evolved! New title: " + trait.getTitle());
            String evolutionMsg = String.format("§cThrough bloodshed, %s has evolved, now known as %s, %s!",
                    trait.getName(), trait.getName(), trait.getTitle());
            broadcastToPlayers((ServerLevel) demon.level, Component.literal(evolutionMsg));
            logStoryEvent(StoryEvent.evolution(demon.level.getGameTime(), demonUUID,
                    trait.getName(), trait.getTitle()));
        }

        // Log player kill story event (H1)
        logStoryEvent(StoryEvent.playerKilled(demon.level.getGameTime(), demonUUID,
                trait.getName(), player.getUUID(), player.getGameProfile().getName()));

        System.out.println("[NEMESIS DEBUG] " + trait.getName() + " has killed player " + player.getGameProfile().getName() + " and grown stronger.");
        setDirty();
    }

    public void killDemon(UUID demonUUID) {
        if (demonDataCache.containsKey(demonUUID)) {
            DemonTrait trait = getDemonTrait(demonUUID);
            String demonName = trait != null ? trait.getName() : "Unknown";
            Optional<DemonRank> rank = getKizukiRank(demonUUID);

            System.out.println("[NEMESIS DEBUG] DEMON KILLED: " + demonName +
                (rank.isPresent() ? " (" + rank.get().getDisplayName().getString() + ")" : " (Fodder)") +
                " permanently removed from hierarchy");

            rankedFodder.remove(demonUUID);
            demonDataCache.remove(demonUUID);
            removeDemonFromSocialStructures(demonUUID);

            kizukiSlots.entrySet().removeIf(entry -> entry.getValue().equals(demonUUID));

            System.out.println("[NEMESIS DEBUG] A Nemesis Demon has been slain permanently.");
            setDirty();
        } else {
            System.out.println("[NEMESIS DEBUG] killDemon called for unknown demon UUID: " + demonUUID);
        }
    }

    public void setKizukiRank(DemonRank rank, UUID demonId) {
        DemonTrait trait = getDemonTrait(demonId);
        String demonName = trait != null ? trait.getName() : "Unknown";

        // Remove from any existing rank
        Optional<DemonRank> oldRank = getKizukiRank(demonId);
        if (oldRank.isPresent()) {
            System.out.println("[NEMESIS DEBUG] RANK CHANGE: " + demonName + " moved from " +
                oldRank.get().getDisplayName().getString() + " to " + rank.getDisplayName().getString());
        } else {
            System.out.println("[NEMESIS DEBUG] RANK ASSIGNMENT: " + demonName + " assigned to " +
                rank.getDisplayName().getString() + " (was fodder)");
        }

        kizukiSlots.entrySet().removeIf(entry -> entry.getValue().equals(demonId));
        kizukiSlots.put(rank, demonId);
        rankedFodder.remove(demonId);
        setDirty();
    }

    /** Minimum blood points a fodder demon needs to be auto-promoted to Kizuki. */
    private static final int MINIMUM_BP_FOR_KIZUKI = 200;

    /**
     * Fills a vacant Kizuki slot by cascading promotions upward from the next
     * lower rank. If no next-lower Kizuki exists, promotes the strongest fodder
     * demon that meets the BP threshold. Each promoted demon gets a PROMOTED memory (F2).
     */
    public void handlePowerVacuum(DemonRank slainKizukiRank) {
        System.out.println("[NEMESIS DEBUG] ═══ POWER VACUUM: " + slainKizukiRank.getDisplayName().getString() + " slot vacant ═══");
        long time = 0L; // Approximate; no level ref here, but memories still record 0

        DemonRank currentVacancy = slainKizukiRank;

        // Walk downward through all moon ranks, promoting each demon one step up
        while (currentVacancy != null && currentVacancy.isMoonRank()) {
            // Find the next occupied rank below this vacancy
            DemonRank nextOccupied = null;
            for (int ord = currentVacancy.ordinal() - 1; ord >= DemonRank.LOWER_MOON_6.ordinal(); ord--) {
                DemonRank candidate = DemonRank.values()[ord];
                if (candidate.isMoonRank() && kizukiSlots.containsKey(candidate)) {
                    nextOccupied = candidate;
                    break;
                }
            }

            if (nextOccupied != null) {
                UUID promoterUUID = kizukiSlots.get(nextOccupied);
                DemonTrait promotedTrait = getDemonTrait(promoterUUID);
                String promotedName = promotedTrait != null ? promotedTrait.getName() : "Unknown";
                int promoterBP = promotedTrait != null ? promotedTrait.getBloodPoints() : 0;

                // Enforce BP threshold — a demon must be strong enough for the target rank
                int bpThreshold = getOrganicPromotionBpThreshold(currentVacancy);
                if (promoterBP < bpThreshold) {
                    System.out.println("[NEMESIS DEBUG] Cascade BLOCKED: " + promotedName + " in " +
                        nextOccupied.getDisplayName().getString() + " has " + promoterBP +
                        " BP but needs " + bpThreshold + " for " + currentVacancy.getDisplayName().getString() +
                        " — slot left empty");
                    break;
                }

                System.out.println("[NEMESIS DEBUG] Cascading promotion: " + promotedName + " from " +
                    nextOccupied.getDisplayName().getString() + " to " + currentVacancy.getDisplayName().getString() +
                    " (BP: " + promoterBP + " >= threshold: " + bpThreshold + ")");

                kizukiSlots.put(currentVacancy, promoterUUID);
                kizukiSlots.remove(nextOccupied);

                // Write PROMOTED memory
                if (promotedTrait != null) {
                    promotedTrait.addMemory(DemonMemory.of(MemoryType.PROMOTED, time, null, promoterUUID));
                    logStoryEvent(StoryEvent.promotion(time, promoterUUID,
                            promotedTrait.getName(), currentVacancy.getDisplayName().getString()));
                }
                // Continue cascading — the promoted demon's old rank is now vacant
                currentVacancy = nextOccupied;
            } else {
                // No more Kizuki below to cascade — try to promote top fodder into the lowest vacant moon rank
                // The lowest vacant moon rank is always currentVacancy at this point,
                // but we should specifically fill LM6 if it's empty (normal case)
                DemonRank lowestVacantMoon = currentVacancy;

                System.out.println("[NEMESIS DEBUG] No lower Kizuki to cascade for " + lowestVacantMoon.getDisplayName().getString() +
                    " - attempting to promote top fodder (minimum BP: " + MINIMUM_BP_FOR_KIZUKI + ")");
                Optional<UUID> topFodderUUID = rankedFodder.stream()
                        .filter(uuid -> {
                            DemonTrait t = getDemonTrait(uuid);
                            return t != null && t.getBloodPoints() >= MINIMUM_BP_FOR_KIZUKI;
                        })
                        .max(Comparator.comparingInt(uuid -> {
                            DemonTrait t = getDemonTrait(uuid);
                            return t != null ? t.getBloodPoints() : 0;
                        }));

                if (topFodderUUID.isPresent()) {
                    UUID promoterId = topFodderUUID.get();
                    DemonTrait fodderTrait = getDemonTrait(promoterId);
                    String fodderName = fodderTrait != null ? fodderTrait.getName() : "Unknown";
                    int fodderBP = fodderTrait != null ? fodderTrait.getBloodPoints() : 0;

                    System.out.println("[NEMESIS DEBUG] Promoting fodder to " + lowestVacantMoon.getDisplayName().getString() +
                        ": " + fodderName + " (BP: " + fodderBP + ")");

                    rankedFodder.remove(promoterId);
                    kizukiSlots.put(lowestVacantMoon, promoterId);

                    if (fodderTrait != null) {
                        fodderTrait.addMemory(DemonMemory.of(MemoryType.PROMOTED, time, null, promoterId));
                        logStoryEvent(StoryEvent.promotion(time, promoterId,
                                fodderTrait.getName(), lowestVacantMoon.getDisplayName().getString()));
                    }
                } else {
                    System.out.println("[NEMESIS DEBUG] No qualifying fodder found for " + lowestVacantMoon.getDisplayName().getString() +
                        " (need BP >= " + MINIMUM_BP_FOR_KIZUKI + ") - slot remains empty");
                }

                System.out.println("[NEMESIS DEBUG] Power vacuum cascade complete - " + lowestVacantMoon.getDisplayName().getString() +
                    " slot " + (kizukiSlots.containsKey(lowestVacantMoon) ? "filled" : "remains empty"));
                break;
            }
        }
        System.out.println("[NEMESIS DEBUG] Power vacuum resolved in the Kizuki ranks.");
        setDirty();
    }

    @Nullable public DemonTrait getDemonTrait(UUID uuid) {
        return this.demonDataCache.get(uuid);
    }

    public void addDemon(UUID uuid, DemonTrait trait) {
        if (!demonDataCache.containsKey(uuid)) {
            demonDataCache.put(uuid, trait);
            rankedFodder.add(uuid);

            System.out.println("[NEMESIS DEBUG] New demon registered: " + trait.getName() + " " + trait.getTitle() +
                " (BP:" + trait.getBloodPoints() + ", " + trait.getBehavior() + "/" + trait.getPersonality() + ")");
            setDirty();
        }
    }

    public Optional<DemonRank> getKizukiRank(UUID demonUUID) {
        for (Map.Entry<DemonRank, UUID> entry : kizukiSlots.entrySet()) {
            if (entry.getValue().equals(demonUUID)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void tickSocialAlliances(long gameTime) {
        expireActiveAlliances(gameTime);

        if (this.battleSessionTracker != null) {
            activeAlliances.entrySet().removeIf(entry -> !this.battleSessionTracker.isInBattle(entry.getKey()));
        }
    }

    public Optional<UUID> getDemonIdForRank(DemonRank rank) {
        if (rank == null) {
            return Optional.empty();
        }

        if (rank == DemonRank.FODDER) {
            return getRandomFodderDemonId();
        }

        return Optional.ofNullable(kizukiSlots.get(rank));
    }

    public Optional<UUID> getRandomFodderDemonId() {
        List<UUID> candidates = rankedFodder.stream()
                .filter(id -> getDemonTrait(id) != null)
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public @Nullable UUID getAllianceLeader(UUID memberId, long currentTime) {
        for (Map.Entry<UUID, ActiveAlliance> entry : activeAlliances.entrySet()) {
            if (entry.getValue().expiresAt() > currentTime && entry.getValue().members().contains(memberId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void tryMobilizeAlliesForFight(UUID leaderId, @Nullable UUID targetId, boolean targetIsPlayer,
                                          ServerLevel level, BlockPos anchor, int requestedAllies) {
        if (leaderId == null || targetId == null) return;

        if (this.battleSessionTracker != null && this.battleSessionTracker.isInBattle(leaderId)) {
            return;
        }

        boolean isResponder = isAllianceMember(leaderId, level.getGameTime());

        // Prevent cascading summons beyond depth 1
        if (isResponder) {
            UUID myLeader = getAllianceLeader(leaderId, level.getGameTime());
            // If my leader is also a responder, I am depth 2 (Grandchild), so I cannot summon.
            if (myLeader != null && isAllianceMember(myLeader, level.getGameTime())) {
                System.out.println("[NEMESIS DEBUG] Mobilization blocked: " + leaderId + " is a secondary summon (depth 2+)");
                return;
            }
        }

        ActiveAlliance existing = activeAlliances.get(leaderId);
        long now = level.getGameTime();
        if (existing != null && existing.expiresAt() > now) {
            return;
        }

        DemonTrait leaderTrait = getDemonTrait(leaderId);
        if (leaderTrait == null) return;

        int allyBudget = Math.max(0, Math.min(MAX_ALLIES_PER_CALL_HARD, requestedAllies));
        if (allyBudget == 0) return;

        List<UUID> candidates = findCompatibleAllies(leaderId, targetId, targetIsPlayer, level, allyBudget);

        if (isResponder) {
            Set<UUID> packMembers = demonPacks.getOrDefault(leaderId, Collections.emptySet());
            candidates = candidates.stream()
                    .filter(packMembers::contains)
                    .toList();
        }

        if (candidates.isEmpty()) {
            return;
        }

        net.minecraft.world.entity.Entity targetEntity = targetIsPlayer
                ? level.getPlayerByUUID(targetId)
                : level.getEntity(targetId);

        if (!(targetEntity instanceof net.minecraft.world.entity.LivingEntity livingTarget)) {
            return;
        }

        Set<UUID> committed = new HashSet<>();

        for (UUID allyId : candidates) {
            if (level.getEntity(allyId) != null) {
                continue;
            }

            int distance = 40 + random.nextInt(41); // 40-80 blocks
            double angle = random.nextDouble() * 2 * Math.PI;
            int xOffset = (int) (Math.cos(angle) * distance);
            int zOffset = (int) (Math.sin(angle) * distance);
            BlockPos pos = anchor.offset(xOffset, 0, zOffset);

            DemonTrait allyTrait = getDemonTrait(allyId);
            String allyName = allyTrait != null ? allyTrait.getName() : "Unknown";
            String reason = "Ally for " + leaderTrait.getName() + (isResponder ? " (Pack)" : "");

            System.out.println("[NEMESIS DEBUG] Mobilizing ally: " + allyName +
                " for leader " + leaderTrait.getName() +
                (isResponder ? " (Pack member call)" : ""));

            boolean spawned = spawnDemonFromData(allyId, level, pos, reason);
            if (!spawned) continue;

            net.minecraft.world.entity.Entity spawnedEntity = level.getEntity(allyId);
            if (spawnedEntity instanceof NemesisDemonEntity allySpawned) {
                allySpawned.setTarget(livingTarget);
                committed.add(allyId);
            }
        }

        if (!committed.isEmpty()) {
            // Use battle session to track validity instead of arbitrary time limit
            activeAlliances.put(leaderId, new ActiveAlliance(committed, targetId, targetIsPlayer, Long.MAX_VALUE));
        }
    }

    /**
     * Returns true if the given demon is currently a member (summoned ally) in an active alliance.
     * Used to prevent cascading summons where summoned allies call their own reinforcements.
     */
    public boolean isAllianceMember(UUID demonId, long currentTime) {
        for (ActiveAlliance alliance : activeAlliances.values()) {
            if (alliance.expiresAt() > currentTime && alliance.members().contains(demonId)) {
                return true;
            }
        }
        return false;
    }

    public void trySpawnHunterForPlayer(ServerPlayer player) {
        if (player.level.getDifficulty() == Difficulty.PEACEFUL) return;
        ResourceLocation currentBiome = player.level.getBiome(player.blockPosition()).unwrapKey().get().location();

        // Build candidates from ALL demons — territory match, relationship, or emotion (J2)
        List<UUID> potentialHunters = new ArrayList<>();
        for (Map.Entry<UUID, DemonTrait> entry : demonDataCache.entrySet()) {
            UUID demonId = entry.getKey();
            DemonTrait trait = entry.getValue();
            if (trait == null) continue;

            boolean territoryMatch = trait.getTerritory().equals(currentBiome);
            boolean hasRelationship = false;
            boolean hasEmotionalDrive = false;

            // Check relationship toward this player (J2)
            DemonRelationship rel = trait.getRelationship(player.getUUID());
            if (rel != null && (rel.getRivalryIntensity() > 0.15f || rel.getEncounterCount() > 0)) {
                hasRelationship = true;
            }

            // Check emotion drive toward player
            var emotions = trait.getEmotionState();
            if (emotions.getIntensity(com.wassuples.demonslayer.nemesis.trait.EmotionType.HATE_PLAYER) > 0.3f
                    || emotions.getIntensity(com.wassuples.demonslayer.nemesis.trait.EmotionType.RIVALRY_GRUDGE) > 0.3f) {
                hasEmotionalDrive = true;
            }

            if (territoryMatch || hasRelationship || hasEmotionalDrive) {
                potentialHunters.add(demonId);
            }
        }

        if (potentialHunters.isEmpty()) return;

        UUID hunterId = potentialHunters.get(random.nextInt(potentialHunters.size()));
        DemonTrait hunterTrait = getDemonTrait(hunterId);
        if (hunterTrait == null) return;

        // 1. Base Chance
        float baseChance = hunterTrait.getBloodPoints() / 5000.0f;

        // 2. Personality Modifier
        float personalityModifier = 1.0f;
        switch (hunterTrait.getBehavior()) {
            case AGGRESSIVE, BERSERKER, TERRITORIAL -> personalityModifier = 1.5f;
            case STRATEGIST, AMBITIOUS -> personalityModifier = 1.2f;
            case COWARDLY, SCAVENGER -> personalityModifier = 0.5f;
        }
        switch (hunterTrait.getPersonality()) {
            case BLOODTHIRSTY, FRENZIED -> personalityModifier *= 1.4f;
            case CAUTIOUS, PARANOID -> personalityModifier *= 0.7f;
        }

        // 3. Power Difference Modifier
        int playerBloodPoints = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(PlayerDemon::getBloodPoints)
                .orElse(50);

        float powerRatio = (float) hunterTrait.getBloodPoints() / Math.max(1, playerBloodPoints);
        float powerDifferenceModifier = 1.0f;
        if (powerRatio > 1.5) {
            powerDifferenceModifier = 1.25f;
        } else if (powerRatio < 0.3) {
            powerDifferenceModifier = 0.2f; // Much weaker → very hesitant (J2: enhanced rank disparity)
        } else if (powerRatio < 0.5) {
            powerDifferenceModifier = 0.4f;
        }

        // 4. Relationship intensity modifier (J2)
        float relationshipModifier = 1.0f;
        DemonRelationship rel = hunterTrait.getRelationship(player.getUUID());
        if (rel != null) {
            relationshipModifier += rel.getRivalryIntensity() * 0.8f;
        }
        var emotions = hunterTrait.getEmotionState();
        relationshipModifier += emotions.getIntensity(com.wassuples.demonslayer.nemesis.trait.EmotionType.HATE_PLAYER) * 0.5f;
        relationshipModifier += ReputationService.getDemonNotorietyHunterRelationshipBonus(player);

        // 5. Difficulty
        float difficultyModifier = switch (player.level.getDifficulty()) {
            case PEACEFUL -> 0.0f;
            case EASY -> 0.75f;
            case NORMAL -> 1.0f;
            case HARD -> 1.3f;
            default -> 1.0f;
        };

        float finalChance = baseChance * personalityModifier * powerDifferenceModifier * relationshipModifier * difficultyModifier;
        finalChance = Math.min(0.95f, finalChance);

        if (random.nextFloat() < finalChance) {
            if (((ServerLevel)player.level).getEntity(hunterId) == null) {
                System.out.println("[NEMESIS DEBUG] HUNTER SPAWN: " + hunterTrait.getName() + " dispatched to hunt " +
                    player.getGameProfile().getName() + " (chance=" + String.format("%.2f%%", finalChance * 100) +
                    ", personality=" + hunterTrait.getPersonality() + ")");
                boolean spawned = spawnDemonFromData(hunterId, (ServerLevel) player.level,
                        player.blockPosition().offset(-5 + random.nextInt(10), 0, -5 + random.nextInt(10)), "Hunter Dispatch");
                if (spawned) {
                    tryMobilizeAlliesForFight(hunterId, player.getUUID(), true,
                            (ServerLevel) player.level, player.blockPosition(), 2);
                }

                // Log story event (H1)
                logStoryEvent(StoryEvent.hunterSent(((ServerLevel) player.level).getGameTime(),
                        hunterId, hunterTrait.getName(),
                        player.getUUID(), player.getGameProfile().getName()));
            }
        }
    }

    public boolean spawnDemonFromData(UUID demonId, ServerLevel level, BlockPos pos, String reason) {
        DemonTrait trait = getDemonTrait(demonId);
        if (trait == null) return false;

        if (level.getEntity(demonId) != null) {
            return false;
        }

        NemesisDemonEntity demon = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);

        demon.setUUID(demonId);
        demon.applyTraitData(trait);
        demon.setPos(pos.getX(), pos.getY(), pos.getZ());
        demon.setTarget(level.getNearestPlayer(demon, 100));

        level.addFreshEntity(demon);
        System.out.println("Spawning hunter: " + trait.getName() + " [Reason: " + reason + "]");
        return true;
    }

    // ── World blood battles (J1) ────────────────────────────────────────

    /**
     * Attempts to spawn a demon-vs-demon world battle near an online player (J1).
     * Called periodically from the server tick (every ~5 minutes, with a chance).
     */
    public void trySpawnWorldBattle(ServerLevel level) {
        // Don't spam world battles — max 1 active at a time
        if (!activeWorldBattles.isEmpty()) {
            System.out.println("[NEMESIS DEBUG] World battle skipped: one already active");
            return;
        }

        System.out.println("[NEMESIS DEBUG] Attempting to spawn a world battle...");

        // Find two demons with a rivalry
        UUID demon1Id = null, demon2Id = null;
        float bestRivalry = 0.3f; // minimum threshold

        for (Map.Entry<UUID, DemonTrait> entry : demonDataCache.entrySet()) {
            UUID id = entry.getKey();
            DemonTrait trait = entry.getValue();
            for (Map.Entry<UUID, DemonRelationship> relEntry : trait.getRelationships().entrySet()) {
                UUID otherId = relEntry.getKey();
                if (!demonDataCache.containsKey(otherId)) continue; // other demon must still exist
                if (relEntry.getValue().getRivalryIntensity() > bestRivalry) {
                    bestRivalry = relEntry.getValue().getRivalryIntensity();
                    demon1Id = id;
                    demon2Id = otherId;
                }
            }
        }

        // Fallback: random fodder pair
        if (demon1Id == null && rankedFodder.size() >= 2) {
            demon1Id = rankedFodder.get(random.nextInt(rankedFodder.size()));
            do {
                demon2Id = rankedFodder.get(random.nextInt(rankedFodder.size()));
            } while (demon2Id.equals(demon1Id));
        }

        if (demon1Id == null || demon2Id == null) return;

        // Find a random online player to spawn near
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer nearPlayer = players.get(random.nextInt(players.size()));
        if (nearPlayer.level.getDifficulty() == Difficulty.PEACEFUL) return;

        // Check neither demon is already loaded
        if (level.getEntity(demon1Id) != null || level.getEntity(demon2Id) != null) return;

        // Spawn both demons 40-80 blocks away from the player
        int offset1 = 40 + random.nextInt(40);
        int offset2 = 40 + random.nextInt(40);
        int angle1 = random.nextInt(360);
        int angle2 = angle1 + 180; // opposite side
        BlockPos pos1 = nearPlayer.blockPosition().offset(
                (int)(Math.cos(Math.toRadians(angle1)) * offset1), 0,
                (int)(Math.sin(Math.toRadians(angle1)) * offset1));
        BlockPos pos2 = pos1.offset(
                (int)(Math.cos(Math.toRadians(angle2)) * 10), 0,
                (int)(Math.sin(Math.toRadians(angle2)) * 10));

        DemonTrait trait1 = getDemonTrait(demon1Id);
        DemonTrait trait2 = getDemonTrait(demon2Id);
        if (trait1 == null || trait2 == null) return;

        // Spawn both
        NemesisDemonEntity entity1 = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);
        entity1.setUUID(demon1Id);
        entity1.applyTraitData(trait1);
        entity1.setPos(pos1.getX(), pos1.getY(), pos1.getZ());

        NemesisDemonEntity entity2 = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);
        entity2.setUUID(demon2Id);
        entity2.applyTraitData(trait2);
        entity2.setPos(pos2.getX(), pos2.getY(), pos2.getZ());

        // Target each other instead of the player
        entity1.setTarget(entity2);
        entity2.setTarget(entity1);

        level.addFreshEntity(entity1);
        level.addFreshEntity(entity2);

        tryMobilizeAlliesForFight(demon1Id, demon2Id, false, level, pos1, 2);
        tryMobilizeAlliesForFight(demon2Id, demon1Id, false, level, pos2, 2);

        activeWorldBattles.put(demon1Id, demon2Id);

        // Log story event
        logStoryEvent(StoryEvent.worldBattle(level.getGameTime(), demon1Id, demon2Id,
                trait1.getName(), trait2.getName()));

        // Announce to nearby players
        broadcastToPlayers(level, Component.literal(String.format(
                "§5Two demons, %s and %s, have begun a blood battle nearby!",
                trait1.getName(), trait2.getName())).withStyle(ChatFormatting.DARK_PURPLE));

        System.out.printf("[Nemesis System] World battle spawned: %s vs %s%n",
                trait1.getName(), trait2.getName());
    }

    /**
     * Handles the conclusion of a demon-vs-demon world battle (J1).
     * Called from NemesisSystemEvents when a demon in an active world battle dies.
     */
    public void handleWorldBattleConclusion(UUID deadDemonId, UUID killerDemonId, ServerLevel level) {
        System.out.println("[NEMESIS DEBUG] ═══ WORLD BATTLE CONCLUDED ═══");
        // Verify this is a world battle
        UUID partner = activeWorldBattles.remove(deadDemonId);
        if (partner == null) {
            // Check the reverse mapping
            for (Map.Entry<UUID, UUID> entry : activeWorldBattles.entrySet()) {
                if (entry.getValue().equals(deadDemonId)) {
                    partner = entry.getKey();
                    activeWorldBattles.remove(partner);
                    break;
                }
            }
        }
        if (partner == null) return; // not a world battle

        DemonTrait winnerTrait = getDemonTrait(killerDemonId);
        DemonTrait loserTrait = getDemonTrait(deadDemonId);
        long time = level.getGameTime();

        // Write memories + growth (same as offscreen battles — F3)
        if (winnerTrait != null && loserTrait != null) {
            winnerTrait.addMemory(DemonMemory.of(MemoryType.WON_EASILY, time, deadDemonId, killerDemonId));
            loserTrait.addMemory(DemonMemory.of(MemoryType.LOST_BADLY, time, killerDemonId, deadDemonId));
            winnerTrait.setLastBattleTime(time);
            loserTrait.setLastBattleTime(time);
            winnerTrait.getGrowthStats().recordBattle();
            winnerTrait.getGrowthStats().recordWin();
            winnerTrait.getGrowthStats().incrementDemonsKilled();
            loserTrait.getGrowthStats().recordBattle();
            loserTrait.getGrowthStats().recordLoss();
            updateRelationshipAfterBattle(winnerTrait, killerDemonId, deadDemonId, false, time);
            updateRelationshipAfterBattle(loserTrait, deadDemonId, killerDemonId, false, time);

            // BP transfer
            int pointsGained = Math.max(10, loserTrait.getBloodPoints() / 10);
            winnerTrait.addBloodPoints(pointsGained);

            logStoryEvent(StoryEvent.demonKilled(time, deadDemonId, loserTrait.getName(),
                    killerDemonId, winnerTrait.getName()));

            broadcastToPlayers(level, Component.literal(String.format(
                    "§5The world battle concludes! %s has defeated %s!",
                    winnerTrait.getName(), loserTrait.getName())).withStyle(ChatFormatting.DARK_PURPLE));
        }

        // Give CONFIRMED intel to all nearby players
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            // Players within 100 blocks of either combatant gain FULL intel
            if (!worldStoryLog.isEmpty()) {
                StoryEvent latestEvent = worldStoryLog.get(0);
                double distance = player.blockPosition().distSqr(
                        new BlockPos(player.getX(), player.getY(), player.getZ()));
                // Since we don't have the exact positions, give CONFIRMED to all online
                playerIntelManager.setIntel(player.getUUID(), latestEvent.getTimestamp(),
                        PlayerIntelManager.IntelTier.CONFIRMED);
            }
        }

        // Get the loser's Kizuki rank BEFORE killing them (killDemon removes from kizukiSlots)
        Optional<DemonRank> loserRank = getKizukiRank(deadDemonId);

        // Kill the loser demon
        killDemon(deadDemonId);
        // Handle power vacuum if the loser was Kizuki
        loserRank.ifPresent(this::handlePowerVacuum);

        setDirty();
    }

    /**
     * Checks if a demon is part of an active world battle (J1).
     */
    public boolean isInWorldBattle(UUID demonId) {
        return activeWorldBattles.containsKey(demonId) || activeWorldBattles.containsValue(demonId);
    }

    public void initialPopulation(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        System.out.println("[NEMESIS DEBUG] ═══ INITIAL POPULATION: First world load! Populating the demon roster... ═══");
        BlueprintLoader.loadBlueprints();

        long gameTime = level.getGameTime();

        if (level.getGameRules().getBoolean(ModGameRules.USE_CANON_KIZUKI)) {
            System.out.println("[NEMESIS DEBUG] Using canonical Kizuki blueprints...");
            for (DemonRank rank : DemonRank.values()) {
                if (rank.isMoonRank()) {
                    DemonBlueprint bp = BlueprintLoader.getBlueprint(rank.getBlueprintId());
                    if (bp != null) {
                        UUID demonUUID = UUID.randomUUID();
                        DemonTrait trait = new DemonTrait();
                        DemonGenerator.initializeBlueprintTrait(trait, bp, gameTime);

                        if (bp.bloodPoints() > 0) {
                            trait.addBloodPoints(bp.bloodPoints());
                        }
                        if (bp.taunts() != null) {
                            trait.setBlueprintTaunts(bp.taunts());
                        }
                        if (bp.announcements() != null) {
                            trait.setBlueprintAnnouncements(bp.announcements());
                        }

                        this.addDemon(demonUUID, trait);
                        this.setKizukiRank(rank, demonUUID);
                        System.out.println("[NEMESIS DEBUG] Populated " + rank.getDisplayName().getString() + " with " + trait.getName() + " " + trait.getTitle() +
                            " (BP:" + trait.getBloodPoints() + ", V:" + trait.getVigor() + "/S:" + trait.getStrength() + "/A:" + trait.getAgility() + ")");
                    } else {
                        System.out.println("[NEMESIS DEBUG] WARNING: No blueprint found for " + rank.getBlueprintId() + ", generating procedural demon.");
                        UUID demonUUID = UUID.randomUUID();
                        DemonTrait trait = new DemonTrait();
                        DemonGenerator.initializeProceduralTrait(trait, gameTime);
                        ensureKizukiMinimumBp(trait, rank);
                        this.addDemon(demonUUID, trait);
                        this.setKizukiRank(rank, demonUUID);
                    }
                }
            }
        } else {
            System.out.println("[NEMESIS DEBUG] Generating random Kizuki (canonical blueprints disabled)...");
            for (DemonRank rank : DemonRank.values()) {
                if (rank.isMoonRank()) {
                    UUID demonUUID = UUID.randomUUID();
                    DemonTrait trait = new DemonTrait();
                    DemonGenerator.initializeProceduralTrait(trait, gameTime);
                    ensureKizukiMinimumBp(trait, rank);
                    this.addDemon(demonUUID, trait);
                    this.setKizukiRank(rank, demonUUID);
                }
            }
        }

        System.out.println("[NEMESIS DEBUG] Filling fodder roster...");
        refillDemonRoster(level);
        System.out.println("[NEMESIS DEBUG] ═══ INITIAL POPULATION COMPLETE: " + kizukiSlots.size() + " Kizuki, " + rankedFodder.size() + " Fodder, " + demonDataCache.size() + " total demons ═══");
        setDirty();
    }

    private void ensureKizukiMinimumBp(DemonTrait trait, DemonRank rank) {
        if (trait == null || rank == null || !rank.isMoonRank()) {
            return;
        }

        int requiredBp = getOrganicPromotionBpThreshold(rank);
        if (trait.getBloodPoints() < requiredBp) {
            int variation = random.nextInt(51); // 0-50 variation
            trait.setBloodPoints(requiredBp + variation);
        }
    }

    public boolean isKizuki(UUID uuid) {
        return kizukiSlots.containsValue(uuid);
    }

    public static DemonHierarchyManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        DemonHierarchyManager manager = storage.computeIfAbsent(nbt -> DemonHierarchyManager.load(nbt, level),
                () -> new DemonHierarchyManager(level), "demon_hierarchy");

        // Lazy init on first access, and only if we haven't been initialized yet.
        if (!manager.initialized) {
            manager.initialPopulation(level);
        }

        return manager;
    }

    @Override public @NotNull CompoundTag save(CompoundTag nbt) {
        // Save Kizuki
        CompoundTag kizukiTag = new CompoundTag();
        kizukiSlots.forEach((rank, uuid) -> kizukiTag.putUUID(rank.name(), uuid));
        nbt.put("kizukiSlots", kizukiTag);

        // Save Fodder
        ListTag fodderTag = new ListTag();
        rankedFodder.forEach(uuid -> fodderTag.add(StringTag.valueOf(uuid.toString())));
        nbt.put("rankedFodder", fodderTag);

        // Save Cache
        CompoundTag cacheTag = new CompoundTag();
        demonDataCache.forEach((uuid, trait) -> cacheTag.put(uuid.toString(), trait.serializeNBT()));
        nbt.put("demonDataCache", cacheTag);

        CompoundTag packsTag = new CompoundTag();
        demonPacks.forEach((leader, members) -> {
            ListTag membersTag = new ListTag();
            members.stream()
                    .filter(demonDataCache::containsKey)
                    .forEach(member -> membersTag.add(StringTag.valueOf(member.toString())));
            if (!membersTag.isEmpty()) {
                packsTag.put(leader.toString(), membersTag);
            }
        });
        nbt.put("demonPacks", packsTag);

        nbt.putInt("worldDayCounter", this.worldDayCounter);
        nbt.putBoolean("initialized", this.initialized);

        // Story log (H1)
        ListTag storyTag = new ListTag();
        for (StoryEvent event : worldStoryLog) {
            storyTag.add(event.toNBT());
        }
        nbt.put("worldStoryLog", storyTag);

        // Player intel (H2)
        nbt.put("playerIntel", playerIntelManager.serializeNBT());

        return nbt;
    }

    public static DemonHierarchyManager load(CompoundTag nbt, ServerLevel level) {
        DemonHierarchyManager manager = new DemonHierarchyManager();

        manager.worldDayCounter = nbt.getInt("worldDayCounter");
        manager.initialized = nbt.getBoolean("initialized");

        // Load Kizuki
        CompoundTag kizukiTag = nbt.getCompound("kizukiSlots");
        for (String key : kizukiTag.getAllKeys()) {
            DemonRank rank = DemonRank.valueOf(key);
            manager.kizukiSlots.put(rank, kizukiTag.getUUID(key));
        }

        // Load Fodder
        ListTag fodderTag = nbt.getList("rankedFodder", 8); // 8 = String
        fodderTag.forEach(tag -> manager.rankedFodder.add(UUID.fromString(tag.getAsString())));

        // Load Cache
        CompoundTag cacheTag = nbt.getCompound("demonDataCache");
        for (String key : cacheTag.getAllKeys()) {
            DemonTrait trait = new DemonTrait();
            trait.deserializeNBT(cacheTag.getCompound(key));
            manager.demonDataCache.put(UUID.fromString(key), trait);
        }

        if (nbt.contains("demonPacks", 10)) {
            CompoundTag packsTag = nbt.getCompound("demonPacks");
            for (String key : packsTag.getAllKeys()) {
                try {
                    UUID leader = UUID.fromString(key);
                    ListTag membersTag = packsTag.getList(key, 8);
                    Set<UUID> members = new HashSet<>();
                    for (int i = 0; i < membersTag.size(); i++) {
                        try {
                            UUID memberId = UUID.fromString(membersTag.getString(i));
                            if (manager.demonDataCache.containsKey(memberId)) {
                                members.add(memberId);
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (members.contains(leader) && members.size() >= 2) {
                        manager.demonPacks.put(leader, members);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load story log (H1)
        if (nbt.contains("worldStoryLog")) {
            ListTag storyTag = nbt.getList("worldStoryLog", 10); // 10 = CompoundTag
            for (int i = 0; i < storyTag.size() && i < MAX_STORY_LOG; i++) {
                try {
                    manager.worldStoryLog.add(StoryEvent.fromNBT(storyTag.getCompound(i)));
                } catch (Exception ignored) {}
            }
        }

        // Load player intel (H2)
        if (nbt.contains("playerIntel")) {
            manager.playerIntelManager.deserializeNBT(nbt.getCompound("playerIntel"));
        }

        // No side effects here; population happens lazily in get().
        return manager;
    }
}

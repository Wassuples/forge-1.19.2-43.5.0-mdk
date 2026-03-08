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
import com.wassuples.demonslayer.nemesis.blueprint.BlueprintLoader;
import com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
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

public class DemonHierarchyManager extends SavedData {
    private final Map<DemonRank, UUID> kizukiSlots = new EnumMap<>(DemonRank.class);
    private final List<UUID> rankedFodder = new ArrayList<>();
    private final Map<UUID, DemonTrait> demonDataCache = new HashMap<>();
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

    public DemonHierarchyManager() {}

    /**
     * IMPORTANT: Do not do worldgen/population work in the constructor.
     * SavedData can be constructed in scenarios where the backing NBT isn't yet loaded,
     * or the instance is created temporarily. Initialization is handled lazily in get().
     */
    public DemonHierarchyManager(ServerLevel level) {
        // Intentionally empty.
    }

    public Collection<DemonTrait> getAllDemonTraits() {
        // Return an unmodifiable view to prevent external mutation of the internal cache
        return Collections.unmodifiableCollection(demonDataCache.values());
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
        System.out.println("[Nemesis System] Running daily simulation for Day " + worldDayCounter);

        // Fodder vs Fodder. Always happens
        simulateFodderVsFodder(level);

        // Challenger vs Kizuki. Every 3-4 days
        if (worldDayCounter > 0 && worldDayCounter % 3 == 0) { // every 3 days
            simulateChallengerVsKizuki(level);
        }

        // Kizuki vs Kizuki. Every 9-10 days, and less likely
        if (worldDayCounter > 0 && worldDayCounter % 9 == 0 && random.nextFloat() < 0.5f) { // every 9 days, 50% chance
            simulateKizukiVsKizuki(level);
        }

        refillDemonRoster(level);
        setDirty();
    }

    private BattleResult simulateBattle(UUID aggressorId, UUID defenderId, ServerLevel level) {
        DemonTrait aggressor = getDemonTrait(aggressorId);
        DemonTrait defender = getDemonTrait(defenderId);

        // Intel-gaining
        IntelLevel intel = getIntelLevel(aggressor);

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

    private IntelLevel getIntelLevel(DemonTrait aggressor) {
        float roll = random.nextFloat();
        float fullIntelChance = 0.05f; // 5% chance for full intel by default

        switch (aggressor.getBehavior()) {
            case STRATEGIST, CALM ->
                // These demons are natural planners and will almost always gather intel.
                    fullIntelChance += 0.65f; // +65% (Total: 75%)
            case AMBITIOUS, OVERLORD, SKIRMISHER ->
                // These demons understand the value of knowing their enemy to achieve their goals.
                    fullIntelChance += 0.30f; // +30% (Total: 40%)
            case DUELIST, STALWART ->
                // These demons are more focused on their own strength but will do some homework.
                    fullIntelChance += 0.15f; // +15% (Total: 25%)
            case AGGRESSIVE ->
                // They might glance at a dossier, but they're eager to fight.
                    fullIntelChance -= 0.05f; // -5% (Total: 5%)
            case BERSERKER, COWARDLY ->
                // Berserkers are too enraged to plan, and Cowards are too scared to scout.
                    fullIntelChance = 0.01f; // Set to 1%
        }

        // These traits represent individual quirks that fine-tune their diligence.
        switch (aggressor.getPersonality()) {
            // Very Diligent Personalities
            case CALCULATING, OBSERVANT, PATIENT, CAUTIOUS, PARANOID ->
                    fullIntelChance += 0.20f; // +20%

            // Moderately Diligent Personalities
            case MANIPULATIVE, OPPORTUNISTIC, PROTECTIVE ->
                    fullIntelChance += 0.10f; // +10%

            // Reckless or Apathetic Personalities
            case COCKY, ARROGANT, RUTHLESS, MOCKING ->
                    fullIntelChance -= 0.15f; // -15%
            case FRENZIED, BLOODTHIRSTY, GLUTTONOUS ->
                // These primal demons act on instinct, not information.
                    fullIntelChance -= 0.25f; // -25%
            case LAZY, WEARY ->
                // Too unmotivated to bother with research.
                    fullIntelChance -= 0.20f; // -20%
        }

        // clamp intel
        fullIntelChance = Math.max(0.01f, Math.min(0.95f, fullIntelChance));
        // roll for partial, full, or no intel.
        if (roll < fullIntelChance) return IntelLevel.FULL;
        if (roll < fullIntelChance + ((1.0f - fullIntelChance) * 0.5f)) return IntelLevel.PARTIAL;
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
        float health = (demon.getVigor() * 10) + (demon.getBloodPoints() / 2f);
        float damage = demon.getStrength() * 5f;
        float agility = demon.getAgility();

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

        // Offensive Strengths
        if (demon.hasQuirk(Quirk.HEAVY_HITTER)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.SHOCKWAVE_STOMP)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.VENOMOUS)) damage *= 1.1f;
        if (demon.hasQuirk(Quirk.EXECUTIONER)) damage *= 1.05f;

        // Defensive Strengths
        if (demon.hasQuirk(Quirk.BLOOD_SIPHON)) health *= 1.05f;
        if (demon.hasQuirk(Quirk.DEATH_DEFIANT)) health *= 1.2f;
        if (demon.hasQuirk(Quirk.TENACIOUS) && demon.getBloodPoints() < opponent.getBloodPoints()) {
            health *= 1.2f; // Underdog bonus
        }

        // Mobility Strengths
        if (demon.hasQuirk(Quirk.NIMBLE)) agility *= 1.1f;

        // Miscellaneous Strengths
        if (demon.hasQuirk(Quirk.PACK_CALLER)) { // Having allies is a major buff, but also gives Dishonored for blood battles.
            health *= 1.1f;
            damage *= 1.15f;
        }
        if (demon.hasQuirk(Quirk.NIGHT_STALKER) && isNight) {
            agility *= 1.25f;
            damage *= 1.1f;
        }

        // Combat Weaknesses
        if (demon.hasQuirk(Quirk.BRITTLE) && opponentTags.contains("PHYSICAL_BLUNT")) {
            health *= 0.85f;
        }
        if (demon.hasQuirk(Quirk.OVERCONFIDENT)) damage *= 0.95f;
        if (demon.hasQuirk(Quirk.GLASS_CANNON)) {
            damage *= 1.25f;
            health *= 0.7f;
        }
        if (demon.hasQuirk(Quirk.BLIND_RAGE) && demon.getBloodPoints() < opponent.getBloodPoints()) {
            damage *= 1.1f;
            health *= 0.9f;
        }

        // Environmental Weaknesses
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

    private void simulateFodderVsFodder(ServerLevel level) {
        if (rankedFodder.size() < 2) return;

        UUID fighter1Id = rankedFodder.get(random.nextInt(rankedFodder.size()));
        UUID fighter2Id;
        do {
            fighter2Id = rankedFodder.get(random.nextInt(rankedFodder.size()));
        } while (fighter1Id.equals(fighter2Id));

        // May make this less random later, but for now, fuck me
        UUID aggressorId = random.nextBoolean() ? fighter1Id : fighter2Id;
        UUID defenderId = aggressorId.equals(fighter1Id) ? fighter2Id : fighter1Id;

        BattleResult result = simulateBattle(aggressorId, defenderId, level);

        DemonTrait winner = getDemonTrait(result.winnerId());
        DemonTrait loser = getDemonTrait(result.loserId());
        if (winner == null || loser == null) return;

        int pointsGained = Math.max(10, loser.getBloodPoints() / 10);
        winner.addBloodPoints(pointsGained);
        killDemon(result.loserId());

        String announcement = SimulationAnnouncer.forFodderDuel(winner, loser, result.winnerTTK(), result.loserTTK());
        broadcastToPlayers(level, Component.literal(String.format(announcement, winner.getName(), loser.getName())).withStyle(ChatFormatting.GRAY));
    }

    private void simulateChallengerVsKizuki(ServerLevel level) {
        if (rankedFodder.isEmpty() || !kizukiSlots.containsKey(DemonRank.LOWER_MOON_6)) return;

        rankedFodder.sort(Comparator.comparingInt(uuid -> -getDemonTrait(uuid).getBloodPoints()));
        UUID challengerId = rankedFodder.get(0);
        UUID defenderId = kizukiSlots.get(DemonRank.LOWER_MOON_6);

        BattleResult result = simulateBattle(challengerId, defenderId, level);

        if (result.winnerId().equals(challengerId)) {
            // Huzzah! A victory
            DemonTrait challengerTrait = getDemonTrait(challengerId);
            DemonTrait lm6Trait = getDemonTrait(defenderId);
            if (challengerTrait == null || lm6Trait == null) return;

            killDemon(defenderId);
            rankedFodder.remove(challengerId);
            kizukiSlots.put(DemonRank.LOWER_MOON_6, challengerId);

            String announcement = SimulationAnnouncer.forChallengerUpset(challengerTrait, lm6Trait);
            broadcastToPlayers(level, Component.literal(String.format(announcement,
                    challengerTrait.getName() + " " + challengerTrait.getTitle(),
                    lm6Trait.getName())).withStyle(ChatFormatting.RED));
        }
    }

    private void simulateKizukiVsKizuki(ServerLevel level) {
        List<DemonRank> lowerMoons = Arrays.stream(DemonRank.values())
                .filter(r -> r.ordinal() >= DemonRank.LOWER_MOON_6.ordinal() && r.ordinal() < DemonRank.LOWER_MOON_1.ordinal())
                .toList();

        if (lowerMoons.isEmpty()) return;

        DemonRank challengerRank = lowerMoons.get(random.nextInt(lowerMoons.size()));
        DemonRank defenderRank = DemonRank.values()[challengerRank.ordinal() - 1];

        UUID challengerId = kizukiSlots.get(challengerRank);
        UUID defenderId = kizukiSlots.get(defenderRank);

        if (challengerId == null || defenderId == null) return;

        BattleResult result = simulateBattle(challengerId, defenderId, level);

        if (result.winnerId().equals(challengerId)) {
            // SHOCKING UPSET
            DemonTrait challengerTrait = getDemonTrait(challengerId);
            DemonTrait defenderTrait = getDemonTrait(defenderId);
            if (challengerTrait == null || defenderTrait == null) return;

            kizukiSlots.put(defenderRank, challengerId);
            kizukiSlots.remove(challengerRank);
            killDemon(defenderId);
            handlePowerVacuum(challengerRank);

            String announcement = SimulationAnnouncer.forKizukiUpset(challengerTrait, defenderTrait, defenderRank);
            broadcastToPlayers(level, Component.literal(String.format(announcement,
                    challengerTrait.getName(),
                    defenderTrait.getName(),
                    defenderRank.getDisplayName().getString())).withStyle(ChatFormatting.DARK_PURPLE));
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
        if (needed <= 0) return;

        int toGenerate = Math.min(needed, maxToGenerateThisCall);
        for (int i = 0; i < toGenerate; i++) {
            System.out.println("[Nemesis System] Generating a procedural demon to fill roster.");

            NemesisDemonEntity demon = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);
            DemonGenerator.initializeProcedural(demon, level);

            // Important: we only register it into SavedData; we are NOT spawning it into the world here.
            demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> this.addDemon(demon.getUUID(), (DemonTrait) trait));
        }
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
            System.out.println("Blood Battle initiated between Challenger " + challenger + " and Defender " + defender);
            setDirty();
        }
    }

    public void recordInterference(UUID defenderUUID, UUID attackerUUID) {
        if (this.activeBattle != null && this.activeBattle.defender.equals(defenderUUID)) {
            // Only record interference if: the attacker != original challenger
            if (!this.activeBattle.challenger.equals(attackerUUID)) {
                this.activeBattle.interferers.add(attackerUUID);
                System.out.println("Interference recorded in blood battle by " + attackerUUID);
                setDirty();
            }
        }
    }

    @Nullable public BloodBattle getActiveBattle() {
        return this.activeBattle;
    }

    public void endBloodBattle() {
        this.activeBattle = null;
        System.out.println("Blood Battle has concluded.");
        setDirty();
    }

    private String getTauntForSituation(DemonTrait trait, String key, String playerName) {
        // demon-specific blueprint taunts
        String taunt = getRandomElement(trait.getBlueprintTaunts().get(key));
        if (taunt != null) return taunt.replace("{player}", playerName);

        // personality-specific blueprint taunts
        Optional<DemonBlueprint> personalityBp = BlueprintLoader.getPersonalityDefaults(trait.getPersonality());
        if (personalityBp.isPresent() && personalityBp.get().taunts() != null) {
            taunt = getRandomElement(personalityBp.get().taunts().get(key));
            if (taunt != null) return taunt.replace("{player}", playerName);
        }

        // global defaults blueprint taunts
        Optional<DemonBlueprint> globalBp = BlueprintLoader.getGlobalDefaults();
        if (globalBp.isPresent() && globalBp.get().taunts() != null) {
            taunt = getRandomElement(globalBp.get().taunts().get(key));
            if (taunt != null) return taunt.replace("{player}", playerName);
        }

        // Ultimate fallback
        return "You have fallen.";
    }

    public void demonKillsPlayer(NemesisDemonEntity demon, Player player, DamageSource source, float amount) {
        UUID demonUUID = demon.getUUID();
        DemonTrait trait = demonDataCache.get(demonUUID);
        if (trait == null) return;


        int playerBloodPoints = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(PlayerDemon::getBloodPoints)
                .orElse(0);
        int demonBloodPoints = trait.getBloodPoints();

        if (demonBloodPoints < playerBloodPoints * 0.5f) { // DemonBP < 50% PlayerBP
            trait.addBloodPoints(100); // Underdog
        } else if (demonBloodPoints < playerBloodPoints * 0.8f) { // DemonBP < 80% PlayerBP
            trait.addBloodPoints(85); // Underdog, but not as much
        } else { // DemonBP >= 80% PlayerBP
            trait.addBloodPoints(60); // Not underdog
        }

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
        trait.addPlayerTaunt(player.getUUID(), taunt);

        if (hasEvolved) {
            String evolutionMsg = String.format("§cThrough bloodshed, %s has evolved, now known as %s, %s!",
                    trait.getName(), trait.getName(), trait.getTitle());
            broadcastToPlayers((ServerLevel) demon.level, Component.literal(evolutionMsg));
        }

        System.out.println("[Nemesis System] " + trait.getName() + " has killed a player and grown stronger.");
        setDirty();
    }

    public void killDemon(UUID demonUUID) {
        if (demonDataCache.containsKey(demonUUID)) {
            rankedFodder.remove(demonUUID);
            demonDataCache.remove(demonUUID);

            kizukiSlots.entrySet().removeIf(entry -> entry.getValue().equals(demonUUID));

            System.out.println("A Nemesis Demon has been slain permanently.");
            setDirty();
        }
    }

    public void setKizukiRank(DemonRank rank, UUID demonId) {
        kizukiSlots.entrySet().removeIf(entry -> entry.getValue().equals(demonId));
        kizukiSlots.put(rank, demonId);
        rankedFodder.remove(demonId);
        setDirty();
    }

    public void handlePowerVacuum(DemonRank slainKizukiRank) {
        DemonRank currentRank = slainKizukiRank;
        while (currentRank != null && currentRank.ordinal() >= DemonRank.LOWER_MOON_6.ordinal()) {
            DemonRank nextLowerRank = DemonRank.values().length > currentRank.ordinal() + 1 ? DemonRank.values()[currentRank.ordinal() + 1] : null;

            if (nextLowerRank != null && kizukiSlots.containsKey(nextLowerRank)) {
                UUID promoterUUID = kizukiSlots.get(nextLowerRank);
                kizukiSlots.put(currentRank, promoterUUID);
            } else {
                Optional<DemonTrait> topFodderTrait = rankedFodder.stream()
                        .map(this::getDemonTrait)
                        .filter(Objects::nonNull)
                        .max(Comparator.comparingInt(DemonTrait::getBloodPoints));

                if (topFodderTrait.isPresent()) {
                    UUID topFodderUUID = demonDataCache.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(topFodderTrait.get()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);

                    if (topFodderUUID != null) {
                        rankedFodder.remove(topFodderUUID);
                        kizukiSlots.put(DemonRank.LOWER_MOON_6, topFodderUUID);
                    }
                }
                break;
            }
            currentRank = nextLowerRank;
        }
        System.out.println("A power vacuum has been filled in the Kizuki ranks.");
        setDirty();
    }

    @Nullable public DemonTrait getDemonTrait(UUID uuid) {
        return this.demonDataCache.get(uuid);
    }

    public void addDemon(UUID uuid, DemonTrait trait) {
        if (!demonDataCache.containsKey(uuid)) {
            demonDataCache.put(uuid, trait);
            rankedFodder.add(uuid);
            // We don't need to sort here every time, we can sort when needed

            System.out.println("New Nemesis Demon registered: " + trait.getName() + " " + trait.getTitle());
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

    public void trySpawnHunterForPlayer(ServerPlayer player) {
        if (player.level.getDifficulty() == Difficulty.PEACEFUL) return;
        ResourceLocation currentBiome = player.level.getBiome(player.blockPosition()).unwrapKey().get().location();

        List<UUID> potentialHunters = new ArrayList<>();
        for (Map.Entry<UUID, DemonTrait> entry : demonDataCache.entrySet()) {
            // Only consider demons who see this biome as their territory
            if (entry.getValue().getTerritory().equals(currentBiome)) {
                potentialHunters.add(entry.getKey());
            }
        }

        if (potentialHunters.isEmpty()) return;

        UUID hunterId = potentialHunters.get(random.nextInt(potentialHunters.size()));
        DemonTrait hunterTrait = getDemonTrait(hunterId);
        if (hunterTrait == null) return;

        // 1. Base Chance: Based on demon's raw power.
        // The divisor determines how many BP equals a 100% base chance.
        float baseChance = hunterTrait.getBloodPoints() / 5000.0f;

        // 2. Personality Modifier: Does this demon want to hunt?
        // TODO ADD MORE SHIT HERE LATER
        float personalityModifier = 1.0f;
        switch (hunterTrait.getBehavior()) {
            case AGGRESSIVE, BERSERKER, TERRITORIAL -> personalityModifier = 1.5f; // Eager to fight
            case STRATEGIST, AMBITIOUS -> personalityModifier = 1.2f; // Will hunt if it's advantageous
            case COWARDLY, SCAVENGER -> personalityModifier = 0.5f; // Prefers to avoid direct confrontation
        }
        switch (hunterTrait.getPersonality()) {
            case BLOODTHIRSTY, FRENZIED -> personalityModifier *= 1.4f; // Greatly increases desire to hunt
            case CAUTIOUS, PARANOID -> personalityModifier *= 0.7f; // Reduces desire to hunt
        }


        // 3. Power Difference Modifier: How confident is the demon against this specific player?
        int playerBloodPoints = player.getCapability(PlayerDemonProvider.PLAYER_DEMON)
                .map(PlayerDemon::getBloodPoints)
                .orElse(50); // Assume a base value if player is not a demon or has 0 BP

        float powerRatio = (float) hunterTrait.getBloodPoints() / Math.max(1, playerBloodPoints);
        float powerDifferenceModifier = 1.0f;
        if (powerRatio > 1.5) {
            powerDifferenceModifier = 1.25f; // Confident, demon is 50%+ stronger
        } else if (powerRatio < 0.5) {
            powerDifferenceModifier = 0.4f; // Hesitant, demon is less than half the player's power
        }

        // 4. Difficulty
        float difficultyModifier = switch (player.level.getDifficulty()) {
            case PEACEFUL -> 0.0f; // Should never happen due to earlier check, but just in case
            case EASY -> 0.75f;
            case NORMAL -> 1.0f;
            case HARD -> 1.3f;
            default -> 1.0f;
        };

        float finalChance = baseChance * personalityModifier * powerDifferenceModifier * difficultyModifier;
        finalChance = Math.min(0.95f, finalChance);

        if (random.nextFloat() < finalChance) {
            // Check if the demon isn't already loaded in the world
            if (((ServerLevel)player.level).getEntity(hunterId) == null) {
                System.out.printf("[Nemesis System] Hunter %s is attempting to spawn. Final Chance: %.2f%%%n", hunterTrait.getName(), finalChance * 100);
                spawnDemonFromData(hunterId, (ServerLevel) player.level, player.blockPosition().offset(-5 + random.nextInt(10), 0, -5 + random.nextInt(10)));
            }
        }
    }

    public void spawnDemonFromData(UUID demonId, ServerLevel level, BlockPos pos) {
        DemonTrait trait = getDemonTrait(demonId);
        if (trait == null) return;

        NemesisDemonEntity demon = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);

        demon.setUUID(demonId);
        demon.applyTraitData(trait);
        demon.setPos(pos.getX(), pos.getY(), pos.getZ());

        demon.setTarget(level.getNearestPlayer(demon, 100));

        level.addFreshEntity(demon);
        System.out.println("Spawning hunter: " + trait.getName());
    }

    public void initialPopulation(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        System.out.println("[Nemesis System] First world load! Populating the demon roster...");
        BlueprintLoader.loadBlueprints();

        if (level.getGameRules().getBoolean(ModGameRules.USE_CANON_KIZUKI)) {
            System.out.println("[Nemesis] Using canonical Kizuki blueprints...");
            for (DemonRank rank : DemonRank.values()) {
                if (rank.isMoonRank()) {
                    DemonBlueprint bp = BlueprintLoader.getBlueprint(rank.getBlueprintId());
                    if (bp != null) {
                        NemesisDemonEntity demon = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);
                        DemonGenerator.initializeBlueprint(demon, bp, level);
                        demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
                            if (bp.bloodPoints() > 0) {
                                trait.addBloodPoints(bp.bloodPoints());
                            }
                            if (bp.taunts() != null) {
                                trait.setBlueprintTaunts(bp.taunts());
                            }
                            if (bp.announcements() != null) {
                                trait.setBlueprintAnnouncements(bp.announcements());
                            }
                            this.addDemon(demon.getUUID(), (DemonTrait) trait);
                            this.setKizukiRank(rank, demon.getUUID());
                        });
                    }
                }
            }
        } else {
            System.out.println("[Nemesis System] Generating random Kizuki...");
            for (DemonRank rank : DemonRank.values()) {
                if (rank.isMoonRank()) {
                    NemesisDemonEntity demon = new NemesisDemonEntity(ModEntityTypes.NEMESIS_DEMON.get(), level);
                    DemonGenerator.initializeProcedural(demon, level);
                    demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
                        this.addDemon(demon.getUUID(), (DemonTrait) trait);
                        this.setKizukiRank(rank, demon.getUUID());
                    });
                }
            }
        }

        refillDemonRoster(level);
        setDirty();
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

        nbt.putInt("worldDayCounter", this.worldDayCounter);
        nbt.putBoolean("initialized", this.initialized);

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

        // No side effects here; population happens lazily in get().
        return manager;
    }
}

package com.wassuples.demonslayer.demon.ai;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.nemesis.blueprint.NemesisSkinRegistry;
import com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.IDemonTrait;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Demon AI: Demon Generator.
 */
public class DemonGenerator {

    private static final Random random = new Random();

    private static final List<ResourceLocation> TERRITORIES = List.of(
            Biomes.PLAINS.location(), Biomes.FOREST.location(), Biomes.TAIGA.location(),
            Biomes.SWAMP.location(), Biomes.SAVANNA.location(), Biomes.SNOWY_SLOPES.location(),
            Biomes.DESERT.location(), Biomes.JUNGLE.location(), Biomes.MUSHROOM_FIELDS.location()
    );
    private static final Map<Behavior, List<Personality>> PERSONALITY_MAP = Map.ofEntries(
            Map.entry(Behavior.AGGRESSIVE, List.of(Personality.RUTHLESS, Personality.COCKY)),
            Map.entry(Behavior.BERSERKER, List.of(Personality.FRENZIED, Personality.BLOODTHIRSTY)),
            Map.entry(Behavior.AMBITIOUS, List.of(Personality.ARROGANT, Personality.CALCULATING)),
            Map.entry(Behavior.DUELIST, List.of(Personality.HONOR_BOUND, Personality.COCKY)),
            Map.entry(Behavior.STALWART, List.of(Personality.STOIC, Personality.PATIENT)),
            Map.entry(Behavior.BODYGUARD, List.of(Personality.PROTECTIVE, Personality.DEVOTED)),
            Map.entry(Behavior.TERRITORIAL, List.of(Personality.PARANOID, Personality.COMMANDING)),
            Map.entry(Behavior.CALM, List.of(Personality.OBSERVANT, Personality.CAUTIOUS)),
            Map.entry(Behavior.COWARDLY, List.of(Personality.NERVOUS, Personality.WHINING)),
            Map.entry(Behavior.STRATEGIST, List.of(Personality.CALCULATING, Personality.MANIPULATIVE)),
            Map.entry(Behavior.SKIRMISHER, List.of(Personality.MOCKING, Personality.CAUTIOUS)),
            Map.entry(Behavior.TRICKSTER, List.of(Personality.JESTING, Personality.DECEPTIVE)),
            Map.entry(Behavior.SCAVENGER, List.of(Personality.OPPORTUNISTIC, Personality.DECEPTIVE)),
            Map.entry(Behavior.OVERLORD, List.of(Personality.COMMANDING, Personality.ARROGANT, Personality.LAZY)),
            Map.entry(Behavior.LOYALIST, List.of(Personality.DEVOTED, Personality.FANATICAL)),
            Map.entry(Behavior.SADIST, List.of(Personality.CRUEL, Personality.MELODRAMATIC))
    );
    private static final List<Quirk> PROCEDURAL_STRENGTH_QUIRKS = List.of(
            Quirk.HEAVY_HITTER,
            Quirk.BLOOD_SIPHON,
            Quirk.SHOCKWAVE_STOMP,
            Quirk.VENOMOUS,
            Quirk.EXECUTIONER,
            Quirk.FIRE_RESISTANT,
            Quirk.FROST_BLOODED,
            Quirk.ARROW_PROOF,
            Quirk.TENACIOUS,
            Quirk.UNSHAKABLE,
            Quirk.RAPID_RECOVERY,
            Quirk.DEATH_DEFIANT,
            Quirk.NIMBLE,
            Quirk.NIGHT_STALKER,
            Quirk.KEEN_Scent,
            Quirk.PACK_CALLER
    );
    private static final List<Quirk> PROCEDURAL_WEAKNESS_QUIRKS = List.of(
            Quirk.FEAR_OF_WATER,
            Quirk.BRITTLE,
            Quirk.SUNLIGHT_SICKNESS,
            Quirk.FLAMMABLE_FLESH,
            Quirk.PURIFIED_WEAKNESS,
            Quirk.OVERCONFIDENT,
            Quirk.GLASS_CANNON,
            Quirk.WIND_UP,
            Quirk.BLIND_RAGE,
            Quirk.OBSESSIVE_HUNTER,
            Quirk.SOUND_SENSITIVE,
            Quirk.MAGPIE,
            Quirk.EASILY_TAUNTED
    );

    public static void initializeProcedural(NemesisDemonEntity demon, ServerLevel level) {
        demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            initializeProceduralTrait(trait, level.getGameTime());
        });
    }

    /**
     * Initializes a DemonTrait directly with procedural data, without requiring an entity.
     * This is the preferred method for offline roster population.
     */
    public static void initializeProceduralTrait(IDemonTrait trait, long gameTime) {
        trait.setSourceBlueprintId("procedural");
        trait.setSkinLocation(NemesisSkinRegistry.pickRandomNonCanonSkin().toString());
        trait.setBirthTime(gameTime);

        Behavior chosenBehavior = getRandomElement(List.of(Behavior.values()));
        trait.setBehavior(chosenBehavior);
        trait.setPersonality(generatePersonality(chosenBehavior));
        trait.setName(getRandomElement(DemonIdentityConfigLoader.getNames()));
        trait.setTitle(getRandomElement(DemonIdentityConfigLoader.getTitles()));
        trait.setAssignedBDA(getRandomBda());
        trait.setBloodPoints(5 + random.nextInt(15));
        trait.setTerritory(getRandomElement(TERRITORIES));

        // Procedural demons roll either 2 strengths + 1 weakness, or 1 strength + 2 weaknesses.
        boolean strengthLean = random.nextBoolean();
        int strengthCount = strengthLean ? 2 : 1;
        int weaknessCount = strengthLean ? 1 : 2;
        List<Quirk> strengthQuirks = pickDistinctQuirks(PROCEDURAL_STRENGTH_QUIRKS, strengthCount);
        List<Quirk> weaknessQuirks = pickDistinctQuirks(PROCEDURAL_WEAKNESS_QUIRKS, weaknessCount);
        strengthQuirks.forEach(trait::addQuirk);
        weaknessQuirks.forEach(trait::addQuirk);

        List<Quirk> rolledQuirks = new ArrayList<>(strengthQuirks.size() + weaknessQuirks.size());
        rolledQuirks.addAll(strengthQuirks);
        rolledQuirks.addAll(weaknessQuirks);

        int[] stats = applyGenerationStatModifiers(rolledQuirks, 5, 5, 5);
        trait.setVigor(stats[0]);
        trait.setStrength(stats[1]);
        trait.setAgility(stats[2]);

        // Intel-gathering ability (G4): derived from behavior + personality
        trait.setBaseIntelGathering(calculateBaseIntel(chosenBehavior, trait.getPersonality()));
    }

    public static void initializeBlueprint(NemesisDemonEntity demon, DemonBlueprint blueprint, ServerLevel level) {
        demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            initializeBlueprintTrait(trait, blueprint, level.getGameTime());
        });
    }

    /**
     * Initializes a DemonTrait directly from a blueprint, without requiring an entity.
     * This is the preferred method for offline roster population from canonical blueprints.
     */
    public static void initializeBlueprintTrait(IDemonTrait trait, DemonBlueprint blueprint, long gameTime) {
        trait.setSourceBlueprintId(blueprint.id());
        if (NemesisSkinRegistry.parseSkinLocation(blueprint.skinLocation()) != null) {
            trait.setSkinLocation(NemesisSkinRegistry.parseSkinLocation(blueprint.skinLocation()).toString());
        } else {
            var canonSkin = NemesisSkinRegistry.getCanonicalSkinForBlueprint(blueprint.id());
            trait.setSkinLocation((canonSkin != null ? canonSkin : NemesisSkinRegistry.pickRandomNonCanonSkin()).toString());
        }
        trait.setBirthTime(gameTime);
        trait.setName(blueprint.name());
        trait.setTitle(blueprint.title());
        trait.setBehavior(blueprint.behavior());
        trait.setPersonality(blueprint.personality());
        trait.setAssignedBDA(blueprint.bloodDemonArt());

        if (blueprint.quirks() != null) {
            blueprint.quirks().forEach(trait::addQuirk);
        }

        trait.setVigor(blueprint.stats().vigor);
        trait.setStrength(blueprint.stats().strength);
        trait.setAgility(blueprint.stats().agility);

        trait.setBloodPoints(50 + random.nextInt(100));
        trait.setTerritory(getRandomElement(TERRITORIES));

        trait.setBaseIntelGathering(calculateBaseIntel(blueprint.behavior(), blueprint.personality()));
    }


    private static <T> T getRandomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private static Personality generatePersonality(Behavior behavior) {
        List<Personality> preferred = PERSONALITY_MAP.get(behavior);

        if (preferred != null && !preferred.isEmpty() && random.nextFloat() < 0.75f) {
            return getRandomElement(preferred);
        } else {
            // 25% chance to pick any personality
            return getRandomElement(List.of(Personality.values()));
        }
    }
    private static BloodDemonArts getRandomBda() {
        // We don't want to assign NONE or the super powerful ones initially
        List<BloodDemonArts> availableBdas = List.of(
                BloodDemonArts.THREADS, BloodDemonArts.SASH, BloodDemonArts.HAEMO, BloodDemonArts.SLEEP
        );
        return getRandomElement(availableBdas);
    }

    private static List<Quirk> pickDistinctQuirks(List<Quirk> pool, int count) {
        if (pool.isEmpty() || count <= 0) {
            return List.of();
        }
        List<Quirk> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        return new ArrayList<>(shuffled.subList(0, Math.min(count, shuffled.size())));
    }

    private static int[] applyGenerationStatModifiers(List<Quirk> quirks, int vigor, int strength, int agility) {
        int outVigor = vigor;
        int outStrength = strength;
        int outAgility = agility;

        for (Quirk quirk : quirks) {
            switch (quirk) {
                // Offensive strengths
                case HEAVY_HITTER -> outStrength += 3;
                case BLOOD_SIPHON -> {
                    outVigor += 2;
                    outStrength += 1;
                }
                case SHOCKWAVE_STOMP -> outStrength += 2;
                case VENOMOUS -> {
                    outStrength += 1;
                    outAgility += 1;
                }
                case EXECUTIONER -> outStrength += 2;

                // Defensive strengths
                case FIRE_RESISTANT -> outVigor += 2;
                case FROST_BLOODED -> {
                    outVigor += 1;
                    outAgility += 1;
                }
                case ARROW_PROOF -> outVigor += 2;
                case TENACIOUS -> outVigor += 3;
                case UNSHAKABLE -> outVigor += 2;
                case RAPID_RECOVERY -> {
                    outVigor += 2;
                    outAgility += 1;
                }
                case DEATH_DEFIANT -> outVigor += 4;

                // Mobility / utility strengths
                case NIMBLE -> outAgility += 4;
                case NIGHT_STALKER -> {
                    outAgility += 2;
                    outStrength += 1;
                }
                case KEEN_Scent -> {
                    outStrength += 1;
                    outAgility += 1;
                }
                case PACK_CALLER -> {
                    outVigor += 2;
                    outStrength += 1;
                }

                // Weaknesses
                case FEAR_OF_WATER -> outAgility -= 2;
                case BRITTLE -> outVigor -= 3;
                case SUNLIGHT_SICKNESS -> {
                    outVigor -= 2;
                    outAgility -= 1;
                }
                case FLAMMABLE_FLESH -> outVigor -= 2;
                case PURIFIED_WEAKNESS -> {
                    outVigor -= 1;
                    outStrength -= 1;
                }
                case OVERCONFIDENT -> outAgility -= 2;
                case GLASS_CANNON -> {
                    outStrength += 4;
                    outVigor -= 4;
                }
                case WIND_UP -> {
                    outStrength += 2;
                    outAgility -= 2;
                }
                case BLIND_RAGE -> {
                    outStrength += 1;
                    outVigor -= 1;
                    outAgility -= 2;
                }
                case OBSESSIVE_HUNTER -> outAgility -= 1;
                case SOUND_SENSITIVE -> outAgility -= 2;
                case MAGPIE -> {
                    outStrength -= 1;
                    outAgility -= 1;
                }
                case EASILY_TAUNTED -> {
                    outVigor -= 1;
                    outAgility -= 1;
                }
                case DISHONORED -> {
                    // Not part of procedural pools; ignore if present from external edits.
                }
            }
        }

        return new int[] { outVigor, outStrength, outAgility };
    }

    /**
     * Derives a demon's base intel-gathering ability from its Behavior
     * and Personality. Range [0.05, 0.95]. Mirrors the old random roll
     * logic in DemonHierarchyManager but computes a persistent value.
     */
    public static float calculateBaseIntel(Behavior behavior, Personality personality) {
        float base = 0.20f;
        switch (behavior) {
            case STRATEGIST, CALM        -> base = 0.75f;
            case AMBITIOUS, OVERLORD, SKIRMISHER -> base = 0.45f;
            case DUELIST, STALWART       -> base = 0.30f;
            case AGGRESSIVE              -> base = 0.15f;
            case BERSERKER, COWARDLY     -> base = 0.05f;
            default -> {} // keep 0.20
        }
        switch (personality) {
            case CALCULATING, OBSERVANT, PATIENT, CAUTIOUS, PARANOID -> base += 0.15f;
            case MANIPULATIVE, OPPORTUNISTIC, PROTECTIVE             -> base += 0.08f;
            case COCKY, ARROGANT, RUTHLESS, MOCKING                  -> base -= 0.10f;
            case FRENZIED, BLOODTHIRSTY, GLUTTONOUS                  -> base -= 0.15f;
            case LAZY, WEARY                                         -> base -= 0.12f;
            default -> {} // no modifier
        }
        return Math.max(0.05f, Math.min(0.95f, base));
    }
}

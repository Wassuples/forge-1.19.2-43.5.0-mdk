package com.wassuples.demonslayer.demon.ai;

import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.nemesis.ai.Behavior;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import com.wassuples.demonslayer.nemesis.blueprint.DemonBlueprint;
import com.wassuples.demonslayer.nemesis.trait.DemonTraitProvider;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class DemonGenerator {

    private static final Random random = new Random();

    // TODO load from a config file, and expand
    private static final List<String> NAMES = List.of("Akuma", "Kage", "Oni", "Yami", "Ketsu");
    private static final List<String> TITLES = List.of("the Bloodied", "of the Cursed Peak", "the Shadow", "the Feaster");
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

    public static void initializeProcedural(NemesisDemonEntity demon, ServerLevel level) {
        demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            trait.setSourceBlueprintId("procedural");

            Behavior chosenBehavior = getRandomElement(List.of(Behavior.values()));
            trait.setBehavior(chosenBehavior);
            trait.setPersonality(generatePersonality(chosenBehavior));
            trait.setName(getRandomElement(NAMES));
            trait.setTitle(getRandomElement(TITLES));
            trait.setAssignedBDA(getRandomBda());
            trait.setBloodPoints(5 + random.nextInt(15));
            trait.setTerritory(getRandomElement(TERRITORIES));

            // TODO make more random, maybe 2 strengths and 1 weakness or vice versa
            Quirk strengthQuirk = getRandomStrength();
            Quirk weaknessQuirk = getRandomWeakness();
            trait.addQuirk(strengthQuirk);
            trait.addQuirk(weaknessQuirk);

            int baseVigor = 5, baseStrength = 5, baseAgility = 5;

            if (strengthQuirk == Quirk.NIMBLE) baseAgility += 5;
            if (strengthQuirk == Quirk.TENACIOUS) baseVigor += 5;
            if (strengthQuirk == Quirk.HEAVY_HITTER) baseStrength += 5;
            if (weaknessQuirk == Quirk.BRITTLE) baseVigor -= 2;
            if (weaknessQuirk == Quirk.GLASS_CANNON) {
                baseStrength += 7;
                baseVigor -= 4;
            }

            trait.setVigor(baseVigor);
            trait.setStrength(baseStrength);
            trait.setAgility(baseAgility);
        });
    }

    public static void initializeBlueprint(NemesisDemonEntity demon, DemonBlueprint blueprint, ServerLevel level) {
        demon.getCapability(DemonTraitProvider.DEMON_TRAIT).ifPresent(trait -> {
            trait.setSourceBlueprintId(blueprint.id());
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
        });
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

    private static Quirk getRandomStrength() {
        List<Quirk> strengths = List.of(Quirk.NIMBLE, Quirk.HEAVY_HITTER, Quirk.TENACIOUS, Quirk.FIRE_RESISTANT);
        return getRandomElement(strengths);
    }

    private static Quirk getRandomWeakness() {
        List<Quirk> weaknesses = List.of(Quirk.BRITTLE, Quirk.FEAR_OF_WATER, Quirk.OVERCONFIDENT, Quirk.SUNLIGHT_SICKNESS);
        return getRandomElement(weaknesses);
    }
}
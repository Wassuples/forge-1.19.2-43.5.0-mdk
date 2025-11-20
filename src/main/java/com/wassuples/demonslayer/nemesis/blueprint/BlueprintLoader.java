package com.wassuples.demonslayer.nemesis.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class BlueprintLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    // Specific demon blueprints (e.g., "LM5")
    private static final Map<String, DemonBlueprint> BLUEPRINTS = new HashMap<>();
    // Personality default blueprints (e.g., Personality.ARROGANT)
    private static final Map<Personality, DemonBlueprint> PERSONALITY_DEFAULTS = new EnumMap<>(Personality.class);
    // The single global default blueprint
    private static DemonBlueprint globalDefaults = null;

    private static final Path BLUEPRINT_PATH = FMLPaths.CONFIGDIR.get().resolve("demonslayer/blueprints");

    public static void loadBlueprints() {
        // Clear all caches before loading
        BLUEPRINTS.clear();
        PERSONALITY_DEFAULTS.clear();
        globalDefaults = null;

        File folder = BLUEPRINT_PATH.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
            LOGGER.info("[Nemesis System] Created blueprint directory at: {}", folder.getAbsolutePath());
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            LOGGER.info("[Nemesis System] No demon blueprints found in config folder.");
            return;
        }

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                DemonBlueprint blueprint = GSON.fromJson(reader, DemonBlueprint.class);
                String fileName = file.getName();

                // Check for special blueprint files first
                if (fileName.equalsIgnoreCase("_defaults.json")) {
                    globalDefaults = blueprint;
                    LOGGER.info("[Nemesis System] Loaded global default blueprint.");
                } else if (fileName.toLowerCase().startsWith("personality_")) {
                    String personalityName = fileName.substring("personality_".length(), fileName.length() - ".json".length()).toUpperCase();
                    try {
                        Personality personality = Personality.valueOf(personalityName);
                        PERSONALITY_DEFAULTS.put(personality, blueprint);
                        LOGGER.info("[Nemesis System] Loaded default blueprint for personality: {}", personality);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("[Nemesis System] Found personality file with invalid name: {}. Skipping.", fileName);
                    }
                } else {
                    // It's a regular demon blueprint
                    if (blueprint != null && blueprint.id() != null) {
                        BLUEPRINTS.put(blueprint.id(), blueprint);
                        LOGGER.info("[Nemesis System] Loaded demon blueprint: {}", blueprint.id());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("[Nemesis System] Failed to load demon blueprint: {}", file.getName(), e);
            }
        }
    }

    public static DemonBlueprint getBlueprint(String id) {
        return BLUEPRINTS.get(id);
    }

    public static List<DemonBlueprint> getAllBlueprints() {
        return new ArrayList<>(BLUEPRINTS.values());
    }

    public static Optional<DemonBlueprint> getPersonalityDefaults(Personality personality) {
        return Optional.ofNullable(PERSONALITY_DEFAULTS.get(personality));
    }

    public static Optional<DemonBlueprint> getGlobalDefaults() {
        return Optional.ofNullable(globalDefaults);
    }
}
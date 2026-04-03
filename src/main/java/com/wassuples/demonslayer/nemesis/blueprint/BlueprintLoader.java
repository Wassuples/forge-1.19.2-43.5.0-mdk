package com.wassuples.demonslayer.nemesis.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.wassuples.demonslayer.nemesis.ai.Personality;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads {@link DemonBlueprint} JSON files from the {@code config/demonslayer/} directory.
 *
 * <p>Blueprints define canonical Kizuki demons. The loader reads all {@code .json} files,
 * deserializes them via Gson, and indexes them by blueprint ID for use by
 * {@link com.wassuples.demonslayer.nemesis.DemonHierarchyManager}.
 *
 * @see DemonBlueprint
 */
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
    private static final String DEFAULT_BLUEPRINT_RESOURCE_ROOT = "data/demonslayer/nemesis/blueprints";
    private static final List<String> DEFAULT_BLUEPRINT_FILES = List.of(
            "LM1.json", "LM2.json", "LM3.json", "LM4.json", "LM5.json", "LM6.json",
            "UM1.json", "UM2.json", "UM3.json", "UM4.json", "UM5.json", "UM6.json",
            "personality_arrogant.json", "personality_bloodthirsty.json", "personality_cocky.json",
            "personality_manipulative.json", "personality_nervous.json", "personality_ruthless.json",
            "personality_stoic.json", "_defaults.json"
    );

    public static void loadBlueprints() {
        // Clear all caches before loading
        BLUEPRINTS.clear();
        PERSONALITY_DEFAULTS.clear();
        globalDefaults = null;

        System.out.println("[NEMESIS DEBUG] Loading blueprints from " + BLUEPRINT_PATH.toAbsolutePath());

        File folder = BLUEPRINT_PATH.toFile();
        if (!folder.exists() && !folder.mkdirs()) {
            LOGGER.warn("[Nemesis System] Failed to create blueprint directory: {}", folder.getAbsolutePath());
        }

        seedBlueprintDefaults(folder);

        if (!folder.exists()) {
            LOGGER.warn("[Nemesis System] Blueprint directory is unavailable: {}", folder.getAbsolutePath());
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.out.println("[NEMESIS DEBUG] No blueprint JSON files found");
            LOGGER.info("[Nemesis System] No demon blueprints found in config folder.");
            return;
        }

        System.out.println("[NEMESIS DEBUG] Found " + files.length + " blueprint files to load");

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

        System.out.println("[NEMESIS DEBUG] Blueprint loading complete: " + BLUEPRINTS.size() + " demon blueprints, " +
            PERSONALITY_DEFAULTS.size() + " personality defaults, global defaults " + (globalDefaults != null ? "loaded" : "missing"));
    }

    private static void seedBlueprintDefaults(File folder) {
        for (String fileName : DEFAULT_BLUEPRINT_FILES) {
            Path targetPath = folder.toPath().resolve(fileName);
            if (Files.exists(targetPath)) {
                continue;
            }

            String resourcePath = DEFAULT_BLUEPRINT_RESOURCE_ROOT + "/" + fileName;
            try (InputStream stream = BlueprintLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    LOGGER.warn("[Nemesis System] Missing bundled default blueprint resource: {}", resourcePath);
                    continue;
                }

                String rawJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                String normalized = ensureSkinLocation(rawJson, fileName);
                Files.writeString(targetPath, normalized, StandardCharsets.UTF_8);
                LOGGER.info("[Nemesis System] Seeded default blueprint: {}", fileName);
            } catch (IOException e) {
                LOGGER.error("[Nemesis System] Failed to seed default blueprint: {}", fileName, e);
            }
        }
    }

    private static String ensureSkinLocation(String rawJson, String fileName) {
        try {
            JsonObject object = JsonParser.parseString(rawJson).getAsJsonObject();
            if (object.has("skin_location") && !object.get("skin_location").isJsonNull()
                    && !object.get("skin_location").getAsString().isBlank()) {
                return rawJson;
            }

            String blueprintId = object.has("id") && !object.get("id").isJsonNull()
                    ? object.get("id").getAsString()
                    : fileName.replace(".json", "");
            var canonical = NemesisSkinRegistry.getCanonicalSkinForBlueprint(blueprintId);
            object.addProperty("skin_location",
                    canonical != null ? canonical.toString() : NemesisSkinRegistry.DEFAULT_TEXTURE.toString());
            return GSON.toJson(object);
        } catch (Exception e) {
            LOGGER.warn("[Nemesis System] Could not normalize skin_location for {}. Using raw content.", fileName);
            return rawJson;
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

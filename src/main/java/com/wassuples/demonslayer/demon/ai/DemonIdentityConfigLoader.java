package com.wassuples.demonslayer.demon.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads configurable procedural demon names/titles from config/demonslayer/procedural_identity.json.
 */
public final class DemonIdentityConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("demonslayer/procedural_identity.json");

    private static volatile List<String> cachedNames = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultNames.DEFAULT_NAMES;
    private static volatile List<String> cachedTitles = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultTitles.DEFAULT_TITLES;
    private static volatile boolean loaded;

    private DemonIdentityConfigLoader() {
    }

    public static List<String> getNames() {
        ensureLoaded();
        return cachedNames;
    }

    public static List<String> getTitles() {
        ensureLoaded();
        return cachedTitles;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (DemonIdentityConfigLoader.class) {
            if (!loaded) {
                loadOrCreate();
                loaded = true;
            }
        }
    }

    private static void loadOrCreate() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                writeDefaults();
                cachedNames = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultNames.DEFAULT_NAMES;
                cachedTitles = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultTitles.DEFAULT_TITLES;
                LOGGER.info("[Nemesis System] Created procedural identity config at {}", CONFIG_PATH.toAbsolutePath());
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                List<String> names = sanitize(data != null ? data.names : null);
                List<String> titles = sanitize(data != null ? data.titles : null);

                cachedNames = names.isEmpty() ? com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultNames.DEFAULT_NAMES : names;
                cachedTitles = titles.isEmpty() ? com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultTitles.DEFAULT_TITLES : titles;

                if (names.isEmpty() || titles.isEmpty()) {
                    LOGGER.warn("[Nemesis System] procedural_identity.json had empty/invalid names or titles; defaults were used for missing entries.");
                }
            }
        } catch (Exception e) {
            cachedNames = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultNames.DEFAULT_NAMES;
            cachedTitles = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultTitles.DEFAULT_TITLES;
            LOGGER.error("[Nemesis System] Failed to load procedural identity config; using built-in defaults.", e);
        }
    }

    private static void writeDefaults() throws IOException {
        ConfigData defaults = new ConfigData();
        defaults.names = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultNames.DEFAULT_NAMES;
        defaults.titles = com.wassuples.demonslayer.demon.ai.DemonIdentityDefaultTitles.DEFAULT_TITLES;
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(defaults, writer);
        }
    }

    private static List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }

        return unique.isEmpty() ? List.of() : List.copyOf(new ArrayList<>(unique));
    }

    private static final class ConfigData {
        private List<String> names;
        private List<String> titles;
    }
}

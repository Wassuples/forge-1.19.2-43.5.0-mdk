package com.wassuples.demonslayer.missions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MissionReloadListener implements PreparableReloadListener {
    private static final Logger LOGGER = DemonSlayer.LOGGER;
    private static final String DIRECTORY = "missions";

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> prepare(resourceManager), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(MissionRegistry::loadAll, gameExecutor);
    }

    private Collection<MissionDefinition> prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, MissionDefinition> loaded = new HashMap<>();
        resourceManager.listResources(DIRECTORY, rl -> rl.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (InputStream stream = resource.open();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        JsonElement raw = JsonParser.parseReader(reader);
                        JsonObject json = raw.getAsJsonObject();
                        ResourceLocation missionId = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath().substring(DIRECTORY.length() + 1, resourceLocation.getPath().length() - 5));
                        MissionDefinition definition = MissionLoader.parseDefinition(missionId, json);
                        loaded.put(missionId, definition);
                    } catch (IOException ex) {
                        LOGGER.error("Failed to load mission {}", resourceLocation, ex);
                    }
                });
        return loaded.values();
    }
}

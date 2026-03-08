package com.wassuples.demonslayer.worldgen.tree;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Grows the Wisteria sapling into the Wisteria tree.
 */
public class WisteriaSaplingGrower extends AbstractTreeGrower {

    private static final ResourceLocation WISTERIA_TREE_ID = new ResourceLocation("demonslayer", "trees_wisteria");
    private static final ResourceKey<ConfiguredFeature<?, ?>> WISTERIA_TREE_KEY =
            ResourceKey.create(BuiltinRegistries.CONFIGURED_FEATURE.key(), WISTERIA_TREE_ID);

    @Override
    protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        // Correct approach for datapack-defined tree features:
        // 1) Prefer the *runtime* registry access from the active server (includes datapacks).
        // 2) Fall back to BuiltinRegistries for dev environments / early init.
        // 3) Never throw here; returning null just makes the sapling fail to grow instead of crashing.

        // Runtime registry (datapacks loaded)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                // Forge 1.19.2: worldgen registries live in the server's RegistryAccess.
                @SuppressWarnings("deprecation")
                Registry<ConfiguredFeature<?, ?>> runtimeRegistry = server.registryAccess().registry(BuiltinRegistries.CONFIGURED_FEATURE.key()).orElse(null);
                if (runtimeRegistry != null) {
                    Holder<ConfiguredFeature<?, ?>> holder = runtimeRegistry.getHolder(WISTERIA_TREE_KEY).orElse(null);
                    if (holder != null) return holder;
                }
            } catch (Throwable ignored) {
                // If mappings/registry plumbing differs, continue to fallback.
            }
        }

        // Fallback registry (may not include datapack entries)
        return BuiltinRegistries.CONFIGURED_FEATURE.getHolder(WISTERIA_TREE_KEY).orElse(null);
    }
}

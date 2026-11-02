package com.wassuples.demonslayer.worldgen;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.tree.WisteriaTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom world-generation feature types. Configured/placed features that use them live in the data pack.
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, DemonSlayer.MOD_ID);

    /** {@code demonslayer:wisteria_tree}, config {@code {}}. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> WISTERIA_TREE =
            FEATURES.register("wisteria_tree", () -> new WisteriaTreeFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}

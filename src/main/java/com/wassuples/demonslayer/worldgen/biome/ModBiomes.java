package com.wassuples.demonslayer.worldgen.biome;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBiomes {
    public static final ResourceKey<Biome> FUJIKASANE_FOREST = registerKey("fujikasane_forest");

    private static ResourceKey<Biome> registerKey(String name) {
        return ResourceKey.create(ForgeRegistries.BIOMES.getRegistryKey(), new ResourceLocation(DemonSlayer.MOD_ID, name));
    }
}
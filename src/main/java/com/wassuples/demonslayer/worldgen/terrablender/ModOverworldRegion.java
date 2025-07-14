package com.wassuples.demonslayer.worldgen.terrablender;

import com.mojang.datafixers.util.Pair;
import com.wassuples.demonslayer.worldgen.biome.ModBiomes;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

public class ModOverworldRegion extends Region {

    public ModOverworldRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // This is where you link your biome to specific climate parameters.
        this.addBiome(mapper,
                // We're defining RANGES now, not single points, to make the biome bigger.
                // We're also targeting high erosion, which is where mountains often form.
                Climate.parameters(
                        // Temperature: Cool to Neutral
                        new Climate.Parameter(10, 40),
                        // Humidity: Any
                        Climate.Parameter.span(-100, 100),
                        // Continentalness: Inland
                        new Climate.Parameter(30, 100),
                        // Erosion: High (Mountainous)
                        new Climate.Parameter(40, 100),
                        // Depth: Any
                        Climate.Parameter.span(0.0F, 1.0F),
                        // Weirdness: Any
                        Climate.Parameter.span(-2.0F, 2.0F),
                        0.0F), // Offset
                ModBiomes.FUJIKASANE_FOREST
        );
    }
}
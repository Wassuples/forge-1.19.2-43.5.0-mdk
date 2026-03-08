package com.wassuples.demonslayer.worldgen.terrablender;

import com.mojang.datafixers.util.Pair;
import com.wassuples.demonslayer.worldgen.biome.ModBiomes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * World generation: Mod Overworld Region.
 */
public class ModOverworldRegion extends Region {
    public ModOverworldRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addBiome(mapper, Climate.parameters(
                        Climate.Parameter.span(-1.0f, 1.0f), // Temperature
                        Climate.Parameter.span(-1.0f, 1.0f), // Humidity
                        Climate.Parameter.span(0.3f, 0.8f),  // Continentalness
                        Climate.Parameter.span(-0.4f, 0.2f), // Erosion (Low)
                        Climate.Parameter.span(0.9f, 1.4f),  // Depth (High)
                        Climate.Parameter.span(0.5f, 1.0f),  // Weirdness (High)
                        0.0f), // Offset
                ModBiomes.FUJIKASANE_PLATEAU
        );

        this.addBiome(mapper, Climate.parameters(
                        Climate.Parameter.span(-1.0f, 1.0f), // Temperature
                        Climate.Parameter.span(-1.0f, 1.0f), // Humidity
                        Climate.Parameter.span(0.3f, 0.8f),  // Continentalness
                        Climate.Parameter.span(0.2f, 0.7f),  // Erosion (High)
                        Climate.Parameter.span(0.7f, 1.1f),  // Depth (High)
                        Climate.Parameter.span(0.5f, 1.0f),  // Weirdness (High)
                        0.0f), // Offset
                ModBiomes.FUJIKASANE_SLOPES
        );

        // Mount Yoko
        this.addBiome(mapper, Climate.parameters(
                        // Keep this biome tightly in the "mountain peaks" neighborhood.
                        Climate.Parameter.span(0.15f, 0.45f),  // Temperature (mild)
                        Climate.Parameter.span(-0.15f, 0.25f), // Humidity
                        Climate.Parameter.span(0.55f, 0.95f),  // Continentalness (inland/high)
                        Climate.Parameter.span(-0.85f, -0.65f),  // Erosion (very low -> sharper terrain)
                        Climate.Parameter.span(0.9f, 1.25f),      // Depth (extremely high vertices)
                        Climate.Parameter.span(0.8f, 1.25f),      // Weirdness (extreme variance)
                        0.0f),
                ModBiomes.MOUNT_YOKO
        );
    }
}
package com.wassuples.demonslayer.worldgen.terrablender;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

/**
 * World generation: Mod Terra Blender.
 */
public class ModTerraBlender {

    public static void registerRegions() {
        // Lower weight for more rarity (e.g., 2 instead of 5)
        Regions.register(new ModOverworldRegion(new ResourceLocation(DemonSlayer.MOD_ID, "overworld"), 2));
    }
}
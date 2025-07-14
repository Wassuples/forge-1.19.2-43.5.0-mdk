package com.wassuples.demonslayer.worldgen.terrablender;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;

public class ModTerraBlender {

    public static void registerRegions() {
        // This method is called during the FMLCommonSetupEvent to register the region.
        // The weight (e.g., 5) determines how common the region is. Higher numbers are more common.
        Regions.register(new ModOverworldRegion(new ResourceLocation(DemonSlayer.MOD_ID, "overworld"), 5));
    }
}
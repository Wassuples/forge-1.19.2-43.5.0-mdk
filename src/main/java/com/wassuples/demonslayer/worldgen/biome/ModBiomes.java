package com.wassuples.demonslayer.worldgen.biome;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModBiomes {
    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registry.BIOME_REGISTRY, DemonSlayer.MOD_ID);

    // Keys for your new biomes
    public static final ResourceKey<Biome> FUJIKASANE_PLATEAU = registerKey("fujikasane_plateau");
    public static final ResourceKey<Biome> FUJIKASANE_SLOPES = registerKey("fujikasane_slopes");
    public static final ResourceKey<Biome> MOUNT_YOKO = registerKey("mount_yoko");

    public static void register(IEventBus eventBus) {
        BIOMES.register(eventBus);
    }

    private static ResourceKey<Biome> registerKey(String name) {
        return ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(DemonSlayer.MOD_ID, name));
    }
}
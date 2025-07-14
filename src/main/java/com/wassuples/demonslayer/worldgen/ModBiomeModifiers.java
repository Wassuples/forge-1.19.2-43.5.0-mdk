package com.wassuples.demonslayer.worldgen;

import com.mojang.serialization.Codec;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.biome.modifier.AddBiomesBiomeModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, DemonSlayer.MOD_ID);

    public static RegistryObject<Codec<AddBiomesBiomeModifier>> ADD_BIOMES_BY_TAG =
            BIOME_MODIFIER_SERIALIZERS.register("add_biomes", () -> AddBiomesBiomeModifier.CODEC);


    public static void register(IEventBus eventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
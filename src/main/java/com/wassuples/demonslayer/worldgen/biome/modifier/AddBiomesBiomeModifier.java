package com.wassuples.demonslayer.worldgen.biome.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wassuples.demonslayer.worldgen.ModBiomeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.List;

public record AddBiomesBiomeModifier(HolderSet<Biome> biomes, List<MobSpawnSettings.SpawnerData> spawners) implements BiomeModifier {
    public static final Codec<AddBiomesBiomeModifier> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            HolderSet.codec(Biome.CODEC).fieldOf("biomes").forGetter(AddBiomesBiomeModifier::biomes),
            MobSpawnSettings.SpawnerData.CODEC.listOf().fieldOf("spawners").forGetter(AddBiomesBiomeModifier::spawners)
    ).apply(builder, AddBiomesBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // This is not used for adding biomes, only for modifying existing ones.
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.ADD_BIOMES_BY_TAG.get();
    }
}
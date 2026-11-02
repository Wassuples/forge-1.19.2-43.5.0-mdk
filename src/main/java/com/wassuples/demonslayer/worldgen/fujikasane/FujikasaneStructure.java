package com.wassuples.demonslayer.worldgen.fujikasane;

import com.mojang.serialization.Codec;
import com.wassuples.demonslayer.worldgen.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Mt. Fujikasane: one lone mountain per world (placed by a single-position {@code concentric_rings} structure set, see
 * {@code data/demonslayer/worldgen/structure_set/fujikasane.json}). The whole mountain — terrain, wisteria band,
 * stairway, torii, lanterns and the shrine plaza where the Final Selection begins and ends — is one procedural piece,
 * {@link FujikasaneMountainPiece}, shaped by {@link FujikasaneLayout}.
 */
public class FujikasaneStructure extends Structure {
    public static final Codec<FujikasaneStructure> CODEC = simpleCodec(FujikasaneStructure::new);

    public FujikasaneStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int baseY = FujikasaneLayout.computeBaseY(context.chunkGenerator(), context.heightAccessor(), context.randomState(), cx, cz);
        return Optional.of(new GenerationStub(new BlockPos(cx, baseY, cz),
                builder -> builder.addPiece(new FujikasaneMountainPiece(cx, cz, baseY, context.heightAccessor().getMinBuildHeight()))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.FUJIKASANE.get();
    }
}

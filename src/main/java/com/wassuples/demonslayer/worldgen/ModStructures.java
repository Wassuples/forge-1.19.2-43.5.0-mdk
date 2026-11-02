package com.wassuples.demonslayer.worldgen;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneMountainPiece;
import com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneStructure;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Code-defined structure types and piece types. The structures themselves (and where they go) are in the data pack. */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registry.STRUCTURE_TYPE_REGISTRY, DemonSlayer.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registry.STRUCTURE_PIECE_REGISTRY, DemonSlayer.MOD_ID);

    public static final RegistryObject<StructureType<FujikasaneStructure>> FUJIKASANE =
            STRUCTURE_TYPES.register("fujikasane", () -> () -> FujikasaneStructure.CODEC);

    public static final RegistryObject<StructurePieceType> FUJIKASANE_MOUNTAIN =
            PIECE_TYPES.register("fujikasane_mountain", () -> (StructurePieceType.ContextlessType) FujikasaneMountainPiece::new);

    /** {@code demonslayer:fujikasane} — the structure (data pack), also used by {@code /locate structure}. */
    public static final ResourceKey<Structure> FUJIKASANE_KEY = ResourceKey.create(Registry.STRUCTURE_REGISTRY, new ResourceLocation(DemonSlayer.MOD_ID, "fujikasane"));
    /** Structure tag holding only the mountain, for {@code ServerLevel#findNearestMapStructure}. */
    public static final TagKey<Structure> FUJIKASANE_TAG = TagKey.create(Registry.STRUCTURE_REGISTRY, new ResourceLocation(DemonSlayer.MOD_ID, "fujikasane"));

    private ModStructures() {
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
    }
}

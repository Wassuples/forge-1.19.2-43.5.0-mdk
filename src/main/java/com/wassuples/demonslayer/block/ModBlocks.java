package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.worldgen.tree.WisteriaSaplingGrower;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Deferred registry for all custom blocks in the mod.
 *
 * <p>Includes Wisteria wood set (log, planks, leaves, sapling), ores (tamahagane, scarlet ore),
 * crafting stations ({@link SwordsmithTableBlock}, {@link SunForgeBlock}).
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DemonSlayer.MOD_ID);

    // ---------------------------------------------------------------- Wisteria wood set
    public static final RegistryObject<Block> STRIPPED_WISTERIA_LOG = registerBlock("stripped_wisteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)));
    public static final RegistryObject<Block> STRIPPED_WISTERIA_WOOD = registerBlock("stripped_wisteria_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_WOOD)));
    public static final RegistryObject<Block> WISTERIA_LOG = registerBlock("wisteria_log",
            () -> new WisteriaLogBlock(STRIPPED_WISTERIA_LOG, BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));
    public static final RegistryObject<Block> WISTERIA_WOOD = registerBlock("wisteria_wood",
            () -> new WisteriaLogBlock(STRIPPED_WISTERIA_WOOD, BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)));
    public static final RegistryObject<Block> WISTERIA_PLANKS = registerBlock("wisteria_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
    public static final RegistryObject<Block> WISTERIA_STAIRS = registerBlock("wisteria_stairs",
            () -> new StairBlock(() -> WISTERIA_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS)));
    public static final RegistryObject<Block> WISTERIA_SLAB = registerBlock("wisteria_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)));
    public static final RegistryObject<Block> WISTERIA_FENCE = registerBlock("wisteria_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)));
    public static final RegistryObject<Block> WISTERIA_FENCE_GATE = registerBlock("wisteria_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE)));
    public static final RegistryObject<Block> WISTERIA_DOOR = registerDoubleBlock("wisteria_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR).noOcclusion()));
    public static final RegistryObject<Block> WISTERIA_TRAPDOOR = registerBlock("wisteria_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR).noOcclusion()));
    public static final RegistryObject<Block> WISTERIA_BUTTON = registerBlock("wisteria_button",
            () -> new WoodButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON)));
    public static final RegistryObject<Block> WISTERIA_PRESSURE_PLATE = registerBlock("wisteria_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE)));
    /** Blossom curtains that hang under wisteria canopies. */
    public static final RegistryObject<Block> HANGING_WISTERIA = registerBlock("hanging_wisteria",
            () -> new HangingWisteriaBlock(BlockBehaviour.Properties.of(Material.PLANT, net.minecraft.world.level.material.MaterialColor.COLOR_PURPLE)
                    .noCollission().instabreak().sound(net.minecraft.world.level.block.SoundType.AZALEA_LEAVES)));

    public static final RegistryObject<Block> WISTERIA_LEAVES = registerBlock("wisteria_leaves",
            () -> new WisteriaLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    // ---------------------------------------------------------------- shrine & torii (Mt. Fujikasane)
    /** Vermilion-lacquered post or beam (torii pillars, shrine frame). */
    public static final RegistryObject<Block> VERMILION_PILLAR = registerBlock("vermilion_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of(Material.WOOD, net.minecraft.world.level.material.MaterialColor.COLOR_RED)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    /** Black lacquered wood (torii kasagi lintels). */
    public static final RegistryObject<Block> BLACK_LACQUER = registerBlock("black_lacquer",
            () -> new Block(BlockBehaviour.Properties.of(Material.WOOD, net.minecraft.world.level.material.MaterialColor.COLOR_BLACK)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    /** A bedroll unrolled under someone resting (no item; placed by the rest system). */
    public static final RegistryObject<Block> LAID_BEDROLL = BLOCKS.register("laid_bedroll",
            () -> new LaidBedrollBlock(BlockBehaviour.Properties.of(Material.WOOL, net.minecraft.world.level.material.MaterialColor.COLOR_PURPLE)
                    .strength(0.2F).noOcclusion().noLootTable().sound(SoundType.WOOL)));
    /** Tōrō stone lantern, lit. */
    public static final RegistryObject<Block> STONE_LANTERN = registerBlock("stone_lantern",
            () -> new StoneLanternBlock(BlockBehaviour.Properties.of(Material.STONE, net.minecraft.world.level.material.MaterialColor.STONE)
                    .strength(1.5F, 6.0F).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 15)));
    /** Paper shoji wall in a wooden lattice. */
    public static final RegistryObject<Block> SHOJI_SCREEN = registerBlock("shoji_screen",
            () -> new Block(BlockBehaviour.Properties.of(Material.WOOD, net.minecraft.world.level.material.MaterialColor.SNOW)
                    .strength(1.0F).sound(SoundType.BAMBOO)));
    /** Kawara clay roof tiles. */
    public static final RegistryObject<Block> KAWARA_TILES = registerBlock("kawara_tiles",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE, net.minecraft.world.level.material.MaterialColor.COLOR_GRAY)
                    .strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_TILES)));
    public static final RegistryObject<Block> KAWARA_TILE_STAIRS = registerBlock("kawara_tile_stairs",
            () -> new StairBlock(() -> KAWARA_TILES.get().defaultBlockState(), BlockBehaviour.Properties.copy(KAWARA_TILES.get())));
    public static final RegistryObject<Block> KAWARA_TILE_SLAB = registerBlock("kawara_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(KAWARA_TILES.get())));

    public static final RegistryObject<Block> WISTERIA_SAPLING = registerBlock("wisteria_sapling",
            () -> new SaplingBlock(new WisteriaSaplingGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    /** No item; registered into the vanilla flower pot in common setup. */
    public static final RegistryObject<Block> POTTED_WISTERIA_SAPLING = BLOCKS.register("potted_wisteria_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WISTERIA_SAPLING, BlockBehaviour.Properties.copy(Blocks.POTTED_OAK_SAPLING)));


    public static final RegistryObject<Block> SCARLET_ORE = registerBlock("scarlet_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops(),
                    UniformInt.of(0, 2)));

    public static final RegistryObject<Block> SUN_FORGE = registerBlock("sun_forge",
            () -> new SunForgeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()
                    .lightLevel(s -> s.getValue(SunForgeBlock.LIT) ? 13 : 0)));

    public static final RegistryObject<Block> SWORDSMITHS_TABLE = registerBlock("swordsmiths_table",
            () -> new SwordsmithTableBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

    public static final RegistryObject<Block> GRAVITY_TELEPORTER = registerBlock("gravity_teleporter",
            () -> new GravityTeleporterBlock(BlockBehaviour.Properties.of(Material.STONE).strength(3f).requiresCorrectToolForDrops()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    /** Doors and other two-tall blocks need the double-high item so the top half is placed correctly. */
    private static <T extends Block> RegistryObject<T> registerDoubleBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new net.minecraft.world.item.DoubleHighBlockItem(toReturn.get(),
                new Item.Properties().tab(com.wassuples.demonslayer.item.ModCreativeModeTab.DEMON_SLAYER_TAB)));
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(com.wassuples.demonslayer.item.ModCreativeModeTab.DEMON_SLAYER_TAB)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

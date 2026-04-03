package com.wassuples.demonslayer.block;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.worldgen.tree.WisteriaSaplingGrower;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    public static final RegistryObject<Block> WISTERIA_LOG = registerBlock("wisteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));

    public static final RegistryObject<Block> WISTERIA_LEAVES = registerBlock("wisteria_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    public static final RegistryObject<Block> WISTERIA_SAPLING = registerBlock("wisteria_sapling",
            () -> new SaplingBlock(new WisteriaSaplingGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));


    public static final RegistryObject<Block> SCARLET_ORE = registerBlock("scarlet_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops(),
                    UniformInt.of(0, 2)));

    public static final RegistryObject<Block> SUN_FORGE = registerBlock("sun_forge",
            () -> new SunForgeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> SWORDSMITHS_TABLE = registerBlock("swordsmiths_table",
            () -> new SwordsmithTableBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(com.wassuples.demonslayer.item.ModCreativeModeTab.DEMON_SLAYER_TAB)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

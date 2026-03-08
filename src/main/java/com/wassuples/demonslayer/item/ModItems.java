package com.wassuples.demonslayer.item;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred registry for all mod items.
 *
 * <p>Includes Nichirin swords, crafting materials, consumables (Muzan's Blood, wisteria items),
 * and spawn eggs for all custom entities.
 *
 * @see ModCreativeModeTab
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DemonSlayer.MOD_ID);

    public static final RegistryObject<Item> NICHIRIN_BLADE = ITEMS.register("nichirin_blade",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_GIYU = ITEMS.register("nichirin_blade_giyu",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_SHINOBU = ITEMS.register("nichirin_blade_shinobu",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_MITSURI = ITEMS.register("nichirin_blade_mitsuri",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_OBANAI = ITEMS.register("nichirin_blade_obanai",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_SANEMI = ITEMS.register("nichirin_blade_sanemi",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_GYOMEI = ITEMS.register("nichirin_blade_gyomei",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_MUICHIRO = ITEMS.register("nichirin_blade_muichiro",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_RENGOKU = ITEMS.register("nichirin_blade_rengoku",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_TENGEN = ITEMS.register("nichirin_blade_tengen",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_JIGORO = ITEMS.register("nichirin_blade_jigoro",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> NICHIRIN_BLADE_KANAE = ITEMS.register("nichirin_blade_kanae",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_NICHIRIN_TAB)));

    public static final RegistryObject<Item> MUZANS_BLOOD = ITEMS.register("muzans_blood",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)
                    .food(new FoodProperties.Builder().alwaysEat().effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 1), 1.0F).build())));

    public static final RegistryObject<Item> RAW_SCARLET = ITEMS.register("raw_scarlet",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> SCARLET_CRIMSON_IRON_SAND = ITEMS.register("scarlet_crimson_iron_sand",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> NICHIRIN_STEEL = ITEMS.register("nichirin_steel",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    // DEMONS
    public static final RegistryObject<Item> FODDER_DEMON_SPAWN_EGG = ITEMS.register("fodder_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.FODDER_DEMON, 0x2B2B2B, 0x7A0000,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWLY_DEMON_SPAWN_EGG = ITEMS.register("lowly_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWLY_DEMON, 0x1E1E1E, 0xB30000,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> COMMON_DEMON_SPAWN_EGG = ITEMS.register("common_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.COMMON_DEMON, 0x1C1C2A, 0xB0002A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> GREATER_DEMON_SPAWN_EGG = ITEMS.register("greater_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.GREATER_DEMON, 0x101020, 0xCC0033,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> ELITE_DEMON_SPAWN_EGG = ITEMS.register("elite_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.ELITE_DEMON, 0x0B0B14, 0xFF0033,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> HAND_DEMON_SPAWN_EGG = ITEMS.register("hand_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.HAND_DEMON, 0x2A1414, 0xE6C7C7,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));

    // LOWER MOONS
    public static final RegistryObject<Item> LOWER_MOON_ONE_SPAWN_EGG = ITEMS.register("lower_moon_one_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_ONE, 0x140A14, 0x6A1B6A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWER_MOON_TWO_SPAWN_EGG = ITEMS.register("lower_moon_two_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_TWO, 0x0A1420, 0x1B6A9B,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWER_MOON_THREE_SPAWN_EGG = ITEMS.register("lower_moon_three_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_THREE, 0x142014, 0x3A7A2A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWER_MOON_FOUR_SPAWN_EGG = ITEMS.register("lower_moon_four_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_FOUR, 0x201414, 0x7A2A2A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWER_MOON_FIVE_SPAWN_EGG = ITEMS.register("lower_moon_five_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_FIVE, 0x141420, 0x2A2A7A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOWER_MOON_SIX_SPAWN_EGG = ITEMS.register("lower_moon_six_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOWER_MOON_SIX, 0x20140A, 0xC27A1B,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));

    // UPPER MOONS
    public static final RegistryObject<Item> UPPER_MOON_ONE_SPAWN_EGG = ITEMS.register("upper_moon_one_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.UPPER_MOON_ONE, 0x0A0A14, 0xB31B3A,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> UPPER_MOON_TWO_SPAWN_EGG = ITEMS.register("upper_moon_two_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.UPPER_MOON_TWO, 0x0A1414, 0x1BB3B3,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> UPPER_MOON_THREE_SPAWN_EGG = ITEMS.register("upper_moon_three_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.UPPER_MOON_THREE, 0x14140A, 0xB3B31B,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> UPPER_MOON_FOUR_SPAWN_EGG = ITEMS.register("upper_moon_four_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.UPPER_MOON_FOUR, 0x140A0A, 0xB31B1B,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> UPPER_MOON_FIVE_SPAWN_EGG = ITEMS.register("upper_moon_five_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.UPPER_MOON_FIVE, 0x0A141E, 0x1B6AB3,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));

    // EXISTING HASHIRA / NPC SPAWN EGGS
    public static final RegistryObject<Item> WATER_HASHIRA_SPAWN_EGG = ITEMS.register("giyu_tomioka_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WATER_HASHIRA, 0x1A2A4C, 0x4D82C6,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> INSECT_HASHIRA_SPAWN_EGG = ITEMS.register("shinobu_kocho_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.INSECT_HASHIRA, 0x3C1E5C, 0xBD639F,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> LOVE_HASHIRA_SPAWN_EGG = ITEMS.register("mitsuri_kanroji_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOVE_HASHIRA, 0xF5A9B8, 0x90EE90,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> SNAKE_HASHIRA_SPAWN_EGG = ITEMS.register("obanai_iguro_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SNAKE_HASHIRA, 0x242424, 0xFFFFFF,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> WIND_HASHIRA_SPAWN_EGG = ITEMS.register("sanemi_shinazugawa_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WIND_HASHIRA, 0x9DC183, 0xFFFFFF,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> STONE_HASHIRA_SPAWN_EGG = ITEMS.register("gyomei_himejima_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.STONE_HASHIRA, 0x4A4A4A, 0x9F2B68,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> MIST_HASHIRA_SPAWN_EGG = ITEMS.register("muichiro_tokito_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.MIST_HASHIRA, 0x2C3E50, 0x6AFB92,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> FLAME_HASHIRA_SPAWN_EGG = ITEMS.register("kyojuro_rengoku_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.FLAME_HASHIRA, 0xFFA500, 0xDC143C,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> FLOWER_HASHIRA_SPAWN_EGG = ITEMS.register("kanae_kocho_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.FLOWER_HASHIRA, 0xBA55D3, 0xFFC0CB,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> SOUND_HASHIRA_SPAWN_EGG = ITEMS.register("tengen_uzui_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SOUND_HASHIRA, 0x8B0000, 0xFFD700,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> THUNDER_HASHIRA_SPAWN_EGG = ITEMS.register("jigoro_kuwajima_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.THUNDER_HASHIRA, 0xFFD700, 0xFFA500,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> CROW_SPAWN_EGG = ITEMS.register("crow_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.CROW, 0x2b2b2b, 0xf4f4f4,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));
    public static final RegistryObject<Item> TAMAYO_SPAWN_EGG = ITEMS.register("tamayo_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.TAMAYO, 0x5D2B4F, 0xEAD7C9,
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_SPAWN_EGGS_TAB)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

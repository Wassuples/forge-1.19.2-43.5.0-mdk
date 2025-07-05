package com.wassuples.demonslayer.item;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DemonSlayer.MOD_ID);

    public static final RegistryObject<Item> NICHIRIN_BLADE = ITEMS.register("nichirin_blade",
            () -> new NichirinSwordItem(Tiers.NETHERITE, 10, 5f,
                    new Item.Properties().stacksTo(1).tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> MUZANS_BLOOD = ITEMS.register("muzans_blood",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)
                    .food(new FoodProperties.Builder().alwaysEat().effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 1), 1.0F).build())));


    public static final RegistryObject<Item> WATER_HASHIRA_SPAWN_EGG = ITEMS.register("giyu_tomioka_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WATER_HASHIRA, 0x1A2A4C, 0x4D82C6, // Deep Blue & Water Blue
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> INSECT_HASHIRA_SPAWN_EGG = ITEMS.register("shinobu_kocho_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.INSECT_HASHIRA, 0x3C1E5C, 0xBD639F, // Dark Purple & Magenta
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> LOVE_HASHIRA_SPAWN_EGG = ITEMS.register("mitsuri_kanroji_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.LOVE_HASHIRA, 0xF5A9B8, 0x90EE90, // Pastel Pink & Light Green
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> SNAKE_HASHIRA_SPAWN_EGG = ITEMS.register("obanai_iguro_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SNAKE_HASHIRA, 0x242424, 0xFFFFFF, // Black & White
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> WIND_HASHIRA_SPAWN_EGG = ITEMS.register("sanemi_shinazugawa_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.WIND_HASHIRA, 0x9DC183, 0xFFFFFF, // Pale Green & White
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> STONE_HASHIRA_SPAWN_EGG = ITEMS.register("gyomei_himejima_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.STONE_HASHIRA, 0x4A4A4A, 0x9F2B68, // Dark Gray & Purple
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> MIST_HASHIRA_SPAWN_EGG = ITEMS.register("muichiro_tokito_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.MIST_HASHIRA, 0x2C3E50, 0x6AFB92, // Dark Slate & Mint Green
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> FLAME_HASHIRA_SPAWN_EGG = ITEMS.register("kyojuro_rengoku_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.FLAME_HASHIRA, 0xFFA500, 0xDC143C, // Orange & Crimson Red
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> FLOWER_HASHIRA_SPAWN_EGG = ITEMS.register("kanae_kocho_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.FLOWER_HASHIRA, 0xBA55D3, 0xFFC0CB, // Medium Orchid & Pink
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> SOUND_HASHIRA_SPAWN_EGG = ITEMS.register("tengen_uzui_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.SOUND_HASHIRA, 0x8B0000, 0xFFD700, // Dark Red & Gold
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));

    public static final RegistryObject<Item> THUNDER_HASHIRA_SPAWN_EGG = ITEMS.register("jigoro_kuwajima_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntityTypes.THUNDER_HASHIRA, 0xFFD700, 0xFFA500, // Gold & Orange
                    new Item.Properties().tab(ModCreativeModeTab.DEMON_SLAYER_TAB)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

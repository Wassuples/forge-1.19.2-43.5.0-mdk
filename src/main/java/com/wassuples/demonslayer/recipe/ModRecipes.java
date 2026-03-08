package com.wassuples.demonslayer.recipe;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred registry for custom recipe serializers and recipe types.
 *
 * <p>Includes Sun Forge smelting and Swordsmith Table crafting recipes.
 */
public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DemonSlayer.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, DemonSlayer.MOD_ID);

    public static final RegistryObject<RecipeSerializer<SunForgeRecipe>> SUN_FORGE_SERIALIZER =
            SERIALIZERS.register("sun_forge", SunForgeRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<SunForgeRecipe>> SUN_FORGE_TYPE =
            TYPES.register("sun_forge", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return DemonSlayer.MOD_ID + ":sun_forge";
                }
            });

    public static final RegistryObject<RecipeSerializer<SwordsmithingRecipe>> SWORDSMITHING_SERIALIZER =
            SERIALIZERS.register("swordsmithing", SwordsmithingRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<SwordsmithingRecipe>> SWORDSMITHING_TYPE =
            TYPES.register("swordsmithing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return DemonSlayer.MOD_ID + ":swordsmithing";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}

package com.wassuples.demonslayer.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wassuples.demonslayer.block.ModBlocks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Simple 2-input -> 1-output recipe for the Sun Forge.
 *
 * JSON format (data/<namespace>/recipes/<name>.json):
 * {
 *   "type": "demonslayer:sun_forge",
 *   "ingredients": [ { ... }, { ... } ],
 *   "result": { "item": "...", "count": 1 },
 *   "cookTime": 100
 * }
 */
public class SunForgeRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int cookTime;

    public SunForgeRecipe(ResourceLocation id, NonNullList<Ingredient> ingredients, ItemStack result, int cookTime) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.cookTime = cookTime;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (level.isClientSide()) return false;
        if (ingredients.size() < 2) return false;

        ItemStack a = container.getItem(0);
        ItemStack b = container.getItem(1);

        // Order-independent matching: (a matches 0 && b matches 1) OR swapped
        return (ingredients.get(0).test(a) && ingredients.get(1).test(b)) ||
               (ingredients.get(0).test(b) && ingredients.get(1).test(a));
    }

    @Override
    public ItemStack assemble(Container container) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    public int getCookTime() {
        return cookTime;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SUN_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SUN_FORGE_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        Block block = ModBlocks.SUN_FORGE.get();
        return new ItemStack(block.asItem());
    }

    public static class Serializer implements RecipeSerializer<SunForgeRecipe> {
        private static final int DEFAULT_COOK_TIME = 100;

        @Override
        public SunForgeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
            if (ingredientsJson.size() != 2) {
                throw new IllegalArgumentException("Sun Forge recipe must have exactly 2 ingredients");
            }

            NonNullList<Ingredient> ingredients = NonNullList.withSize(2, Ingredient.EMPTY);
            ingredients.set(0, Ingredient.fromJson(ingredientsJson.get(0)));
            ingredients.set(1, Ingredient.fromJson(ingredientsJson.get(1)));

            ItemStack result = net.minecraftforge.common.crafting.CraftingHelper.getItemStack(
                    GsonHelper.getAsJsonObject(json, "result"), true);

            int cookTime = GsonHelper.getAsInt(json, "cookTime", DEFAULT_COOK_TIME);

            return new SunForgeRecipe(recipeId, ingredients, result, cookTime);
        }

        @Nullable
        @Override
        public SunForgeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            int cookTime = buffer.readVarInt();
            return new SunForgeRecipe(recipeId, ingredients, result, cookTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SunForgeRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ing : recipe.ingredients) {
                ing.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.cookTime);
        }
    }
}

package com.wassuples.demonslayer.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A shaped crafting recipe that is ONLY matched/used by the Swordsmith's Table menu.
 *
 * JSON format is identical to vanilla shaped recipes, except the type is:
 *   "type": "demonslayer:swordsmithing"
 */
public class SwordsmithingRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

    private final ShapedRecipe wrapped;

    public SwordsmithingRecipe(ShapedRecipe wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return wrapped.matches(container, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer container) {
        return wrapped.assemble(container);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return wrapped.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem() {
        return wrapped.getResultItem();
    }

    @Override
    public ResourceLocation getId() {
        return wrapped.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SWORDSMITHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SWORDSMITHING_TYPE.get();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return wrapped.getRemainingItems(container);
    }

    @Override
    public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
        return wrapped.getIngredients();
    }

    @Override
    public boolean isSpecial() {
        return wrapped.isSpecial();
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public ItemStack getToastSymbol() {
        return wrapped.getToastSymbol();
    }

    public static class Serializer implements RecipeSerializer<SwordsmithingRecipe> {

        @Override
        public SwordsmithingRecipe fromJson(ResourceLocation recipeId, com.google.gson.JsonObject json) {
            ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromJson(recipeId, json);
            return new SwordsmithingRecipe(shaped);
        }

        @Nullable
        @Override
        public SwordsmithingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromNetwork(recipeId, buffer);
            return shaped == null ? null : new SwordsmithingRecipe(shaped);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SwordsmithingRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe.wrapped);
        }
    }
}

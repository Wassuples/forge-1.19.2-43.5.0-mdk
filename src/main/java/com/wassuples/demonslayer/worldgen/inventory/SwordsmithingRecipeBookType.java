package com.wassuples.demonslayer.worldgen.inventory;

import net.minecraft.world.inventory.RecipeBookType;

/**
 * We reuse the vanilla CRAFTING recipe book UI for the swordsmith table.
 * (This is the simplest option; blocking vanilla recipes is handled by using a custom recipe type.)
 */
public final class SwordsmithingRecipeBookType {
    public static final RecipeBookType TYPE = RecipeBookType.CRAFTING;

    private SwordsmithingRecipeBookType() {
    }
}


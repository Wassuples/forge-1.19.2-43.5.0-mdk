package com.wassuples.demonslayer.item.custom;

import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

/**
 * Utility methods for working with Hashira-specific Nichirin swords.
 *
 * Contract:
 * - Hashira swords are represented as either distinct items (preferred for drops/creative tab)
 *   or (fallback) as a generic {@link ModItems#NICHIRIN_BLADE} with CustomModelData.
 */
public final class HashiraSwordHelper {
    private static final String NBT_CUSTOM_MODEL_DATA = "CustomModelData";

    private HashiraSwordHelper() {
    }

    /**
     * Maps a breathing style to the Hashira sword model.
     */
    @Nullable
    public static SwordModel getModelForStyle(@Nullable BreathingStyles style) {
        if (style == null) return null;
        return switch (style) {
            case WATER -> SwordModel.WATER;
            case INSECT -> SwordModel.INSECT;
            case LOVE -> SwordModel.LOVE;
            case SERPENT -> SwordModel.SNAKE;
            case WIND -> SwordModel.WIND;
            case STONE -> SwordModel.STONE;
            case MIST -> SwordModel.MIST;
            case FLAME -> SwordModel.FLAME;
            case SOUND -> SwordModel.SOUND;
            case THUNDER -> SwordModel.THUNDER;
            case FLOWER -> SwordModel.FLOWER;
            default -> null;
        };
    }

    /**
     * Returns the registered item for a Hashira sword for the given style (if present).
     */
    @Nullable
    public static RegistryObject<Item> getItemForStyle(@Nullable BreathingStyles style) {
        SwordModel model = getModelForStyle(style);
        if (model == null) return null;

        return switch (model) {
            case WATER -> ModItems.NICHIRIN_BLADE_GIYU;
            case INSECT -> ModItems.NICHIRIN_BLADE_SHINOBU;
            case LOVE -> ModItems.NICHIRIN_BLADE_MITSURI;
            case SNAKE -> ModItems.NICHIRIN_BLADE_OBANAI;
            case WIND -> ModItems.NICHIRIN_BLADE_SANEMI;
            case STONE -> ModItems.NICHIRIN_BLADE_GYOMEI;
            case MIST -> ModItems.NICHIRIN_BLADE_MUICHIRO;
            case FLAME -> ModItems.NICHIRIN_BLADE_RENGOKU;
            case SOUND -> ModItems.NICHIRIN_BLADE_TENGEN;
            case THUNDER -> ModItems.NICHIRIN_BLADE_JIGORO;
            case FLOWER -> ModItems.NICHIRIN_BLADE_KANAE;
            default -> null;
        };
    }

    /**
     * Creates an ItemStack for a Hashira sword for the given style.
     * Prefers a dedicated registered item; falls back to the generic Nichirin with CustomModelData.
     */
    public static ItemStack createSwordForStyle(@Nullable BreathingStyles style) {
        RegistryObject<Item> item = getItemForStyle(style);
        if (item != null) {
            return new ItemStack(item.get());
        }

        SwordModel model = getModelForStyle(style);
        if (model != null) {
            return NichirinSwordItem.createSword(model);
        }

        return ItemStack.EMPTY;
    }

    /**
     * Determines if a given stack is a Hashira sword.
     */
    public static boolean isHashiraSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        if (item == ModItems.NICHIRIN_BLADE_GIYU.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_SHINOBU.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_MITSURI.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_OBANAI.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_SANEMI.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_GYOMEI.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_MUICHIRO.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_RENGOKU.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_TENGEN.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_JIGORO.get()) return true;
        if (item == ModItems.NICHIRIN_BLADE_KANAE.get()) return true;

        // Fallback: generic nichirin with a non-zero model.
        return item == ModItems.NICHIRIN_BLADE.get() && getModelFromStack(stack) != null;
    }

    /**
     * Extracts the sword model from an ItemStack (supports both dedicated items and CustomModelData).
     */
    @Nullable
    public static SwordModel getModelFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        if (item == ModItems.NICHIRIN_BLADE_GIYU.get()) return SwordModel.WATER;
        if (item == ModItems.NICHIRIN_BLADE_SHINOBU.get()) return SwordModel.INSECT;
        if (item == ModItems.NICHIRIN_BLADE_MITSURI.get()) return SwordModel.LOVE;
        if (item == ModItems.NICHIRIN_BLADE_OBANAI.get()) return SwordModel.SNAKE;
        if (item == ModItems.NICHIRIN_BLADE_SANEMI.get()) return SwordModel.WIND;
        if (item == ModItems.NICHIRIN_BLADE_GYOMEI.get()) return SwordModel.STONE;
        if (item == ModItems.NICHIRIN_BLADE_MUICHIRO.get()) return SwordModel.MIST;
        if (item == ModItems.NICHIRIN_BLADE_RENGOKU.get()) return SwordModel.FLAME;
        if (item == ModItems.NICHIRIN_BLADE_TENGEN.get()) return SwordModel.SOUND;
        if (item == ModItems.NICHIRIN_BLADE_JIGORO.get()) return SwordModel.THUNDER;
        if (item == ModItems.NICHIRIN_BLADE_KANAE.get()) return SwordModel.FLOWER;

        if (item == ModItems.NICHIRIN_BLADE.get()) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(NBT_CUSTOM_MODEL_DATA)) return null;
            int cmd = tag.getInt(NBT_CUSTOM_MODEL_DATA);
            for (SwordModel model : SwordModel.values()) {
                if (model != SwordModel.DEFAULT && model.getCustomModelData() == cmd) {
                    return model;
                }
            }
        }

        return null;
    }

    /**
     * True if the sword matches the player's current breathing style.
     */
    public static boolean doesSwordMatchStyle(ItemStack stack, @Nullable BreathingStyles style) {
        SwordModel model = getModelFromStack(stack);
        SwordModel expected = getModelForStyle(style);
        return model != null && model == expected;
    }
}


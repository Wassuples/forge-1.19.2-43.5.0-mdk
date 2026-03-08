package com.wassuples.demonslayer.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Defines custom Creative Mode inventory tabs for the mod.
 *
 * <p>Tabs:
 * <ul>
 *   <li><b>Demon Slayer</b> — general mod items and materials</li>
 *   <li><b>Nichirin</b> — all Nichirin sword variants</li>
 *   <li><b>Spawn Eggs</b> — spawn eggs for mod entities</li>
 * </ul>
 */
public class ModCreativeModeTab {
    public static final CreativeModeTab DEMON_SLAYER_TAB = new CreativeModeTab("demonslayertab") {
        @Override
        public ItemStack makeIcon() { return new ItemStack(ModItems.MUZANS_BLOOD.get()); }
    };

    public static final CreativeModeTab DEMON_SLAYER_NICHIRIN_TAB = new CreativeModeTab("demonslayer_nichirin") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.NICHIRIN_BLADE.get());
        }
    };

    public static final CreativeModeTab DEMON_SLAYER_SPAWN_EGGS_TAB = new CreativeModeTab("demonslayer_spawn_eggs") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.CROW_SPAWN_EGG.get());
        }
    };
}

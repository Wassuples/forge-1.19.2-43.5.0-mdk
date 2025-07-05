package com.wassuples.demonslayer.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {
    public static final CreativeModeTab DEMON_SLAYER_TAB = new CreativeModeTab("demonslayertab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.NICHIRIN_BLADE.get());
        }
    };
}

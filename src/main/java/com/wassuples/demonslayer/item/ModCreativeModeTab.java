package com.wassuples.demonslayer.item;

import com.wassuples.demonslayer.item.custom.NichirinSwordItem;
import com.wassuples.demonslayer.item.custom.SwordModel;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {
    public static final CreativeModeTab DEMON_SLAYER_TAB = new CreativeModeTab("demonslayertab") {
        @Override
        public ItemStack makeIcon() {
            return NichirinSwordItem.createSword(SwordModel.DEFAULT);
        }
    };
}

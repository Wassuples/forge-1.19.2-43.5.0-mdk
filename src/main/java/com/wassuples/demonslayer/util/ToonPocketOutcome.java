package com.wassuples.demonslayer.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Everything Pocket can pull out, in reel order. Shared by PocketForm (what happens) and the in-world slot
 * machine renderer (reel icon + caption), so adding an entry here adds it to both.
 */
public enum ToonPocketOutcome {
    MALLET(Items.IRON_AXE, "MALLET!"),
    HOT_POTATO(Items.TNT, "HOT POTATO!"),
    FORTUNE_COOKIE(Items.COOKIE, "FORTUNE!"),
    FISH(Items.COD, "FISH?!"),
    SPRING_GLOVE(Items.PISTON, "SPRING GLOVE!"),
    MAGNET(Items.LODESTONE, "MAGNET!"),
    RUBBER_CHICKEN(Items.CHICKEN, "RUBBER CHICKEN!"),
    RAIN_CLOUD(Items.LIGHTNING_ROD, "RAIN CLOUD!"),
    CREAM_PIE(Items.PUMPKIN_PIE, "CREAM PIE!"),
    BANANA_BUNCH(Items.YELLOW_DYE, "BANANAS!"),
    CUCKOO_CLOCK(Items.CLOCK, "CUCKOO!"),
    SPRING_SHOES(Items.SLIME_BLOCK, "SPRING SHOES!");

    private static final ToonPocketOutcome[] VALUES = values();
    private final Item icon;
    private final String caption;
    private ItemStack iconStack;

    ToonPocketOutcome(Item icon, String caption) {
        this.icon = icon;
        this.caption = caption;
    }

    public ItemStack icon() {
        if (iconStack == null) iconStack = new ItemStack(icon);
        return iconStack;
    }

    public String caption() {
        return caption;
    }

    public static int count() {
        return VALUES.length;
    }

    public static ToonPocketOutcome byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}

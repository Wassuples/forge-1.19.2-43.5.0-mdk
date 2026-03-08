package com.wassuples.demonslayer.ranking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Ordered hierarchy of Demon Slayer Corps ranks from {@link #CIVILIAN} to {@link #HASHIRA}.
 *
 * <p>Follows the canonical 10-tier system (Mizunoto → Kinoe) plus Hashira at the top.
 * Each rank has a display color used in chat messages and UI elements.
 *
 * @see PlayerProgression#getRank()
 */
public enum Rank {
    CIVILIAN("Civilian", ChatFormatting.DARK_GRAY),
    MIZUNOTO("Mizunoto", ChatFormatting.GRAY),
    MIZUNOE("Mizunoe", ChatFormatting.GRAY),
    KANOTO("Kanoto", ChatFormatting.WHITE),
    KANOE("Kanoe", ChatFormatting.WHITE),
    TSUCHINOTO("Tsuchinoto", ChatFormatting.YELLOW),
    TSUCHINOE("Tsuchinoe", ChatFormatting.YELLOW),
    HINOTO("Hinoto", ChatFormatting.GOLD),
    HINOE("Hinoe", ChatFormatting.GOLD),
    KINOTO("Kinoto", ChatFormatting.AQUA),
    KINOE("Kinoe", ChatFormatting.AQUA),
    HASHIRA("Hashira", ChatFormatting.LIGHT_PURPLE);

    private final String name;
    private final ChatFormatting color;

    Rank(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public MutableComponent getDisplayName() {
        return Component.literal(this.name).withStyle(this.color);
    }
}

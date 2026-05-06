package com.wassuples.demonslayer.reputation.rank;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum SlayerRank {
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

    SlayerRank(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public MutableComponent getDisplayName() {
        return Component.literal(this.name).withStyle(this.color);
    }
}

package com.wassuples.demonslayer.demon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum DemonRank {
    FODDER("Fodder Demon", ChatFormatting.DARK_GRAY),
    LOWLY_DEMON("Lowly Demon", ChatFormatting.GRAY),
    DEMON("Demon", ChatFormatting.WHITE),
    GREATER_DEMON("Greater Demon", ChatFormatting.YELLOW),
    ELITE_DEMON("Elite Demon", ChatFormatting.GOLD),

    LOWER_MOON_6("Lower Moon 6", ChatFormatting.RED),
    LOWER_MOON_5("Lower Moon 5", ChatFormatting.RED),
    LOWER_MOON_4("Lower Moon 4", ChatFormatting.RED),
    LOWER_MOON_3("Lower Moon 3", ChatFormatting.RED),
    LOWER_MOON_2("Lower Moon 2", ChatFormatting.RED),
    LOWER_MOON_1("Lower Moon 1", ChatFormatting.RED),
    UPPER_MOON_6("Upper Moon 6", ChatFormatting.DARK_PURPLE),
    UPPER_MOON_5("Upper Moon 5", ChatFormatting.DARK_PURPLE),
    UPPER_MOON_4("Upper Moon 4", ChatFormatting.DARK_PURPLE),
    UPPER_MOON_3("Upper Moon 3", ChatFormatting.DARK_PURPLE),
    UPPER_MOON_2("Upper Moon 2", ChatFormatting.DARK_PURPLE),
    UPPER_MOON_1("Upper Moon 1", ChatFormatting.DARK_PURPLE);

    private final String name;
    private final ChatFormatting color;

    DemonRank(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public MutableComponent getDisplayName() {
        return Component.literal(this.name).withStyle(this.color);
    }

    public boolean isMoonRank() {
        return this.ordinal() >= LOWER_MOON_6.ordinal() && this.ordinal() < UPPER_MOON_1.ordinal();
    }
}

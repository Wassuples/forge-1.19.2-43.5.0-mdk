package com.wassuples.demonslayer.demon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Ordered hierarchy of demon ranks from weakest ({@link #FODDER}) to strongest ({@link #UPPER_MOON_1}).
 *
 * <p>Ranks above {@link #ELITE_DEMON} are "Moon" ranks and correspond to canonical Twelve Kizuki
 * positions. Moon ranks have an associated {@link #getBlueprintId()} used by the nemesis blueprint
 * system to spawn lore-accurate boss variants.
 *
 * @see PlayerDemon#getRank()
 * @see DemonProgressionHelper#RANK_UP_COSTS
 */
public enum DemonRank {
    FODDER("Fodder Demon", ChatFormatting.DARK_GRAY),
    LOWLY_DEMON("Lowly Demon", ChatFormatting.GRAY),
    DEMON("Demon", ChatFormatting.WHITE),
    GREATER_DEMON("Greater Demon", ChatFormatting.YELLOW),
    ELITE_DEMON("Elite Demon", ChatFormatting.GOLD),

    LOWER_MOON_6("Lower Moon 6", ChatFormatting.RED, "LM6"),
    LOWER_MOON_5("Lower Moon 5", ChatFormatting.RED, "LM5"),
    LOWER_MOON_4("Lower Moon 4", ChatFormatting.RED, "LM4"),
    LOWER_MOON_3("Lower Moon 3", ChatFormatting.RED, "LM3"),
    LOWER_MOON_2("Lower Moon 2", ChatFormatting.RED, "LM2"),
    LOWER_MOON_1("Lower Moon 1", ChatFormatting.RED, "LM1"),
    UPPER_MOON_6("Upper Moon 6", ChatFormatting.DARK_PURPLE, "UM6"),
    UPPER_MOON_5("Upper Moon 5", ChatFormatting.DARK_PURPLE, "UM5"),
    UPPER_MOON_4("Upper Moon 4", ChatFormatting.DARK_PURPLE, "UM4"),
    UPPER_MOON_3("Upper Moon 3", ChatFormatting.DARK_PURPLE, "UM3"),
    UPPER_MOON_2("Upper Moon 2", ChatFormatting.DARK_PURPLE, "UM2"),
    UPPER_MOON_1("Upper Moon 1", ChatFormatting.DARK_PURPLE, "UM1");

    private final String name;
    private final ChatFormatting color;
    @Nullable private final String blueprintId;

    DemonRank(String name, ChatFormatting color) {
        this(name, color, null);
    }

    DemonRank(String name, ChatFormatting color, @Nullable String blueprintId) {
        this.name = name;
        this.color = color;
        this.blueprintId = blueprintId;
    }

    public MutableComponent getDisplayName() {
        return Component.literal(this.name).withStyle(this.color);
    }

    @Nullable public String getBlueprintId() {
        return this.blueprintId;
    }

    public boolean isMoonRank() {
        return this.ordinal() >= LOWER_MOON_6.ordinal();
    }
}

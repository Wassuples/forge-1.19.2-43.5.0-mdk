package com.wassuples.demonslayer.gamerule;

import net.minecraft.world.level.GameRules;

/**
 * Registers custom game rules for tuning mod behavior.
 *
 * <p>Game rules:
 * <ul>
 *   <li>{@code doFormsGriefing} — whether forms can modify terrain (fire, ice, etc.)</li>
 *   <li>{@code useCanonKizuki} — whether to use canonical Twelve Kizuki demon lore</li>
 *   <li>{@code minDemonRosterSize} — minimum number of demons in the nemesis hierarchy</li>
 *   <li>{@code formsCooldownMultiplier} — percentage scalar for all form cooldowns (100 = default)</li>
 * </ul>
 */
public final class ModGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> RULE_FORMS_GRIEFING = GameRules.register(
            "doFormsGriefing",
            GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true)
    );

    public static final GameRules.Key<GameRules.BooleanValue> USE_CANON_KIZUKI = GameRules.register(
            "useCanonKizuki",
            GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true)
    );

    public static final GameRules.Key<GameRules.IntegerValue> MIN_DEMON_ROSTER_SIZE = GameRules.register(
            "minDemonRosterSize",
            GameRules.Category.MISC,
            GameRuleFactory.createIntegerRule(12, 12)
    );

    public static final GameRules.Key<GameRules.BooleanValue> RULE_ENABLE_PARKOUR = GameRules.register(
            "enableParkour",
            GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true)
    );

    /**
     * Global cooldown multiplier for ALL breathing forms and demon forms.
     *
     * This gamerule is expressed as a percentage:
     * - 0   = no cooldowns
     * - 100 = default (no change)
     * - 200 = double cooldowns
     */
    public static final GameRules.Key<GameRules.IntegerValue> FORMS_COOLDOWN_MULTIPLIER_PERCENT = GameRules.register(
            "formsCooldownMultiplier",
            GameRules.Category.MISC,
            GameRuleFactory.createIntegerRule(100, 0)
    );

    public static void register() {}
}

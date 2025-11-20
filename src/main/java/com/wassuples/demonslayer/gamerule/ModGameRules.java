package com.wassuples.demonslayer.gamerule;

import net.minecraft.world.level.GameRules;

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

    public static void register() {}
}
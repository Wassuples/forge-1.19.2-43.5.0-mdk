package com.wassuples.demonslayer.gamerule;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A utility class that defines and registers all custom gamerules for the mod.
 */
public final class ModGameRules {
    private static final Logger LOGGER = LogUtils.getLogger();

    // The key is defined here, ready to be used by other parts of your mod.
    public static GameRules.Key<GameRules.BooleanValue> RULE_FORMS_GRIEFING;

    /**
     * This method must be called once from your main mod constructor to register the gamerules.
     */
    public static void register() {
        RULE_FORMS_GRIEFING = createBooleanGameRule("formsGriefing", GameRules.Category.PLAYER, true);
    }

    /**
     * A private helper method that contains the complex Reflection logic to create and register a boolean gamerule.
     * This is required to bypass a bug in Minecraft 1.19.2 where the 'create' method is not public.
     * @param name The name of the gamerule (e.g., "formsGriefing").
     * @param category The category it belongs to (e.g., PLAYER, MOBS).
     * @param defaultValue The default value (true or false).
     * @return The registered GameRules.Key.
     */
    private static GameRules.Key<GameRules.BooleanValue> createBooleanGameRule(String name, GameRules.Category category, boolean defaultValue) {
        try {
            Method createMethod = GameRules.BooleanValue.class.getDeclaredMethod("create", boolean.class);
            createMethod.setAccessible(true);
            GameRules.Type<GameRules.BooleanValue> ruleType = (GameRules.Type<GameRules.BooleanValue>) createMethod.invoke(null, defaultValue);
            return GameRules.register(name, category, ruleType);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Failed to create and register gamerule '{}' via reflection!", name, e);
            // to prevent a crash
            return null;
        }
    }
}
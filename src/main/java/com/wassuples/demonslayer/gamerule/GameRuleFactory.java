package com.wassuples.demonslayer.gamerule;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reflection-based factory for creating {@link GameRules.Type} instances.
 *
 * <p>Forge 1.19.2 does not expose the {@code GameRules.Type} constructor publicly.
 * This factory uses reflection to access it, enabling registration of custom boolean
 * and integer game rules via {@link ModGameRules}.
 */
public final class GameRuleFactory {

    private static Constructor<GameRules.Type> typeConstructor;
    private static Class<?> visitorCallerClass;

    static {
        try {
            visitorCallerClass = Class.forName("net.minecraft.world.level.GameRules$VisitorCaller");
            Constructor<GameRules.Type> foundConstructor = (Constructor<GameRules.Type>) GameRules.Type.class.getDeclaredConstructor(
                    Supplier.class, Function.class, BiConsumer.class, visitorCallerClass
            );
            foundConstructor.setAccessible(true);
            typeConstructor = foundConstructor;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize GameRuleFactory via reflection.", e);
        }
    }

    public static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean defaultValue) {
        return create(
                BoolArgumentType::bool,
                type -> new GameRules.BooleanValue(type, defaultValue),
                (server, rule) -> {},
                GameRules.GameRuleTypeVisitor::visitBoolean
        );
    }

    public static GameRules.Type<GameRules.IntegerValue> createIntegerRule(int defaultValue, int minValue) {
        return create(
                () -> IntegerArgumentType.integer(minValue),
                type -> new GameRules.IntegerValue(type, defaultValue),
                (server, rule) -> {},
                GameRules.GameRuleTypeVisitor::visitInteger
        );
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> GameRules.Type<T> create(
            Supplier<com.mojang.brigadier.arguments.ArgumentType<?>> argumentType,
            Function<GameRules.Type<T>, T> valueFactory,
            BiConsumer<MinecraftServer, T> callback,
            Visitor<T> visitor) {
        try {
            Object visitorProxy = Proxy.newProxyInstance(
                    visitorCallerClass.getClassLoader(),
                    new Class<?>[]{visitorCallerClass},
                    (proxy, method, args) -> {
                        visitor.call(
                                (GameRules.GameRuleTypeVisitor) args[0],
                                (GameRules.Key<T>) args[1],
                                (GameRules.Type<T>) args[2]
                        );
                        return null;
                    }
            );
            return typeConstructor.newInstance(argumentType, valueFactory, callback, visitorProxy);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GameRule.Type via reflection.", e);
        }
    }

    @FunctionalInterface
    private interface Visitor<T extends GameRules.Value<T>> {
        void call(GameRules.GameRuleTypeVisitor visitor, GameRules.Key<T> key, GameRules.Type<T> type);
    }
}

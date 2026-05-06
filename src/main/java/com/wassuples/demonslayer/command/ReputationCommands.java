package com.wassuples.demonslayer.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wassuples.demonslayer.reputation.HardlineFlag;
import com.wassuples.demonslayer.reputation.PlayerRepRecord;
import com.wassuples.demonslayer.reputation.RepEventType;
import com.wassuples.demonslayer.reputation.ReputationDimension;
import com.wassuples.demonslayer.reputation.ReputationManager;
import com.wassuples.demonslayer.reputation.witness.WitnessType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ReputationCommands {
    private ReputationCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> DIM_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ReputationDimension.values())
                    .map(v -> v.name().toLowerCase())
                    .collect(Collectors.toList()), builder);

    private static final SuggestionProvider<CommandSourceStack> HARDLINE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(HardlineFlag.values())
                    .map(v -> v.name().toLowerCase())
                    .collect(Collectors.toList()), builder);

    private static final SuggestionProvider<CommandSourceStack> EVENT_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(RepEventType.values())
                    .map(v -> v.name().toLowerCase())
                    .collect(Collectors.toList()), builder);

    private static final SuggestionProvider<CommandSourceStack> WITNESS_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(WitnessType.values())
                    .map(v -> v.name().toLowerCase())
                    .collect(Collectors.toList()), builder);

    public static LiteralArgumentBuilder<CommandSourceStack> registerSubtree() {
        return Commands.literal("reputation")
                .then(Commands.literal("view")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ReputationCommands::view)))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("dim", StringArgumentType.word()).suggests(DIM_SUGGESTIONS)
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ctx -> setOrAdd(ctx, true))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("dim", StringArgumentType.word()).suggests(DIM_SUGGESTIONS)
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ctx -> setOrAdd(ctx, false))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ReputationCommands::reset)))
                .then(Commands.literal("hardline")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("flag", StringArgumentType.word()).suggests(HARDLINE_SUGGESTIONS)
                                                .executes(ctx -> setHardline(ctx, true)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("flag", StringArgumentType.word()).suggests(HARDLINE_SUGGESTIONS)
                                                .executes(ctx -> setHardline(ctx, false)))))
                )
                .then(Commands.literal("sinlog")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ReputationCommands::sinLog)))
                .then(Commands.literal("witness")
                        .then(Commands.literal("simulate")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("eventType", StringArgumentType.word()).suggests(EVENT_SUGGESTIONS)
                                                .then(Commands.argument("witnessType", StringArgumentType.word()).suggests(WITNESS_SUGGESTIONS)
                                                        .executes(ReputationCommands::simulateWitness))))))
                .then(Commands.literal("suppress")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ReputationCommands::clearSuppression))))
                .then(Commands.literal("investigate")
                        .then(Commands.literal("trigger")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ReputationCommands::triggerInvestigation))));
    }

    private static int view(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = ctx.getSource().getLevel();
        PlayerRepRecord record = ReputationManager.get(level).getOrCreateRecord(player.getUUID());

        Component msg = Component.literal("\nReputation: " + player.getGameProfile().getName() + "\n")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Corps: " + record.getCorpsRep() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Tanjiro: " + record.getTanjiroRep() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Muzan: " + record.getMuzanRep() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Demon: " + record.getDemonRep() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Suspicion: " + record.getSuspicion() + "\n").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Slayer Rank: " + record.getSlayerRank().name() + "\n").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Hardlines: " + (record.getHardlineFlags().isEmpty() ? "none" : record.getHardlineFlags()) + "\n")
                        .withStyle(ChatFormatting.RED));

        ctx.getSource().sendSuccess(msg, false);
        return 1;
    }

    private static int setOrAdd(CommandContext<CommandSourceStack> ctx, boolean isSet) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = ctx.getSource().getLevel();
        ReputationManager manager = ReputationManager.get(level);

        String dimString = StringArgumentType.getString(ctx, "dim").toUpperCase();
        int value = IntegerArgumentType.getInteger(ctx, "value");

        ReputationDimension dim;
        try {
            dim = ReputationDimension.valueOf(dimString);
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendFailure(Component.literal("Unknown dimension: " + dimString));
            return 0;
        }

        if (isSet) {
            manager.setDimension(player.getUUID(), dim, value);
        } else {
            manager.addDimension(player.getUUID(), dim, value);
        }

        ctx.getSource().sendSuccess(Component.literal((isSet ? "Set " : "Added ") + dim.name() + " for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ReputationManager.get(ctx.getSource().getLevel()).resetPlayer(player.getUUID());
        ctx.getSource().sendSuccess(Component.literal("Reputation reset for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int setHardline(CommandContext<CommandSourceStack> ctx, boolean set) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String flagString = StringArgumentType.getString(ctx, "flag").toUpperCase();
        HardlineFlag flag;
        try {
            flag = HardlineFlag.valueOf(flagString);
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendFailure(Component.literal("Unknown hardline flag: " + flagString));
            return 0;
        }

        ReputationManager manager = ReputationManager.get(ctx.getSource().getLevel());
        if (set) {
            manager.setHardline(player.getUUID(), flag);
        } else {
            manager.clearHardline(player.getUUID(), flag);
        }

        ctx.getSource().sendSuccess(Component.literal((set ? "Set" : "Cleared") + " hardline " + flag.name() + " for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int sinLog(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerRepRecord record = ReputationManager.get(ctx.getSource().getLevel()).getOrCreateRecord(player.getUUID());
        if (record.getRecentSins().isEmpty()) {
            ctx.getSource().sendSuccess(Component.literal("No sins recorded for " + player.getGameProfile().getName()), false);
            return 1;
        }

        Component message = Component.literal("Sin Log for " + player.getGameProfile().getName() + "\n").withStyle(ChatFormatting.GOLD);
        for (var sin : record.getRecentSins()) {
            message = message.copy().append(Component.literal("- day " + sin.worldDay() + ": " + sin.eventType().name() + " -> " + sin.faction().name() + "\n").withStyle(ChatFormatting.GRAY));
        }
        ctx.getSource().sendSuccess(message, false);
        return 1;
    }

    private static int simulateWitness(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String eventName = StringArgumentType.getString(ctx, "eventType").toUpperCase();
        String witnessName = StringArgumentType.getString(ctx, "witnessType").toUpperCase();

        RepEventType eventType;
        WitnessType witnessType;
        try {
            eventType = RepEventType.valueOf(eventName);
            witnessType = WitnessType.valueOf(witnessName);
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendFailure(Component.literal("Invalid eventType or witnessType"));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        ReputationManager.get(level).queueWitnessEvent(player.getUUID(), eventType, witnessType, level.getGameTime());
        ctx.getSource().sendSuccess(Component.literal("Queued witness simulation for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int clearSuppression(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ReputationManager.get(ctx.getSource().getLevel()).clearSuppression(player.getUUID());
        ctx.getSource().sendSuccess(Component.literal("Cleared suppression entries for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int triggerInvestigation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = ctx.getSource().getLevel();
        ReputationManager.get(level).triggerInvestigation(player.getUUID(), level.getGameTime());
        ctx.getSource().sendSuccess(Component.literal("Triggered investigation for " + player.getGameProfile().getName()), true);
        return 1;
    }
}



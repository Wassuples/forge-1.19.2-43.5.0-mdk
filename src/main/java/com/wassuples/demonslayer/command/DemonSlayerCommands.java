package com.wassuples.demonslayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wassuples.demonslayer.breathing.PlayerBreath;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemon;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.nemesis.trait.DemonTrait;
import com.wassuples.demonslayer.nemesis.trait.Quirk;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registers and implements all {@code /demonslayer} server commands.
 *
 * <p>Subcommands include:
 * <ul>
 *   <li>{@code setstyle} / {@code setrank} — change a player's breathing style or slayer rank</li>
 *   <li>{@code setdemon} / {@code setdemonrank} — toggle demon status and set demon rank</li>
 *   <li>{@code unlockform} / {@code unlockstyle} — grant forms/styles to a player</li>
 *   <li>{@code givebloodpoints} — award Blood Points currency</li>
 *   <li>{@code missions} — list, grant, or complete missions</li>
 *   <li>{@code hierarchy} — view/modify the nemesis demon hierarchy</li>
 *   <li>{@code debug} — various diagnostic subcommands</li>
 * </ul>
 *
 * <p>Requires OP level 2. Registered via {@link net.minecraftforge.event.RegisterCommandsEvent}.
 */

/**
 * Demon Slayer Commands.
 */
public class DemonSlayerCommands {

    private enum ModifyAction { SET, ADD }

    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingStyles.values()).map(s -> s.name().toLowerCase()), builder);

    private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(Enum::name).map(String::toLowerCase), builder);

    private static final SuggestionProvider<CommandSourceStack> DEMON_RANK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DemonRank.values()).map(Enum::name).map(String::toLowerCase), builder);

    private static final SuggestionProvider<CommandSourceStack> BDA_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BloodDemonArts.values()).map(s -> s.name().toLowerCase()), builder);

    private static final SuggestionProvider<CommandSourceStack> MASTERY_AMOUNT_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"max", "1", "10", "50", "100", "250", "500", "1000"}, builder);

    private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS = (ctx, builder) -> {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            if (targets.size() == 1) {
                ServerPlayer target = targets.iterator().next();
                target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                        SharedSuggestionProvider.suggest(style.getFormsForCurrentStyle().map(form -> form.name().toLowerCase()), builder));
            }
        } catch (Exception e) {
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingForms.values()).map(f -> f.name().toLowerCase()), builder);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STYLE_ALL = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingStyles.values()).map(s -> s.name().toLowerCase()).collect(Collectors.toList()),
                    builder.suggest("all"));

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FORM_ALL = (ctx, builder) -> {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            if (targets.size() == 1) {
                ServerPlayer target = targets.iterator().next();
                target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                        SharedSuggestionProvider.suggest(style.getFormsForCurrentStyle().map(form -> form.name().toLowerCase()), builder.suggest("all")));
            }
        } catch (Exception e) {
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingForms.values()).map(f -> f.name().toLowerCase()), builder.suggest("all"));
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> DEMON_NAME_SUGGESTIONS = (ctx, builder) -> {
        ServerLevel level = ctx.getSource().getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);
        return SharedSuggestionProvider.suggest(
                manager.getAllDemonTraits().stream().map(DemonTrait::getName),
                builder);
    };

    private static final SuggestionProvider<CommandSourceStack> QUIRK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Quirk.values()).map(q -> q.name().toLowerCase()), builder);

    /**
     * Suggests both online players and nemesis demons.
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_COMBATANTS = (ctx, builder) -> {
        ServerLevel level = ctx.getSource().getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        // Get Nemesis names
        Stream<String> nemesisNames = manager.getAllDemonTraits().stream().map(DemonTrait::getName);

        // Get Player names
        Stream<String> playerNames = ctx.getSource().getServer().getPlayerList().getPlayers().stream().map(p -> p.getGameProfile().getName());

        return SharedSuggestionProvider.suggest(Stream.concat(nemesisNames, playerNames), builder);
    };


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main demonslayer command tree (OP level 2 required)
        dispatcher.register(Commands.literal("demonslayer")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> showHelp(ctx.getSource(), "main"))

                //  HELP COMMAND
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource(), "main"))
                        .then(Commands.argument("topic", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"get", "edit", "reset", "nemesis"}, builder))
                                .executes(ctx -> showHelp(ctx.getSource(), StringArgumentType.getString(ctx, "topic"))))
                )

                //  GET COMMAND
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> getPlayerSummary(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))
                                .then(Commands.literal("rank").executes(ctx -> getPlayerRank(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                                .then(Commands.literal("style").executes(ctx -> getPlayerStyle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                                .then(Commands.literal("breath").executes(ctx -> getPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                                .then(Commands.literal("demon").executes(ctx -> getPlayerDemonStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                        )
                        .then(Commands.literal("styles").executes(DemonSlayerCommands::getStylesList))
                        .then(Commands.literal("ranks").executes(DemonSlayerCommands::getRanksList))
                        .then(Commands.literal("missions").executes(DemonSlayerCommands::getMissionsList))
                        .then(Commands.literal("forms")
                                .then(Commands.argument("style_name", StringArgumentType.word()).suggests(STYLE_SUGGESTIONS)
                                        .executes(ctx -> getFormsListForStyle(ctx.getSource(), StringArgumentType.getString(ctx, "style_name")))))
                )

                //  EDIT COMMAND
                .then(Commands.literal("edit")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("breath")
                                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> executeModifyBreath(ctx, ModifyAction.ADD, false))))
                                        .then(Commands.literal("set")
                                                .then(Commands.literal("current").then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeModifyBreath(ctx, ModifyAction.SET, false))))
                                                .then(Commands.literal("max").then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> executeModifyBreath(ctx, ModifyAction.SET, true))))
                                        )
                                        .then(Commands.literal("fill").executes(ctx -> executeFillBreath(ctx)))
                                )
                                .then(Commands.literal("demon")
                                        .then(Commands.literal("set").then(Commands.argument("is_demon", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetDemonStatus(ctx))))
                                        .then(Commands.literal("blood")
                                                .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> executeModifyDemonBlood(ctx, ModifyAction.ADD))))
                                                .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> executeModifyDemonBlood(ctx, ModifyAction.SET))))
                                        )
                                        .then(Commands.literal("rank").then(Commands.argument("rank_name", StringArgumentType.word()).suggests(DEMON_RANK_SUGGESTIONS)
                                                .executes(ctx -> executeSetDemonRank(ctx))))
                                        .then(Commands.literal("bda").then(Commands.argument("bda_name", StringArgumentType.word()).suggests(BDA_SUGGESTIONS)
                                                .executes(ctx -> executeSetBda(ctx))))
                                )
                                .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.word()).suggests(FORM_SUGGESTIONS)
                                        .executes(ctx -> executeSetForm(ctx))))
                                .then(Commands.literal("mastery")
                                        .then(Commands.literal("style").then(Commands.argument("style_or_all", StringArgumentType.word()).suggests(SUGGEST_STYLE_ALL)
                                                .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> executeModifyStyleMastery(ctx, ModifyAction.ADD))))
                                                .then(Commands.literal("set").then(Commands.argument("amount_or_max", StringArgumentType.word()).suggests(MASTERY_AMOUNT_SUGGESTIONS).executes(ctx -> executeModifyStyleMastery(ctx, ModifyAction.SET))))
                                        ))
                                        .then(Commands.literal("form").then(Commands.argument("form_or_all", StringArgumentType.word()).suggests(SUGGEST_FORM_ALL)
                                                .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> executeModifyFormMastery(ctx, ModifyAction.ADD))))
                                                .then(Commands.literal("set").then(Commands.argument("amount_or_max", StringArgumentType.word()).suggests(MASTERY_AMOUNT_SUGGESTIONS).executes(ctx -> executeModifyFormMastery(ctx, ModifyAction.SET))))
                                        ))
                                )
                                .then(Commands.literal("rank").then(Commands.argument("rank_name", StringArgumentType.word()).suggests(RANK_SUGGESTIONS)
                                        .executes(ctx -> executeSetRank(ctx))))
                                .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.word()).suggests(STYLE_SUGGESTIONS)
                                        .executes(ctx -> executeSetStyle(ctx))))
                        )
                )

                //  OP-ONLY NEMESIS COMMANDS
                .then(Commands.literal("nemesis")
                        .executes(ctx -> showHelp(ctx.getSource(), "nemesis"))
                        .then(Commands.literal("info")
                                .then(Commands.argument("demon_name", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                        .executes(ctx -> executeGetDemonInfo(ctx.getSource(), StringArgumentType.getString(ctx, "demon_name")))))
                        .then(Commands.literal("edit")
                                .then(Commands.argument("demon_name", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                        .then(Commands.literal("name")
                                                .then(Commands.argument("new_name", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "name", StringArgumentType.getString(ctx, "new_name")))))
                                        .then(Commands.literal("title")
                                                .then(Commands.argument("new_title", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "title", StringArgumentType.getString(ctx, "new_title")))))
                                        .then(Commands.literal("vigor")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "vigor", IntegerArgumentType.getInteger(ctx, "value")))))
                                        .then(Commands.literal("strength")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "strength", IntegerArgumentType.getInteger(ctx, "value")))))
                                        .then(Commands.literal("agility")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "agility", IntegerArgumentType.getInteger(ctx, "value")))))
                                        .then(Commands.literal("bloodpoints")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "bloodpoints", IntegerArgumentType.getInteger(ctx, "value")))))
                                        .then(Commands.literal("bda")
                                                .then(Commands.argument("bda_name", StringArgumentType.word()).suggests(BDA_SUGGESTIONS)
                                                        .executes(ctx -> executeSetNemesisProperty(ctx, "bda", StringArgumentType.getString(ctx, "bda_name")))))
                                        .then(Commands.literal("quirk")
                                                .then(Commands.literal("add")
                                                        .then(Commands.argument("quirk_name", StringArgumentType.word()).suggests(QUIRK_SUGGESTIONS)
                                                                .executes(ctx -> executeModifyNemesisQuirk(ctx, ModifyAction.ADD))))
                                                .then(Commands.literal("remove")
                                                        .then(Commands.argument("quirk_name", StringArgumentType.word()).suggests(QUIRK_SUGGESTIONS)
                                                                .executes(ctx -> executeModifyNemesisQuirk(ctx, ModifyAction.SET)))) // Using SET as a proxy for REMOVE
                                        )
                                )
                        )
                        .then(Commands.literal("battle")
                                .then(Commands.literal("start")
                                        .then(Commands.argument("challengerName", StringArgumentType.string()).suggests(SUGGEST_COMBATANTS)
                                                .then(Commands.argument("defenderName", StringArgumentType.string()).suggests(SUGGEST_COMBATANTS)
                                                        .executes(DemonSlayerCommands::executeStartBattle))))
                                .then(Commands.literal("end")
                                        .executes(DemonSlayerCommands::executeEndBattle))
                                .then(Commands.literal("send_hunter")
                                        .then(Commands.argument("demonName", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("targetPlayer", EntityArgument.player())
                                                        .executes(DemonSlayerCommands::executeSendHunter))))
                        )
                        .then(Commands.literal("hierarchy")
                                .executes(DemonSlayerCommands::executeShowHierarchy))
                        .then(Commands.literal("simulate_day")
                            .then(Commands.argument("days", IntegerArgumentType.integer())
                                .executes(ctx -> executeSimulateDay(ctx, IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("social")
                                .executes(DemonSlayerCommands::executeShowSocialSummary)
                                .then(Commands.literal("summary")
                                        .executes(DemonSlayerCommands::executeShowSocialSummary))
                                .then(Commands.literal("packs")
                                        .executes(DemonSlayerCommands::executeShowPacks))
                                .then(Commands.literal("alliances")
                                        .executes(DemonSlayerCommands::executeShowAlliances))
                                .then(Commands.literal("inspect")
                                        .then(Commands.argument("demon_name", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .executes(DemonSlayerCommands::executeInspectSocial)))
                                .then(Commands.literal("compat")
                                        .then(Commands.argument("demon_a", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("demon_b", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                        .executes(DemonSlayerCommands::executePreviewCompatibility))))
                                .then(Commands.literal("compat_target_player")
                                        .then(Commands.argument("leader", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("ally", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                        .then(Commands.argument("target", EntityArgument.player())
                                                                .executes(DemonSlayerCommands::executePreviewCompatibilityVsPlayer)))))
                                .then(Commands.literal("compat_target_demon")
                                        .then(Commands.argument("leader", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("ally", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                        .then(Commands.argument("target", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                                .executes(DemonSlayerCommands::executePreviewCompatibilityVsDemon)))))
                                .then(Commands.literal("mobilize_player")
                                        .then(Commands.argument("leader", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(DemonSlayerCommands::executeMobilizePlayer)
                                                        .then(Commands.argument("requested_allies", IntegerArgumentType.integer(1, 8))
                                                                .executes(DemonSlayerCommands::executeMobilizePlayer)))))
                                .then(Commands.literal("mobilize_demon")
                                        .then(Commands.argument("leader", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .then(Commands.argument("target", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                        .executes(DemonSlayerCommands::executeMobilizeDemon)
                                                        .then(Commands.argument("requested_allies", IntegerArgumentType.integer(1, 8))
                                                                .executes(DemonSlayerCommands::executeMobilizeDemon)))))
                                .then(Commands.literal("rebuild_packs")
                                        .executes(DemonSlayerCommands::executeRebuildPacks))
                                .then(Commands.literal("disband")
                                        .then(Commands.argument("leader", StringArgumentType.string()).suggests(DEMON_NAME_SUGGESTIONS)
                                                .executes(DemonSlayerCommands::executeDisbandPack)))
                                .then(Commands.literal("clear_alliances")
                                        .executes(DemonSlayerCommands::executeClearAlliances))
                        )
                )

                //  RESET COMMAND
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players()))
                ));

        // Separate public nemesis command (no OP required) for story viewing
        dispatcher.register(Commands.literal("nemesis")
                .then(Commands.literal("story")
                        .executes(ctx -> executeShowStory(ctx, 10))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                .executes(ctx -> executeShowStory(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
        );
    }

    //  HELPER METHODS

    /**
     * Gets the UUID of a combatant, checking both online players and nemesis demons.
     */
    private static Optional<UUID> getCombatantUUID(CommandSourceStack source, String name) {
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        // Try to find a player first
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        if (player != null) {
            return Optional.of(player.getUUID());
        }

        // If not a player, try to find a nemesis demon
        return manager.getDemonUUIDByName(name);
    }

    //  EXECUTOR METHODS (FOR MULTI-TARGETING)

    private static int executeStartBattle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String challengerName = StringArgumentType.getString(ctx, "challengerName");
        String defenderName = StringArgumentType.getString(ctx, "defenderName");
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        if (manager.getActiveBattle() != null) {
            source.sendFailure(Component.literal("A blood battle is already in progress. Use '/demonslayer nemesis battle end' first."));
            return 0;
        }

        Optional<UUID> challengerUUIDOpt = getCombatantUUID(source, challengerName);
        if (challengerUUIDOpt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown challenger: " + challengerName));
            return 0;
        }

        Optional<UUID> defenderUUIDOpt = getCombatantUUID(source, defenderName);
        if (defenderUUIDOpt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown defender: " + defenderName));
            return 0;
        }

        UUID challengerUUID = challengerUUIDOpt.get();
        UUID defenderUUID = defenderUUIDOpt.get();

        if (challengerUUID.equals(defenderUUID)) {
            source.sendFailure(Component.literal("Challenger and defender cannot be the same entity."));
            return 0;
        }

        manager.startBloodBattle(challengerUUID, defenderUUID);
        source.sendSuccess(Component.literal("Forcibly started a blood battle between " + challengerName + " and " + defenderName + "."), true);
        return 1;
    }

    private static int executeEndBattle(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        if (manager.getActiveBattle() == null) {
            source.sendFailure(Component.literal("No blood battle is currently in progress."));
            return 0;
        }

        manager.endBloodBattle();
        source.sendSuccess(Component.literal("Forcibly ended the active blood battle."), true);
        return 1;
    }

    private static int executeSendHunter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String demonName = StringArgumentType.getString(ctx, "demonName");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "targetPlayer");
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        Optional<UUID> demonUUIDOpt = manager.getDemonUUIDByName(demonName);
        if (demonUUIDOpt.isEmpty()) {
            source.sendFailure(Component.literal("No nemesis demon found with that name: " + demonName));
            return 0;
        }

        if (manager.getDemonTrait(demonUUIDOpt.get()) == null) {
            source.sendFailure(Component.literal("Could not retrieve trait data for " + demonName));
            return 0;
        }

        // Spawn the demon near the player
        manager.spawnDemonFromData(demonUUIDOpt.get(), targetPlayer.getLevel(), targetPlayer.blockPosition().north(3), "Admin Command: send-hunter");
        source.sendSuccess(Component.literal("Sent " + demonName + " to hunt " + targetPlayer.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int executeShowHierarchy(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());
        DemonHierarchyManager.DemonDisplayDataLists data = manager.getDemonDisplayData();

        MutableComponent msg = Component.literal("\n §6═══ Twelve Kizuki ═══\n").withStyle(ChatFormatting.GOLD);

        for (DemonDisplayData d : data.kizuki()) {
            if ("---".equals(d.name())) {
                msg.append(Component.literal(" " + d.rank() + ": ").withStyle(ChatFormatting.GRAY))
                   .append(Component.literal("(Empty)\n").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                msg.append(Component.literal(" " + d.rank() + ": ").withStyle(ChatFormatting.GRAY))
                   .append(Component.literal(d.name() + " ").withStyle(ChatFormatting.WHITE))
                   .append(Component.literal(d.title()).withStyle(ChatFormatting.DARK_GRAY))
                   .append(Component.literal(" (BP: " + d.bloodPoints() + ")\n").withStyle(ChatFormatting.RED));
            }
        }

        if (!data.fodder().isEmpty()) {
            msg.append(Component.literal(" §7── Top Fodder ──\n").withStyle(ChatFormatting.GRAY));
            for (DemonDisplayData d : data.fodder()) {
                msg.append(Component.literal("  " + d.name() + " " + d.title()).withStyle(ChatFormatting.DARK_GRAY))
                   .append(Component.literal(" (BP: " + d.bloodPoints() + ")\n").withStyle(ChatFormatting.GRAY));
            }
        }

        msg.append(Component.literal(" §6══════════════════\n").withStyle(ChatFormatting.GOLD));
        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executeShowStory(CommandContext<CommandSourceStack> ctx, int count) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());
        java.util.List<com.wassuples.demonslayer.nemesis.story.StoryEvent> log = manager.getWorldStoryLog();

        if (log.isEmpty()) {
            source.sendSuccess(Component.literal("No story events recorded yet.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        // Check if executor is a player and their permission level
        UUID playerId = null;
        boolean hasOpAccess = source.hasPermission(2);
        if (source.getEntity() instanceof ServerPlayer sp) {
            playerId = sp.getUUID();
        }

        MutableComponent msg = Component.literal("\n §5═══ Nemesis Story Log ═══\n").withStyle(ChatFormatting.DARK_PURPLE);
        int shown = 0;
        com.wassuples.demonslayer.nemesis.story.PlayerIntelManager intelMgr = manager.getPlayerIntelManager();

        for (com.wassuples.demonslayer.nemesis.story.StoryEvent event : log) {
            if (shown >= count) break;

            String rendered;
            if (hasOpAccess) {
                // OP players see everything with FULL intel tier
                rendered = event.renderFull();
            } else if (playerId != null) {
                // Non-OP players respect their intel tier
                rendered = intelMgr.renderForPlayer(event, playerId);
                if (rendered == null) {
                    // Player has no intel on this event — show as a hidden entry
                    rendered = "§8[Unknown event]";
                }
            } else {
                // Console/command block — show full
                rendered = event.renderFull();
            }

            msg.append(Component.literal(" §7• " + rendered + "\n"));
            shown++;
        }

        msg.append(Component.literal(" §5══════════════════════\n").withStyle(ChatFormatting.DARK_PURPLE));
        msg.append(Component.literal(" Showing " + shown + " of " + log.size() + " events.").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executeSimulateDay(CommandContext<CommandSourceStack> ctx, int days) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);
        for (int i = 0; i < days; i++) {
            manager.runDailySimulation(level);
        }
        source.sendSuccess(Component.literal("Forced a daily simulation cycle. Check the story log for results.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeGetDemonInfo(CommandSourceStack source, String demonName) {
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());
        Optional<UUID> demonUUIDOpt = manager.getDemonUUIDByName(demonName);

        if (demonUUIDOpt.isEmpty()) {
            source.sendFailure(Component.literal("No nemesis demon found with that name."));
            return 0;
        }

        DemonTrait trait = manager.getDemonTrait(demonUUIDOpt.get());
        if (trait == null) {
            source.sendFailure(Component.literal("Could not retrieve trait data for demon."));
            return 0;
        }

        MutableComponent message = Component.literal("\n Nemesis Info: " + trait.getName() + " ").withStyle(ChatFormatting.GOLD);
        manager.getKizukiRank(demonUUIDOpt.get()).ifPresentOrElse(
                rank -> message.append("\nRank: ").withStyle(ChatFormatting.GRAY).append(rank.getDisplayName()),
                () -> message.append("\nRank: ").withStyle(ChatFormatting.GRAY).append(Component.literal("Fodder").withStyle(ChatFormatting.DARK_GRAY))
        );
        message.append("\nTitle: ").withStyle(ChatFormatting.GRAY).append(Component.literal(trait.getTitle()).withStyle(ChatFormatting.WHITE));
        message.append("\nBlood Points: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(trait.getBloodPoints())).withStyle(ChatFormatting.RED));
        message.append("\nBDA: ").withStyle(ChatFormatting.GRAY).append(Component.literal(trait.getAssignedBDA().name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        message.append("\nStats (V/S/A): ").withStyle(ChatFormatting.GRAY).append(Component.literal(trait.getVigor() + "/" + trait.getStrength() + "/" + trait.getAgility()).withStyle(ChatFormatting.YELLOW));
        message.append("\nQuirks: ").withStyle(ChatFormatting.GRAY).append(Component.literal(trait.getQuirks().stream().map(Enum::name).collect(Collectors.joining(", "))).withStyle(ChatFormatting.AQUA));
        message.append("\n-").withStyle(ChatFormatting.DARK_GRAY);

        source.sendSuccess(message, false);
        return 1;
    }

    private static int executeSetNemesisProperty(CommandContext<CommandSourceStack> ctx, String property, Object value) throws CommandSyntaxException {
        String demonName = StringArgumentType.getString(ctx, "demon_name");
        DemonHierarchyManager manager = DemonHierarchyManager.get(ctx.getSource().getLevel());
        Optional<UUID> demonUUIDOpt = manager.getDemonUUIDByName(demonName);

        if (demonUUIDOpt.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No nemesis demon found with that name."));
            return 0;
        }

        DemonTrait trait = manager.getDemonTrait(demonUUIDOpt.get());
        if (trait == null) return 0;

        try {
            switch (property.toLowerCase()) {
                case "name" -> trait.setName((String) value);
                case "title" -> trait.setTitle((String) value);
                case "vigor" -> trait.setVigor((Integer) value);
                case "strength" -> trait.setStrength((Integer) value);
                case "agility" -> trait.setAgility((Integer) value);
                case "bloodpoints" -> trait.setBloodPoints((Integer) value);
                case "bda" -> trait.setAssignedBDA(BloodDemonArts.valueOf(((String) value).toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid value: " + value));
            return 0;
        }


        ctx.getSource().sendSuccess(Component.literal("Set " + property + " for " + demonName + " to " + value), true);
        manager.setDirty();
        return 1;
    }

    private static int executeModifyNemesisQuirk(CommandContext<CommandSourceStack> ctx, ModifyAction action) throws CommandSyntaxException {
        String demonName = StringArgumentType.getString(ctx, "demon_name");
        String quirkName = StringArgumentType.getString(ctx, "quirk_name");
        DemonHierarchyManager manager = DemonHierarchyManager.get(ctx.getSource().getLevel());
        Optional<UUID> demonUUIDOpt = manager.getDemonUUIDByName(demonName);

        if (demonUUIDOpt.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No nemesis demon found with that name."));
            return 0;
        }

        DemonTrait trait = manager.getDemonTrait(demonUUIDOpt.get());
        if (trait == null) return 0;

        try {
            Quirk quirk = Quirk.valueOf(quirkName.toUpperCase());
            if (action == ModifyAction.ADD) {
                trait.addQuirk(quirk);
                ctx.getSource().sendSuccess(Component.literal("Added quirk " + quirk.name() + " to " + demonName), true);
            } else {
                trait.removeQuirk(quirk);
                ctx.getSource().sendSuccess(Component.literal("Removed quirk " + quirk.name() + " from " + demonName), true);
            }
            manager.setDirty();
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid quirk name: " + quirkName));
            return 0;
        }

        return 1;
    }

    private static int executeModifyBreath(CommandContext<CommandSourceStack> ctx, ModifyAction action, boolean isMax) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        for (ServerPlayer target : targets) {
            modifyPlayerBreath(target, action, amount, isMax);
        }
        String actionStr = action == ModifyAction.ADD ? "Added " + amount + " to" : "Set";
        String breathType = isMax ? "max" : "current";
        ctx.getSource().sendSuccess(Component.literal(String.format("%s %s breath for %d player(s)", actionStr, breathType, targets.size())), true);
        return targets.size();
    }

    private static int executeFillBreath(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            fillPlayerBreath(target);
        }
        ctx.getSource().sendSuccess(Component.literal("Filled breath for " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int executeSetDemonStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        boolean isDemon = BoolArgumentType.getBool(ctx, "is_demon");
        for (ServerPlayer target : targets) {
            setPlayerDemonStatus(target, isDemon);
        }
        ctx.getSource().sendSuccess(Component.literal(String.format("Set demon status to %s for %d player(s)", isDemon, targets.size())), true);
        return targets.size();
    }

    private static int executeModifyDemonBlood(CommandContext<CommandSourceStack> ctx, ModifyAction action) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        for (ServerPlayer target : targets) {
            modifyPlayerDemonBlood(target, action, amount);
        }
        String actionStr = action == ModifyAction.ADD ? "Added " + amount + " to" : "Set";
        ctx.getSource().sendSuccess(Component.literal(String.format("%s blood points for %d player(s)", actionStr, targets.size())), true);
        return targets.size();
    }

    private static int executeSetDemonRank(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String rankName = StringArgumentType.getString(ctx, "rank_name");
        try {
            DemonRank newRank = DemonRank.valueOf(rankName.toUpperCase());
            for (ServerPlayer target : targets) {
                setPlayerDemonRank(target, newRank);
            }
            ctx.getSource().sendSuccess(Component.literal("Set demon rank to ").append(newRank.getDisplayName()).append(" for " + targets.size() + " player(s)"), true);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid demon rank: " + rankName));
            return 0;
        }
        return targets.size();
    }

    private static int executeSetBda(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String bdaName = StringArgumentType.getString(ctx, "bda_name");
        try {
            BloodDemonArts newBda = BloodDemonArts.valueOf(bdaName.toUpperCase());
            for (ServerPlayer target : targets) {
                setPlayerBda(target, newBda);
            }
            ctx.getSource().sendSuccess(Component.literal("Set BDA to " + newBda.getDisplayName() + " for " + targets.size() + " player(s)"), true);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid BDA name: " + bdaName));
            return 0;
        }
        return targets.size();
    }

    private static int executeSetForm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String formName = StringArgumentType.getString(ctx, "form_name");
        try {
            BreathingForms newForm = BreathingForms.valueOf(formName.toUpperCase());
            for (ServerPlayer target : targets) {
                setPlayerForm(ctx.getSource(), target, newForm);
            }
            ctx.getSource().sendSuccess(Component.literal("Set form to " + newForm.getDisplayName() + " for " + targets.size() + " player(s) (if applicable)"), true);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid form name: " + formName));
            return 0;
        }
        return targets.size();
    }

    private static int executeModifyStyleMastery(CommandContext<CommandSourceStack> ctx, ModifyAction action) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String styleOrAll = StringArgumentType.getString(ctx, "style_or_all");
        String amountOrMax;
        int amount = 0;

        try {
            if (action == ModifyAction.SET) {
                amountOrMax = StringArgumentType.getString(ctx, "amount_or_max");
                amount = "max".equalsIgnoreCase(amountOrMax) ? 1000 : Integer.parseInt(amountOrMax);
            } else { // ADD
                amount = IntegerArgumentType.getInteger(ctx, "amount");
            }

            for(ServerPlayer target : targets) {
                modifyPlayerStyleMastery(target, styleOrAll, amount, action);
            }
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid argument. Expected 'all', a style name, or a number."));
            return 0;
        }


        ctx.getSource().sendSuccess(Component.literal("Updated style mastery for " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int executeModifyFormMastery(CommandContext<CommandSourceStack> ctx, ModifyAction action) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String formOrAll = StringArgumentType.getString(ctx, "form_or_all");
        String amountOrMax;
        int amount = 0;

        try {
            if (action == ModifyAction.SET) {
                amountOrMax = StringArgumentType.getString(ctx, "amount_or_max");
                amount = "max".equalsIgnoreCase(amountOrMax) ? 100 : Integer.parseInt(amountOrMax);
            } else { // ADD
                amount = IntegerArgumentType.getInteger(ctx, "amount");
            }

            for(ServerPlayer target : targets) {
                modifyPlayerFormMastery(target, formOrAll, amount, action);
            }
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid argument. Expected 'all', a form name, or a number."));
            return 0;
        }


        ctx.getSource().sendSuccess(Component.literal("Updated form mastery for " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int executeSetRank(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String rankName = StringArgumentType.getString(ctx, "rank_name");
        try {
            Rank newRank = Rank.valueOf(rankName.toUpperCase());
            for (ServerPlayer target : targets) {
                setPlayerRank(target, newRank);
            }
            ctx.getSource().sendSuccess(Component.literal("Set rank to ").append(newRank.getDisplayName()).append(" for " + targets.size() + " player(s)"), true);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid rank name: " + rankName));
            return 0;
        }
        return targets.size();
    }

    private static int executeSetStyle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String styleName = StringArgumentType.getString(ctx, "style_name");
        try {
            BreathingStyles newStyle = BreathingStyles.valueOf(styleName.toUpperCase());
            for (ServerPlayer target : targets) {
                setPlayerStyle(target, newStyle);
            }
            ctx.getSource().sendSuccess(Component.literal("Set style to " + newStyle.name() + " for " + targets.size() + " player(s)"), true);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
        return targets.size();
    }

    private static int executeResetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            resetPlayerData(target);
        }
        ctx.getSource().sendSuccess(Component.literal("Reset Demon Slayer data for " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    //  LOGIC METHODS (FOR SINGLE PLAYER)

    private static void modifyPlayerBreath(ServerPlayer target, ModifyAction action, int amount, boolean isMax) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            if (isMax) {
                breath.setMaxBreath(action == ModifyAction.SET ? amount : breath.getMaxBreath() + amount);
            } else {
                breath.setBreath(action == ModifyAction.SET ? amount : breath.getBreath() + amount);
            }
            ModUtils.syncBreath(target);
        });
    }

    private static void fillPlayerBreath(ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.setBreath(breath.getMaxBreath());
            ModUtils.syncBreath(target);
        });
    }

    private static void setPlayerDemonStatus(ServerPlayer target, boolean isDemon) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            demon.setDemon(isDemon);
            ModUtils.syncDemon(target);
        });
    }

    private static void modifyPlayerDemonBlood(ServerPlayer target, ModifyAction action, int amount) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;
            demon.setBloodPoints(action == ModifyAction.SET ? amount : demon.getBloodPoints() + amount);
            ModUtils.syncDemon(target);
        });
    }

    private static void setPlayerDemonRank(ServerPlayer target, DemonRank newRank) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;
            demon.setRank(newRank);
            ModUtils.syncDemon(target);
        });
    }

    private static void setPlayerBda(ServerPlayer target, BloodDemonArts newBda) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (!demon.isDemon()) return;
            demon.unlockBda(newBda); // Ensure it's unlocked
            demon.setEquippedBda(newBda);
            ModUtils.syncDemon(target);
        });
    }

    private static void setPlayerForm(CommandSourceStack source, ServerPlayer target, BreathingForms newForm) {
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            if (newForm.getStyle() != style.getCurrentStyles()) {
                source.sendFailure(Component.literal("Cannot set form for " + target.getDisplayName().getString() + ": Form " + newForm.name() + " is not part of their current style."));
            } else {
                style.setFormIndex(newForm.ordinal());
                ModUtils.syncStyle(target);
            }
        });
    }

    private static void modifyPlayerStyleMastery(ServerPlayer target, String styleOrAll, int amount, ModifyAction action) {
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            if ("all".equalsIgnoreCase(styleOrAll)) {
                boolean unlockAllStyles = action == ModifyAction.SET && amount >= 1000;

                BreathingStyles[] stylesToModify = unlockAllStyles
                        ? BreathingStyles.values()
                        : progression.getUnlockedStyles().toArray(new BreathingStyles[0]);

                for (BreathingStyles style : stylesToModify) {
                    if (unlockAllStyles && !progression.isStyleUnlocked(style)) {
                        progression.unlockStyle(style);
                    }
                    int currentMastery = progression.getStyleMastery(style);
                    progression.setStyleMastery(style, action == ModifyAction.SET ? amount : currentMastery + amount);
                }
            } else {
                BreathingStyles style = BreathingStyles.valueOf(styleOrAll.toUpperCase());
                // Ensure the style is unlocked when granting mastery
                if (!progression.isStyleUnlocked(style)) {
                    progression.unlockStyle(style);
                }
                int currentMastery = progression.getStyleMastery(style);
                progression.setStyleMastery(style, action == ModifyAction.SET ? amount : currentMastery + amount);
            }
            ModUtils.syncProgression(target);
        });
    }

    private static void modifyPlayerFormMastery(ServerPlayer target, String formOrAll, int amount, ModifyAction action) {
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                if ("all".equalsIgnoreCase(formOrAll)) {
                    Arrays.stream(BreathingForms.values())
                            .filter(f -> f.getStyle() == style.getCurrentStyles() && progression.isFormUnlocked(f))
                            .forEach(form -> {
                                int currentMastery = progression.getFormMastery(form);
                                progression.setFormMastery(form, action == ModifyAction.SET ? amount : currentMastery + amount);
                            });
                } else {
                    BreathingForms form = BreathingForms.valueOf(formOrAll.toUpperCase());
                    int currentMastery = progression.getFormMastery(form);
                    progression.setFormMastery(form, action == ModifyAction.SET ? amount : currentMastery + amount);
                }
                ModUtils.syncProgression(target);
            });
        });
    }

    private static void setPlayerRank(ServerPlayer target, Rank newRank) {
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            progression.setRank(newRank);
            ModUtils.applyRankStatBonuses(target, newRank);
            ModUtils.syncProgression(target);
        });
    }

    private static void setPlayerStyle(ServerPlayer target, BreathingStyles newStyle) {
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                if (!progression.isStyleUnlocked(newStyle)) {
                    progression.unlockStyle(newStyle);
                }
                style.changeStyle(newStyle.ordinal());
                ModUtils.syncStyle(target);
                ModUtils.syncProgression(target);
            });
        });
    }

    private static void resetPlayerData(ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(c -> c.copyFrom(new PlayerBreath()));
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(c -> c.copyFrom(new PlayerDemon()));
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(c -> c.copyFrom(new PlayerProgression()));
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(c -> c.copyFrom(new PlayerStyle()));
        ModUtils.syncAll(target);
    }

    //  GET & LIST METHODS

    private static int getPlayerSummary(CommandSourceStack source, ServerPlayer target) {
        MutableComponent message = Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY)
                .append(" " + target.getDisplayName().getString() + "'s Stats \n").withStyle(ChatFormatting.GOLD);
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                message.append(Component.literal("Race: ").withStyle(ChatFormatting.GRAY)).append(Component.literal("Demon").withStyle(ChatFormatting.RED));
                message.append(Component.literal("\nRank: ").withStyle(ChatFormatting.GRAY)).append(demon.getRank().getDisplayName());
                message.append(Component.literal("\nBlood Points: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(demon.getBloodPoints())).withStyle(ChatFormatting.DARK_RED));
                message.append(Component.literal("\nBlood Demon Art: ").withStyle(ChatFormatting.GRAY)).append(demon.getEquippedBda().getDisplayName());
            } else {
                target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                    target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                            message.append(Component.literal("Race: ").withStyle(ChatFormatting.GRAY)).append(Component.literal("Human").withStyle(ChatFormatting.BLUE));
                            message.append(Component.literal("\nRank: ").withStyle(ChatFormatting.GRAY)).append(progression.getRank().getDisplayName());
                            message.append(Component.literal("\nStyle: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(style.getCurrentStyles().toString()).withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal("\nForm: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(style.getCurrentForms().getDisplayName()).withStyle(ChatFormatting.YELLOW));
                            message.append(Component.literal("\nBreath: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(breath.getBreath() + " / " + breath.getMaxBreath()).withStyle(ChatFormatting.GREEN));
                        });
                    });
                });
            }
        });
        message.append(Component.literal("\n-").withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(message, false);
        return 1;
    }

    private static int getPlayerRank(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s rank is ").append(progression.getRank().getDisplayName()), true)
        );
        return 1;
    }

    private static int getPlayerStyle(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s style is " + style.getCurrentStyles()), true)
        );
        return 1;
    }

    private static int getPlayerBreath(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s breath is " + breath.getBreath() + "/" + breath.getMaxBreath()), true)
        );
        return 1;
    }

    private static int getPlayerDemonStatus(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + " is a Demon.").withStyle(ChatFormatting.RED), false);
            } else {
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + " is a Human.").withStyle(ChatFormatting.AQUA), false);
            }
        });
        return 1;
    }

    private static int getStylesList(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.literal("Available Breathing Styles:\n").withStyle(ChatFormatting.GOLD);
        for (BreathingStyles style : BreathingStyles.values()) {
            message.append(Component.literal("- " + style.toString()).withStyle(s -> s.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s style " + style.name().toLowerCase()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set your style to " + style))))).append("\n");
        }
        context.getSource().sendSuccess(message, false);
        return 1;
    }

    private static int getRanksList(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.literal("Available Slayer Ranks:\n").withStyle(ChatFormatting.GOLD);
        for (Rank rank : Rank.values()) {
            message.append(Component.literal("- ").withStyle(ChatFormatting.GRAY))
                    .append(rank.getDisplayName().withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s rank " + rank.name().toLowerCase()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set your rank to ").append(rank.getDisplayName())))))
                    .append("\n");
        }
        context.getSource().sendSuccess(message, false);
        return 1;
    }

    private static int getMissionsList(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.literal("All Registered Missions:\n").withStyle(ChatFormatting.GOLD);
        MissionRegistry.getAllMissions().forEach(mission -> message.append(Component.literal("- " + mission.id().toString()).withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, mission.title())))).append("\n"));
        context.getSource().sendSuccess(message, false);
        return 1;
    }

    private static int getFormsListForStyle(CommandSourceStack source, String styleName) {
        try {
            BreathingStyles style = BreathingStyles.valueOf(styleName.toUpperCase());
            MutableComponent message = Component.literal("Available Forms for " + style + ":\n").withStyle(ChatFormatting.GOLD);
            Arrays.stream(BreathingForms.values()).filter(form -> form.getStyle() == style)
                    .forEach(form -> message.append(Component.literal("- " + form.getDisplayName())
                                    .withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s form " + form.name().toLowerCase()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set your form to " + form.getDisplayName())))))
                            .append("\n"));
            source.sendSuccess(message, false);
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
    }

    //  HELP METHOD

    private static int showHelp(CommandSourceStack source, String topic) {
        MutableComponent help = Component.literal("\n Demon Slayer Help ").withStyle(ChatFormatting.GOLD);

        switch(topic.toLowerCase()) {
            case "get":
                help.append("\n/demonslayer get <target> [subcommand]")
                        .append("\n  - ").append(Component.literal("Summary of a player.").withStyle(ChatFormatting.GRAY))
                        .append("\n  - ").append(Component.literal("Subcommands: rank, style, breath, demon").withStyle(ChatFormatting.GRAY))
                        .append("\n/demonslayer get <list>")
                        .append("\n  - ").append(Component.literal("Lists all available options.").withStyle(ChatFormatting.GRAY))
                        .append("\n  - ").append(Component.literal("Lists: styles, ranks, missions, forms <style>").withStyle(ChatFormatting.GRAY));
                break;
            case "edit":
                help.append("\n/demonslayer edit <targets> <subcommand> ...")
                        .append("\n  - ").append(Component.literal("Modifies data for one or more players (@s, @a, etc).").withStyle(ChatFormatting.GRAY))
                        .append("\n  - ").append(Component.literal("Most numerical values accept 'add' or 'set' actions.").withStyle(ChatFormatting.GRAY));
                break;
            case "reset":
                help.append("\n/demonslayer reset <targets>")
                        .append("\n  - ").append(Component.literal("Resets all Demon Slayer data for player(s) to default.").withStyle(ChatFormatting.RED));
                break;
            case "nemesis":
                help.append("\n/demonslayer nemesis <subcommand> ...")
                        .append("\n  - ").append(Component.literal("Manage the Nemesis Demon system.").withStyle(ChatFormatting.GRAY))
                        .append("\n  - ").append(Component.literal("info <name>").withStyle(ChatFormatting.AQUA).append(Component.literal(": Get stats for a Nemesis.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("edit <name> ...").withStyle(ChatFormatting.AQUA).append(Component.literal(": Change Nemesis traits.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("hierarchy").withStyle(ChatFormatting.AQUA).append(Component.literal(": View the Twelve Kizuki roster.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("story [count]").withStyle(ChatFormatting.AQUA).append(Component.literal(": View recent story events.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("simulate_day").withStyle(ChatFormatting.AQUA).append(Component.literal(": Force a daily simulation cycle.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("battle start <challenger> <defender>").withStyle(ChatFormatting.AQUA).append(Component.literal(": Force a blood battle.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("battle end").withStyle(ChatFormatting.AQUA).append(Component.literal(": Force-end the current battle.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("battle send_hunter <name> <player>").withStyle(ChatFormatting.AQUA).append(Component.literal(": Spawn a Nemesis to hunt a player.").withStyle(ChatFormatting.GRAY)))
                        .append("\n  - ").append(Component.literal("social ...").withStyle(ChatFormatting.AQUA).append(Component.literal(": Packs/alliances summary, inspect, compatibility previews, and mobilization tools.").withStyle(ChatFormatting.GRAY)));
                break;
            default: // main help
                help.append("\nBase command for managing Demon Slayer data.")
                        .append("\nUse ").append(Component.literal("/demonslayer help <topic>").withStyle(ChatFormatting.YELLOW)
                                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer help "))))
                        .append(" for more info.")
                        .append("\nTopics: ")
                        .append(Component.literal("get").withStyle(ChatFormatting.AQUA)
                                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/demonslayer help get"))))
                        .append(", ").append(Component.literal("edit").withStyle(ChatFormatting.AQUA)
                                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/demonslayer help edit"))))
                        .append(", ").append(Component.literal("reset").withStyle(ChatFormatting.AQUA)
                                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/demonslayer help reset"))))
                        .append(", ").append(Component.literal("nemesis").withStyle(ChatFormatting.AQUA)
                                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/demonslayer help nemesis"))));
                break;
        }

        source.sendSuccess(help, false);
        return 1;
    }

    private static Optional<UUID> resolveDemonId(CommandSourceStack source, DemonHierarchyManager manager, String inputName, String argLabel) {
        Optional<UUID> resolved = manager.getDemonUUIDByName(inputName);
        if (resolved.isEmpty()) {
            source.sendFailure(Component.literal("No nemesis demon found for " + argLabel + ": " + inputName));
        }
        return resolved;
    }

    private static String getDemonNameOrFallback(DemonHierarchyManager manager, UUID demonId) {
        DemonTrait trait = manager.getDemonTrait(demonId);
        if (trait != null && trait.getName() != null && !trait.getName().isBlank()) {
            return trait.getName();
        }
        return demonId.toString().substring(0, 8);
    }

    private static int getRequestedAllies(CommandContext<CommandSourceStack> ctx, int fallback) {
        try {
            return IntegerArgumentType.getInteger(ctx, "requested_allies");
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int executeShowSocialSummary(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        List<DemonHierarchyManager.PackSnapshot> packs = manager.getPackSnapshots();
        List<DemonHierarchyManager.AllianceSnapshot> alliances = manager.getActiveAllianceSnapshots(level);

        int packedDemons = packs.stream().mapToInt(p -> p.memberIds().size()).sum();
        int allianceMembers = alliances.stream().mapToInt(a -> a.memberIds().size()).sum();

        MutableComponent msg = Component.literal("\nNemesis Social Summary\n").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Packs: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(packs.size())).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" | Packed demons: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(packedDemons)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\nActive alliances: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(alliances.size())).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" | Mobilized allies: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(allianceMembers)).withStyle(ChatFormatting.LIGHT_PURPLE));

        if (!packs.isEmpty()) {
            DemonHierarchyManager.PackSnapshot topPack = packs.get(0);
            msg.append(Component.literal("\nTop pack: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(topPack.leaderName()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" (size " + topPack.memberIds().size() + ", avg strength " + topPack.averageStrengthScore() + ")").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (!alliances.isEmpty()) {
            DemonHierarchyManager.AllianceSnapshot nextAlliance = alliances.get(0);
            long ticksLeft = Math.max(0L, nextAlliance.expiresAt() - level.getGameTime());
            msg.append(Component.literal("\nNext alliance expiry: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(nextAlliance.leaderName() + " in " + (ticksLeft / 20L) + "s").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        msg.append(Component.literal("\nUse /demonslayer nemesis social packs|alliances|inspect for details.").withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executeShowPacks(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());
        List<DemonHierarchyManager.PackSnapshot> packs = manager.getPackSnapshots();

        if (packs.isEmpty()) {
            source.sendSuccess(Component.literal("No active packs currently exist.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        MutableComponent msg = Component.literal("\nNemesis Packs\n").withStyle(ChatFormatting.GOLD);
        for (int i = 0; i < packs.size(); i++) {
            DemonHierarchyManager.PackSnapshot pack = packs.get(i);
            String members = String.join(", ", pack.memberNames());
            msg.append(Component.literal((i + 1) + ") ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(pack.leaderName()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" | size " + pack.memberIds().size() + " | avg strength " + pack.averageStrengthScore()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n   Members: " + members + "\n").withStyle(ChatFormatting.DARK_GRAY));
        }

        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executeShowAlliances(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);
        List<DemonHierarchyManager.AllianceSnapshot> alliances = manager.getActiveAllianceSnapshots(level);

        if (alliances.isEmpty()) {
            source.sendSuccess(Component.literal("No active temporary alliances.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        MutableComponent msg = Component.literal("\nActive Alliances\n").withStyle(ChatFormatting.GOLD);
        for (int i = 0; i < alliances.size(); i++) {
            DemonHierarchyManager.AllianceSnapshot alliance = alliances.get(i);
            long ticksLeft = Math.max(0L, alliance.expiresAt() - level.getGameTime());
            String members = String.join(", ", alliance.memberNames());
            String targetType = alliance.targetIsPlayer() ? "Player" : "Demon";

            msg.append(Component.literal((i + 1) + ") ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(alliance.leaderName()).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal(" -> " + targetType + ": " + alliance.targetName()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" | allies " + alliance.memberIds().size() + " | expires in " + (ticksLeft / 20L) + "s").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("\n   Members: " + members + "\n").withStyle(ChatFormatting.DARK_GRAY));
        }

        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executeInspectSocial(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        String demonName = StringArgumentType.getString(ctx, "demon_name");
        Optional<UUID> demonIdOpt = resolveDemonId(source, manager, demonName, "demon_name");
        if (demonIdOpt.isEmpty()) return 0;

        UUID demonId = demonIdOpt.get();
        String canonicalName = getDemonNameOrFallback(manager, demonId);

        List<DemonHierarchyManager.PackSnapshot> packs = manager.getPackSnapshots();
        List<DemonHierarchyManager.AllianceSnapshot> alliances = manager.getActiveAllianceSnapshots(level);

        MutableComponent msg = Component.literal("\nSocial Inspect: " + canonicalName + "\n").withStyle(ChatFormatting.GOLD);

        Optional<UUID> leaderOpt = manager.getPackLeaderFor(demonId);
        if (leaderOpt.isPresent()) {
            UUID leaderId = leaderOpt.get();
            DemonHierarchyManager.PackSnapshot pack = packs.stream().filter(p -> p.leaderId().equals(leaderId)).findFirst().orElse(null);
            if (pack != null) {
                boolean isLeader = leaderId.equals(demonId);
                msg.append(Component.literal("Pack role: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(isLeader ? "Leader" : "Member").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("\nLeader: " + pack.leaderName()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("\nPack size: " + pack.memberIds().size()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("\nPack members: " + String.join(", ", pack.memberNames()) + "\n").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            msg.append(Component.literal("Pack role: None\n").withStyle(ChatFormatting.GRAY));
        }

        List<DemonHierarchyManager.AllianceSnapshot> relevantAlliances = alliances.stream()
                .filter(a -> a.leaderId().equals(demonId) || a.memberIds().contains(demonId))
                .toList();

        if (relevantAlliances.isEmpty()) {
            msg.append(Component.literal("Alliance role: None").withStyle(ChatFormatting.GRAY));
        } else {
            msg.append(Component.literal("Alliance entries:\n").withStyle(ChatFormatting.GRAY));
            for (DemonHierarchyManager.AllianceSnapshot a : relevantAlliances) {
                boolean isLeader = a.leaderId().equals(demonId);
                long ticksLeft = Math.max(0L, a.expiresAt() - level.getGameTime());
                msg.append(Component.literal(" - " + (isLeader ? "Leader" : "Member") + " in " + a.leaderName() + " alliance vs " + a.targetName() + " (" + (ticksLeft / 20L) + "s)\n")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        source.sendSuccess(msg, false);
        return 1;
    }

    private static int executePreviewCompatibility(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        String demonA = StringArgumentType.getString(ctx, "demon_a");
        String demonB = StringArgumentType.getString(ctx, "demon_b");

        Optional<UUID> aIdOpt = resolveDemonId(source, manager, demonA, "demon_a");
        if (aIdOpt.isEmpty()) return 0;
        Optional<UUID> bIdOpt = resolveDemonId(source, manager, demonB, "demon_b");
        if (bIdOpt.isEmpty()) return 0;

        float base = manager.previewCompatibility(aIdOpt.get(), bIdOpt.get());
        if (base < -1.0f) {
            source.sendFailure(Component.literal("Could not evaluate compatibility for those demons."));
            return 0;
        }

        source.sendSuccess(Component.literal(String.format("Compatibility %s <-> %s: %.2f", demonA, demonB, base)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executePreviewCompatibilityVsPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        String leaderName = StringArgumentType.getString(ctx, "leader");
        String allyName = StringArgumentType.getString(ctx, "ally");
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        Optional<UUID> leaderIdOpt = resolveDemonId(source, manager, leaderName, "leader");
        if (leaderIdOpt.isEmpty()) return 0;
        Optional<UUID> allyIdOpt = resolveDemonId(source, manager, allyName, "ally");
        if (allyIdOpt.isEmpty()) return 0;

        float base = manager.previewCompatibility(leaderIdOpt.get(), allyIdOpt.get());
        float bonus = manager.previewMutualEnemyBonus(leaderIdOpt.get(), allyIdOpt.get(), target.getUUID(), true);
        float total = base + bonus;

        source.sendSuccess(Component.literal(String.format(
                "%s + %s vs player %s -> base %.2f, enemy bonus %.2f, total %.2f",
                leaderName, allyName, target.getGameProfile().getName(), base, bonus, total
        )).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int executePreviewCompatibilityVsDemon(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        String leaderName = StringArgumentType.getString(ctx, "leader");
        String allyName = StringArgumentType.getString(ctx, "ally");
        String targetName = StringArgumentType.getString(ctx, "target");

        Optional<UUID> leaderIdOpt = resolveDemonId(source, manager, leaderName, "leader");
        if (leaderIdOpt.isEmpty()) return 0;
        Optional<UUID> allyIdOpt = resolveDemonId(source, manager, allyName, "ally");
        if (allyIdOpt.isEmpty()) return 0;
        Optional<UUID> targetIdOpt = resolveDemonId(source, manager, targetName, "target");
        if (targetIdOpt.isEmpty()) return 0;

        float base = manager.previewCompatibility(leaderIdOpt.get(), allyIdOpt.get());
        float bonus = manager.previewMutualEnemyBonus(leaderIdOpt.get(), allyIdOpt.get(), targetIdOpt.get(), false);
        float total = base + bonus;

        source.sendSuccess(Component.literal(String.format(
                "%s + %s vs demon %s -> base %.2f, enemy bonus %.2f, total %.2f",
                leaderName, allyName, targetName, base, bonus, total
        )).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int executeMobilizePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        String leaderName = StringArgumentType.getString(ctx, "leader");
        Optional<UUID> leaderIdOpt = resolveDemonId(source, manager, leaderName, "leader");
        if (leaderIdOpt.isEmpty()) return 0;

        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int requestedAllies = getRequestedAllies(ctx, 2);

        Optional<DemonHierarchyManager.AllianceSnapshot> before = manager.getActiveAllianceSnapshots(level).stream()
                .filter(a -> a.leaderId().equals(leaderIdOpt.get()))
                .findFirst();

        manager.tryMobilizeAlliesForFight(leaderIdOpt.get(), target.getUUID(), true, level, target.blockPosition(), requestedAllies);

        Optional<DemonHierarchyManager.AllianceSnapshot> after = manager.getActiveAllianceSnapshots(level).stream()
                .filter(a -> a.leaderId().equals(leaderIdOpt.get()))
                .findFirst();

        if (after.isPresent()) {
            DemonHierarchyManager.AllianceSnapshot alliance = after.get();
            long ticksLeft = Math.max(0L, alliance.expiresAt() - level.getGameTime());
            if (before.isPresent() && before.get().expiresAt() == alliance.expiresAt()) {
                source.sendSuccess(Component.literal("Leader already has an active alliance targeting " + alliance.targetName() + " (" + (ticksLeft / 20L) + "s left).")
                        .withStyle(ChatFormatting.GRAY), false);
            } else {
                source.sendSuccess(Component.literal("Mobilized " + alliance.memberIds().size() + " allies for " + leaderName + " against player " + target.getGameProfile().getName() + ".")
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return 1;
        }

        source.sendFailure(Component.literal("No allies mobilized. Ensure compatible demons exist and the target is valid."));
        return 0;
    }

    private static int executeMobilizeDemon(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        String leaderName = StringArgumentType.getString(ctx, "leader");
        String targetName = StringArgumentType.getString(ctx, "target");

        Optional<UUID> leaderIdOpt = resolveDemonId(source, manager, leaderName, "leader");
        if (leaderIdOpt.isEmpty()) return 0;
        Optional<UUID> targetIdOpt = resolveDemonId(source, manager, targetName, "target");
        if (targetIdOpt.isEmpty()) return 0;

        net.minecraft.world.entity.Entity targetEntity = level.getEntity(targetIdOpt.get());
        if (!(targetEntity instanceof net.minecraft.world.entity.LivingEntity)) {
            source.sendFailure(Component.literal("Target demon must be spawned in-world before mobilization: " + targetName));
            return 0;
        }

        int requestedAllies = getRequestedAllies(ctx, 2);

        Optional<DemonHierarchyManager.AllianceSnapshot> before = manager.getActiveAllianceSnapshots(level).stream()
                .filter(a -> a.leaderId().equals(leaderIdOpt.get()))
                .findFirst();

        manager.tryMobilizeAlliesForFight(leaderIdOpt.get(), targetIdOpt.get(), false, level, targetEntity.blockPosition(), requestedAllies);

        Optional<DemonHierarchyManager.AllianceSnapshot> after = manager.getActiveAllianceSnapshots(level).stream()
                .filter(a -> a.leaderId().equals(leaderIdOpt.get()))
                .findFirst();

        if (after.isPresent()) {
            DemonHierarchyManager.AllianceSnapshot alliance = after.get();
            long ticksLeft = Math.max(0L, alliance.expiresAt() - level.getGameTime());
            if (before.isPresent() && before.get().expiresAt() == alliance.expiresAt()) {
                source.sendSuccess(Component.literal("Leader already has an active alliance targeting " + alliance.targetName() + " (" + (ticksLeft / 20L) + "s left).")
                        .withStyle(ChatFormatting.GRAY), false);
            } else {
                source.sendSuccess(Component.literal("Mobilized " + alliance.memberIds().size() + " allies for " + leaderName + " against demon " + targetName + ".")
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return 1;
        }

        source.sendFailure(Component.literal("No allies mobilized. Ensure compatible demons exist and the target is valid."));
        return 0;
    }

    private static int executeRebuildPacks(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        DemonHierarchyManager manager = DemonHierarchyManager.get(level);

        manager.forceRebuildPacks(level);
        List<DemonHierarchyManager.PackSnapshot> packs = manager.getPackSnapshots();

        source.sendSuccess(Component.literal("Rebuilt demon packs. Active packs: " + packs.size()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeDisbandPack(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        String leaderName = StringArgumentType.getString(ctx, "leader");
        Optional<UUID> leaderIdOpt = resolveDemonId(source, manager, leaderName, "leader");
        if (leaderIdOpt.isEmpty()) return 0;

        boolean removed = manager.disbandPack(leaderIdOpt.get());
        if (!removed) {
            source.sendFailure(Component.literal("No active pack found for leader " + leaderName + "."));
            return 0;
        }

        source.sendSuccess(Component.literal("Disbanded pack led by " + leaderName + ".").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int executeClearAlliances(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DemonHierarchyManager manager = DemonHierarchyManager.get(source.getLevel());

        int cleared = manager.clearActiveAlliances();
        manager.setDirty();
        source.sendSuccess(Component.literal("Cleared " + cleared + " active alliance(s).").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
}

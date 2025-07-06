package com.wassuples.demonslayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.DemonRank;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import com.wassuples.demonslayer.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DemonSlayerCommands {
    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingStyles.values()).map(s -> s.name().toLowerCase()), builder);
    private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(Enum::name).map(String::toLowerCase), builder);
    private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS = (ctx, builder) -> {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                    SharedSuggestionProvider.suggest(style.getFormsForCurrentStyle().map(form -> form.name().toLowerCase()), builder));
        } catch (Exception e) {
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingForms.values()).map(f -> f.name().toLowerCase()), builder);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MAX = (ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"max"}, builder);
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STYLE_ALL = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingStyles.values()).map(s -> s.name().toLowerCase()).collect(Collectors.toList()),
                    builder.suggest("all"));
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FORM_ALL = (ctx, builder) -> {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                    SharedSuggestionProvider.suggest(style.getFormsForCurrentStyle().map(form -> form.name().toLowerCase()), builder.suggest("all")));
        } catch (Exception e) {
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingForms.values()).map(f -> f.name().toLowerCase()), builder.suggest("all"));
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_RANK_MAX = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(Enum::name).map(String::toLowerCase), builder.suggest("max"));
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_DEMON_RANK_MAX = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DemonRank.values()).map(Enum::name).map(String::toLowerCase), builder.suggest("max"));


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("demonslayer")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getPlayerSummary(context.getSource(), EntityArgument.getPlayer(context, "target")))
                        )
                )

                .then(Commands.literal("edit")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("breath").then(Commands.argument("amount_or_max", StringArgumentType.word()).suggests(SUGGEST_MAX)
                                        .executes(ctx -> setPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "amount_or_max")))))
                                .then(Commands.literal("maxbreath").then(Commands.argument("amount", StringArgumentType.word()).suggests(SUGGEST_RANK_MAX)
                                        .executes(ctx -> setPlayerMaxBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "amount")))))
                                .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.word()).suggests(STYLE_SUGGESTIONS)
                                        .executes(context -> setPlayerStyle(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "style_name")))))
                                .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.word()).suggests(FORM_SUGGESTIONS)
                                        .executes(context -> setPlayerForm(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "form_name")))))
                                .then(Commands.literal("rank").then(Commands.argument("rank_or_max", StringArgumentType.word()).suggests(RANK_SUGGESTIONS)
                                        .executes(context -> setPlayerRank(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "rank_or_max")))))
                                .then(Commands.literal("mastery")
                                        .then(Commands.literal("style").then(Commands.argument("style_or_all", StringArgumentType.word()).suggests(SUGGEST_STYLE_ALL)
                                                .then(Commands.argument("amount_or_max", StringArgumentType.word())
                                                        .executes(ctx -> setPlayerStyleMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "style_or_all"), StringArgumentType.getString(ctx, "amount_or_max"))))))
                                        .then(Commands.literal("form").then(Commands.argument("form_or_all", StringArgumentType.word()).suggests(SUGGEST_FORM_ALL)
                                                .then(Commands.argument("amount_or_max", StringArgumentType.word())
                                                        .executes(ctx -> setPlayerFormMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "form_or_all"), StringArgumentType.getString(ctx, "amount_or_max")))))))
                                .then(Commands.literal("demon")
                                        .then(Commands.literal("set").executes(ctx -> setPlayerDemonStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), true)))
                                        .then(Commands.literal("remove").executes(ctx -> setPlayerDemonStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), false)))
                                        .then(Commands.literal("rank").then(Commands.argument("rank_or_max", StringArgumentType.word()).suggests(SUGGEST_DEMON_RANK_MAX)
                                                .executes(ctx -> setPlayerDemonRank(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "rank_or_max")))))
                                        .then(Commands.literal("blood").then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(ctx -> setPlayerDemonBlood(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "amount"))))))
                        )
                )

                .then(Commands.literal("list")
                        .then(Commands.literal("styles").executes(context -> listStyles(context.getSource())))
                        .then(Commands.literal("ranks").executes(context -> listRanks(context.getSource())))
                        .then(Commands.literal("missions").executes(context -> listMissions(context.getSource())))
                        .then(Commands.literal("forms")
                                .executes(ctx -> listFormsForCurrentStyle(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("style_name", StringArgumentType.word()).suggests(STYLE_SUGGESTIONS)
                                        .executes(ctx -> listFormsForSpecificStyle(ctx.getSource(), StringArgumentType.getString(ctx, "style_name")))))
                )

                .then(Commands.literal("unlock")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("style").then(Commands.argument("style_or_all", StringArgumentType.word()).suggests(SUGGEST_STYLE_ALL)
                                        .executes(ctx -> unlockPlayerStyle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "style_or_all")))))
                                .then(Commands.literal("form").then(Commands.argument("form_or_all", StringArgumentType.word()).suggests(SUGGEST_FORM_ALL)
                                        .executes(ctx -> unlockPlayerForm(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "form_or_all")))))
                        )
                )
        );
    }

    // --- GET METHODS ---

    private static int getPlayerSummary(CommandSourceStack source, ServerPlayer target) {
        MutableComponent message = Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY)
                .append("--- " + target.getDisplayName().getString() + "'s Stats ---\n").withStyle(ChatFormatting.GOLD);

        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                message.append(Component.literal("Race: ").withStyle(ChatFormatting.GRAY)).append(Component.literal("Demon").withStyle(ChatFormatting.RED));
                message.append(Component.literal("\nRank: ").withStyle(ChatFormatting.GRAY)).append(demon.getRank().getDisplayName());
                message.append(Component.literal("\nBlood Points: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(demon.getBloodPoints())).withStyle(ChatFormatting.DARK_RED));
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
        message.append(Component.literal("\n-------------------------").withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(message, false);
        return 1;
    }

    private static int getPlayerBreath(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s breath is " + breath.getBreath() + "/" + breath.getMaxBreath()), true));
        return 1;
    }

    private static int getPlayerStyle(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s style is " + style.getCurrentStyles()), true));
        return 1;
    }

    private static int getPlayerForm(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s current form is " + style.getCurrentForms().getDisplayName()), true));
        return 1;
    }

    private static int getPlayerRank(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s rank is ").append(progression.getRank().getDisplayName()), true));
        return 1;
    }

    private static int getPlayerMissions(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            MutableComponent message = Component.literal(target.getDisplayName().getString() + "'s Missions:").withStyle(ChatFormatting.GOLD);
            message.append(Component.literal("\n-- ACTIVE --").withStyle(ChatFormatting.YELLOW));
            if (missions.getActiveMissionIds().isEmpty()) {
                message.append(Component.literal("\nNone").withStyle(ChatFormatting.GRAY));
            } else {
                missions.getActiveMissionIds().forEach(id -> message.append(Component.literal("\n- " + id).withStyle(ChatFormatting.WHITE)));
            }
            source.sendSuccess(message, false);
        });
        return 1;
    }

    private static int getPlayerStyleMastery(CommandSourceStack source, ServerPlayer target, String styleName) {
        try {
            BreathingStyles style = BreathingStyles.valueOf(styleName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                int mastery = progression.getStyleMastery(style);
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s mastery for " + style.name() + " is " + mastery), true);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
    }

    private static int getPlayerFormMastery(CommandSourceStack source, ServerPlayer target, String formName) {
        try {
            BreathingForms form = BreathingForms.valueOf(formName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                int mastery = progression.getFormMastery(form);
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s mastery for " + form.getDisplayName() + " is " + mastery), true);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formName));
            return 0;
        }
    }

    private static int getPlayerDemonStatus(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            if (demon.isDemon()) {
                MutableComponent message = Component.literal(target.getDisplayName().getString() + " is a Demon.").withStyle(ChatFormatting.RED);
                message.append(Component.literal("\n- Rank: ").withStyle(ChatFormatting.GRAY)).append(demon.getRank().getDisplayName());
                message.append(Component.literal("\n- Blood Points: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(demon.getBloodPoints())).withStyle(ChatFormatting.DARK_RED));
                source.sendSuccess(message, false);
            } else {
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + " is a Human.").withStyle(ChatFormatting.AQUA), false);
            }
        });
        return 1;
    }

    // --- EDIT METHODS ---

    private static int setPlayerBreath(CommandSourceStack source, ServerPlayer target, String amountOrMax) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            if ("max".equalsIgnoreCase(amountOrMax)) {
                breath.setBreath(breath.getMaxBreath());
                source.sendSuccess(Component.literal("Filled " + target.getDisplayName().getString() + "'s breath"), true);
            } else {
                try {
                    int amount = Integer.parseInt(amountOrMax);
                    breath.setBreath(amount);
                    source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s current breath to " + amount), true);
                } catch (NumberFormatException e) {
                    source.sendFailure(Component.literal("Invalid amount: " + amountOrMax));
                }
            }
            ModUtils.syncBreath(target);
        });
        return 1;
    }

    private static int setPlayerMaxBreath(CommandSourceStack source, ServerPlayer target, String amountStr) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            try {
                int amount = Integer.parseInt(amountStr);
                if (amount < 1) {
                    source.sendFailure(Component.literal("Max breath must be at least 1."));
                    return;
                }
                breath.setMaxBreath(amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s max breath to " + amount), true);
                ModUtils.syncBreath(target);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Invalid amount: " + amountStr));
            }
        });
        return 1;
    }

    private static int addPlayerBreath(CommandSourceStack source, ServerPlayer target, int amount) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.addBreath(amount);
            source.sendSuccess(Component.literal("Modified " + target.getDisplayName().getString() + "'s breath by " + amount), true);
            ModUtils.syncBreath(target);
        });
        return 1;
    }

    private static int fillPlayerBreath(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.setBreath(breath.getMaxBreath());
            source.sendSuccess(Component.literal("Filled " + target.getDisplayName().getString() + "'s breath"), true);
            ModUtils.syncBreath(target);
        });
        return 1;
    }

    private static int setPlayerStyle(CommandSourceStack source, ServerPlayer target, String styleName) {
        try {
            BreathingStyles newStyle = BreathingStyles.valueOf(styleName.toUpperCase());
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                    if (!progression.isStyleUnlocked(newStyle)) {
                        progression.unlockStyle(newStyle);
                    }
                    style.changeStyle(newStyle.ordinal());
                    source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s style to " + newStyle.name()), true);
                    ModUtils.syncStyle(target);
                    ModUtils.syncProgression(target);
                });
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
    }

    private static int setPlayerForm(CommandSourceStack source, ServerPlayer target, String formName) {
        try {
            BreathingForms newForm = BreathingForms.valueOf(formName.toUpperCase());
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                if (newForm.getStyle() != style.getCurrentStyles()) {
                    source.sendFailure(Component.literal("Form " + newForm.name() + " is not part of the " + style.getCurrentStyles().name() + " style."));
                } else {
                    style.setFormIndex(newForm.ordinal());
                    source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s form to " + newForm.getDisplayName()), true);
                    ModUtils.syncStyle(target);
                }
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formName));
            return 0;
        }
    }

    private static int setPlayerRank(CommandSourceStack source, ServerPlayer target, String rankOrMax) {
        try {
            Rank newRank;
            if ("max".equalsIgnoreCase(rankOrMax)) {
                newRank = Rank.values()[Rank.values().length - 1];
            } else {
                newRank = Rank.valueOf(rankOrMax.toUpperCase());
            }
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                progression.setRank(newRank);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s rank to ").append(newRank.getDisplayName()), true);
                ModUtils.syncProgression(target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid rank name: " + rankOrMax));
            return 0;
        }
    }

    private static int setPlayerStyleMastery(CommandSourceStack source, ServerPlayer target, String styleOrAll, String amountOrMax) {
        try {
            int amount;
            if ("max".equalsIgnoreCase(amountOrMax)) {
                amount = 1000;
            } else {
                amount = Integer.parseInt(amountOrMax);
            }

            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                if ("all".equalsIgnoreCase(styleOrAll)) {
                    progression.getUnlockedStyles().forEach(style -> progression.setStyleMastery(style, amount));
                    source.sendSuccess(Component.literal("Set all of " + target.getDisplayName().getString() + "'s unlocked style masteries to " + amount), true);
                } else {
                    BreathingStyles style = BreathingStyles.valueOf(styleOrAll.toUpperCase());
                    progression.setStyleMastery(style, amount);
                    source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s mastery for " + style.name() + " to " + amount), true);
                }
                ModUtils.syncProgression(target);
            });
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid amount: " + amountOrMax));
            return 0;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleOrAll));
            return 0;
        }
    }

    private static int setPlayerFormMastery(CommandSourceStack source, ServerPlayer target, String formOrAll, String amountOrMax) {
        try {
            int amount;
            if ("max".equalsIgnoreCase(amountOrMax)) {
                amount = 100;
            } else {
                amount = Integer.parseInt(amountOrMax);
            }

            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    if ("all".equalsIgnoreCase(formOrAll)) {
                        Arrays.stream(BreathingForms.values())
                                .filter(f -> f.getStyle() == style.getCurrentStyles() && progression.isFormUnlocked(f))
                                .forEach(form -> progression.setFormMastery(form, amount));
                        source.sendSuccess(Component.literal("Set all of " + target.getDisplayName().getString() + "'s unlocked form masteries for the current style to " + amount), true);
                    } else {
                        BreathingForms form = BreathingForms.valueOf(formOrAll.toUpperCase());
                        progression.setFormMastery(form, amount);
                        source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s mastery for " + form.getDisplayName() + " to " + amount), true);
                    }
                    ModUtils.syncProgression(target);
                });
            });
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid amount: " + amountOrMax));
            return 0;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formOrAll));
            return 0;
        }
    }

    private static int setPlayerDemonStatus(CommandSourceStack source, ServerPlayer target, boolean isDemon) {
        target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            demon.setDemon(isDemon);
            source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + " demon status to " + isDemon), true);
            ModUtils.syncDemon(target);
        });
        return 1;
    }

    private static int setPlayerDemonRank(CommandSourceStack source, ServerPlayer target, String rankOrMax) {
        try {
            DemonRank newRank;
            if ("max".equalsIgnoreCase(rankOrMax)) {
                newRank = DemonRank.values()[DemonRank.values().length - 1];
            } else {
                newRank = DemonRank.valueOf(rankOrMax.toUpperCase());
            }
            target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) {
                    source.sendFailure(Component.literal(target.getDisplayName().getString() + " is not a demon."));
                    return;
                }
                demon.setRank(newRank);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s demon rank to ").append(newRank.getDisplayName()), true);
                ModUtils.syncDemon(target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid demon rank name: " + rankOrMax));
            return 0;
        }
    }

    private static int setPlayerDemonBlood(CommandSourceStack source, ServerPlayer target, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            target.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                if (!demon.isDemon()) {
                    source.sendFailure(Component.literal(target.getDisplayName().getString() + " is not a demon."));
                    return;
                }
                demon.setBloodPoints(amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s blood points to " + amount), true);
                ModUtils.syncDemon(target);
            });
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid amount: " + amountStr));
            return 0;
        }
    }

    // --- LIST METHODS ---

    private static int listStyles(CommandSourceStack source) {
        MutableComponent message = Component.literal("Available Breathing Styles:\n").withStyle(ChatFormatting.GOLD);
        for (BreathingStyles style : BreathingStyles.values()) {
            message.append(Component.literal("- " + style.toString()).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s style " + style.name().toLowerCase()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set your style to " + style))))).append("\n");
        }
        source.sendSuccess(message, false);
        return 1;
    }

    private static int listRanks(CommandSourceStack source) {
        MutableComponent message = Component.literal("Available Slayer Ranks:\n").withStyle(ChatFormatting.GOLD);
        for (Rank rank : Rank.values()) {
            message.append(Component.literal("- ").withStyle(ChatFormatting.GRAY))
                    .append(rank.getDisplayName().withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s rank " + rank.name().toLowerCase()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set your rank to ").append(rank.getDisplayName())))))
                    .append("\n");
        }
        source.sendSuccess(message, false);
        return 1;
    }

    private static int listMissions(CommandSourceStack source) {
        MutableComponent message = Component.literal("All Registered Missions:\n").withStyle(ChatFormatting.GOLD);
        MissionRegistry.getAllMissions().forEach(mission -> message.append(Component.literal("- " + mission.id().toString()).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/demonslayer get mission " + source.getPlayer().getScoreboardName() + " " + mission.id())) // This is a conceptual command; you'd need to implement it
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, mission.title())))).append("\n"));
        source.sendSuccess(message, false);
        return 1;
    }

    private static int listFormsForCurrentStyle(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        return target.getCapability(PlayerStyleProvider.PLAYER_STYLE).map(style -> {
            try {
                return listFormsForSpecificStyle(source, style.getCurrentStyles().name());
            } catch (CommandSyntaxException e) { return 0; }
        }).orElse(0);
    }

    private static int listFormsForSpecificStyle(CommandSourceStack source, String styleName) throws CommandSyntaxException {
        try {
            BreathingStyles style = BreathingStyles.valueOf(styleName.toUpperCase());
            MutableComponent message = Component.literal("Available Forms for " + style + ":\n").withStyle(ChatFormatting.GOLD);
            Arrays.stream(BreathingForms.values()).filter(form -> form.getStyle() == style)
                    .forEach(form -> message.append(Component.literal("- " + form.getDisplayName())
                                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
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

    // --- UNLOCK METHODS ---

    private static int unlockPlayerStyle(CommandSourceStack source, ServerPlayer target, String styleOrAll) {
        try {
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                if ("all".equalsIgnoreCase(styleOrAll)) {
                    Arrays.stream(BreathingStyles.values()).forEach(progression::unlockStyle);
                    source.sendSuccess(Component.literal("Unlocked all styles for " + target.getDisplayName().getString()), true);
                } else {
                    BreathingStyles styleToUnlock = BreathingStyles.valueOf(styleOrAll.toUpperCase());
                    if (progression.isStyleUnlocked(styleToUnlock)) {
                        source.sendFailure(Component.literal(target.getDisplayName().getString() + " already has that style unlocked."));
                    } else {
                        progression.unlockStyle(styleToUnlock);
                        source.sendSuccess(Component.literal("Unlocked " + styleToUnlock.name() + " for " + target.getDisplayName().getString()), true);
                    }
                }
                ModUtils.syncProgression(target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleOrAll));
            return 0;
        }
    }

    private static int unlockPlayerForm(CommandSourceStack source, ServerPlayer target, String formOrAll) {
        try {
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    if ("all".equalsIgnoreCase(formOrAll)) {
                        Arrays.stream(BreathingForms.values())
                                .filter(f -> f.getStyle() == style.getCurrentStyles())
                                .forEach(progression::unlockForm);
                        source.sendSuccess(Component.literal("Unlocked all forms for current style for " + target.getDisplayName().getString()), true);
                    } else {
                        BreathingForms formToUnlock = BreathingForms.valueOf(formOrAll.toUpperCase());
                        if (progression.isFormUnlocked(formToUnlock)) {
                            source.sendFailure(Component.literal(target.getDisplayName().getString() + " already has that form unlocked."));
                        } else {
                            progression.unlockForm(formToUnlock);
                            source.sendSuccess(Component.literal("Unlocked " + formToUnlock.getDisplayName() + " for " + target.getDisplayName().getString()), true);
                        }
                    }
                    ModUtils.syncProgression(target);
                });
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formOrAll));
            return 0;
        }
    }
}
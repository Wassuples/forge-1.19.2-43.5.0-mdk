package com.wassuples.demonslayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyle;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.events.ModEvents;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.PlayerBreathSyncS2CPacket;
import com.wassuples.demonslayer.networking.packet.PlayerMissionSyncS2CPacket;
import com.wassuples.demonslayer.networking.packet.PlayerProgressionSyncS2CPacket;
import com.wassuples.demonslayer.networking.packet.PlayerStyleSyncS2CPacket;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
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

    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingStyles.values()).map(s -> s.name().toLowerCase()), builder);

    private static final SuggestionProvider<CommandSourceStack> RANK_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(Enum::name).map(String::toLowerCase), builder);

    private static final SuggestionProvider<CommandSourceStack> MISSION_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(MissionRegistry.getAllMissions().stream().map(m -> m.id().toString()), builder);

    private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS = (context, builder) -> {
        try {
            // Determine the target player. If the 'target' argument exists, use it. Otherwise, use the command sender.
            ServerPlayer target = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("target"))
                    ? EntityArgument.getPlayer(context, "target")
                    : context.getSource().getPlayerOrException();

            // Provide suggestions based on the target's current style.
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                    SharedSuggestionProvider.suggest(style.getFormsForCurrentStyle().map(form -> form.name().toLowerCase()), builder));
        } catch (Exception e) {
            // Fallback if the target can't be determined.
            SharedSuggestionProvider.suggest(Arrays.stream(BreathingForms.values()).map(f -> f.name().toLowerCase()), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("demonslayer")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("get")
                        .executes(context -> getPlayerSummary(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getPlayerSummary(context.getSource(), EntityArgument.getPlayer(context, "target")))
                                .then(Commands.literal("style").executes(context -> getPlayerStyle(context.getSource(), EntityArgument.getPlayer(context, "target"))))
                                .then(Commands.literal("form").executes(context -> getPlayerForm(context.getSource(), EntityArgument.getPlayer(context, "target"))))
                                .then(Commands.literal("rank").executes(context -> getPlayerRank(context.getSource(), EntityArgument.getPlayer(context, "target"))))
                                .then(Commands.literal("missions").executes(context -> getPlayerMissions(context.getSource(), EntityArgument.getPlayer(context, "target"))))
                                .then(Commands.literal("mastery")
                                        .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                                .executes(ctx -> getPlayerStyleMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "style_name")))))
                                        .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
                                                .executes(ctx -> getPlayerFormMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "form_name"))))))
                                .then(Commands.literal("breath")
                                        .then(Commands.literal("current").executes(context -> getPlayerBreath(context.getSource(), EntityArgument.getPlayer(context, "target"), "current")))
                                        .then(Commands.literal("max").executes(context -> getPlayerBreath(context.getSource(), EntityArgument.getPlayer(context, "target"), "max"))))))

                .then(Commands.literal("edit")
                        // This block handles editing YOURSELF (@s)
                        .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                .executes(context -> setPlayerStyle(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "style_name")))))
                        .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
                                .executes(context -> setPlayerForm(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "form_name")))))
                        .then(Commands.literal("rank").then(Commands.argument("rank_name", StringArgumentType.string()).suggests(RANK_SUGGESTIONS)
                                .executes(context -> setPlayerRank(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "rank_name")))))
                        .then(Commands.literal("mission")
                                .then(Commands.argument("mission_id", ResourceLocationArgument.id()).suggests(MISSION_SUGGESTIONS)
                                        .then(Commands.literal("accept").executes(ctx -> acceptPlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))
                                        .then(Commands.literal("complete").executes(ctx -> completePlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))
                                        .then(Commands.literal("abandon").executes(ctx -> abandonPlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))))
                        .then(Commands.literal("mastery")
                                .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setPlayerStyleMastery(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "style_name"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                                .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> setPlayerFormMastery(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "form_name"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                        // This block handles editing a TARGET player
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                        .executes(context -> setPlayerStyle(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "style_name")))))
                                .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
                                        .executes(context -> setPlayerForm(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "form_name")))))
                                .then(Commands.literal("mission")
                                        .then(Commands.argument("mission_id", ResourceLocationArgument.id()).suggests(MISSION_SUGGESTIONS)
                                                .then(Commands.literal("accept").executes(ctx -> acceptPlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))
                                                .then(Commands.literal("complete").executes(ctx -> completePlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))
                                                .then(Commands.literal("abandon").executes(ctx -> abandonPlayerMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), ResourceLocationArgument.getId(ctx, "mission_id"))))))
                                .then(Commands.literal("rank").then(Commands.argument("rank_name", StringArgumentType.string()).suggests(RANK_SUGGESTIONS)
                                        .executes(context -> setPlayerRank(context.getSource(), EntityArgument.getPlayer(context, "target"), StringArgumentType.getString(context, "rank_name")))))
                                .then(Commands.literal("mastery")
                                        .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setPlayerStyleMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "style_name"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                                        .then(Commands.literal("form").then(Commands.argument("form_name", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setPlayerFormMastery(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "form_name"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                                .then(Commands.literal("breath")
                                        .then(Commands.literal("set")
                                                .then(Commands.literal("current").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> setPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), "current", IntegerArgumentType.getInteger(ctx, "amount")))))
                                                .then(Commands.literal("max").then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> setPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), "max", IntegerArgumentType.getInteger(ctx, "amount"))))))
                                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> addPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType.getInteger(ctx, "amount")))))
                                        .then(Commands.literal("remove").then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> removePlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType.getInteger(ctx, "amount")))))
                                        .then(Commands.literal("fill").executes(ctx -> fillPlayerBreath(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))))

                .then(Commands.literal("unlock")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("style").then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                        .executes(ctx -> unlockPlayerStyle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "style_name")))))))

                .then(Commands.literal("list")
                        .then(Commands.literal("styles").executes(context -> listStyles(context.getSource())))
                        .then(Commands.literal("ranks").executes(context -> listRanks(context.getSource())))
                        .then(Commands.literal("missions").executes(context -> listMissions(context.getSource())))
                        .then(Commands.literal("forms")
                                .executes(context -> listForms(context.getSource(), context.getSource().getPlayerOrException().getCapability(PlayerStyleProvider.PLAYER_STYLE).map(PlayerStyle::getCurrentStyles).orElse(null)))
                                .then(Commands.argument("style_name", StringArgumentType.string()).suggests(STYLE_SUGGESTIONS)
                                        .executes(context -> listForms(context.getSource(), BreathingStyles.valueOf(StringArgumentType.getString(context, "style_name").toUpperCase()))))))
        );
    }

    private static int getPlayerSummary(CommandSourceStack source, ServerPlayer target) {
        MutableComponent message = Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY)
                .append("------------------------------------")
                .append(Component.literal("\nDemon Slayer Stats for ").withStyle(ChatFormatting.GOLD).append(target.getDisplayName()));

        target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                    BreathingStyles currentStyle = style.getCurrentStyles();
                    BreathingForms currentForm = style.getCurrentForms();

                    message.append(Component.literal("\n Rank: ").withStyle(ChatFormatting.GRAY)).append(progression.getRank().getDisplayName());
                    message.append(Component.literal("\n Style: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(currentStyle.toString()).withStyle(ChatFormatting.AQUA));
                    message.append(Component.literal("\n  - Style Mastery: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(progression.getStyleMastery(currentStyle))).withStyle(ChatFormatting.LIGHT_PURPLE));
                    message.append(Component.literal("\n Active Form: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(currentForm.getDisplayName()).withStyle(ChatFormatting.YELLOW));
                    message.append(Component.literal("\n  - Form Mastery: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(String.valueOf(progression.getFormMastery(currentForm))).withStyle(ChatFormatting.LIGHT_PURPLE));
                    message.append(Component.literal("\n Breath: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(breath.getBreath() + " / " + breath.getMaxBreath()).withStyle(ChatFormatting.GREEN));
                });
            });
        });
        message.append(Component.literal("\n------------------------------------").withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(message, false);
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

            message.append(Component.literal("\n-- COMPLETED --").withStyle(ChatFormatting.GREEN));
            if (missions.getCompletedMissionIds().isEmpty()) {
                message.append(Component.literal("\nNone").withStyle(ChatFormatting.GRAY));
            } else {
                missions.getCompletedMissionIds().forEach(id -> message.append(Component.literal("\n- " + id).withStyle(ChatFormatting.DARK_GRAY)));
            }

            source.sendSuccess(message, false);
        });
        return 1;
    }

    private static int listMissions(CommandSourceStack source) {
        MutableComponent message = Component.literal("All Registered Missions:\n").withStyle(ChatFormatting.GOLD);
        MissionRegistry.getAllMissions().forEach(mission -> {
            message.append(Component.literal("- " + mission.id().toString()).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit " + source.getPlayer().getScoreboardName() + " mission " + mission.id() + " accept"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, mission.title()))))
                    .append("\n");
        });
        source.sendSuccess(message, false);
        return 1;
    }

    private static int acceptPlayerMission(CommandSourceStack source, ServerPlayer target, ResourceLocation missionId) {
        Mission mission = MissionRegistry.getMission(missionId);
        if (mission == null) {
            source.sendFailure(Component.literal("Unknown mission ID: " + missionId));
            return 0;
        }
        target.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            missions.acceptMission(mission, target);
            source.sendSuccess(Component.literal("Accepted mission '").append(mission.title()).append("' for " + target.getDisplayName().getString()), true);
            // Sync data
            CompoundTag missionData = new CompoundTag();
            missions.saveNBTData(missionData);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), target);
        });
        return 1;
    }

    private static int completePlayerMission(CommandSourceStack source, ServerPlayer target, ResourceLocation missionId) {
        Mission mission = MissionRegistry.getMission(missionId);
        if (mission == null) {
            source.sendFailure(Component.literal("Unknown mission ID: " + missionId));
            return 0;
        }
        target.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            if (!missions.isActive(missionId)) {
                source.sendFailure(Component.literal(target.getDisplayName().getString() + " does not have that mission active."));
                return;
            }
            // Grant rewards and complete
            mission.rewards().forEach(reward -> reward.grant(target));
            missions.completeMission(mission);
            source.sendSuccess(Component.literal("Force-completed mission '").append(mission.title()).append("' for " + target.getDisplayName().getString()), true);
            // Sync data
            CompoundTag missionData = new CompoundTag();
            missions.saveNBTData(missionData);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), target);
        });
        return 1;
    }

    private static int abandonPlayerMission(CommandSourceStack source, ServerPlayer target, ResourceLocation missionId) {
        target.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(missions -> {
            if (!missions.isActive(missionId)) {
                source.sendFailure(Component.literal(target.getDisplayName().getString() + " does not have that mission active."));
                return;
            }
            missions.abandonMission(missionId);
            source.sendSuccess(Component.literal("Abandoned mission " + missionId + " for " + target.getDisplayName().getString()), true);
            // Sync data
            CompoundTag missionData = new CompoundTag();
            missions.saveNBTData(missionData);
            ModMessages.sendToPlayer(new PlayerMissionSyncS2CPacket(missionData), target);
        });
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

    private static int getPlayerBreath(CommandSourceStack source, ServerPlayer target, String type) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            if ("current".equals(type)) {
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s current breath is " + breath.getBreath()), true);
            } else if ("max".equals(type)) {
                source.sendSuccess(Component.literal(target.getDisplayName().getString() + "'s max breath is " + breath.getMaxBreath()), true);
            }
        });
        return 1;
    }

    private static int listStyles(CommandSourceStack source) {
        MutableComponent message = Component.literal("Available Breathing Styles:\n").withStyle(ChatFormatting.GOLD);
        for (BreathingStyles style : BreathingStyles.values()) {
            message.append(Component.literal("- " + style.toString()).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s style " + style.name().toLowerCase()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set style to " + style))))).append("\n");
        }
        source.sendSuccess(message, false);
        return 1;
    }

    private static int listRanks(CommandSourceStack source) {
        MutableComponent message = Component.literal("Available Ranks:\n").withStyle(ChatFormatting.GOLD);
        for (Rank rank : Rank.values()) {
            message.append(Component.literal("- ").withStyle(ChatFormatting.GRAY))
                    .append(rank.getDisplayName().withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s rank " + rank.name().toLowerCase()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to suggest setting rank to ").append(rank.getDisplayName())))))
                    .append("\n");
        }
        source.sendSuccess(message, false);
        return 1;
    }

    private static int listForms(CommandSourceStack source, BreathingStyles style) {
        if (style == null) {
            source.sendFailure(Component.literal("Could not determine a style. Specify one or have the target player select one."));
            return 0;
        }
        MutableComponent message = Component.literal("Available Forms for " + style + ":\n").withStyle(ChatFormatting.GOLD);
        Arrays.stream(BreathingForms.values()).filter(form -> form.getStyle() == style)
                .forEach(form -> message.append(Component.literal("- " + form.getDisplayName())
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/demonslayer edit @s form " + form.name().toLowerCase()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set form to " + form.getDisplayName())))))
                        .append("\n"));
        source.sendSuccess(message, false);
        return 1;
    }

    private static int unlockPlayerStyle(CommandSourceStack source, ServerPlayer target, String styleName) {
        try {
            BreathingStyles styleToUnlock = BreathingStyles.valueOf(styleName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                if (progression.isStyleUnlocked(styleToUnlock)) {
                    source.sendFailure(Component.literal(target.getDisplayName().getString() + " already has the " + styleToUnlock.name() + " style unlocked."));
                } else {
                    progression.unlockStyle(styleToUnlock);
                    source.sendSuccess(Component.literal("Unlocked " + styleToUnlock.name() + " for " + target.getDisplayName().getString()), true);
                    ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(target.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), target);
                }
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
    }

    private static int setPlayerStyle(CommandSourceStack source, ServerPlayer target, String styleName) {
        try {
            BreathingStyles newStyle = BreathingStyles.valueOf(styleName.toUpperCase());
            target.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                    style.changeStyle(newStyle.ordinal());
                    progression.unlockStyle(newStyle);
                    source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s style to " + newStyle.name()), true);
                    ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), target);
                    ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(target.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), target);
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
                    ModMessages.sendToPlayer(new PlayerStyleSyncS2CPacket(style.getForm(), style.getStyleIndex()), target);
                }
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formName));
            return 0;
        }
    }

    private static int setPlayerRank(CommandSourceStack source, ServerPlayer target, String rankName) {
        try {
            Rank newRank = Rank.valueOf(rankName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                progression.setRank(newRank);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s rank to ").append(newRank.getDisplayName()), true);
                ModEvents.syncProgression(target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid rank name: " + rankName));
            return 0;
        }
    }

    private static int setPlayerStyleMastery(CommandSourceStack source, ServerPlayer target, String styleName, int amount) {
        try {
            BreathingStyles style = BreathingStyles.valueOf(styleName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                progression.unlockStyle(style);
                progression.setStyleMastery(style, amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s mastery for " + style.name() + " to " + amount), true);
                ModEvents.syncProgression(target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid style name: " + styleName));
            return 0;
        }
    }

    private static int setPlayerFormMastery(CommandSourceStack source, ServerPlayer target, String formName, int amount) {
        try {
            BreathingForms form = BreathingForms.valueOf(formName.toUpperCase());
            target.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                progression.setFormMastery(form, amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s mastery for " + form.getDisplayName() + " to " + amount), true);
                ModMessages.sendToPlayer(new PlayerProgressionSyncS2CPacket(target.getId(), progression.getRank(), progression.getUnlockedStyles(), progression.getStyleMasteryMap(), progression.getFormMasteryMap()), target);
            });
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid form name: " + formName));
            return 0;
        }
    }

    private static int setPlayerBreath(CommandSourceStack source, ServerPlayer target, String type, int amount) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            if ("current".equals(type)) {
                breath.setBreath(amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s current breath to " + amount), true);
            } else if ("max".equals(type)) {
                breath.setMaxBreath(amount);
                source.sendSuccess(Component.literal("Set " + target.getDisplayName().getString() + "'s max breath to " + amount), true);
            }
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), target);
        });
        return 1;
    }

    private static int addPlayerBreath(CommandSourceStack source, ServerPlayer target, int amount) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.addBreath(amount);
            source.sendSuccess(Component.literal("Added " + amount + " to " + target.getDisplayName().getString() + "'s breath. New total: " + breath.getBreath()), true);
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), target);
        });
        return 1;
    }

    private static int removePlayerBreath(CommandSourceStack source, ServerPlayer target, int amount) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.subBreath(amount);
            source.sendSuccess(Component.literal("Removed " + amount + " from " + target.getDisplayName().getString() + "'s breath. New total: " + breath.getBreath()), true);
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), target);
        });
        return 1;
    }

    private static int fillPlayerBreath(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
            breath.setBreath(breath.getMaxBreath());
            source.sendSuccess(Component.literal("Filled " + target.getDisplayName().getString() + "'s breath to max (" + breath.getBreath() + ")"), true);
            ModMessages.sendToPlayer(new PlayerBreathSyncS2CPacket(breath.getBreath(), breath.getMaxBreath()), target);
        });
        return 1;
    }
}
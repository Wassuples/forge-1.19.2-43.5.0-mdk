package com.wassuples.demonslayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wassuples.demonslayer.missions.MissionManager;
import com.wassuples.demonslayer.missions.MissionRegistry;
import com.wassuples.demonslayer.missions.PlayerMissions;
import com.wassuples.demonslayer.missions.PlayerMissionsProvider;
import com.wassuples.demonslayer.missions.StoryMission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /demonslayer mission ...} (OP 2) — inspect and drive story missions for testing.
 * <ul>
 *   <li>{@code status <player>}</li>
 *   <li>{@code start <player> <mission>} — skips prerequisites; forfeits any active mission first</li>
 *   <li>{@code complete <player> <outcome>} — e.g. {@code survived}, {@code hand_demon_slain}</li>
 *   <li>{@code advance <player>} — next step (Final Selection: begin trial / begin night / survive night)</li>
 *   <li>{@code fail <player>} / {@code reset <player> [mission]}</li>
 *   <li>{@code deliver <player>} — the pending Nichirin blade arrives now</li>
 * </ul>
 * Final Selection counts only naturally lived nights, so /time jumps don't advance it; use {@code advance}.
 *
 * <p>{@code /demonslayer fujikasane locate|tp|place} (OP 2) — find this world's Mt. Fujikasane, go to its shrine, or
 * raise one at your position (for worlds made before the mountain existed; heavy).
 */
public final class MissionCommands {
    private static final SuggestionProvider<CommandSourceStack> MISSIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(MissionRegistry.getAllMissions().stream().filter(StoryMission::isPlayable).map(StoryMission::id), builder);

    private MissionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("demonslayer")
                .then(Commands.literal("mission").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player())
                                .executes(MissionCommands::status)))
                        .then(Commands.literal("start").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("mission", ResourceLocationArgument.id()).suggests(MISSIONS)
                                        .executes(MissionCommands::start))))
                        .then(Commands.literal("complete").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("outcome", StringArgumentType.word())
                                        .executes(MissionCommands::complete))))
                        .then(Commands.literal("fail").then(Commands.argument("player", EntityArgument.player())
                                .executes(MissionCommands::fail)))
                        .then(Commands.literal("advance").then(Commands.argument("player", EntityArgument.player())
                                .executes(MissionCommands::advance)))
                        .then(Commands.literal("deliver").then(Commands.argument("player", EntityArgument.player())
                                .executes(MissionCommands::deliver)))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx, null))
                                .then(Commands.argument("mission", ResourceLocationArgument.id()).suggests(MISSIONS)
                                        .executes(ctx -> reset(ctx, ResourceLocationArgument.getId(ctx, "mission")))))))
                .then(Commands.literal("fujikasane").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("locate").executes(MissionCommands::locate))
                        .then(Commands.literal("tp").executes(MissionCommands::teleport))
                        .then(Commands.literal("place").executes(MissionCommands::place))));
    }

    private static int deliver(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerMissions data = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).orElse(null);
        if (data == null || !data.hasTimer(com.wassuples.demonslayer.missions.story.FinalSelectionMission.BLADE_DUE)) {
            ctx.getSource().sendFailure(Component.literal("No blade is on its way."));
            return 0;
        }
        com.wassuples.demonslayer.missions.story.FinalSelectionMission.deliverBlade(player, data);
        ctx.getSource().sendSuccess(Component.literal("Delivered."), true);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx) {
        com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout layout =
                com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites.find(ctx.getSource().getServer());
        if (layout == null) {
            ctx.getSource().sendFailure(Component.literal("This world has no Mt. Fujikasane (its site was generated before the mountain existed). Use /demonslayer fujikasane place."));
            return 0;
        }
        net.minecraft.core.BlockPos p = layout.plazaCenter();
        ctx.getSource().sendSuccess(Component.literal("Mt. Fujikasane: summit " + layout.centerX + ", " + layout.centerZ
                + "; shrine " + p.getX() + " " + p.getY() + " " + p.getZ()), false);
        return 1;
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout layout =
                com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites.find(ctx.getSource().getServer());
        if (layout == null) {
            ctx.getSource().sendFailure(Component.literal("This world has no Mt. Fujikasane."));
            return 0;
        }
        net.minecraft.core.BlockPos p = layout.plazaCenter();
        player.teleportTo(ctx.getSource().getServer().overworld(), p.getX() + 4.5, p.getY(), p.getZ() + 0.5, 90F, 0F);
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (player.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            ctx.getSource().sendFailure(Component.literal("Mt. Fujikasane can only stand in the overworld."));
            return 0;
        }
        com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneLayout layout =
                com.wassuples.demonslayer.worldgen.fujikasane.FujikasaneSites.placeAt(player.getLevel(), player.blockPosition());
        net.minecraft.core.BlockPos p = layout.plazaCenter();
        player.teleportTo(player.getLevel(), p.getX() + 4.5, p.getY(), p.getZ() + 0.5, 90F, 0F);
        ctx.getSource().sendSuccess(Component.literal("Raised Mt. Fujikasane around you; shrine at " + p.getX() + " " + p.getY() + " " + p.getZ()), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerMissions data = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).orElse(null);
        if (data == null) return 0;
        MutableComponent msg = Component.literal("Missions for " + player.getGameProfile().getName() + "\n").withStyle(ChatFormatting.GOLD);
        msg.append(Component.literal("Active: " + (data.hasActive() ? data.getActiveId() + " " + data.getActiveState() : "none") + "\n").withStyle(ChatFormatting.WHITE));
        data.getCompleted().forEach((id, outcome) -> msg.append(Component.literal("  done " + id + " -> " + outcome + "\n").withStyle(ChatFormatting.GREEN)));
        msg.append(Component.literal("Perks: " + data.getPerks()).withStyle(ChatFormatting.AQUA));
        ctx.getSource().sendSuccess(msg, false);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "mission");
        Component refusal = MissionManager.tryAccept(player, id, true);
        if (refusal != null) {
            ctx.getSource().sendFailure(refusal);
            return 0;
        }
        ctx.getSource().sendSuccess(Component.literal("Started " + id), true);
        return 1;
    }

    private static int complete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!MissionManager.forceComplete(player, StringArgumentType.getString(ctx, "outcome"))) {
            ctx.getSource().sendFailure(Component.literal("No active mission."));
            return 0;
        }
        ctx.getSource().sendSuccess(Component.literal("Completed."), true);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!MissionManager.debugAdvance(player)) {
            ctx.getSource().sendFailure(Component.literal("No active mission."));
            return 0;
        }
        ctx.getSource().sendSuccess(Component.literal("Advanced."), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerMissions data = player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).orElse(null);
        if (data == null || !data.hasActive() || !MissionManager.abandon(player, data.getActiveId(), false)) {
            ctx.getSource().sendFailure(Component.literal("No active mission."));
            return 0;
        }
        ctx.getSource().sendSuccess(Component.literal("Mission forfeited."), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        MissionManager.reset(player, id);
        ctx.getSource().sendSuccess(Component.literal("Reset " + (id == null ? "all missions" : id.toString())), true);
        return 1;
    }
}

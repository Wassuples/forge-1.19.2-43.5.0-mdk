package com.wassuples.demonslayer.missions;

import com.google.common.collect.ImmutableList;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.ranking.PlayerProgression;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class MissionRegistry {
    private static final Map<ResourceLocation, Mission> MISSIONS = new HashMap<>();

    public static void register() {
        // RANK UP MISSIONS
        registerMission(new Mission(
                new ResourceLocation(DemonSlayer.MOD_ID, "rank_up_final_selection"),
                MissionType.RANK_UP,
                Component.literal("Rank Up: Final Selection"),
                ImmutableList.of(
                        Component.literal("To officially join the Demon Slayer Corps, you must survive seven nights on Mt. Fujikasane.")
                ),
                ImmutableList.of(new SurviveObjective(24000 * 7)), // 7 in-game days
                ImmutableList.of(
                        new RankReward(Rank.MIZUNOTO),
                        new ItemReward(() -> ModItems.NICHIRIN_BLADE.get(), 1)
                ),
                Rank.MIZUNOTO // This is the rank you are trying to achieve
        ));

        // RADIANT QUESTS
        registerMission(new Mission(
                new ResourceLocation(DemonSlayer.MOD_ID, "radiant_kill_zombies"),
                MissionType.RADIANT_QUEST,
                Component.literal("Thin the Horde"),
                ImmutableList.of(Component.literal("The undead are becoming a nuisance.")),
                ImmutableList.of(new KillObjective(EntityType.ZOMBIE, 10)),
                ImmutableList.of(new StyleMasteryReward(null, 25)),
                Rank.MIZUNOTO
        ));
    }

    private static void registerMission(Mission mission) {
        MISSIONS.put(mission.id(), mission);
    }

    public static Mission getMission(ResourceLocation id) {
        return MISSIONS.get(id);
    }

    public static List<Mission> getAllMissions() {
        return List.copyOf(MISSIONS.values());
    }

    public static List<Mission> getAvailableMissionsForPlayer(ServerPlayer player) {
        final List<Mission> availableMissions = new ArrayList<>();

        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(playerMissions -> {

                List<Mission> filteredMissions = MISSIONS.values().stream()
                        .filter(mission -> {
                            // Is the mission already active or completed?
                            if (playerMissions.isActive(mission.id()) || playerMissions.hasCompleted(mission.id())) {
                                return false;
                            }
                            // Does the player meet the rank requirement?
                            if (mission.type() == MissionType.RANK_UP) {
                                // For rank-up quests, you can start it if you are the rank BELOW it.
                                return progression.getRank().ordinal() == mission.requirement().ordinal() - 1;
                            }
                            // For other quests, you need to be at least that rank.
                            return progression.getRank().ordinal() >= mission.requirement().ordinal();
                        })
                        .collect(Collectors.toList());

                availableMissions.addAll(filteredMissions);
            });
        });

        return availableMissions;
    }
}
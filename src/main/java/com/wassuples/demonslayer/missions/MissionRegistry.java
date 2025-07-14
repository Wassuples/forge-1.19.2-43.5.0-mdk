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
        registerMission(new Mission(
                new ResourceLocation(DemonSlayer.MOD_ID, "story_final_selection"),
                MissionType.STORY,
                Component.literal("Final Selection"),
                ImmutableList.of(
                        Component.literal("To join the Demon Slayer Corps, you must survive for seven nights on wisteria-ringed Mt. Fujikasane, a prison for captured demons."),
                        Component.literal("Fleeing the mountain will result in failure.")
                ),
                ImmutableList.of(new SurviveInBiomeObjective(new ResourceLocation(DemonSlayer.MOD_ID, "fujikasane_forest"), 24000 * 7)),
                ImmutableList.of(
                        new RankReward(Rank.MIZUNOTO),
                        new ItemReward(() -> ModItems.NICHIRIN_BLADE.get(), 1),
                        StyleMasteryReward.current(150)
                ),
                Rank.CIVILIAN
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
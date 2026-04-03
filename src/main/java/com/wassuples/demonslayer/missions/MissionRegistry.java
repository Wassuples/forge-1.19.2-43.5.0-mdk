package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Global server-side registry of all available {@link Mission} instances.
 *
 * <p>Populated by {@link MissionReloadListener} from JSON datapacks on server start
 * and {@code /reload}. Synced to clients on datapack sync events.
 *
 * <p>Provides lookup by ID, filtering by rank requirement, and random mission selection.
 *
 * @see Mission
 * @see MissionReloadListener
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID)
public class MissionRegistry {
    private static final Map<ResourceLocation, Mission> MISSIONS = new HashMap<>();

    static void loadAll(Collection<MissionDefinition> definitions) {
        MISSIONS.clear();
        definitions.stream().map(Mission::fromDefinition).forEach(mission -> MISSIONS.put(mission.id(), mission));
    }

    public static Mission getMission(ResourceLocation id) {
        return MISSIONS.get(id);
    }

    public static List<Mission> getAllMissions() {
        return List.copyOf(MISSIONS.values());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // TODO: send mission registry packets to clients
    }

    public static List<Mission> getAvailableMissionsForPlayer(ServerPlayer player) {
        final List<Mission> availableMissions = new ArrayList<>();

        player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(playerMissions -> {

                    List<Mission> filteredMissions = MISSIONS.values().stream()
                            .filter(mission -> {
                                if (playerMissions.isActive(mission.id()) || playerMissions.isCompleted(mission.id())) {
                                    return false;
                                }
                                if (mission.type() == MissionType.RANK_UP) {
                                    return progression.getRank().ordinal() == mission.requirement().ordinal() - 1;
                                }
                                return progression.getRank().ordinal() >= mission.requirement().ordinal();
                            })
                            .collect(Collectors.toList());

                    availableMissions.addAll(filteredMissions);
                })
        );

        return availableMissions;
    }
}

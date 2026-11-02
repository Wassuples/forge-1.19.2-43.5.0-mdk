package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import com.wassuples.demonslayer.missions.story.PlannedMission;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All story missions, in order. Missions are code-defined scripts (see {@link StoryMission}); the ones not built yet
 * are registered as {@link PlannedMission} placeholders so the crow's list shows the whole story.
 */
public final class MissionRegistry {
    private static final Map<ResourceLocation, StoryMission> MISSIONS = new LinkedHashMap<>();

    public static final ResourceLocation FINAL_SELECTION_ID = FinalSelectionMission.ID;

    static {
        register(FinalSelectionMission.INSTANCE);
        Object[][] planned = {
                {"kidnappers_bog", 1, Arc.II}, {"asakusa", 2, Arc.III}, {"tamayo", 3, Arc.III},
                {"tsuzumi_mansion", 4, Arc.IV}, {"mt_natagumo", 5, Arc.V}, {"mothers_doll", 6, Arc.V},
                {"spider_family", 7, Arc.V}, {"the_traitor", 8, Arc.V}, {"the_lower_moon", 9, Arc.VI},
                {"mugen_train", 10, Arc.VII}, {"akaza", 11, Arc.VII}, {"entertainment_district", 12, Arc.VIII},
                {"daki_and_gyutaro", 13, Arc.VIII}, {"swordsmith_village", 14, Arc.IX}, {"hashira_training", 15, Arc.X},
                {"infinity_castle_summoning", 16, Arc.X}, {"shinobu_vs_doma", 17, Arc.XI}, {"zenitsu_vs_kaigaku", 18, Arc.XI},
                {"tanjiro_giyu_vs_akaza", 19, Arc.XI}, {"kanao_inosuke_vs_doma", 20, Arc.XI}, {"mitsuri_obanai_vs_nakime", 21, Arc.XI},
                {"slayers_vs_kokushibo", 22, Arc.XI}, {"slayers_vs_muzan", 23, Arc.XII}, {"slayers_vs_demon_tanjiro", 24, Arc.XII}
        };
        for (Object[] p : planned) {
            register(new PlannedMission((String) p[0], (Integer) p[1], (Arc) p[2]));
        }
    }

    private MissionRegistry() {
    }

    private static void register(StoryMission mission) {
        MISSIONS.put(mission.id(), mission);
    }

    @Nullable
    public static StoryMission get(ResourceLocation id) {
        return MISSIONS.get(id);
    }

    /** All missions ordered by number. */
    public static List<StoryMission> getAllMissions() {
        List<StoryMission> list = new ArrayList<>(MISSIONS.values());
        list.sort(Comparator.comparingInt(StoryMission::number));
        return list;
    }
}

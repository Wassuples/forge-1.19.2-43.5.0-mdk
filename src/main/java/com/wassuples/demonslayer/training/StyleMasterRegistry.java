package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.reputation.HardlineFlag;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static registry of every style master defined in the game design doc.
 *
 * <p>Corps-Rep thresholds (from REPUTATION_SYSTEM.md):
 * <ul>
 *   <li>Recognized  =   1+</li>
 *   <li>Trusted     = 200+</li>
 *   <li>Honored     = 400+</li>
 *   <li>Revered     = 600+</li>
 * </ul>
 *
 * <p>Tanjiro-Rep thresholds:
 * <ul>
 *   <li>Acquaintance =  100+</li>
 *   <li>Comrade      =  250+</li>
 * </ul>
 */
public final class StyleMasterRegistry {

    private static final List<StyleMasterRecord> ALL;

    static {
        List<StyleMasterRecord> list = new ArrayList<>();

        // -- WATER ------------------------------------------------------------

        list.add(new StyleMasterRecord(
                "giyu_tomioka",
                BreathingStyles.WATER,
                "Giyu Tomioka",
                true,
                "River District - 2-3 fixed rotating locations",
                StyleMasterAccess.builder()
                        .minRank(Rank.TSUCHINOTO)
                        .minCorpsRep(1)
                        .build(),
                Arrays.asList(
                        BreathingForms.WATER_9,
                        BreathingForms.WATER_10,
                        BreathingForms.WATER_11)
        ));

        list.add(new StyleMasterRecord(
                "sakonji_urokodaki",
                BreathingStyles.WATER,
                "Sakonji Urokodaki",
                false,
                "Mt. Sagiri - fixed location",
                StyleMasterAccess.builder()
                        .minCorpsRep(0)
                        .minTanjiroRep(0)
                        .build(),
                Arrays.asList(
                        BreathingForms.WATER_1,
                        BreathingForms.WATER_2,
                        BreathingForms.WATER_3,
                        BreathingForms.WATER_4,
                        BreathingForms.WATER_5,
                        BreathingForms.WATER_6,
                        BreathingForms.WATER_7,
                        BreathingForms.WATER_8,
                        BreathingForms.WATER_9,
                        BreathingForms.WATER_10)
        ));

        // -- FLAME ------------------------------------------------------------

        list.add(new StyleMasterRecord(
                "rengoku_kyojuro",
                BreathingStyles.FLAME,
                "Rengoku Kyojuro",
                true,
                "Flame Hashira Estate - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KINOE)
                        .minCorpsRep(600)
                        .build(),
                Arrays.asList(
                        BreathingForms.FLAME_1,
                        BreathingForms.FLAME_2,
                        BreathingForms.FLAME_3,
                        BreathingForms.FLAME_4,
                        BreathingForms.FLAME_5,
                        BreathingForms.FLAME_9)
        ));

        list.add(new StyleMasterRecord(
                "shinjuro_rengoku",
                BreathingStyles.FLAME,
                "Shinjuro Rengoku",
                false,
                "Flame Hashira Estate - inner room",
                StyleMasterAccess.builder()
                        .minCorpsRep(1)
                        .requireFlag(HardlineFlag.MET_RENGOKU)
                        .build(),
                // Shinjuro abandoned the style; he can accelerate fewer forms
                Arrays.asList(
                        BreathingForms.FLAME_1,
                        BreathingForms.FLAME_2,
                        BreathingForms.FLAME_3)
        ));

        // -- THUNDER ----------------------------------------------------------

        list.add(new StyleMasterRecord(
                "jigoro_kuwajima",
                BreathingStyles.THUNDER,
                "Jigoro Kuwajima",
                false,
                "Remote mountain village - fixed location, long travel",
                StyleMasterAccess.builder()
                        .minRank(Rank.KANOTO)
                        // no Corps rep requirement; travel distance is the primary gate
                        .build(),
                Arrays.asList(
                        BreathingForms.THUNDER_1,
                        BreathingForms.THUNDER_2,
                        BreathingForms.THUNDER_3,
                        BreathingForms.THUNDER_4,
                        BreathingForms.THUNDER_5,
                        BreathingForms.THUNDER_6)
        ));

        // -- MIST -------------------------------------------------------------

        list.add(new StyleMasterRecord(
                "muichiro_tokito",
                BreathingStyles.MIST,
                "Muichiro Tokito",
                true,
                "Swordsmith Village (post-Mission 13) or Butterfly Estate",
                StyleMasterAccess.builder()
                        .minRank(Rank.TSUCHINOE)
                        .minCorpsRep(400)
                        .build(),
                Arrays.asList(
                        BreathingForms.MIST_5,
                        BreathingForms.MIST_6,
                        BreathingForms.MIST_7)
        ));

        // -- WIND -------------------------------------------------------------
        // FIX: requireKizukiKill() replaces requireFlag(KILLED_LOWER_MOON).
        // The rep flag was never being set on kill; the progression-based check
        // (hasKilledKizuki()) is the one that IS properly set in ModEvents.

        list.add(new StyleMasterRecord(
                "sanemi_shinazugawa",
                BreathingStyles.WIND,
                "Sanemi Shinazugawa",
                true,
                "Wind Hashira dojo - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.TSUCHINOE)
                        .minCorpsRep(400)
                        .requireKizukiKill()
                        .blockFlag(HardlineFlag.PROTECTED_NEZUKO)
                        .build(),
                Arrays.asList(
                        BreathingForms.WIND_7,
                        BreathingForms.WIND_8,
                        BreathingForms.WIND_9)
        ));

        // -- STONE ------------------------------------------------------------
        // FIX: removed minTanjiroRep(250). Gyomei does not check Tanjiro affinity.

        list.add(new StyleMasterRecord(
                "gyomei_himejima",
                BreathingStyles.STONE,
                "Gyomei Himejima",
                true,
                "Stone Hashira temple - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KINOE)
                        .minCorpsRep(600)
                        .build(),
                Arrays.asList(
                        BreathingForms.STONE_4,
                        BreathingForms.STONE_5)
        ));

        // -- INSECT -----------------------------------------------------------

        list.add(new StyleMasterRecord(
                "shinobu_kocho",
                BreathingStyles.INSECT,
                "Shinobu Kocho",
                true,
                "Butterfly Estate - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KANOE)
                        .minCorpsRep(200)
                        .maxMuzanRep(649) // refuses if Muzan Favor >= Favored (650)
                        .build(),
                Arrays.asList(
                        BreathingForms.INSECT_2,
                        BreathingForms.INSECT_3,
                        BreathingForms.INSECT_4)
        ));

        // -- LOVE -------------------------------------------------------------

        list.add(new StyleMasterRecord(
                "mitsuri_kanroji",
                BreathingStyles.LOVE,
                "Mitsuri Kanroji",
                true,
                "Love Hashira garden estate - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KANOTO)
                        .minCorpsRep(1)
                        .build(),
                Arrays.asList(
                        BreathingForms.LOVE_3,
                        BreathingForms.LOVE_5,
                        BreathingForms.LOVE_6)
        ));

        // -- SERPENT ----------------------------------------------------------

        list.add(new StyleMasterRecord(
                "obanai_iguro",
                BreathingStyles.SERPENT,
                "Obanai Iguro",
                true,
                "Serpent Hashira compound - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KINOTO)
                        .minCorpsRep(400)
                        .maxSuspicion(200)
                        .build(),
                Arrays.asList(
                        BreathingForms.SERPENT_3,
                        BreathingForms.SERPENT_4,
                        BreathingForms.SERPENT_5)
        ));

        // -- SOUND ------------------------------------------------------------

        list.add(new StyleMasterRecord(
                "tengen_uzui_active",
                BreathingStyles.SOUND,
                "Tengen Uzui",
                true,
                "Sound Hashira Estate - fixed location",
                StyleMasterAccess.builder()
                        .minRank(Rank.KANOE)
                        .minCorpsRep(200)
                        .build(),
                Arrays.asList(
                        BreathingForms.SOUND_4,
                        BreathingForms.SOUND_5)
        ));

        list.add(new StyleMasterRecord(
                "tengen_uzui_retired",
                BreathingStyles.SOUND,
                "Tengen Uzui (Retired)",
                false,
                "Entertainment District safe house - post-Mission 11",
                StyleMasterAccess.builder()
                        .minCorpsRep(1)
                        .build(),
                Collections.singletonList(BreathingForms.SOUND_1)
        ));

        ALL = Collections.unmodifiableList(list);
    }

    private StyleMasterRegistry() {}

    /** All registered masters, in definition order. */
    public static List<StyleMasterRecord> all() { return ALL; }

    /** Returns the master record with the given ID, or empty. */
    public static Optional<StyleMasterRecord> byId(String masterId) {
        return ALL.stream().filter(m -> m.masterId().equals(masterId)).findFirst();
    }

    /** Returns all masters for the given style. */
    public static List<StyleMasterRecord> forStyle(BreathingStyles style) {
        return ALL.stream().filter(m -> m.style() == style).collect(Collectors.toList());
    }
}

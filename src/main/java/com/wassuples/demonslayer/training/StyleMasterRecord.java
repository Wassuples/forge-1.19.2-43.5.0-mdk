package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.ranking.Rank;
import com.wassuples.demonslayer.reputation.HardlineFlag;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Immutable data record describing a single style master NPC (Hashira or non-Hashira).
 *
 * <p>Fields map directly to the Style Master Table from the design doc.
 *
 * @param masterId            Unique stable string key (e.g. {@code "giyu_tomioka"})
 * @param style               The {@link BreathingStyles} this master teaches
 * @param masterName          Display name shown in dialogue
 * @param isHashira           Whether this master is an active Hashira
 * @param locationDescription Lore description of where this master can be found
 * @param access              {@link StyleMasterAccess} conditions that must be met
 * @param acceleratedForms    Forms whose mastery-gain from training is doubled by this master.
 *                            Empty = no acceleration benefit (master still unlocks the style).
 */
public record StyleMasterRecord(
        String masterId,
        BreathingStyles style,
        String masterName,
        boolean isHashira,
        String locationDescription,
        StyleMasterAccess access,
        List<BreathingForms> acceleratedForms
) {
    public StyleMasterRecord {
        acceleratedForms = Collections.unmodifiableList(acceleratedForms);
    }
}

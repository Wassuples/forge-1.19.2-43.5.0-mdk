package com.wassuples.demonslayer.training;

import com.wassuples.demonslayer.styles.BreathingStyles;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Volatile (non-persisted) record of a player's active style-minigame session.
 *
 * <p>Stored in {@link StyleMinigameServerManager#activeSessions} while the player's minigame
 * screen is open on the client. Popped when the result packet arrives.
 *
 * @param playerId      UUID of the student player
 * @param style         Style being taught / trained
 * @param isTraining    {@code false} = initial unlock; {@code true} = mastery training
 * @param teacherUUID   UUID of the teaching player, or {@code null} if taught by an NPC
 */
public record StyleMinigameSession(
        UUID playerId,
        BreathingStyles style,
        boolean isTraining,
        @Nullable UUID teacherUUID
) {}

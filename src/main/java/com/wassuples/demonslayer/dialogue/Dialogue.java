package com.wassuples.demonslayer.dialogue;

import org.jetbrains.annotations.Nullable;

/** A conversation script: produces its first node for a session (null = nothing to say). */
@FunctionalInterface
public interface Dialogue {
    @Nullable
    DialogueNode start(DialogueSession session);
}

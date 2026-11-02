package com.wassuples.demonslayer.dialogue;

import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import net.minecraft.server.level.ServerPlayer;

/** A live conversation between a player and an NPC (server-side). */
public final class DialogueSession {
    public final ServerPlayer player;
    public final StoryNpcEntity npc;
    final int token;
    DialogueNode node;
    /** Conversations opened by the story (not by right-clicking) may be answered from further away. */
    final boolean remote;

    DialogueSession(ServerPlayer player, StoryNpcEntity npc, int token, boolean remote) {
        this.player = player;
        this.npc = npc;
        this.token = token;
        this.remote = remote;
    }
}

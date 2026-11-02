package com.wassuples.demonslayer.dialogue;

import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Which conversation an NPC opens when a player talks to them. */
public final class NpcDialogues {
    private NpcDialogues() {
    }

    @Nullable
    public static Dialogue forNpc(ServerPlayer player, StoryNpcEntity npc) {
        StoryCharacter c = npc.character();
        if (c.isTwin()) return FinalSelectionMission.twinsDialogue();
        if (c == StoryCharacter.SWORDSMITH) {
            return s -> DialogueNode.of(null, net.minecraft.network.chat.Component.translatable("npc.demonslayer.swordsmith.talk"))
                    .end(net.minecraft.network.chat.Component.translatable("npc.demonslayer.ask.bye"));
        }
        return FinalSelectionMission.candidateDialogue(c);
    }
}

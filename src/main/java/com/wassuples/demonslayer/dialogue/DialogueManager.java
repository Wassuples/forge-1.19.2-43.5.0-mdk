package com.wassuples.demonslayer.dialogue;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import com.wassuples.demonslayer.entity.custom.story.StoryNpcEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.OpenDialogueS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of NPC conversations. The server owns the script: the client only ever sees the current page and
 * answers by index, and every answer is checked against the open session (token, distance, NPC still alive).
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueManager {
    private static final Map<UUID, DialogueSession> SESSIONS = new HashMap<>();
    private static final double MAX_DIST = 10.0D, MAX_DIST_REMOTE = 40.0D;
    private static int nextToken = 1;

    private DialogueManager() {
    }

    /** Right-click: opens whatever this NPC has to say to this player right now. */
    public static void open(ServerPlayer player, StoryNpcEntity npc) {
        Dialogue dialogue = NpcDialogues.forNpc(player, npc);
        if (dialogue != null) start(player, npc, dialogue, false);
    }

    /** Opens a conversation the story starts (e.g. Tanjiro running up to offer his help). */
    public static void openRemote(ServerPlayer player, StoryNpcEntity npc, Dialogue dialogue) {
        start(player, npc, dialogue, true);
    }

    private static void start(ServerPlayer player, StoryNpcEntity npc, Dialogue dialogue, boolean remote) {
        close(player, false);
        DialogueSession session = new DialogueSession(player, npc, nextToken++, remote);
        DialogueNode first = dialogue.start(session);
        if (first == null) return;
        SESSIONS.put(player.getUUID(), session);
        show(session, first);
    }

    public static boolean isTalking(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    private static void show(DialogueSession session, DialogueNode node) {
        session.node = node;
        StoryNpcEntity speakerEntity = speakerEntity(session, node.speaker);
        StoryCharacter speaker = node.speaker != null ? node.speaker : session.npc.character();
        speakerEntity.beginTalking(session.player, 20 * 60);
        if (speakerEntity != session.npc) session.npc.beginTalking(session.player, 20 * 60);
        List<Component> labels = new ArrayList<>();
        List<Boolean> primary = new ArrayList<>();
        if (node.options.isEmpty()) {
            labels.add(Component.translatable("dialogue.demonslayer.farewell"));
            primary.add(false);
        } else {
            for (DialogueNode.Option o : node.options) {
                labels.add(o.label());
                primary.add(o.primary());
            }
        }
        ModMessages.sendToPlayer(new OpenDialogueS2CPacket(session.token, speakerEntity.getId(),
                speakerEntity.character() == speaker ? speakerEntity.storyName() : speaker.displayName(), speaker.title(),
                speaker.color(), node.text, labels, primary, node.picker == null ? "" : node.picker), session.player);
    }

    /** The NPC who says this node: the one spoken to, or a named character standing close by (the other twin). */
    private static StoryNpcEntity speakerEntity(DialogueSession session, @Nullable StoryCharacter speaker) {
        if (speaker == null || session.npc.character() == speaker) return session.npc;
        List<StoryNpcEntity> near = session.npc.level.getEntitiesOfClass(StoryNpcEntity.class, session.npc.getBoundingBox().inflate(10.0D),
                e -> e.isAlive() && e.character() == speaker);
        return near.isEmpty() ? session.npc : near.get(0);
    }

    /** The client picked answer {@code index} ({@code -1}: closed the screen). */
    public static void choose(ServerPlayer player, int token, int index) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.token != token) return;
        if (index < 0) {
            close(player, false);
            return;
        }
        double max = session.remote ? MAX_DIST_REMOTE : MAX_DIST;
        if (!session.npc.isAlive() || session.npc.level != player.level || session.npc.distanceToSqr(player) > max * max || !player.isAlive()) {
            close(player, true);
            return;
        }
        DialogueNode node = session.node;
        if (node.options.isEmpty()) {
            close(player, true);
            return;
        }
        if (index >= node.options.size()) return;
        DialogueNode next = node.options.get(index).next().apply(session);
        if (SESSIONS.get(player.getUUID()) != session) return;   // the answer opened another conversation
        if (next == null) close(player, true);
        else show(session, next);
    }

    /** Ends a conversation the player started with {@code npc} (not one the story opened, like Tanjiro's offer). */
    public static void closeChatWith(ServerPlayer player, StoryNpcEntity npc) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.npc == npc && !session.remote) close(player, true);
    }

    /** Ends the player's conversation if it is with {@code npc} (e.g. an offer that has since been settled). */
    public static void closeIfTalkingTo(ServerPlayer player, StoryNpcEntity npc) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.npc == npc) close(player, true);
    }

    /** Ends the player's conversation (and closes their screen when {@code tellClient}). */
    public static void close(ServerPlayer player, boolean tellClient) {
        DialogueSession session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        session.npc.endTalking(player);
        if (tellClient) ModMessages.sendToPlayer(OpenDialogueS2CPacket.close(session.token), player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DialogueSession session = SESSIONS.remove(event.getEntity().getUUID());
        if (session != null) session.npc.endTalking(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) close(player, true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) close(player, true);
    }
}

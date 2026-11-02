package com.wassuples.demonslayer.dialogue;

import com.wassuples.demonslayer.entity.custom.story.StoryCharacter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * One page of a conversation: who speaks, what they say, and the answers the player can give. An answer leads to the
 * next node (or ends the conversation when its function returns null). A node with no answers gets a single
 * "continue/farewell" answer that ends the conversation.
 */
public final class DialogueNode {
    @Nullable
    final StoryCharacter speaker;
    final Component text;
    final List<Option> options = new ArrayList<>();
    /** Non-null: the answers are picked on a special screen (e.g. "ore": the ores laid out in 3D). */
    @Nullable
    String picker;

    private DialogueNode(@Nullable StoryCharacter speaker, Component text) {
        this.speaker = speaker;
        this.text = text;
    }

    /** {@code speaker} null = the NPC that was spoken to. */
    public static DialogueNode of(@Nullable StoryCharacter speaker, Component text) {
        return new DialogueNode(speaker, text);
    }

    /** Show this node's answers on a special picker screen instead of as buttons. */
    public DialogueNode picker(String picker) {
        this.picker = picker;
        return this;
    }

    public DialogueNode option(Component label, Function<DialogueSession, DialogueNode> next) {
        options.add(new Option(label, next, false));
        return this;
    }

    /** An answer drawn highlighted (the story's main path: accepting a trial, choosing an ore...). */
    public DialogueNode primary(Component label, Function<DialogueSession, DialogueNode> next) {
        options.add(new Option(label, next, true));
        return this;
    }

    /** Leads straight to {@code next}. */
    public DialogueNode then(Component label, DialogueNode next) {
        return option(label, s -> next);
    }

    /** Ends the conversation. */
    public DialogueNode end(Component label) {
        return option(label, s -> null);
    }

    record Option(Component label, Function<DialogueSession, DialogueNode> next, boolean primary) {
    }
}

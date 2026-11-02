package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One row of the crow's mission list: everything the client shows about a mission, computed on the server.
 *
 * @param briefing objective outline for the details pane
 * @param note     the status explanation (why it's locked, how it ended, ...)
 */
public record MissionView(ResourceLocation id, int number, Arc arc, Component title, Component location, Component summary,
                          Status status, Component note, List<Component> briefing) {

    public enum Status {
        /** Prerequisites not met, or not yet implemented. */
        LOCKED,
        AVAILABLE,
        ACTIVE,
        COMPLETED
    }

    public static MissionView decode(FriendlyByteBuf buf) {
        return new MissionView(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Arc.class), buf.readComponent(),
                buf.readComponent(), buf.readComponent(), buf.readEnum(Status.class), buf.readComponent(),
                buf.readList(FriendlyByteBuf::readComponent));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeVarInt(number);
        buf.writeEnum(arc);
        buf.writeComponent(title);
        buf.writeComponent(location);
        buf.writeComponent(summary);
        buf.writeEnum(status);
        buf.writeComponent(note);
        buf.writeCollection(briefing, FriendlyByteBuf::writeComponent);
    }
}

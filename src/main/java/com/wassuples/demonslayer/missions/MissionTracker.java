package com.wassuples.demonslayer.missions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * What the HUD tracker shows for the active mission. Built on the server by the mission script each second and sent
 * to the owning client, so the client never needs mission logic.
 *
 * @param footer a status line under the objectives (time to dawn, direction to the mountain, a warning...)
 * @param urgent draws the footer in the warning colour
 */
public record MissionTracker(ResourceLocation id, Component arc, Component title, List<Line> lines, Component footer, boolean urgent) {

    /** One objective line. {@code optional} lines are drawn dimmer and marked as such. */
    public record Line(Component text, boolean done, boolean optional) {
    }

    public static MissionTracker decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Component arc = buf.readComponent();
        Component title = buf.readComponent();
        int n = buf.readVarInt();
        List<Line> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lines.add(new Line(buf.readComponent(), buf.readBoolean(), buf.readBoolean()));
        }
        return new MissionTracker(id, arc, title, lines, buf.readComponent(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeComponent(arc);
        buf.writeComponent(title);
        buf.writeVarInt(lines.size());
        for (Line l : lines) {
            buf.writeComponent(l.text());
            buf.writeBoolean(l.done());
            buf.writeBoolean(l.optional());
        }
        buf.writeComponent(footer);
        buf.writeBoolean(urgent);
    }
}

package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.events.StyleTeachingEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: reports the outcome of a style minigame session.
 *
 * <p>Payload:
 * <ul>
 *   <li>{@code sessionId} – must match an active server session for the sending player</li>
 *   <li>{@code passed}    – {@code true} = player completed the minigame successfully</li>
 * </ul>
 *
 * <p>The server-side handler in {@link StyleTeachingEvents} validates the session, applies the
 * progression reward (style unlock or mastery gain) on pass, or records the failure consequence.
 */
public class StyleMinigameResultC2SPacket {

    private final UUID sessionId;
    private final boolean passed;

    public StyleMinigameResultC2SPacket(UUID sessionId, boolean passed) {
        this.sessionId = sessionId;
        this.passed    = passed;
    }

    public StyleMinigameResultC2SPacket(FriendlyByteBuf buf) {
        this.sessionId = buf.readUUID();
        this.passed    = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeBoolean(passed);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            StyleTeachingEvents.handleMinigameResult(player, sessionId, passed);
        });
        return true;
    }
}

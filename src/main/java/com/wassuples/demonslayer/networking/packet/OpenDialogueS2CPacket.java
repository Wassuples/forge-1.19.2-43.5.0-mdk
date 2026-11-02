package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.dialogue.ClientDialogueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shows one page of an NPC conversation (or, with {@link #isClose()}, closes the conversation screen).
 */
public class OpenDialogueS2CPacket {
    public final int token;
    /** Entity drawn as the portrait; -1 closes the screen. */
    public final int entityId;
    public final Component speaker, epithet, text;
    public final int color;
    public final List<Component> options;
    public final List<Boolean> primary;
    /** Empty for a normal page; otherwise the special picker screen to show (e.g. "ore"). */
    public final String picker;

    public OpenDialogueS2CPacket(int token, int entityId, Component speaker, Component epithet, int color, Component text,
                                 List<Component> options, List<Boolean> primary, String picker) {
        this.token = token;
        this.entityId = entityId;
        this.speaker = speaker;
        this.epithet = epithet;
        this.color = color;
        this.text = text;
        this.options = options;
        this.primary = primary;
        this.picker = picker;
    }

    public static OpenDialogueS2CPacket close(int token) {
        return new OpenDialogueS2CPacket(token, -1, Component.empty(), Component.empty(), 0, Component.empty(), List.of(), List.of(), "");
    }

    public boolean isClose() {
        return entityId < 0;
    }

    public OpenDialogueS2CPacket(FriendlyByteBuf buf) {
        this.token = buf.readVarInt();
        this.entityId = buf.readVarInt();
        this.speaker = buf.readComponent();
        this.epithet = buf.readComponent();
        this.color = buf.readInt();
        this.text = buf.readComponent();
        int n = Math.min(buf.readVarInt(), 9);
        List<Component> opts = new ArrayList<>();
        List<Boolean> prim = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            opts.add(buf.readComponent());
            prim.add(buf.readBoolean());
        }
        this.options = opts;
        this.primary = prim;
        this.picker = buf.readUtf(16);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(token);
        buf.writeVarInt(entityId);
        buf.writeComponent(speaker);
        buf.writeComponent(epithet);
        buf.writeInt(color);
        buf.writeComponent(text);
        int n = Math.min(options.size(), 9);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeComponent(options.get(i));
            buf.writeBoolean(i < primary.size() && primary.get(i));
        }
        buf.writeUtf(picker, 16);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDialogueHandler.handle(this)));
        context.setPacketHandled(true);
        return true;
    }
}

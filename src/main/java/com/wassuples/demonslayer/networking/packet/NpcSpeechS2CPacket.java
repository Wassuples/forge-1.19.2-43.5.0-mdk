package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.dialogue.ClientDialogueHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** An NPC says something out loud: shown as a speech bubble above them to everyone nearby. */
public class NpcSpeechS2CPacket {
    public final int entityId;
    public final Component text;
    public final int ticks;

    public NpcSpeechS2CPacket(int entityId, Component text, int ticks) {
        this.entityId = entityId;
        this.text = text;
        this.ticks = ticks;
    }

    public NpcSpeechS2CPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readComponent(), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeComponent(text);
        buf.writeVarInt(ticks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDialogueHandler.speech(this)));
        context.setPacketHandled(true);
        return true;
    }
}

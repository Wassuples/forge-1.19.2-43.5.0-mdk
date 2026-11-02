package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player's answer in a conversation ({@code index} -1: they closed the screen). Validated by DialogueManager. */
public class DialogueChoiceC2SPacket {
    private final int token, index;

    public DialogueChoiceC2SPacket(int token, int index) {
        this.token = token;
        this.index = index;
    }

    public DialogueChoiceC2SPacket(FriendlyByteBuf buf) {
        this.token = buf.readVarInt();
        this.index = buf.readVarInt() - 1;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(token);
        buf.writeVarInt(index + 1);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) DialogueManager.choose(player, token, index);
        });
        context.setPacketHandled(true);
        return true;
    }
}

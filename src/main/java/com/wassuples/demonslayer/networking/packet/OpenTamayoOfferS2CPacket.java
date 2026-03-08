package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.TamayoConsentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenTamayoOfferS2CPacket {
    private final int tamayoEntityId;
    private final List<Component> pages;

    public OpenTamayoOfferS2CPacket(int tamayoEntityId, List<Component> pages) {
        this.tamayoEntityId = tamayoEntityId;
        this.pages = pages;
    }

    public OpenTamayoOfferS2CPacket(FriendlyByteBuf buf) {
        this.tamayoEntityId = buf.readInt();
        int size = buf.readVarInt();
        List<Component> tmp = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tmp.add(buf.readComponent());
        }
        this.pages = tmp;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.tamayoEntityId);
        buf.writeVarInt(this.pages.size());
        for (Component c : this.pages) {
            buf.writeComponent(c);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Guard against spam: if already on this screen, don't reopen.
            if (mc.screen instanceof TamayoConsentScreen) return;

            mc.setScreen(new TamayoConsentScreen(this.tamayoEntityId, this.pages));
        });
        return true;
    }
}


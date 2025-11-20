package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.nemesis.DemonHierarchyManager;
import com.wassuples.demonslayer.networking.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestHierarchyC2SPacket {

    public RequestHierarchyC2SPacket() { }

    public RequestHierarchyC2SPacket(FriendlyByteBuf buf) { }

    public void toBytes(FriendlyByteBuf buf) { }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.getLevel();
            DemonHierarchyManager manager = DemonHierarchyManager.get(level);

            DemonHierarchyManager.DemonDisplayDataLists displayData = manager.getDemonDisplayData();
            ModMessages.sendToPlayer(new UpdateHierarchyS2CPacket(displayData.kizuki(), displayData.fodder()), player);
        });
        return true;
    }
}
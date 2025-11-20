package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.RankingBoardScreen;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class UpdateHierarchyS2CPacket {
    private final List<DemonDisplayData> kizuki;
    private final List<DemonDisplayData> fodder;

    public UpdateHierarchyS2CPacket(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder) {
        this.kizuki = kizuki;
        this.fodder = fodder;
    }

    public UpdateHierarchyS2CPacket(FriendlyByteBuf buf) {
        this.kizuki = buf.readList(DemonDisplayData::decode);
        this.fodder = buf.readList(DemonDisplayData::decode);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(kizuki, (b, data) -> data.encode(b));
        buf.writeCollection(fodder, (b, data) -> data.encode(b));
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE LOGIC
            Minecraft.getInstance().setScreen(new RankingBoardScreen(this.kizuki, this.fodder));
        });
        return true;
    }
}
package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.gui.CrowDispatchScreen;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class OpenCrowDispatchS2CPacket {
    private final List<DemonDisplayData> kizuki;
    private final List<DemonDisplayData> fodder;
    private final List<Mission> activeMissions;
    private final List<Mission> availableMissions;
    private final List<Mission> completedMissions;

    public OpenCrowDispatchS2CPacket(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder, List<Mission> active, List<Mission> available, List<Mission> completed) {
        this.kizuki = kizuki;
        this.fodder = fodder;
        this.activeMissions = active;
        this.availableMissions = available;
        this.completedMissions = completed;
    }

    public OpenCrowDispatchS2CPacket(FriendlyByteBuf buf) {
        this.kizuki = buf.readList(DemonDisplayData::decode);
        this.fodder = buf.readList(DemonDisplayData::decode);
        this.activeMissions = buf.readList(Mission::new);
        this.availableMissions = buf.readList(Mission::new);
        this.completedMissions = buf.readList(Mission::new);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(kizuki, (b, d) -> d.encode(b));
        buf.writeCollection(fodder, (b, d) -> d.encode(b));
        buf.writeCollection(activeMissions, (b, m) -> m.toBytes(b));
        buf.writeCollection(availableMissions, (b, m) -> m.toBytes(b));
        buf.writeCollection(completedMissions, (b, m) -> m.toBytes(b));
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // We are on the client thread here.
            Minecraft.getInstance().setScreen(new CrowDispatchScreen(kizuki, fodder, activeMissions, availableMissions, completedMissions));
        });
        return true;
    }
}


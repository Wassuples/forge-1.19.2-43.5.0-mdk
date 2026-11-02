package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.ClientMissionHandler;
import com.wassuples.demonslayer.missions.MissionView;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client packet: open the crow dispatch (Kizuki rankings + the story mission list).
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class OpenCrowDispatchS2CPacket {
    public final List<DemonDisplayData> kizuki;
    public final List<DemonDisplayData> fodder;
    public final List<MissionView> missions;

    public OpenCrowDispatchS2CPacket(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder, List<MissionView> missions) {
        this.kizuki = kizuki;
        this.fodder = fodder;
        this.missions = missions;
    }

    public OpenCrowDispatchS2CPacket(FriendlyByteBuf buf) {
        this.kizuki = buf.readList(DemonDisplayData::decode);
        this.fodder = buf.readList(DemonDisplayData::decode);
        this.missions = buf.readList(MissionView::decode);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(kizuki, (b, d) -> d.encode(b));
        buf.writeCollection(fodder, (b, d) -> d.encode(b));
        buf.writeCollection(missions, (b, m) -> m.encode(b));
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMissionHandler.openDispatch(this)));
        context.setPacketHandled(true);
        return true;
    }
}

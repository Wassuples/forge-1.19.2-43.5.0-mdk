package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.mission.MissionNoticeOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Story messages drawn by the mod's own HUD instead of chat / the vanilla title: a {@code BANNER} (large brush-stroke
 * title with a subtitle, e.g. "Night 3") or a {@code NOTICE} (a line in the scroll under the mission tracker).
 */
public class MissionNoticeS2CPacket {
    public static final int NOTICE = 0, BANNER = 1, WARNING = 2, REST = 3;
    public final int kind;
    public final Component title, subtitle;
    public final int color, ticks;

    public MissionNoticeS2CPacket(int kind, Component title, Component subtitle, int color, int ticks) {
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.color = color;
        this.ticks = ticks;
    }

    public MissionNoticeS2CPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readComponent(), buf.readComponent(), buf.readInt(), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeComponent(title);
        buf.writeComponent(subtitle);
        buf.writeInt(color);
        buf.writeVarInt(ticks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MissionNoticeOverlay.push(this)));
        context.setPacketHandled(true);
        return true;
    }
}

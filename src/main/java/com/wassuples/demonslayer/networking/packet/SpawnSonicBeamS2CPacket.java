package com.wassuples.demonslayer.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpawnSonicBeamS2CPacket {

    private final Vec3 startPos;
    private final Vec3 endPos;

    public SpawnSonicBeamS2CPacket(Vec3 start, Vec3 end) {
        this.startPos = start;
        this.endPos = end;
    }

    public SpawnSonicBeamS2CPacket(FriendlyByteBuf buf) {
        this.startPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.endPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(startPos.x);
        buf.writeDouble(startPos.y);
        buf.writeDouble(startPos.z);
        buf.writeDouble(endPos.x);
        buf.writeDouble(endPos.y);
        buf.writeDouble(endPos.z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handle(this.startPos, this.endPos));
        });
        return true;
    }

    private static class ClientPacketHandler {
        private static void handle(Vec3 start, Vec3 end) {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            level.addParticle(ParticleTypes.SONIC_BOOM, end.x, end.y, end.z, 0, 0, 0);

            double distance = start.distanceTo(end);
            int segments = (int)(distance * 2);

            for (int i = 0; i <= segments; i++) {
                double progress = (double) i / segments;
                Vec3 point = start.lerp(end, progress);
                level.addParticle(ParticleTypes.CLOUD, point.x, point.y, point.z, 0, 0, 0);
            }
        }
    }
}
package com.wassuples.demonslayer.networking.packet;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C Packet to render a stream of particles from a start to an end point.
 * Used for the visual effect of the Ten Thousand Slime-Fish swarm.
 */
public class SpawnParticleStreamS2CPacket {

    private final Vec3 startPos;
    private final Vec3 endPos;

    public SpawnParticleStreamS2CPacket(Vec3 start, Vec3 end) {
        this.startPos = start;
        this.endPos = end;
    }

    public SpawnParticleStreamS2CPacket(FriendlyByteBuf buf) {
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

            final DustParticleOptions fishBody = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 0.6f), 0.7f); // Sea-foam green

            double distance = start.distanceTo(end);
            int segments = (int)(distance * 4);

            for (int i = 0; i <= segments; i++) {
                double progress = (double) i / segments;
                Vec3 particlePos = start.lerp(end, progress);
                level.addParticle(fishBody, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
        }
    }
}
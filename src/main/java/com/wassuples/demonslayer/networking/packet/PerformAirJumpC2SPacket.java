package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.wassuples.demonslayer.util.VFXHelper.spawnParticleArc;

public class PerformAirJumpC2SPacket {

    public PerformAirJumpC2SPacket() {}
    public PerformAirJumpC2SPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Vec3 lookVec = player.getLookAngle();
            Vec3 jumpVec = new Vec3(lookVec.x * 0.8, 0.7, lookVec.z * 0.8);
            player.setDeltaMovement(player.getDeltaMovement().add(jumpVec));
            player.hurtMarked = true;

            player.level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnParticleArc(player, net.minecraft.core.particles.ParticleTypes.SPLASH, 1.5, 360, 30);
        });
        return true;
    }
}
package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.entity.custom.BodhisattvaEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BodhisattvaInputC2SPacket {
    private final Vec3 movement;
    private final boolean primaryAttack;
    private final boolean secondaryAttack;

    public BodhisattvaInputC2SPacket(Vec3 movement, boolean primaryAttack, boolean secondaryAttack) {
        this.movement = movement;
        this.primaryAttack = primaryAttack;
        this.secondaryAttack = secondaryAttack;
    }

    public BodhisattvaInputC2SPacket(FriendlyByteBuf buf) {
        this.movement = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.primaryAttack = buf.readBoolean();
        this.secondaryAttack = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(this.movement.x);
        buf.writeDouble(this.movement.y);
        buf.writeDouble(this.movement.z);
        buf.writeBoolean(this.primaryAttack);
        buf.writeBoolean(this.secondaryAttack);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.getVehicle() instanceof BodhisattvaEntity bodhisattva)) {
                return;
            }

            // Apply inputs to the Bodhisattva entity
            bodhisattva.moveWithInput(this.movement);
            if (this.primaryAttack) {
                bodhisattva.performBreathAttack();
            }
            if (this.secondaryAttack) {
                bodhisattva.performSwipeAttack();
            }
        });
        return true;
    }
}
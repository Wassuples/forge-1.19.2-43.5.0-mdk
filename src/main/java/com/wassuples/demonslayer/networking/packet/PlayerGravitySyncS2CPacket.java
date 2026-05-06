package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client packet: synchronizes an entity's gravity orientation state.
 */
public class PlayerGravitySyncS2CPacket {
    private final int entityId;
    private final GravityDirection gravity;
    private final GravityDirection previousGravity;
    private final float transitionProgress;

    public PlayerGravitySyncS2CPacket(int entityId, GravityDirection gravity, GravityDirection previousGravity, float transitionProgress) {
        this.entityId = entityId;
        this.gravity = gravity;
        this.previousGravity = previousGravity;
        this.transitionProgress = transitionProgress;
    }

    public PlayerGravitySyncS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.gravity = GravityDirection.valueOf(buf.readUtf());
        this.previousGravity = GravityDirection.valueOf(buf.readUtf());
        this.transitionProgress = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.gravity.name());
        buf.writeUtf(this.previousGravity.name());
        buf.writeFloat(this.transitionProgress);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            Entity entity = mc.level.getEntity(this.entityId);
            if (entity == null) {
                return;
            }

            entity.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(cap -> {
                GravityDirection currentGravity = cap.getGravity();
                if (entity == mc.player && currentGravity != this.gravity && this.transitionProgress < 1.0F) {
                    GravityUtil.reanchorEntityForGravityChange(entity, currentGravity, this.gravity);
                    entity.xo = entity.getX();
                    entity.yo = entity.getY();
                    entity.zo = entity.getZ();
                }

                cap.applySyncedState(this.gravity, this.previousGravity, this.transitionProgress);
                GravityUtil.refreshGravityHitbox(entity, this.gravity);
            });
        });
        return true;
    }
}

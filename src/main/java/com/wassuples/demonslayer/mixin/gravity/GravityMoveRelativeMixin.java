package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class GravityMoveRelativeMixin {

    @Inject(method = "moveRelative(FLnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void demonslayer$remapMoveRelative(float speed, Vec3 relative, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir == GravityDirection.DOWN) return;

            Vec3 normalized = relative.lengthSqr() > 1.0 ? relative.normalize() : relative;

            float yawToUse = gravity.getGravityYaw();

            Vec3 worldDelta = GravityUtil.getMovementVector(
                self.getLookAngle(),
                yawToUse,
                normalized.scale(speed),
                dir
            );
            self.setDeltaMovement(self.getDeltaMovement().add(worldDelta));
            ci.cancel();
        });
    }
}
